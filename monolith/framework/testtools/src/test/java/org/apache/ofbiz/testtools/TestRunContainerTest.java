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

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.ThreadContext;
import org.apache.ofbiz.base.container.ContainerException;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.service.LocalDispatcher;
import org.junit.jupiter.api.Test;

import junit.framework.TestCase;
import junit.framework.TestResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * Exercises TestRunContainer.runSuiteEntries() and reportSuiteExecutionFailure() directly, rather than
 * through start()'s for loop, which needs a full ofbiz --test container bootstrap (StartupCommand, a
 * real ModelTestSuite/Delegator, ...) to construct.
 */
class TestRunContainerTest {

    @Test
    void escapedExceptionIsReportedAsASuiteLevelError() {
        RecordingSink sink = new RecordingSink();
        RuntimeException thrown = new RuntimeException("boom");

        TestRunContainer.reportSuiteExecutionFailure("myFakeSuite", thrown, sink);

        assertThat(sink.testFinishedCalls, contains(new RecordingSink.FinishedCall(
                "myFakeSuite", "suiteExecutionError", SuiteReportSink.Outcome.error(thrown))));
    }

    @Test
    void syntheticFailureMarkerIsNamedAfterTheSuite() {
        RecordingSink sink = new RecordingSink();

        TestRunContainer.reportSuiteExecutionFailure("myFakeSuite", new RuntimeException("boom"), sink);

        assertThat(sink.testStartedCalls, contains("myFakeSuite#suiteExecutionError"));
    }

    @Test
    void reportSuiteExecutionFailureDispatchesToEverySink() {
        RecordingSink sinkA = new RecordingSink();
        RecordingSink sinkB = new RecordingSink();

        TestRunContainer.reportSuiteExecutionFailure("myFakeSuite", new RuntimeException("boom"), sinkA, sinkB);

        assertThat(sinkA.testFinishedCalls.size(), is(1));
        assertThat(sinkB.testFinishedCalls.size(), is(1));
    }

    @Test
    void runSuiteEntriesVisitsEntriesInDeclaredOrderAcrossBothEngines() {
        RecordingSink sink = new RecordingSink();
        List<SuiteEntry> entries = List.of(
                new SuiteEntry.Junit3Entry(new NamedCase("first")),
                new SuiteEntry.JupiterEntry(OneTestFixture.class),
                new SuiteEntry.Junit3Entry(new NamedCase("third")));

        TestRunContainer.runSuiteEntries(entries, mock(Delegator.class), mock(LocalDispatcher.class), sink);

        assertThat(sink.testStartedCalls, contains(
                NamedCase.class.getName() + "#first",
                OneTestFixture.class.getName() + "#onlyTest",
                NamedCase.class.getName() + "#third"));
    }

    @Test
    void runSuiteEntriesReportsAJunit3FailureThroughTheSameSink() {
        RecordingSink sink = new RecordingSink();
        List<SuiteEntry> entries = List.of(new SuiteEntry.Junit3Entry(new FailingCase()));

        TestRunContainer.runSuiteEntries(entries, mock(Delegator.class), mock(LocalDispatcher.class), sink);

        assertThat(sink.testFinishedCalls.get(0).outcome(), instanceOf(SuiteReportSink.Outcome.Failure.class));
    }

    @Test
    void runSuiteEntriesClearsTheTestCaseMdcFieldEvenWhenAJunit3EngineEscapesBeforeEndTest() {
        RecordingSink sink = new RecordingSink();
        List<SuiteEntry> entries = List.of(new SuiteEntry.Junit3Entry(new StartsThenThrowsBeforeEndTestCase()));

        assertThrows(RuntimeException.class, () ->
                TestRunContainer.runSuiteEntries(entries, mock(Delegator.class), mock(LocalDispatcher.class), sink));

        assertThat(ThreadContext.get(JupiterTestExtension.TEST_CASE_MDC_KEY), nullValue());
    }

    @Test
    void runSuiteEntriesWithAMethodNameOnlyRunsThatOneMethod() {
        TwoTestFixture.firstRunCount = 0;
        TwoTestFixture.secondRunCount = 0;
        RecordingSink sink = new RecordingSink();
        List<SuiteEntry> entries = List.of(new SuiteEntry.JupiterEntry(TwoTestFixture.class));

        TestRunContainer.runSuiteEntries(
                entries, mock(Delegator.class), mock(LocalDispatcher.class), Map.of(), "first", sink);

        assertThat(TwoTestFixture.firstRunCount, is(1));
        assertThat(TwoTestFixture.secondRunCount, is(0));
        assertThat(sink.testStartedCalls, contains(TwoTestFixture.class.getName() + "#first"));
    }

