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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.service.LocalDispatcher;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestResult;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Adapts a JUnit 5 Jupiter test class to the JUnit 3 junit.framework.Test contract so it can run
 * inside TestRunContainer/ModelTestSuite side-by-side with junit-test-suite (JUnit 3) test-cases,
 * sharing the same suite-level Delegator/LocalDispatcher and reporting through the same
 * TestResult/TestListener/XML pipeline TestRunContainer already has. Execution goes through the
 * real JUnit Platform Launcher, so @Test/@ParameterizedTest/@Disabled behave exactly as they
 * would under `./gradlew test`.
 */
public final class JupiterTestSuite implements Test {

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

    public JupiterTestSuite(Class<?> testClass) {
        this.testClass = testClass;
        this.launcher = LauncherFactory.create();
        this.request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(testClass))
                .build();
        this.testCaseCount = (int) launcher.discover(request).countTestIdentifiers(TestIdentifier::isTest);
    }

    public void setDelegator(Delegator delegator) {
        this.delegator = delegator;
    }

    public void setDispatcher(LocalDispatcher dispatcher) {
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
                        Test leaf = new JupiterLeafTest(reportingName(testIdentifier), testClass.getName());
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
                        return;
                    }
                    Test leaf = leafTests.get(testIdentifier.getUniqueId());
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
     */
    private static String reportingName(TestIdentifier testIdentifier) {
        String withoutParamTypes = testIdentifier.getLegacyReportingName().replaceAll("\\([^)]*\\)", "");
        Matcher indexSuffix = INDEX_SUFFIX.matcher(withoutParamTypes);
        if (!indexSuffix.matches()) {
            return withoutParamTypes;
        }
        String invocationLabel = testIdentifier.getDisplayName().replaceFirst("^\\[\\d+]\\s*", "");
        return indexSuffix.group(1) + "[" + invocationLabel + "]";
    }

    /**
     * Public (not private): Ant's JUnitVersionHelper.getTestCaseName() reflectively invokes
     * getName() on this object from outside this package, without calling setAccessible() first.
     * A non-public nested class would make that Method.invoke() throw IllegalAccessException,
     * which Ant swallows silently, so the XML report would just show "unknown" as the name.
     */
    public static final class JupiterLeafTest implements Test {
        private final String name;
        private final String className;

        JupiterLeafTest(String name, String className) {
            this.name = name;
            this.className = className;
        }

        @Override
        public int countTestCases() {
            return 1;
        }

        @Override
        public void run(TestResult result) {
            throw new UnsupportedOperationException("JupiterLeafTest is a reporting handle only, it cannot be run directly");
        }

        /**
         * Not part of the Test interface: Ant's XMLJUnitResultFormatter (JUnitVersionHelper.getTestCaseName)
         * looks this up by reflection for any non-TestCase Test implementation, falling back to "unknown"
         * if absent. Kept as the bare legacy reporting name (no class prefix) to match how JUnit 3 test
         * methods are named in the same report.
         */
        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name + "(" + className + ")";
        }
    }

}
