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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.service.LocalDispatcher;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Injects the per-suite Delegator/LocalDispatcher that ModelTestSuite already builds for JUnit 3
 * test-cases into Jupiter test classes run through JupiterTestSuite.
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
 * <p>JupiterTestSuite.run() executes tests synchronously on the calling thread. This is pinned, not
 * merely assumed of Jupiter's default: the discovery request built in JupiterTestSuite's
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
 * it doesn't. The Delegator/LocalDispatcher injected here are the single instances
 * ModelTestSuite.prepareTest() builds once for the whole {@code <test-suite>}, shared across every
 * test method and every Jupiter/JUnit 3 class in that suite, exactly as JUnit 3 test-cases already
 * share them today. TestRunContainer rolls back all accumulated mutations once, after the entire
 * suite finishes - not per test method - so a test can observe data created by an earlier test in
 * the same suite, and ordering between test-cases in the suite's testdef XML can matter.
 */
public class JupiterTestExtension implements ParameterResolver, TestInstancePostProcessor, ExecutionCondition {

    /** Read by build.gradle's `test` task ({@code excludeTags}) and by {@link JunitJupiterTest}. */
    public static final String INTEGRATION_TAG = "jupiterIntegration";

    static final ThreadLocal<Delegator> CURRENT_DELEGATOR = new ThreadLocal<>();
    static final ThreadLocal<LocalDispatcher> CURRENT_DISPATCHER = new ThreadLocal<>();

    /**
     * Disables classes/methods run outside the ofbiz --test container instead of letting them reach
     * postProcessTestInstance()/resolveParameter() (or, for JupiterTestHelper-based classes, a
     * NullPointerException from a getDelegator()/getDispatcher() caller). Both ThreadLocals are
     * checked rather than just one so a class relying on only a Delegator or only a
     * LocalDispatcher isn't disabled by a coincidentally-unset ThreadLocal it never actually reads
     * - in practice JupiterTestSuite.run() arms both together.
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
                + "bridge is only populated on the thread that calls JupiterTestSuite.run(), and only for classes "
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
     * Adapts a JUnit 5 Jupiter test class to the JUnit 3 junit.framework.Test contract so it can run
     * inside TestRunContainer/ModelTestSuite side-by-side with junit-test-suite (JUnit 3) test-cases,
     * sharing the same suite-level Delegator/LocalDispatcher and reporting through the same
     * TestResult/TestListener/XML pipeline TestRunContainer already has. Execution goes through the
     * real JUnit Platform Launcher, so @Test/@ParameterizedTest/@Disabled behave exactly as they
     * would under `./gradlew test`. The discovery request pins the default method orderer to
     * {@code MethodOrderer.OrderAnnotation}, replacing Jupiter's own unordered default so a class's
     * execution order is always whatever its {@code @Order} annotations say (or unspecified only
     * among methods that declare none) rather than an unpredictable per-run default. It also pins
     * {@code junit.jupiter.execution.parallel.enabled} to {@code false}, so every test method
     * executes on the calling thread regardless of any system property or properties file that
     * might otherwise request parallelism - see the class-level javadoc above for why that matters
     * to the ThreadLocal bridge.
     */
    static final class JupiterTestSuite implements Test {

        private static final String MODULE = JupiterTestSuite.class.getName();
        private static final Pattern INDEX_SUFFIX = Pattern.compile("(.*)\\[\\d+\\]$");

        private final Class<?> testClass;
        private final Launcher launcher;
        private final LauncherDiscoveryRequest request;
        private final int testCaseCount;
        // Stored, not applied immediately: ModelTestSuite.prepareTest() calls setDelegator()/
        // setDispatcher() once for every JupiterTestSuite in a <test-suite>, before any of them run.
        // Pushing straight to the shared ThreadLocal there would let one instance's post-run cleanup
        // (see run() below) wipe state a sibling instance still needs. Applying them in run() instead
        // means each instance re-arms its own state right before it executes.
        private Delegator delegator;
        private LocalDispatcher dispatcher;

        JupiterTestSuite(Class<?> testClass) {
            this.testClass = testClass;
            this.launcher = LauncherFactory.create();
            this.request = LauncherDiscoveryRequestBuilder.request()
                    .selectors(selectClass(testClass))
                    .configurationParameter(
                            "junit.jupiter.testmethod.order.default",
                            "org.junit.jupiter.api.MethodOrderer$OrderAnnotation")
                    .configurationParameter("junit.jupiter.execution.parallel.enabled", "false")
                    .build();
            this.testCaseCount = (int) launcher.discover(request).countTestIdentifiers(
                    id -> id.isTest() && !isStaticallyDisabled(id));
        }

