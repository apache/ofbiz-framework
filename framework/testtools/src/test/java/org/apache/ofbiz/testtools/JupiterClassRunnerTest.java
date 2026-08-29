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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.ThreadContext;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.service.LocalDispatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;

/**
 * Exercises JupiterTestExtension.JupiterClassRunner directly, the replacement for the old
 * JupiterTestSuite/JupiterLeafTest scenarios that used to live in JupiterInjectionGuardsTest.
 */
class JupiterClassRunnerTest {

    private static final String PARALLEL_ENABLED = "junit.jupiter.execution.parallel.enabled";
    private static final String PARALLEL_MODE_DEFAULT = "junit.jupiter.execution.parallel.mode.default";

    @AfterEach
    void clearThreadLocals() {
        JupiterTestExtension.CURRENT_DELEGATOR.remove();
        JupiterTestExtension.CURRENT_DISPATCHER.remove();
        JupiterTestExtension.CURRENT_TEST_PARAMS.remove();
        JupiterTestExtension.CURRENT_TEST_METHOD_NAME.remove();
    }

    @Test
    void parallelExecutionStaysDisabledEvenWhenSystemPropertiesRequestIt() {
        System.setProperty(PARALLEL_ENABLED, "true");
        System.setProperty(PARALLEL_MODE_DEFAULT, "concurrent");
        try {
            Thread callingThread = Thread.currentThread();
            ThreadRecordingFixture.EXECUTED_ON.clear();

            JupiterTestExtension.JupiterClassRunner runner = new JupiterTestExtension.JupiterClassRunner(
                    ThreadRecordingFixture.class, mock(Delegator.class), mock(LocalDispatcher.class), new RecordingSink());
            runner.run();

            synchronized (ThreadRecordingFixture.EXECUTED_ON) {
                assertThat(ThreadRecordingFixture.EXECUTED_ON, hasSize(ThreadRecordingFixture.METHOD_COUNT));
                assertThat(ThreadRecordingFixture.EXECUTED_ON, everyItem(sameInstance(callingThread)));
            }
        } finally {
            System.clearProperty(PARALLEL_ENABLED);
            System.clearProperty(PARALLEL_MODE_DEFAULT);
        }
    }

    @Test
    void assumptionSkipIsReportedAsPassedNotAsAnErrorOrFailure() {
        RecordingSink sink = new RecordingSink();

        new JupiterTestExtension.JupiterClassRunner(AssumptionFixture.class, mock(Delegator.class),
                mock(LocalDispatcher.class), sink).run();

        // startTest()/testFinished() still ran (unlike a @Disabled test, which never starts), so the
        // test counts as reported; it must not land as a Failure/Error outcome the way a bare
        // Throwable used to before ABORTED got its own branch.
        assertThat(sink.testFinishedCalls, hasSize(1));
        assertThat(sink.testFinishedCalls.get(0).outcome(), instanceOf(SuiteReportSink.Outcome.Passed.class));
    }

    @Test
    void disabledMethodIsExcludedFromExecution() {
        RecordingSink sink = new RecordingSink();

        new JupiterTestExtension.JupiterClassRunner(DisabledCountFixture.class, mock(Delegator.class),
                mock(LocalDispatcher.class), sink).run();

        assertThat(sink.testStartedCalls, hasSize(1));
        assertThat(sink.testStartedCalls.get(0), is(DisabledCountFixture.class.getName() + "#enabledTest"));
    }

    @Test
    void throwingBeforeAllIsReportedAsAnErrorInsteadOfSilentlyDiscarded() {
        RecordingSink sink = new RecordingSink();

        new JupiterTestExtension.JupiterClassRunner(ThrowingBeforeAllFixture.class, mock(Delegator.class),
                mock(LocalDispatcher.class), sink).run();

        // The @Test method itself never starts - JUnit 5 reports the failure once, on the class
        // container, with no [test-method:...] identifier at all - so this comes entirely from
        // reportContainerFailure(), not from triggersBeforeAll() itself. Without that reporting, this
        // class would silently contribute nothing to the report at all.
        assertThat(sink.testFinishedCalls, hasSize(1));
        SuiteReportSink.Outcome.Error error = (SuiteReportSink.Outcome.Error) sink.testFinishedCalls.get(0).outcome();
        assertThat(error.throwable().getMessage(), is("boom"));
    }

