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

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.service.LocalDispatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;

import junit.framework.TestResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Exercises the fail-fast guards added to JupiterTestExtension directly (Concerns 2 and 3 of the
 * community discussion), without needing the full ofbiz --test container: the ThreadLocal bridge
 * and field/parameter reflection are set up and torn down by hand here.
 */
class JupiterInjectionGuardsTest {

    private final JupiterTestExtension extension = new JupiterTestExtension();

    private static final String PARALLEL_ENABLED = "junit.jupiter.execution.parallel.enabled";
    private static final String PARALLEL_MODE_DEFAULT = "junit.jupiter.execution.parallel.mode.default";

    @AfterEach
    void clearThreadLocals() {
        JupiterTestExtension.CURRENT_DELEGATOR.remove();
        JupiterTestExtension.CURRENT_DISPATCHER.remove();
    }

    @Test
    void correctlyNamedFieldsGetInjected() throws Exception {
        Delegator delegator = mock(Delegator.class);
        LocalDispatcher dispatcher = mock(LocalDispatcher.class);
        JupiterTestExtension.CURRENT_DELEGATOR.set(delegator);
        JupiterTestExtension.CURRENT_DISPATCHER.set(dispatcher);

        CorrectlyNamedFields instance = new CorrectlyNamedFields();
        extension.postProcessTestInstance(instance, null);

        assertThat(instance.delegator, sameInstance(delegator));
        assertThat(instance.dispatcher, sameInstance(dispatcher));
    }

    @Test
    void classWithNoDelegatorOrDispatcherFieldsIsUntouched() {
        JupiterTestExtension.CURRENT_DELEGATOR.set(mock(Delegator.class));
        JupiterTestExtension.CURRENT_DISPATCHER.set(mock(LocalDispatcher.class));

        assertDoesNotThrow(() -> extension.postProcessTestInstance(new NoRelevantFields(), null));
    }

    @Test
    void misnamedDelegatorFieldFailsFastInsteadOfSilentlySkipping() {
        JupiterTestExtension.CURRENT_DELEGATOR.set(mock(Delegator.class));

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                extension.postProcessTestInstance(new MisnamedDelegatorField(), null));