        /**
         * Excludes {@literal @}Disabled methods/classes from countTestCases() so it agrees with the
         * TestResult.runCount() TestRunContainer actually logs: run() below never calls startTest()
         * for a disabled test (see executionSkipped()), so runCount() never counts it either. Discovery
         * alone can't see every reason a test might not run - an ExecutionCondition like this class's own
         * evaluateExecutionCondition() is only evaluated at execution time - but a bare {@literal @}Disabled
         * is visible right here via reflection on the MethodSource, which covers the common case cheaply.
         * Any resolution failure falls back to "not disabled" (matches the old, over-counting behavior)
         * rather than risk hiding a test that actually runs.
         */
        private static boolean isStaticallyDisabled(TestIdentifier identifier) {
            return identifier.getSource()
                    .filter(MethodSource.class::isInstance)
                    .map(MethodSource.class::cast)
                    .map(JupiterTestSuite::isDisabledMethodSource)
                    .orElse(false);
        }

        private static boolean isDisabledMethodSource(MethodSource source) {
            try {
                Class<?> testClass = Class.forName(source.getClassName());
                if (testClass.isAnnotationPresent(Disabled.class)) {
                    return true;
                }
                for (Method method : testClass.getDeclaredMethods()) {
                    if (method.getName().equals(source.getMethodName()) && method.isAnnotationPresent(Disabled.class)) {
                        return true;
                    }
                }
            } catch (ClassNotFoundException e) {
                return false;
            }
            return false;
        }

        void setDelegator(Delegator delegator) {
            this.delegator = delegator;
        }

        void setDispatcher(LocalDispatcher dispatcher) {
            this.dispatcher = dispatcher;
        }

        @Override
        public int countTestCases() {
            return testCaseCount;
        }

        @Override
        public void run(TestResult result) {
            JupiterTestExtension.CURRENT_DELEGATOR.set(delegator);
            JupiterTestExtension.CURRENT_DISPATCHER.set(dispatcher);
            Map<String, Test> leafTests = new HashMap<>();
            try {
                launcher.execute(request, new TestExecutionListener() {
                    @Override
                    public void executionStarted(TestIdentifier testIdentifier) {
                        if (testIdentifier.isTest()) {
                            Test leaf = new JupiterLeafTest(reportingName(testIdentifier, testClass), testClass.getName());
                            leafTests.put(testIdentifier.getUniqueId(), leaf);
                            result.startTest(leaf);
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
                            reportContainerFailure(testIdentifier, testExecutionResult, result);
                            return;
                        }
                        Test leaf = leafTests.get(testIdentifier.getUniqueId());
                        if (testExecutionResult.getStatus() == TestExecutionResult.Status.ABORTED) {
                            // A JUnit 5 Assumptions.assumeTrue/assumeFalse failure: a deliberate skip, not a
                            // defect, so it is reported the same way executionSkipped() reports a @Disabled
                            // test - logged, not routed through addFailure()/addError() - even though, unlike
                            // a @Disabled test, startTest() already ran for it and endTest() still must too.
                            testExecutionResult.getThrowable().ifPresent(throwable ->
                                    Debug.logInfo("[JUNIT] ABORTED: " + testIdentifier.getDisplayName()
                                            + " (" + testClass.getName() + ") - " + throwable.getMessage(), MODULE));
                            result.endTest(leaf);
                            return;
                        }
                        testExecutionResult.getThrowable().ifPresent(throwable -> {
                            if (throwable instanceof AssertionError) {
                                result.addFailure(leaf, new AssertionFailedError(throwable.getMessage()));
                            } else {
                                result.addError(leaf, throwable);
                            }
                        });
                        result.endTest(leaf);
                    }
                });
            } finally {
                JupiterTestExtension.CURRENT_DELEGATOR.remove();
                JupiterTestExtension.CURRENT_DISPATCHER.remove();
            }
        }