    @Test
    void beforeAllStaticMethodReceivesDelegatorViaParameterResolution() {
        Delegator delegator = mock(Delegator.class);

        new JupiterTestExtension.JupiterClassRunner(StaticInjectionFixture.class, delegator,
                mock(LocalDispatcher.class), new RecordingSink()).run();

        assertThat(StaticInjectionFixture.capturedDelegator(), sameInstance(delegator));
    }

    @Test
    void threadLocalsAreDisarmedAfterRun() {
        new JupiterTestExtension.JupiterClassRunner(ThreadRecordingFixture.class, mock(Delegator.class),
                mock(LocalDispatcher.class), new RecordingSink()).run();

        assertThat(JupiterTestExtension.CURRENT_DELEGATOR.get(), nullValue());
        assertThat(JupiterTestExtension.CURRENT_DISPATCHER.get(), nullValue());
    }

    @Test
    void tagsLogContextWithTestCaseDuringExecutionAndClearsItAfter() {
        new JupiterTestExtension.JupiterClassRunner(MdcCapturingFixture.class, mock(Delegator.class),
                mock(LocalDispatcher.class), new RecordingSink()).run();

        assertThat(MdcCapturingFixture.capturedTestCase, is("MdcCapturingFixture#capturesMdc"));
        assertThat(ThreadContext.get("testCase"), nullValue());
    }

    @Test
    void reportsRealClassAndMethodNamesNotASharedSyntheticClass() {
        RecordingSink sink = new RecordingSink();

        new JupiterTestExtension.JupiterClassRunner(DisabledCountFixture.class, mock(Delegator.class),
                mock(LocalDispatcher.class), sink).run();

        assertThat(sink.testFinishedCalls.get(0).classname(), is(DisabledCountFixture.class.getName()));
        assertThat(sink.testFinishedCalls.get(0).name(), is("enabledTest"));
    }

    @Test
    void exceptionEscapingLauncherExecuteIsReportedAsThisClasssErrorInsteadOfPropagating() {
        // Exercises reportClassExecutionFailure() directly rather than forcing a real JUnit Platform
        // internal failure (a JUnitException from a discovery/engine-registration problem) - same
        // established pattern as TestRunContainerTest exercising reportSuiteExecutionFailure()
        // directly instead of forcing something to escape start()'s for loop for real.
        RecordingSink sink = new RecordingSink();
        JupiterTestExtension.JupiterClassRunner runner = new JupiterTestExtension.JupiterClassRunner(
                DisabledCountFixture.class, mock(Delegator.class), mock(LocalDispatcher.class), sink);

        runner.reportClassExecutionFailure(new IllegalStateException("boom"));

        assertThat(sink.testStartedCalls, contains(DisabledCountFixture.class.getName() + "#classExecutionError"));
        SuiteReportSink.Outcome.Error error = (SuiteReportSink.Outcome.Error) sink.testFinishedCalls.get(0).outcome();
        assertThat(error.throwable().getMessage(), is("boom"));
    }

    @Test
    void testParamsThreadLocalIsArmedDuringExecutionAndClearedAfter() {
        RecordingSink sink = new RecordingSink();
        Map<String, Object> testParams = Map.of("greeting", "hello-from-caller");

        new JupiterTestExtension.JupiterClassRunner(
                ParamsRecordingFixture.class, mock(Delegator.class), mock(LocalDispatcher.class), testParams, sink)
                .run();

        assertThat(ParamsRecordingFixture.seenValue, is("hello-from-caller"));
        assertThat(JupiterTestExtension.CURRENT_TEST_PARAMS.get(), nullValue());
    }

    @Test
    void testParamsThreadLocalDefaultsToEmptyMapWhenOmitted() {
        RecordingSink sink = new RecordingSink();

        new JupiterTestExtension.JupiterClassRunner(
                ParamsRecordingFixture.class, mock(Delegator.class), mock(LocalDispatcher.class), sink)
                .run();

        assertThat(ParamsRecordingFixture.paramsWasNull, is(false));
        assertThat(ParamsRecordingFixture.seenValue, is(nullValue()));
    }

    @Test
    void testParamsExposesCallerSuppliedMapVerbatim() {
        RecordingSink sink = new RecordingSink();
        Map<String, Object> testParams = Map.of("exampleTypeId", "REAL_WORLD");

        new JupiterTestExtension.JupiterClassRunner(
                TestParamsFixture.class, mock(Delegator.class), mock(LocalDispatcher.class), testParams, sink)
                .run();

        assertThat(TestParamsFixture.seenValue, is("REAL_WORLD"));
    }