        assertThat(thrown.getMessage(), containsString("myDelegator"));
        assertThat(thrown.getMessage(), containsString("'delegator'"));
    }

    @Test
    void namedFieldWithNoAvailableDelegatorFailsFastInsteadOfLeavingItNull() {
        // CURRENT_DELEGATOR intentionally left unset, simulating running outside the ofbiz --test
        // container or on a worker thread under (unsupported) parallel execution.
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                extension.postProcessTestInstance(new CorrectlyNamedFields(), null));

        assertThat(thrown.getMessage(), containsString("delegator"));
    }

    @Test
    void parameterResolutionFailsFastWhenThreadLocalIsUnset() throws Exception {
        Method dummy = ParameterFixtures.class.getDeclaredMethod("dummy", Delegator.class);
        Parameter delegatorParameter = dummy.getParameters()[0];
        ParameterContext parameterContext = mock(ParameterContext.class);
        when(parameterContext.getParameter()).thenReturn(delegatorParameter);

        ParameterResolutionException thrown = assertThrows(ParameterResolutionException.class, () ->
                extension.resolveParameter(parameterContext, null));

        assertThat(thrown, instanceOf(ParameterResolutionException.class));
    }

    @Test
    void parameterResolutionSucceedsWhenThreadLocalIsSet() throws Exception {
        Delegator delegator = mock(Delegator.class);
        JupiterTestExtension.CURRENT_DELEGATOR.set(delegator);
        Method dummy = ParameterFixtures.class.getDeclaredMethod("dummy", Delegator.class);
        Parameter delegatorParameter = dummy.getParameters()[0];
        ParameterContext parameterContext = mock(ParameterContext.class);
        when(parameterContext.getParameter()).thenReturn(delegatorParameter);

        Object resolved = extension.resolveParameter(parameterContext, null);

        assertThat(resolved, sameInstance(delegator));
    }

    @Test
    void parallelExecutionStaysDisabledEvenWhenSystemPropertiesRequestIt() {
        System.setProperty(PARALLEL_ENABLED, "true");
        System.setProperty(PARALLEL_MODE_DEFAULT, "concurrent");
        try {
            Thread callingThread = Thread.currentThread();
            ThreadRecordingFixture.EXECUTED_ON.clear();

            JupiterTestExtension.JupiterTestSuite suite =
                    new JupiterTestExtension.JupiterTestSuite(ThreadRecordingFixture.class);
            suite.run(new TestResult());

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
    void assumptionSkipIsReportedAsInfoNotAsAnError() {
        Delegator delegator = mock(Delegator.class);
        LocalDispatcher dispatcher = mock(LocalDispatcher.class);

        JupiterTestExtension.JupiterTestSuite suite =
                new JupiterTestExtension.JupiterTestSuite(AssumptionFixture.class);
        suite.setDelegator(delegator);
        suite.setDispatcher(dispatcher);
        TestResult result = new TestResult();
        suite.run(result);

        // startTest()/endTest() still ran (unlike a @Disabled test, which never starts), so the
        // test counts toward runCount(); it must not also land in errorCount()/failureCount() the
        // way a bare Throwable used to before ABORTED got its own branch in executionFinished().
        assertThat(result.runCount(), is(1));
        assertThat(result.errorCount(), is(0));
        assertThat(result.failureCount(), is(0));
    }

    @Test
    void countTestCasesExcludesDisabledMethods() {
        JupiterTestExtension.JupiterTestSuite suite =
                new JupiterTestExtension.JupiterTestSuite(DisabledCountFixture.class);

        assertThat(suite.countTestCases(), is(1));
    }

    @Test
    void throwingBeforeAllIsReportedAsAnErrorInsteadOfSilentlyDiscarded() {
        Delegator delegator = mock(Delegator.class);
        LocalDispatcher dispatcher = mock(LocalDispatcher.class);

        JupiterTestExtension.JupiterTestSuite suite =
                new JupiterTestExtension.JupiterTestSuite(ThrowingBeforeAllFixture.class);
        suite.setDelegator(delegator);
        suite.setDispatcher(dispatcher);
        TestResult result = new TestResult();
        suite.run(result);

        // The @Test method itself never starts - JUnit 5 reports the failure once, on the class
        // container, with no [test-method:...] identifier at all (confirmed by instrumenting the
        // listener directly) - so this runCount()/errorCount() pair comes entirely from the
        // synthetic leaf reportContainerFailure() reports, not from triggersBeforeAll() itself.
        // Without that reporting, this would be results.wasSuccessful() == true for a class whose
        // tests never actually ran - the exact false-positive "all green" this fix prevents.
        assertThat(result.runCount(), is(1));
        assertThat(result.errorCount(), is(1));
        assertThat(result.wasSuccessful(), is(false));
        Throwable reported = result.errors().nextElement().thrownException();
        assertThat(reported.getMessage(), containsString("boom"));
    }

    @Test
    void beforeAllStaticMethodReceivesDelegatorViaParameterResolution() {
        Delegator delegator = mock(Delegator.class);
        LocalDispatcher dispatcher = mock(LocalDispatcher.class);

        JupiterTestExtension.JupiterTestSuite suite =
                new JupiterTestExtension.JupiterTestSuite(StaticInjectionFixture.class);
        suite.setDelegator(delegator);
        suite.setDispatcher(dispatcher);
        suite.run(new TestResult());

        assertThat(StaticInjectionFixture.capturedDelegator(), sameInstance(delegator));
    }

    //ALLOW PUBLIC FIELDS
    static class CorrectlyNamedFields {
        Delegator delegator;
        LocalDispatcher dispatcher;
    }

    static class MisnamedDelegatorField {
        Delegator myDelegator;
    }

    static class NoRelevantFields {
        String name;
    }

    //FORBID PUBLIC FIELDS

    // Tagged so build.gradle's `test` task (excludeTags 'jupiterIntegration') excludes it from
    // plain gradlew test's classpath-scan discovery entirely - it must not be independently
    // discovered and run as its own phantom test class, only constructed and run directly by
    // parallelExecutionStaysDisabledEvenWhenSystemPropertiesRequestIt() above. Do NOT use
    // @JunitJupiterTest here: that composed annotation also adds @ExtendWith(JupiterTestExtension),
    // whose evaluateExecutionCondition() would find CURRENT_DELEGATOR/CURRENT_DISPATCHER both null
    // (this fixture is never passed through setDelegator()/setDispatcher()) and disable every
    // method, making the regression test's hasSize(METHOD_COUNT) assertion fail.
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

    static class ParameterFixtures {
        void dummy(Delegator delegator) {
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

    // No @ExtendWith needed: countTestCases() is read straight off the constructor's discovery
    // result, before run() would ever need CURRENT_DELEGATOR/CURRENT_DISPATCHER armed.
    @Tag(JupiterTestExtension.INTEGRATION_TAG)
    static class DisabledCountFixture {
        @Test
        void enabledTest() {
        }

        @Disabled("only used to verify countTestCases() excludes a statically-disabled method")
        @Test
        void disabledTest() {
        }
    }

    // Unlike ThreadRecordingFixture above, this one needs @ExtendWith(JupiterTestExtension.class)
    // active: a static @BeforeAll method has no test instance for postProcessTestInstance() to
    // inject a field into, so resolveParameter() - reached only through the registered extension -
    // is the only injection path the javadoc documents for it, and this is what exercises that path.
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
            // No-op: exists only so the class has a @Test method for @BeforeAll to run ahead of.
        }
    }

    // Exercises reportContainerFailure(): a throwing @BeforeAll means JUnit 5 reports FAILED once, on
    // the class container, and never starts triggersBeforeAll() below at all.
    @Tag(JupiterTestExtension.INTEGRATION_TAG)
    @ExtendWith(JupiterTestExtension.class)
    static class ThrowingBeforeAllFixture {
        @BeforeAll
        static void explode() {
            throw new IllegalStateException("boom");
        }

        @Test
        void triggersBeforeAll() {
            // No-op: never actually reached - exists only so the class has a @Test method.
        }
    }
}