        /**
         * Without this, a container-level failure - a static {@literal @}BeforeAll (or any other
         * class-level setup JUnit 5 runs before its children) throwing - is silently discarded:
         * executionFinished() above returns before doing anything for a non-test identifier, so the
         * FAILED/ABORTED result JUnit 5 reports once, on the container, never reaches
         * result.addError()/addFailure(). That would leave every {@literal @}Test method in the class
         * never individually started, results.wasSuccessful() still true, and testIntegration reporting
         * full success for a class whose tests never actually ran. Reported as a synthetic leaf (mirroring
         * JupiterLeafTest's own "reporting handle only" pattern below) since TestResult has no native
         * concept of a class-level failure with no associated test case.
         */
        private void reportContainerFailure(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult, TestResult result) {
            TestExecutionResult.Status status = testExecutionResult.getStatus();
            if (status != TestExecutionResult.Status.FAILED && status != TestExecutionResult.Status.ABORTED) {
                return;
            }
            Test leaf = new JupiterLeafTest(testClass.getSimpleName() + ".initializationError", testClass.getName());
            Throwable throwable = testExecutionResult.getThrowable()
                    .orElseGet(() -> new AssertionError("Container '" + testIdentifier.getDisplayName()
                            + "' reported " + status + " with no throwable"));
            result.startTest(leaf);
            result.addError(leaf, throwable);
            result.endTest(leaf);
        }

        /**
         * getLegacyReportingName() reports plain @Test methods as "methodName(ParamType1, ParamType2)"
         * and @ParameterizedTest invocations as "methodName(ParamType1, ParamType2)[index]" - the
         * parameter types come from JUnit 5's own default display name, not from anything meaningful to
         * a report reader here (they're always the JupiterTestExtension-injected Delegator/LocalDispatcher,
         * or CSV-provided arguments already visible elsewhere in the name). Stripping them leaves plain
         * JUnit 3 test methods ("testCreateExample") and Jupiter ones ("shouldCreateExample") looking
         * consistent. For @ParameterizedTest invocations, the bare "[index]" from getLegacyReportingName()
         * is replaced with the test's own @ParameterizedTest(name=...) display text (e.g. "[1] exampleTypeId=CONTRIVED"
         * becomes "shouldCreateExampleAcrossTypes[exampleTypeId=CONTRIVED]"), so each row is identifiable
         * without needing to click into it.
         *
         * <p>Prefixed with the test class's simple name ("AutoAcctgAdminTests.testXxx") because that class
         * is otherwise invisible in the JUnit XML/HTML report: every Jupiter-sourced {@code <testcase>} in a
         * suite shares one {@code classname}, {@code JupiterLeafTest}'s own class
         * ({@code org.apache.ofbiz.testtools.JupiterTestExtension$JupiterTestSuite$JupiterLeafTest}), since
         * Ant's {@code JUnitVersionHelper.getTestCaseClassName()} derives {@code classname} from
         * {@code test.getClass().getName()} with no hook to override it - the only exception is a test
         * object that literally is {@code junit.framework.JUnit4TestCaseFacade}, whose package-private
         * constructor rules out subclassing it from this package. Two different Jupiter classes bundled into
         * the same {@code <test-suite>} can therefore define same-named methods (a real collision:
         * {@code AutoAcctgAdminTests} and {@code AutoAcctgAgreementTests} both have a
         * {@code testAddPaymentMethodTypeGlAssignment}) and be indistinguishable in the report without this
         * prefix, since {@code classname} can't carry it and bare {@code name} previously didn't either.
         */
        private static String reportingName(TestIdentifier testIdentifier, Class<?> testClass) {
            String withoutParamTypes = testIdentifier.getLegacyReportingName().replaceAll("\\([^)]*\\)", "");
            Matcher indexSuffix = INDEX_SUFFIX.matcher(withoutParamTypes);
            String bareName = withoutParamTypes;
            if (indexSuffix.matches()) {
                String invocationLabel = testIdentifier.getDisplayName().replaceFirst("^\\[\\d+]\\s*", "");
                bareName = indexSuffix.group(1) + "[" + invocationLabel + "]";
            }
            return testClass.getSimpleName() + "." + bareName;
        }

        /**
         * Extends junit.framework.TestCase (not a bare Test implementation) so Ant's
         * XMLJUnitResultFormatter resolves the reporting name through JUnitVersionHelper's
         * {@code instanceof TestCase} branch: a Method handle fixed once, at class-init, to
         * {@code TestCase.class.getMethod("getName")} - the stable, public JUnit 3 API this file
         * already depends on - rather than the duck-typed {@code t.getClass().getMethod("getName")}
         * fallback used for arbitrary Test implementors. That fallback is why this class doesn't need
         * to be public: the resolved Method's declaring class is TestCase, so reflection.invoke()
         * succeeds regardless of this nested class's own visibility.
         */
        static final class JupiterLeafTest extends TestCase {
            private final String className;

            JupiterLeafTest(String name, String className) {
                super(name);
                this.className = className;
            }

            @Override
            public void run(TestResult result) {
                throw new UnsupportedOperationException("JupiterLeafTest is a reporting handle only, it cannot be run directly");
            }

            @Override
            public String toString() {
                return getName() + "(" + className + ")";
            }
        }

    }

}