    @Test
    void testParamsIsEmptyMapWhenNoOverridesSupplied() {
        RecordingSink sink = new RecordingSink();

        new JupiterTestExtension.JupiterClassRunner(
                TestParamsFixture.class, mock(Delegator.class), mock(LocalDispatcher.class), sink)
                .run();

        assertThat(TestParamsFixture.seenValue, is(nullValue()));
    }

    @Test
    void namespacedTestParamOverridesFlatKeyForCurrentMethodOnlyOtherMethodsSeeFlat() {
        RecordingSink sink = new RecordingSink();
        Map<String, Object> testParams = Map.of(
                "color", "red",
                "shape", "square",
                "methodOne", Map.of("color", "blue"));

        new JupiterTestExtension.JupiterClassRunner(
                NamespacedTestParamsFixture.class, mock(Delegator.class), mock(LocalDispatcher.class), testParams, sink)
                .run();

        assertThat(NamespacedTestParamsFixture.methodOneSeenColor, is("blue"));
        assertThat(NamespacedTestParamsFixture.methodOneSeenShape, is("square"));
        assertThat(NamespacedTestParamsFixture.methodTwoSeenColor, is("red"));
    }

    @Test
    void siblingNamespacedEntryIsExcludedFromCommonBaseAndDoesNotLeakAcrossMethods() {
        RecordingSink sink = new RecordingSink();
        Map<String, Object> testParams = Map.of(
                "methodOne", Map.of("color", "blue"),
                "methodTwo", Map.of("color", "green"));

        new JupiterTestExtension.JupiterClassRunner(
                NamespacedTestParamsFixture.class, mock(Delegator.class), mock(LocalDispatcher.class), testParams, sink)
                .run();

        assertThat(NamespacedTestParamsFixture.methodOneSeenColor, is("blue"));
        assertThat(NamespacedTestParamsFixture.methodOneSeenMethodTwoRawValue, is(nullValue()));
        assertThat(NamespacedTestParamsFixture.methodTwoSeenColor, is("green"));
    }

    @Test
    void malformedNamespacedEntryFallsBackToCommonBase() {
        RecordingSink sink = new RecordingSink();
        Map<String, Object> testParams = Map.of(
                "color", "red",
                "methodOne", "not-a-map");

        new JupiterTestExtension.JupiterClassRunner(
                NamespacedTestParamsFixture.class, mock(Delegator.class), mock(LocalDispatcher.class), testParams, sink)
                .run();

        assertThat(NamespacedTestParamsFixture.methodOneSeenColor, is("red"));
    }

    @Test
    void currentTestMethodNameThreadLocalIsClearedAfterRun() {
        RecordingSink sink = new RecordingSink();

        new JupiterTestExtension.JupiterClassRunner(
                NamespacedTestParamsFixture.class, mock(Delegator.class), mock(LocalDispatcher.class), sink)
                .run();

        assertThat(JupiterTestExtension.CURRENT_TEST_METHOD_NAME.get(), nullValue());
    }

    @Test
    void failedAssertionInsideATestMethodIsReportedAsAFailureWithRealTypeAndStackTrace() {
        RecordingSink sink = new RecordingSink();

        new JupiterTestExtension.JupiterClassRunner(FailingAssertionFixture.class, mock(Delegator.class),
                mock(LocalDispatcher.class), sink).run();

        assertThat(sink.testFinishedCalls, hasSize(1));
        SuiteReportSink.Outcome.Failure failure = (SuiteReportSink.Outcome.Failure) sink.testFinishedCalls.get(0).outcome();
        // Assertions.assertTrue(false, "expected true") throws with message "expected true ==>
        // expected: <true> but was: <false>" - the "expected true" reason text is a prefix, not the
        // whole message.
        assertThat(failure.message(), containsString("expected true"));
        // The real AssertionError subtype JUnit 5's own Assertions API throws (org.opentest4j.AssertionFailedError),
        // not a JUnit-3-flavored junit.framework.AssertionFailedError manufactured by this class.
        assertThat(failure.type(), is("org.opentest4j.AssertionFailedError"));
        assertThat(failure.stackTrace(), not(emptyString()));
        assertThat(failure.stackTrace(), containsString("FailingAssertionFixture"));
    }

