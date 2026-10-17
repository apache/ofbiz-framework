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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.ThreadContext;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.testtools.SuiteReportSink.Outcome;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;

/**
 * Injects the per-suite Delegator/LocalDispatcher that ModelTestSuite already builds for JUnit 3
 * test-cases into Jupiter test classes run through JupiterClassRunner.
 *
 * <p>The recommended pattern - the one both reference examples use ({@code ExampleTests} and
 * {@code ExampleJupiterTests} in {@code plugins/example/.../test}) - is to {@code implements
 * JupiterTestHelper} and call its getDelegator()/getDispatcher()/getUserLogin()/from()/select()
 * directly: no field, no method parameter, nothing to declare in the test class at all. That
 * interface's default methods read this extension's CURRENT_DELEGATOR/CURRENT_DISPATCHER
 * ThreadLocals directly (see its javadoc), which is also why it works unchanged inside
 * {@literal @}ParameterizedTest methods - getDispatcher() isn't one of the method's declared
 * parameters, so there's no interaction with {@literal @}CsvSource (or any other argument source)
 * ordering at all. See {@code shouldCreateExampleAcrossTypes} in {@code ExampleJupiterTests},
 * which combines a {@literal @}CsvSource-provided {@code String} with a getDispatcher() call.
 *
 * <p>Two lower-level mechanisms remain available for classes that can't rely on
 * {@code JupiterTestHelper}:
 *
 * <ul>
 * <li>declare a "delegator"/"dispatcher" field (any visibility - a bare Groovy property
 * declaration is enough) and it is set once per test instance via postProcessTestInstance() below,
 * the same idea as JUnit 3's EntityTestCase getting Delegator/LocalDispatcher through
 * post-construction setDelegator()/setDispatcher() calls; or</li>
 * <li>declare a Delegator/LocalDispatcher method parameter directly, resolved via
 * resolveParameter() below.</li>
 * </ul>
 *
 * Both are wired from the same suite-scoped values as JupiterTestHelper, so either can be mixed in
 * on the same class if needed. The method-parameter style is needed rather than merely optional in
 * one case: {@literal @}BeforeAll/{@literal @}AfterAll are static, so they run with no test
 * instance for postProcessTestInstance() to inject a field into, or for a default interface method
 * to be called on; a method parameter, resolved per-invocation, is the only way to reach
 * Delegator/LocalDispatcher there.
 *
 * <p>When a {@literal @}ParameterizedTest method does mix {@literal @}CsvSource-provided arguments
 * with a method parameter resolved by this extension (Delegator/LocalDispatcher) rather than
 * JupiterTestHelper, the CSV-provided parameters must come first in the method signature: JUnit 5
 * fills them left-to-right, then resolves the remaining parameters via registered
 * ParameterResolvers.
 *
 * <p>JupiterClassRunner.run() executes tests synchronously on the calling thread. This is pinned, not
 * merely assumed of Jupiter's default: the discovery request built in JupiterClassRunner's
 * constructor sets {@code configurationParameter("junit.jupiter.execution.parallel.enabled",
 * "false")} on the {@code LauncherDiscoveryRequest} itself, which is the highest-precedence
 * configuration source in the JUnit Platform - it wins over a {@code junit-platform.properties}
 * file, a JVM system property, or any future Gradle test-task configuration, so none of those can
 * silently re-enable parallelism out from under this extension. That configurationParameter must
 * not be removed: without it, a test method could be dispatched to a worker thread other than the
 * one launcher.execute() was called from, and the plain ThreadLocal set immediately before that
 * call - which is what all three hooks below (postProcessTestInstance(), resolveParameter(),
 * evaluateExecutionCondition()) read CURRENT_DELEGATOR/CURRENT_DISPATCHER from - is invisible to
 * any other thread.
 *
 * <p><b>Classes run outside the container are skipped, not failed.</b>
 * evaluateExecutionCondition() below disables any class extended with this extension - via
 * {@literal @}JunitJupiterTest or a bare {@literal @}ExtendWith(JupiterTestExtension) - whose
 * CURRENT_DELEGATOR/CURRENT_DISPATCHER ThreadLocals are unset. Under plain {@code gradlew test},
 * {@literal @}JunitJupiterTest classes are already excluded before discovery by their tag (see
 * build.gradle's excludeTags), so this condition is the safety net for the paths that filter doesn't
 * cover: a class using bare {@literal @}ExtendWith(JupiterTestExtension.class) instead of
 * {@literal @}JunitJupiterTest, and an IDE-native test run that bypasses Gradle's test task
 * entirely. This turns what would otherwise be a NullPointerException deep in test logic
 * (JupiterTestHelper's default methods) or the IllegalStateException/
 * ParameterResolutionException thrown by the two hooks below into a reported skip with an
 * actionable reason. Those two hooks' exceptions remain in place as a safety net for a genuine
 * in-container misconfiguration; they are simply unreachable for the outside-the-container case
 * now that the class never gets that far.
 *
 * <p><b>Not per-test isolation.</b> JUnit 5 creates a fresh test instance per {@literal @}Test
 * method by default, which can suggest each method also gets a fresh Delegator/LocalDispatcher -
 * it doesn't. The Delegator/LocalDispatcher injected here are the single instances ModelTestSuite's
 * constructor builds once for the whole {@code <test-suite>}, passed straight into each
 * JupiterClassRunner by TestRunContainer, shared across every test method and every Jupiter/JUnit 3
 * entry in that suite, exactly as JUnit 3 test-cases already share them today via
 * ModelTestSuite.getPreparedTestList()'s injection. TestRunContainer rolls back all accumulated
 * mutations once, after the entire suite finishes - not per test method - so a test can observe data
 * created by an earlier test in the same suite, and ordering between test-cases in the suite's
 * testdef XML can matter.
 */
