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
import org.apache.logging.log4j.ThreadContext;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import junit.framework.TestResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

class Junit3ResultBridgeTest {

    @Test
    void translatesPassFailAndErrorIntoSinkCalls() {
        RecordingSink sink = new RecordingSink();
        TestResult result = new TestResult();
        result.addListener(new Junit3ResultBridge(sink));

        new PassingCase().run(result);
        new FailingCase().run(result);
        new ErroringCase().run(result);

        assertThat(sink.testStartedCalls, contains(
                PassingCase.class.getName() + "#testPass",
                FailingCase.class.getName() + "#testFail",
                ErroringCase.class.getName() + "#testError"));
        assertThat(sink.testFinishedCalls.get(0).outcome(), instanceOf(SuiteReportSink.Outcome.Passed.class));

        SuiteReportSink.Outcome.Failure failure = (SuiteReportSink.Outcome.Failure) sink.testFinishedCalls.get(1).outcome();
        assertThat(failure.message(), is("expected false"));
        assertThat(failure.type(), is("junit.framework.AssertionFailedError"));

        SuiteReportSink.Outcome.Error error = (SuiteReportSink.Outcome.Error) sink.testFinishedCalls.get(2).outcome();
        assertThat(error.throwable(), instanceOf(RuntimeException.class));
        assertThat(error.throwable().getMessage(), is("boom"));
    }

    @Test
    void dispatchesToEverySink() {
        RecordingSink sinkA = new RecordingSink();
        RecordingSink sinkB = new RecordingSink();
        TestResult result = new TestResult();
        result.addListener(new Junit3ResultBridge(sinkA, sinkB));

        new PassingCase().run(result);

        assertThat(sinkA.testFinishedCalls.size(), is(1));
        assertThat(sinkB.testFinishedCalls.size(), is(1));
    }

    @Test
    void reportsExactlyOneTestFinishedForATestWithMultipleAccumulatedFailures() {
        RecordingSink sink = new RecordingSink();
        TestResult result = new TestResult();
        result.addListener(new Junit3ResultBridge(sink));

        new MultiFailureCase().run(result);

        assertThat(sink.testFinishedCalls.size(), is(1));
        SuiteReportSink.Outcome.Failure failure = (SuiteReportSink.Outcome.Failure) sink.testFinishedCalls.get(0).outcome();
        assertThat(failure.message(), is("first failure"));
    }

    @Test
    void tagsLogContextWithTestCaseDuringExecutionAndClearsItAfter() {
        RecordingSink sink = new RecordingSink();
        TestResult result = new TestResult();
        result.addListener(new Junit3ResultBridge(sink));

        new MdcCapturingCase().run(result);

        assertThat(MdcCapturingCase.capturedTestCase(), is("MdcCapturingCase#testCaptureMdc"));
        assertThat(ThreadContext.get("testCase"), nullValue());
    }

    /**
     * Mirrors how ServiceTest/SimpleMethodTest/EntityXmlAssertTest override run(TestResult) directly -
     * calling result.startTest(this), then multiple result.addFailure(this, ...) calls for one logical
     * test, then result.endTest(this) - rather than going through JUnit 3's default reflection-based
     * single-assertion dispatch.
     */
    public static class MultiFailureCase extends TestCase {
        MultiFailureCase() {
            super("testMultiFailure");
        }

        @Override
        public void run(TestResult result) {
            result.startTest(this);
            result.addFailure(this, new AssertionFailedError("first failure"));
            result.addFailure(this, new AssertionFailedError("second failure"));
            result.addFailure(this, new AssertionFailedError("third failure"));
            result.endTest(this);
        }
    }

    public static class MdcCapturingCase extends TestCase {
        private static String capturedTestCase;

        MdcCapturingCase() {
            super("testCaptureMdc");
        }

        public void testCaptureMdc() {
            capturedTestCase = ThreadContext.get("testCase");
        }

        static String capturedTestCase() {
            return capturedTestCase;
        }
    }

    public static class PassingCase extends TestCase {
        PassingCase() {
            super("testPass");
        }

        public void testPass() {
        }
    }

    public static class FailingCase extends TestCase {
        FailingCase() {
            super("testFail");
        }

        public void testFail() {
            fail("expected false");
        }
    }

    public static class ErroringCase extends TestCase {
        ErroringCase() {
            super("testError");
        }

        public void testError() {
            throw new RuntimeException("boom");
        }
    }
}