    @Test
    void thrownExceptionInsideATestMethodIsReportedAsAnErrorNotAFailure() {
        RecordingSink sink = new RecordingSink();

        new JupiterTestExtension.JupiterClassRunner(ThrowingTestFixture.class, mock(Delegator.class),
                mock(LocalDispatcher.class), sink).run();

        assertThat(sink.testFinishedCalls, hasSize(1));
        SuiteReportSink.Outcome.Error error = (SuiteReportSink.Outcome.Error) sink.testFinishedCalls.get(0).outcome();
        assertThat(error.throwable(), instanceOf(RuntimeException.class));
        assertThat(error.throwable().getMessage(), is("boom"));
    }

    @Test
    void methodNameScopesDiscoveryToExactlyThatMethod() {
        TwoMethodFixture.methodOneRunCount = 0;
        TwoMethodFixture.methodTwoRunCount = 0;
        RecordingSink sink = new RecordingSink();

        new JupiterTestExtension.JupiterClassRunner(TwoMethodFixture.class, mock(Delegator.class),
                mock(LocalDispatcher.class), Map.of(), "methodOne", sink).run();

        assertThat(TwoMethodFixture.methodOneRunCount, is(1));
        assertThat(TwoMethodFixture.methodTwoRunCount, is(0));
        assertThat(sink.testStartedCalls, contains(TwoMethodFixture.class.getName() + "#methodOne"));
    }

    @Test
    void nullMethodNameStillRunsTheWholeClassUnchanged() {
        TwoMethodFixture.methodOneRunCount = 0;
        TwoMethodFixture.methodTwoRunCount = 0;
        RecordingSink sink = new RecordingSink();

        new JupiterTestExtension.JupiterClassRunner(TwoMethodFixture.class, mock(Delegator.class),
                mock(LocalDispatcher.class), Map.of(), (String) null, sink).run();

        assertThat(TwoMethodFixture.methodOneRunCount, is(1));
        assertThat(TwoMethodFixture.methodTwoRunCount, is(1));
    }

    @Test
    void unknownMethodNameIsReportedAsAnInitializationErrorNotASilentNoOp() {
        // JUnit Platform's selectMethod() validates lazily during launcher.execute(), not at
        // selector-creation time - an unresolvable method surfaces as a FAILED container
        // (isTest() == false), which JupiterClassRunner already routes through
        // reportContainerFailure() (the same path a throwing @BeforeAll takes), reported as
        // "#initializationError" - confirmed empirically against this project's JUnit Platform
        // version rather than assumed.
        RecordingSink sink = new RecordingSink();

        new JupiterTestExtension.JupiterClassRunner(TwoMethodFixture.class, mock(Delegator.class),
                mock(LocalDispatcher.class), Map.of(), "noSuchMethod", sink).run();

        assertThat(sink.testStartedCalls, contains(TwoMethodFixture.class.getName() + "#initializationError"));
        assertThat(sink.testFinishedCalls, hasSize(1));
        SuiteReportSink.Outcome.Error error = (SuiteReportSink.Outcome.Error) sink.testFinishedCalls.get(0).outcome();
        assertThat(error.throwable().getMessage(), containsString("noSuchMethod"));
    }

    @Test
    void methodNameSelectsAllInvocationsOfAParameterizedMethod() {
        // Regression test for the bug Fix 1 (JupiterClassRunner.selectMethodByName) resolves:
        // DiscoverySelectors.selectMethod(Class, String) alone can never match a method that
        // declares a parameter, which every @ParameterizedTest method does by definition - it would
        // fail with the same "could not find method" error a typo produces.
        ParameterizedMethodFixture.invocationCount = 0;
        RecordingSink sink = new RecordingSink();

        new JupiterTestExtension.JupiterClassRunner(ParameterizedMethodFixture.class, mock(Delegator.class),
                mock(LocalDispatcher.class), Map.of(), "parameterized", sink).run();

        assertThat(ParameterizedMethodFixture.invocationCount, is(3));
        assertThat(sink.testFinishedCalls, hasSize(3));
    }

    //ALLOW PUBLIC FIELDS
    @Tag(JupiterTestExtension.INTEGRATION_TAG)
    static class ThreadRecordingFixture {
        static final int METHOD_COUNT = 4;
        static final List<Thread> EXECUTED_ON = Collections.synchronizedList(new ArrayList<>());

        @Test
        void methodA() {
            EXECUTED_ON.add(Thread.currentThread());
        }