public class JupiterTestExtension implements ParameterResolver, TestInstancePostProcessor, ExecutionCondition {

    /** Read by build.gradle's `test` task ({@code excludeTags}) and by {@link JunitJupiterTest}. */
    public static final String INTEGRATION_TAG = "jupiterIntegration";

    // Must match the %X{testCase} reference in framework/base/config/log4j2.xml's logPattern.
    static final String TEST_CASE_MDC_KEY = "testCase";

    static final ThreadLocal<Delegator> CURRENT_DELEGATOR = new ThreadLocal<>();
    static final ThreadLocal<LocalDispatcher> CURRENT_DISPATCHER = new ThreadLocal<>();
    static final ThreadLocal<Map<String, Object>> CURRENT_TEST_PARAMS = new ThreadLocal<>();

    /**
     * The bare, undecorated method name of the Jupiter {@literal @}Test currently executing on this
     * thread (e.g. "shouldCreateExample") - armed/cleared in JupiterClassRunner's
     * executionStarted()/executionFinished() listener callbacks, the same lifecycle already used for
     * TEST_CASE_MDC_KEY. Lets JupiterTestHelper.getTestParams() look up a namespaced per-method
     * override in CURRENT_TEST_PARAMS without every test method having to identify itself. For a
     * parameterized test, reportingName() returns a decorated name (e.g.
     * "methodName[exampleTypeId=CONTRIVED]"), so namespacing only cleanly targets plain,
     * non-parameterized @Test methods - see JupiterTestHelper.getTestParams()'s javadoc.
     */
    static final ThreadLocal<String> CURRENT_TEST_METHOD_NAME = new ThreadLocal<>();