    @Test
    void runSuiteEntriesWithNoMethodNameStillRunsTheWholeClass() {
        TwoTestFixture.firstRunCount = 0;
        TwoTestFixture.secondRunCount = 0;
        RecordingSink sink = new RecordingSink();
        List<SuiteEntry> entries = List.of(new SuiteEntry.JupiterEntry(TwoTestFixture.class));

        TestRunContainer.runSuiteEntries(entries, mock(Delegator.class), mock(LocalDispatcher.class), sink);

        assertThat(TwoTestFixture.firstRunCount, is(1));
        assertThat(TwoTestFixture.secondRunCount, is(1));
    }

    @Test
    void normalizeMethodNameTreatsBlankAsAbsent() {
        assertThat(TestRunContainer.normalizeMethodName(""), nullValue());
        assertThat(TestRunContainer.normalizeMethodName("   "), nullValue());
    }

    @Test
    void normalizeMethodNamePassesThroughNonBlankValue() {
        assertThat(TestRunContainer.normalizeMethodName("testFindPartiesById"), is("testFindPartiesById"));
    }

    @Test
    void normalizeMethodNamePassesThroughNull() {
        assertThat(TestRunContainer.normalizeMethodName(null), nullValue());
    }

    @Test
    void validateMethodRequiresCaseThrowsWhenMethodGivenWithoutCase() {
        ContainerException thrown = assertThrows(ContainerException.class, () ->
                TestRunContainer.validateMethodRequiresCase("testFindPartiesById", null));

        assertThat(thrown.getMessage(), containsString("method=testFindPartiesById"));
        assertThat(thrown.getMessage(), containsString("case="));
    }

    @Test
    void validateMethodRequiresCaseAllowsMethodWithCase() {
        assertDoesNotThrow(() -> TestRunContainer.validateMethodRequiresCase("testFindPartiesById", "party-tests"));
    }

    @Test
    void validateMethodRequiresCaseAllowsNeitherGiven() {
        assertDoesNotThrow(() -> TestRunContainer.validateMethodRequiresCase(null, null));
    }

    @Test
    void validateMethodAppliesToSuiteThrowsWhenNoJupiterEntryResolved() {
        List<SuiteEntry> entries = List.of(new SuiteEntry.Junit3Entry(new NamedCase("dataLoad")));

        ContainerException thrown = assertThrows(ContainerException.class, () ->
                TestRunContainer.validateMethodAppliesToSuite("testFindPartiesById", "partytests", entries));

        assertThat(thrown.getMessage(), containsString("partytests"));
    }

    @Test
    void validateMethodAppliesToSuiteAllowsAResolvedJupiterEntry() {
        List<SuiteEntry> entries = List.of(
                new SuiteEntry.Junit3Entry(new NamedCase("dataLoad")),
                new SuiteEntry.JupiterEntry(OneTestFixture.class));

        assertDoesNotThrow(() -> TestRunContainer.validateMethodAppliesToSuite("onlyTest", "partytests", entries));
    }

    @Test
    void validateMethodAppliesToSuiteAllowsNullMethodName() {
        List<SuiteEntry> entries = List.of(new SuiteEntry.Junit3Entry(new NamedCase("dataLoad")));

        assertDoesNotThrow(() -> TestRunContainer.validateMethodAppliesToSuite(null, "partytests", entries));
    }

    static class NamedCase extends TestCase {
        NamedCase(String name) {
            super(name);
        }

        @Override
        protected void runTest() {
        }
    }

    static class FailingCase extends TestCase {
        FailingCase() {
            super("failing");
        }

        @Override
        protected void runTest() {
            fail("expected false");
        }
    }

    @org.junit.jupiter.api.Tag(JupiterTestExtension.INTEGRATION_TAG)
    static class OneTestFixture {
        @Test
        void onlyTest() {
        }
    }

    @org.junit.jupiter.api.Tag(JupiterTestExtension.INTEGRATION_TAG)
    static class TwoTestFixture {
        //ALLOW PUBLIC FIELDS
        static int firstRunCount;
        static int secondRunCount;
        //FORBID PUBLIC FIELDS

        @Test
        void first() {
            firstRunCount++;
        }

        @Test
        void second() {
            secondRunCount++;
        }
    }

    /**
     * Mirrors how ServiceTest/SimpleMethodTest/EntityXmlAssertTest override run(TestResult) directly -
     * calling result.startTest(this) (which arms the testCase MDC field via Junit3ResultBridge.startTest())
     * themselves, then running the test's own logic before reaching result.endTest(this). An unchecked
     * exception thrown from that logic - as opposed to the addFailure()/addError() calls those engines
     * normally make - propagates straight out of run(TestResult), skipping endTest() (and therefore
     * Junit3ResultBridge.endTest()'s ThreadContext.remove()) entirely.
     */
    static class StartsThenThrowsBeforeEndTestCase extends TestCase {
        StartsThenThrowsBeforeEndTestCase() {
            super("startsThenThrows");
        }

        @Override
        public void run(TestResult result) {
            result.startTest(this);
            throw new RuntimeException("escaped before endTest()");
        }
    }
}