        @Test
        void methodB() {
            EXECUTED_ON.add(Thread.currentThread());
        }

        @Test
        void methodC() {
            EXECUTED_ON.add(Thread.currentThread());
        }

        @Test
        void methodD() {
            EXECUTED_ON.add(Thread.currentThread());
        }
    }

    @Tag(JupiterTestExtension.INTEGRATION_TAG)
    static class MdcCapturingFixture {
        static String capturedTestCase;

        @Test
        void capturesMdc() {
            capturedTestCase = ThreadContext.get("testCase");
        }
    }

    @Tag(JupiterTestExtension.INTEGRATION_TAG)
    @ExtendWith(JupiterTestExtension.class)
    static class AssumptionFixture {
        @Test
        void skipsViaAssumption() {
            Assumptions.assumeTrue(false, "deliberately never true - exists only to abort this test");
        }
    }

    @Tag(JupiterTestExtension.INTEGRATION_TAG)
    static class DisabledCountFixture {
        @Test
        void enabledTest() {
        }

        @Disabled("only used to verify a disabled method is excluded from execution")
        @Test
        void disabledTest() {
        }
    }

    @Tag(JupiterTestExtension.INTEGRATION_TAG)
    @ExtendWith(JupiterTestExtension.class)
    static class StaticInjectionFixture {
        private static Delegator capturedDelegator;

        @BeforeAll
        static void captureDelegator(Delegator delegator) {
            capturedDelegator = delegator;
        }

        static Delegator capturedDelegator() {
            return capturedDelegator;
        }

        @Test
        void triggersBeforeAll() {
        }
    }

    @Tag(JupiterTestExtension.INTEGRATION_TAG)
    @ExtendWith(JupiterTestExtension.class)
    static class ThrowingBeforeAllFixture {
        @BeforeAll
        static void explode() {
            throw new IllegalStateException("boom");
        }

        @Test
        void triggersBeforeAll() {
        }
    }

    @Tag(JupiterTestExtension.INTEGRATION_TAG)
    static class FailingAssertionFixture {
        @Test
        void failsAnAssertion() {
            Assertions.assertTrue(false, "expected true");
        }
    }

    @Tag(JupiterTestExtension.INTEGRATION_TAG)
    static class ThrowingTestFixture {
        @Test
        void throwsARuntimeException() {
            throw new RuntimeException("boom");
        }
    }

    @Tag(JupiterTestExtension.INTEGRATION_TAG)
    static class TwoMethodFixture {
        static int methodOneRunCount;
        static int methodTwoRunCount;

        @Test
        void methodOne() {
            methodOneRunCount++;
        }

        @Test
        void methodTwo() {
            methodTwoRunCount++;
        }
    }

    @Tag(JupiterTestExtension.INTEGRATION_TAG)
    static class ParameterizedMethodFixture {
        static int invocationCount;

        @ParameterizedTest
        @CsvSource({"a", "b", "c"})
        void parameterized(String value) {
            invocationCount++;
        }
    }

    @Tag(JupiterTestExtension.INTEGRATION_TAG)
    static class ParamsRecordingFixture {
        static String seenValue;
        static Boolean paramsWasNull;

        @Test
        void onlyTest() {
            Map<String, Object> params = JupiterTestExtension.CURRENT_TEST_PARAMS.get();
            paramsWasNull = (params == null);
            seenValue = params == null ? null : (String) params.get("greeting");
        }
    }

    @Tag(JupiterTestExtension.INTEGRATION_TAG)
    static class TestParamsFixture implements JupiterTestHelper {
        static Object seenValue;

        @Test
        void onlyTest() {
            seenValue = getTestParams().get("exampleTypeId");
        }
    }

    @Tag(JupiterTestExtension.INTEGRATION_TAG)
    static class NamespacedTestParamsFixture implements JupiterTestHelper {
        static Object methodOneSeenColor;
        static Object methodOneSeenShape;
        static Object methodOneSeenMethodTwoRawValue;
        static Object methodTwoSeenColor;

        @Test
        void methodOne() {
            methodOneSeenColor = getTestParams().get("color");
            methodOneSeenShape = getTestParams().get("shape");
            methodOneSeenMethodTwoRawValue = getTestParams().get("methodTwo");
        }

        @Test
        void methodTwo() {
            methodTwoSeenColor = getTestParams().get("color");
        }
    }
    //FORBID PUBLIC FIELDS
}