    /**
     * Disables classes/methods run outside the ofbiz --test container instead of letting them reach
     * postProcessTestInstance()/resolveParameter() (or, for JupiterTestHelper-based classes, a
     * NullPointerException from a getDelegator()/getDispatcher() caller). Both ThreadLocals are
     * checked rather than just one so a class relying on only a Delegator or only a
     * LocalDispatcher isn't disabled by a coincidentally-unset ThreadLocal it never actually reads
     * - in practice JupiterClassRunner.run() arms both together.
     */
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext extensionContext) {
        if (CURRENT_DELEGATOR.get() == null && CURRENT_DISPATCHER.get() == null) {
            return ConditionEvaluationResult.disabled(
                    "Requires the ofbiz --test container (Delegator/LocalDispatcher not armed on this "
                            + "thread). Run via 'gradlew testIntegration' or 'ofbiz --test', not plain "
                            + "'gradlew test'.");
        }
        return ConditionEvaluationResult.enabled("Delegator/LocalDispatcher available.");
    }

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext extensionContext) throws Exception {
        injectField(testInstance, "delegator", Delegator.class, CURRENT_DELEGATOR.get());
        injectField(testInstance, "dispatcher", LocalDispatcher.class, CURRENT_DISPATCHER.get());
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Class<?> type = parameterContext.getParameter().getType();
        return type == Delegator.class || type == LocalDispatcher.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Class<?> type = parameterContext.getParameter().getType();
        Object value = type == Delegator.class ? CURRENT_DELEGATOR.get() : CURRENT_DISPATCHER.get();
        if (value == null) {
            throw new ParameterResolutionException(unavailableMessage(type.getSimpleName(),
                    "parameter '" + parameterContext.getParameter().getName() + "'"));
        }
        return value;
    }

    private static void injectField(Object testInstance, String fieldName, Class<?> fieldType, Object value) throws IllegalAccessException {
        Field field = null;
        for (Class<?> clz = testInstance.getClass(); clz != null; clz = clz.getSuperclass()) {
            Field candidate = declaredFieldOrNull(clz, fieldName);
            if (candidate != null && fieldType.isAssignableFrom(candidate.getType())) {
                field = candidate;
                break;
            }
        }
        if (field == null) {
            Field mismatch = findAnyFieldOfType(testInstance.getClass(), fieldType);
            if (mismatch != null) {
                throw new IllegalStateException("Field '" + mismatch.getName() + "' in " + testInstance.getClass().getName()
                        + " is of type " + fieldType.getSimpleName() + ", but field injection only recognizes a field "
                        + "named exactly '" + fieldName + "'. Rename it to '" + fieldName + "', or implement "
                        + "JupiterTestHelper instead (type-based, no field name required).");
            }
            return;
        }
        if (value == null) {
            throw new IllegalStateException(unavailableMessage(fieldType.getSimpleName(),
                    "field '" + fieldName + "' of " + testInstance.getClass().getName()));
        }
        field.setAccessible(true);
        field.set(testInstance, value);
    }

    /**
     * Backstop for Concern 3 (name-literal field injection is otherwise silent on a typo): finds any field of the
     * right type regardless of name, so injectField() can fail loudly with the actual field name and the required
     * one, instead of leaving a misnamed field null with no indication injection was ever attempted.
     */
    private static Field findAnyFieldOfType(Class<?> testClass, Class<?> fieldType) {
        for (Class<?> clz = testClass; clz != null; clz = clz.getSuperclass()) {
            for (Field field : clz.getDeclaredFields()) {
                if (fieldType.isAssignableFrom(field.getType())) {
                    return field;
                }
            }
        }
        return null;
    }

    /**
     * Both injection points (field and parameter) reach here only when the caller has explicitly asked for a
     * Delegator/LocalDispatcher - by declaring the field or parameter - so a null ThreadLocal value here is always a
     * misconfiguration, not a legitimate "test doesn't need it" case. Failing fast at the injection site turns what
     * would otherwise be a mystery NPE deep in test logic into an error that points at the actual cause.
     */
    private static String unavailableMessage(String typeName, String target) {
        return "No " + typeName + " available to inject into " + target + ". JupiterTestExtension's ThreadLocal "
                + "bridge is only populated on the thread that calls JupiterClassRunner.run(), and only for classes "
                + "run through the ofbiz --test container (jupiter-test-suite in a testdef XML). This is null "
                + "because either this class ran outside that container (e.g. plain gradlew test), or JUnit 5 "
                + "parallel execution is enabled for it - both unsupported for delegator/dispatcher injection.";
    }

    private static Field declaredFieldOrNull(Class<?> clz, String fieldName) {
        try {
            return clz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    /**
     * Executes one Jupiter test class through the real JUnit Platform Launcher and reports its
     * results directly to one or more {@link SuiteReportSink}s - replacing JupiterTestSuite/
     * JupiterLeafTest, which used to adapt a Jupiter class to the JUnit 3 junit.framework.Test
     * contract purely so it could plug into TestRunContainer's junit.framework.TestSuite/TestResult
     * pipeline. That impersonation is gone: TestRunContainer now runs a JupiterClassRunner directly
     * for each JupiterEntry in a ModelTestSuite's prepared test list, side-by-side in the same loop
     * as JUnit 3 entries (run through Junit3ResultBridge instead), both feeding the same sink(s) in
     * real execution order.
     *
     * <p>The discovery request pins the default method orderer to {@code MethodOrderer.OrderAnnotation},
     * replacing Jupiter's own unordered default so a class's execution order is always whatever its
     * {@code @Order} annotations say (or unspecified only among methods that declare none) rather than
     * an unpredictable per-run default. It also pins {@code junit.jupiter.execution.parallel.enabled}
     * to {@code false}, so every test method executes on the calling thread regardless of any system
     * property or properties file that might otherwise request parallelism - see the class-level
     * javadoc above for why that matters to the ThreadLocal bridge.
     *
     * <p>Reports real class/method names directly - no synthetic shared reporting handle is needed
     * anymore, since {@link SuiteReportSink#testStarted}/{@link SuiteReportSink#testFinished} take a
     * classname/name pair rather than a junit.framework.Test object. Two same-named methods in
     * different Jupiter classes bundled into the same {@code <test-suite>} (a real collision:
     * {@code AutoAcctgAdminTests} and {@code AutoAcctgAgreementTests} both have a
     * {@code testAddPaymentMethodTypeGlAssignment}) are still distinguishable in the report, now via
     * the {@code classname} attribute itself rather than a name-prefix workaround.
     */
    static final class JupiterClassRunner {

        private static final String MODULE = JupiterClassRunner.class.getName();
        private static final Pattern INDEX_SUFFIX = Pattern.compile("(.*)\\[\\d+\\]$");

        private final Class<?> testClass;
        private final Delegator delegator;
        private final LocalDispatcher dispatcher;
        private final Map<String, Object> testParams;
        private final List<SuiteReportSink> sinks;
        private final Launcher launcher;
        private final LauncherDiscoveryRequest request;

        JupiterClassRunner(Class<?> testClass, Delegator delegator, LocalDispatcher dispatcher, SuiteReportSink... sinks) {
            this(testClass, delegator, dispatcher, Map.of(), null, sinks);
        }

        JupiterClassRunner(Class<?> testClass, Delegator delegator, LocalDispatcher dispatcher,
                Map<String, Object> testParams, SuiteReportSink... sinks) {
            this(testClass, delegator, dispatcher, testParams, null, sinks);
        }

        /**
         * @param methodName when non-null, scopes discovery to exactly this {@literal @}Test/
         *     {@literal @}ParameterizedTest method ({@code selectMethod}) instead of the whole class
         *     ({@code selectClass}) - the {@code ofbiz --test method=} CLI path
         *     (TestRunContainer.start()) supplies this; every other caller passes null and gets
         *     today's whole-class behavior unchanged. Naming a {@literal @}ParameterizedTest method
         *     here selects every invocation of that method, not one specific input row - JUnit
         *     Platform's selectMethod() has no finer granularity than that.
         *
         *     <p><b>Caution:</b> a method run alone this way can behave differently than it does as
         *     part of the whole class - flagUnorderedJupiterTests (build.gradle) exists precisely
         *     because several classes in this codebase were found to have methods that implicitly
         *     depend on declaration order or on a sibling method's side effects. A method scoped
         *     this way may pass alone but fail as part of the full class, or the reverse - that is
         *     not a bug in this parameter, it reflects a pre-existing lack of independence between
         *     methods in the target class.
         */
        JupiterClassRunner(Class<?> testClass, Delegator delegator, LocalDispatcher dispatcher,
                Map<String, Object> testParams, String methodName, SuiteReportSink... sinks) {
            this.testClass = testClass;
            this.delegator = delegator;
            this.dispatcher = dispatcher;
            this.testParams = testParams;
            this.sinks = List.of(sinks);
            this.launcher = LauncherFactory.create();
            DiscoverySelector[] selectors = methodName != null
                    ? selectMethodByName(testClass, methodName)
                    : new DiscoverySelector[] {selectClass(testClass)};
            this.request = LauncherDiscoveryRequestBuilder.request()
                    .selectors(selectors)
                    .configurationParameter(
                            "junit.jupiter.testmethod.order.default",
                            "org.junit.jupiter.api.MethodOrderer$OrderAnnotation")
                    .configurationParameter("junit.jupiter.execution.parallel.enabled", "false")
                    .build();
        }

        /**
         * Resolves {@code methodName} against every declared/inherited method of that name on
         * {@code testClass} and builds one {@code selectMethod} selector per match, instead of relying
         * on {@code DiscoverySelectors.selectMethod(Class, String)} alone.
         *
         * <p>That two-argument overload only matches a zero-parameter method - it delegates to the
         * three-argument overload with an empty parameter-type list, so it can never resolve a
         * {@literal @}ParameterizedTest method (which always declares at least one parameter) or any
         * {@literal @}Test method taking a JupiterTestExtension-resolved Delegator/LocalDispatcher
         * parameter; both fail with the same "could not find method" discovery error a genuine typo
         * produces, silently misreporting a real method as nonexistent. Resolving by reflection first
         * and selecting by {@code Method} instead of by name alone sidesteps that restriction.
         *
         * <p>When no method of that name exists at all, falls back to the plain by-name selector so
         * the same clean "could not find method" discovery failure (routed through
         * reportContainerFailure() below as this class's initializationError) still fires for a
         * genuine typo - this method never throws for an unresolved name itself.
         * @param testClass the class methodName is resolved against
         * @param methodName the requested method name
         * @return one selector per overload/match found, or a single by-name selector if none matched
         */
        private static DiscoverySelector[] selectMethodByName(Class<?> testClass, String methodName) {
            List<Method> matches = ReflectionSupport.findMethods(testClass,
                    method -> method.getName().equals(methodName), HierarchyTraversalMode.TOP_DOWN);
            if (matches.isEmpty()) {
                return new DiscoverySelector[] {selectMethod(testClass, methodName)};
            }
            return matches.stream()
                    .map(method -> (DiscoverySelector) selectMethod(testClass, method))
                    .toArray(DiscoverySelector[]::new);
        }

        /**
         * Runs this class's tests, reporting to every configured sink as JUnit 5 execution events
         * arrive. Isolated per class (tightens item 14 of the JUnit5 improvements catalog): an
         * exception escaping launcher.execute() itself - a JUnitException from a discovery/
         * engine-registration problem, a PreconditionViolationException, or a bug in the listener
         * below - is caught here and reported as this one class's error, so a problem in one Jupiter
         * class can no longer take down sibling entries (JUnit 3 or Jupiter) declared after it in the
         * same testdef {@code <test-suite>}. TestRunContainer's own outer per-suite try/catch remains
         * as a last-resort net for anything escaping the SuiteEntry loop itself.
         */
        void run() {
            JupiterTestExtension.CURRENT_DELEGATOR.set(delegator);
            JupiterTestExtension.CURRENT_DISPATCHER.set(dispatcher);
            JupiterTestExtension.CURRENT_TEST_PARAMS.set(testParams);
            Map<String, Long> startTimes = new HashMap<>();
            try {
                launcher.execute(request, new TestExecutionListener() {
                    @Override
                    public void executionStarted(TestIdentifier testIdentifier) {
                        if (testIdentifier.isTest()) {
                            startTimes.put(testIdentifier.getUniqueId(), System.currentTimeMillis());
                            ThreadContext.put(TEST_CASE_MDC_KEY, testClass.getSimpleName() + "#" + reportingName(testIdentifier));
                            JupiterTestExtension.CURRENT_TEST_METHOD_NAME.set(reportingName(testIdentifier));
                            ReportingSupport.dispatch(sinks, sink -> sink.testStarted(testClass.getName(), reportingName(testIdentifier)));
                        }
                    }

                    @Override
                    public void executionSkipped(TestIdentifier testIdentifier, String reason) {
                        if (testIdentifier.isTest()) {
                            Debug.logInfo("[JUNIT] SKIPPED: " + testIdentifier.getDisplayName()
                                    + " (" + testClass.getName() + ") - " + reason, MODULE);
                        }
                    }

                    @Override
                    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
                        if (!testIdentifier.isTest()) {
                            reportContainerFailure(testIdentifier, testExecutionResult);
                            return;
                        }
                        try {
                            String name = reportingName(testIdentifier);
                            long elapsed = System.currentTimeMillis()
                                    - startTimes.getOrDefault(testIdentifier.getUniqueId(), System.currentTimeMillis());
                            if (testExecutionResult.getStatus() == TestExecutionResult.Status.ABORTED) {
                                // A JUnit 5 Assumptions.assumeTrue/assumeFalse failure: a deliberate skip,
                                // not a defect - logged, not reported as a failure/error, the same way a
                                // @Disabled test is reported via executionSkipped() above, except that
                                // testStarted()/testFinished() must still fire here since the test already
                                // started (see SuiteReportSink.Outcome's javadoc for why this reports Passed).
                                testExecutionResult.getThrowable().ifPresent(throwable ->
                                        Debug.logInfo("[JUNIT] ABORTED: " + testIdentifier.getDisplayName()
                                                + " (" + testClass.getName() + ") - " + throwable.getMessage(), MODULE));
                                ReportingSupport.dispatch(sinks, sink -> sink.testFinished(testClass.getName(), name, elapsed, Outcome.passed()));
                                return;
                            }
                            Outcome outcome = testExecutionResult.getThrowable()
                                    .map(throwable -> throwable instanceof AssertionError
                                            ? Outcome.failure(throwable.getMessage(), throwable.getClass().getName(),
                                                    ReportingSupport.stackTraceOf(throwable))
                                            : Outcome.error(throwable))
                                    .orElseGet(Outcome::passed);
                            ReportingSupport.dispatch(sinks, sink -> sink.testFinished(testClass.getName(), name, elapsed, outcome));
                        } finally {
                            ThreadContext.remove(TEST_CASE_MDC_KEY);
                            JupiterTestExtension.CURRENT_TEST_METHOD_NAME.remove();
                        }
                    }
                });
            } catch (Throwable t) {
                reportClassExecutionFailure(t);
            } finally {
                ThreadContext.remove(TEST_CASE_MDC_KEY);
                JupiterTestExtension.CURRENT_DELEGATOR.remove();
                JupiterTestExtension.CURRENT_DISPATCHER.remove();
                JupiterTestExtension.CURRENT_TEST_PARAMS.remove();
                JupiterTestExtension.CURRENT_TEST_METHOD_NAME.remove();
            }
        }

        /**
         * Without this, a container-level failure - a static {@literal @}BeforeAll (or any other
         * class-level setup JUnit 5 runs before its children) throwing - is silently discarded:
         * executionFinished() above returns before doing anything for a non-test identifier, so the
         * FAILED/ABORTED result JUnit 5 reports once, on the container, never reaches any sink. That
         * would leave every {@literal @}Test method in the class never individually started, and this
         * class contributing nothing to the report, for a class whose tests never actually ran.
         * Reported as a synthetic entry attributed to this class via the {@code classname} argument
         * to testStarted()/testFinished() - {@code name} itself is the fixed literal
         * "initializationError", not derived from the class name - the same "reporting handle only"
         * pattern JupiterLeafTest used to serve, now going straight through the sink instead of a
         * fake junit.framework.Test.
         */
        private void reportContainerFailure(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
            TestExecutionResult.Status status = testExecutionResult.getStatus();
            if (status != TestExecutionResult.Status.FAILED && status != TestExecutionResult.Status.ABORTED) {
                return;
            }
            String name = "initializationError";
            Throwable throwable = testExecutionResult.getThrowable()
                    .orElseGet(() -> new AssertionError("Container '" + testIdentifier.getDisplayName()
                            + "' reported " + status + " with no throwable"));
            ReportingSupport.dispatch(sinks, sink -> sink.testStarted(testClass.getName(), name));
            ReportingSupport.dispatch(sinks, sink -> sink.testFinished(testClass.getName(), name, 0, Outcome.error(throwable)));
        }

        /**
         * Reports an exception that escaped launcher.execute() itself - see run()'s javadoc - as a
         * synthetic entry attributed to this class via the {@code classname} argument to
         * testStarted()/testFinished(); {@code name} itself is the fixed literal
         * "classExecutionError", not derived from the class name.
         *
         * <p>Package-private rather than private so JupiterClassRunnerTest can exercise it directly
         * without needing to force a real JUnit Platform internal failure.
         * @param throwable the exception that escaped launcher.execute()
         */
        void reportClassExecutionFailure(Throwable throwable) {
            Debug.logError(throwable, "[JUNIT] Class '" + testClass.getName() + "' failed to execute: " + throwable, MODULE);
            String name = "classExecutionError";
            ReportingSupport.dispatch(sinks, sink -> sink.testStarted(testClass.getName(), name));
            ReportingSupport.dispatch(sinks, sink -> sink.testFinished(testClass.getName(), name, 0, Outcome.error(throwable)));
        }

        /**
         * For a plain @Test method, the base name is the method name itself ("testCreateExample",
         * "shouldCreateExample") - a report reader needs that to go straight from the report to the
         * source, and losing it behind a @DisplayName's prose was a real readability regression once
         * @DisplayName started being used. getLegacyReportingName() supplies that raw
         * "methodName(ParamType1, ParamType2)" signature unconditionally, regardless of any
         * @DisplayName on the method (Jupiter's MethodBasedTestDescriptor overrides
         * getLegacyReportingBaseName() as final, always returning it). The parameter types aren't
         * meaningful to a report reader here (they're always the JupiterTestExtension-injected
         * Delegator/LocalDispatcher, or CSV-provided arguments already visible elsewhere in the name),
         * so they're stripped. When the method also carries a real @DisplayName - detected by
         * getDisplayName() differing from the same method-name shape JUnit 5's default Standard
         * display name generator would otherwise produce - that text is appended after " - ", e.g.
         * "testTestEntityModels - Test entity models". A method with no @DisplayName is unaffected:
         * getDisplayName() falls back to that identical default shape, so the two sides match and only
         * the bare method name is used.
         *
         * <p>For an @ParameterizedTest invocation, getDisplayName()'s shape is controlled entirely by the
         * developer's @ParameterizedTest(name=...) pattern - the index can be anywhere, or absent - so it
         * can't be used to detect that an identifier is a parameterized invocation. getLegacyReportingName()
         * is used for that detection instead: it reliably ends in "[index]" for every parameterized
         * invocation regardless of the display-name pattern in use. Once detected, the bare "[index]" is
         * replaced with the invocation's own getDisplayName() text (e.g. "[1] exampleTypeId=CONTRIVED"
         * becomes "shouldCreateExampleAcrossTypes[exampleTypeId=CONTRIVED]"), so each row is identifiable
         * without needing to click into it.
         *
         * <p>Unlike the old JupiterLeafTest-based reporting, this name carries no class-name prefix - the
         * real class name is now reported separately as {@code classname} (see {@link #run()}), since
         * SuiteReportSink's testStarted()/testFinished() take a classname/name pair directly instead of a
         * single junit.framework.Test whose class was always the shared JupiterLeafTest.
         */
        private String reportingName(TestIdentifier testIdentifier) {
            String legacyReportingName = testIdentifier.getLegacyReportingName().replaceAll("\\([^)]*\\)", "");
            Matcher indexSuffix = INDEX_SUFFIX.matcher(legacyReportingName);
            if (indexSuffix.matches()) {
                String invocationLabel = testIdentifier.getDisplayName().replaceFirst("^\\[\\d+]\\s*", "");
                return indexSuffix.group(1) + "[" + invocationLabel + "]";
            }
            String methodName = legacyReportingName;
            String displayNameText = testIdentifier.getDisplayName().replaceAll("\\([^)]*\\)$", "");
            return displayNameText.equals(methodName) ? methodName : methodName + " - " + displayNameText;
        }
    }
}
