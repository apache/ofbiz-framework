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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.ThreadContext;
import org.apache.ofbiz.base.container.Container;
import org.apache.ofbiz.base.container.ContainerException;
import org.apache.ofbiz.base.start.StartupCommand;
import org.apache.ofbiz.base.start.StartupCommandUtil;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.testtools.SuiteEntry.Junit3Entry;
import org.apache.ofbiz.testtools.SuiteEntry.JupiterEntry;
import org.apache.ofbiz.testtools.SuiteReportSink.Outcome;

import junit.framework.TestResult;

/**
 * A Container implementation to run the tests configured through this testtools stuff.
 */
public class TestRunContainer implements Container {

    private static final String MODULE = TestRunContainer.class.getName();
    public static final String LOG_DIR = "runtime/logs/test-results/";

    private String name;
    private JunitSuiteWrapper jsWrapper;
    private String methodName;

    @Override
    public void init(List<StartupCommand> ofbizCommands, String name, String configFile) throws ContainerException {
        this.name = name;
        new File(LOG_DIR).mkdir();

        // get the test properties passed by the user in the command line
        Map<String, String> testProps = ofbizCommands.stream()
                .filter(command -> command.getName().equals(StartupCommandUtil.StartupOption.TEST.getName()))
                .map(StartupCommand::getProperties)
                .findFirst().get();

        // set selected log level if passed by user
        setLoggerLevel(testProps.get("loglevel"));

        this.methodName = normalizeMethodName(testProps.get("method"));
        validateMethodRequiresCase(this.methodName, testProps.get("case"));

        this.jsWrapper = prepareJunitSuiteWrapper(testProps);
    }

    @Override
    public boolean start() throws ContainerException {
        // Validated for every resolved suite up front, before any of them run - case= given without
        // suitename= can resolve to suites in more than one testdef document; without this pre-pass,
        // a validation failure on the second (or later) suite would only surface after the first has
        // already executed and written its report, undermining the "method= without a valid target
        // fails before anything runs" guarantee for that (rare, but real) multi-suite case. Guarded on
        // methodName != null so a plain gradlew test/testIntegration run (no method= at all) doesn't
        // pay for this pass or risk it changing behavior - getPreparedTestList() is idempotent to call
        // again in the loop below, but a prepare-time throw from it would otherwise now abort the
        // whole run before any suite executes, instead of after however many already had, which is a
        // real (if narrow) behavior change for a run that never touched method= in the first place.
        if (methodName != null) {
            for (ModelTestSuite modelSuite: jsWrapper.getModelTestSuites()) {
                validateMethodAppliesToSuite(methodName, modelSuite.getSuiteName(), modelSuite.getPreparedTestList());
            }
        }

        boolean failedRun = false;
        for (ModelTestSuite modelSuite: jsWrapper.getModelTestSuites()) {
            String suiteName = modelSuite.getSuiteName();
            List<SuiteEntry> preparedTestList = modelSuite.getPreparedTestList();

            SuiteXmlReportWriter xmlSink = createXmlReportWriter(suiteName);
            SuiteReportLogger logSink = new SuiteReportLogger();
            xmlSink.startSuite(suiteName);
            logSink.startSuite(suiteName);

            try {
                runSuiteEntries(preparedTestList, modelSuite.getDelegator(),
                        modelSuite.getDispatcher(), Map.of(), methodName, xmlSink, logSink);
            } catch (Throwable t) {
                // Everything inside runSuiteEntries() is per-entry isolated already: a JUnit 3 test's
                // own exception is always caught by TestCase.runBare()/TestResult's own
                // protected-invocation machinery, and JupiterClassRunner.run() catches anything
                // escaping its own launcher.execute() call. This is a last-resort net for anything
                // that still escapes the loop itself - without it, that would abort every remaining
                // testdef suite in this loop, not just the one that hit the problem.
                reportSuiteExecutionFailure(suiteName, t, xmlSink, logSink);
            }

            modelSuite.getDelegator().rollback(); // rollback all entity operations
            xmlSink.endSuite();
            logSink.endSuite();

            failedRun = !xmlSink.wasSuccessful() || failedRun;
        }

        if (failedRun) {
            throw new ContainerException("Test run was unsuccessful");
        }
        return true;
    }

    @Override
    public void stop() {
    }

    @Override
    public String getName() {
        return name;
    }

    static void runSuiteEntries(List<SuiteEntry> entries, Delegator delegator, LocalDispatcher dispatcher,
            SuiteReportSink... sinks) {
        runSuiteEntries(entries, delegator, dispatcher, Map.of(), sinks);
    }

    /**
     * @param testParams caller-supplied parameter overrides for Jupiter entries (empty for a plain
     *     {@code gradlew test}/{@code testIntegration} run - see TestRunServices for the API-triggered path)
     * @param sinks where to report results
     */
    static void runSuiteEntries(List<SuiteEntry> entries, Delegator delegator, LocalDispatcher dispatcher,
            Map<String, Object> testParams, SuiteReportSink... sinks) {
        runSuiteEntries(entries, delegator, dispatcher, testParams, null, sinks);
    }

    /**
     * Runs one suite's ordered SuiteEntry list, JUnit 3 entries through junit.framework.TestResult
     * (translated via Junit3ResultBridge) and Jupiter entries through JupiterTestExtension.JupiterClassRunner
     * directly, both feeding the same sink(s) in declared order - so a suite mixing both kinds of
     * entries produces one merged report with no separate merge step.
     *
     * <p>Package-private and static, taking its dependencies as plain parameters, so
     * TestRunContainerTest can exercise it directly without a full ofbiz --test container bootstrap.
     * @param entries the suite's prepared, ordered test entries
     * @param delegator the suite's Delegator, shared by every entry
     * @param dispatcher the suite's LocalDispatcher, shared by every entry
     * @param testParams caller-supplied parameter overrides for Jupiter entries (empty for a plain
     *     {@code gradlew test}/{@code testIntegration} run - see TestRunServices for the API-triggered path)
     * @param methodName when non-null, scopes every JupiterEntry in this call to exactly this
     *     {@literal @}Test/{@literal @}ParameterizedTest method instead of running the whole class -
     *     supplied by the {@code ofbiz --test method=} CLI path (see start() below) and by
     *     TestRunServices' {@code testMethodName}-scoped API-triggered path; null for a plain
     *     {@code gradlew test}/{@code testIntegration} run and for an API-triggered run that omits
     *     testMethodName, both of which run whole classes
     * @param sinks where to report results
     */
    static void runSuiteEntries(List<SuiteEntry> entries, Delegator delegator, LocalDispatcher dispatcher,
            Map<String, Object> testParams, String methodName, SuiteReportSink... sinks) {
        TestResult junit3Result = new TestResult();
        junit3Result.addListener(new Junit3ResultBridge(sinks));
        for (SuiteEntry entry : entries) {
            try {
                if (entry instanceof Junit3Entry junit3Entry) {
                    junit3Entry.test().run(junit3Result);
                } else if (entry instanceof JupiterEntry jupiterEntry) {
                    new JupiterTestExtension.JupiterClassRunner(
                            jupiterEntry.testClass(), delegator, dispatcher, testParams, methodName, sinks).run();
                } else {
                    // SuiteEntry is sealed permits Junit3Entry, JupiterEntry, so this is unreachable today -
                    // but Java 17 doesn't support exhaustive switch over sealed types without preview
                    // features, so this explicit throw is the substitute: a future third variant fails loudly
                    // here instead of being silently skipped.
                    throw new IllegalStateException("Unknown SuiteEntry type: " + entry.getClass());
                }
            } finally {
                // Net for JUnit 3 test engines (ServiceTest/SimpleMethodTest/EntityXmlAssertTest) whose
                // own run(TestResult) overrides can let an unchecked exception escape before reaching
                // Junit3ResultBridge.endTest() - without this, testCase would stay armed on this thread
                // for every subsequent log line until the next test overwrites it. Also correct (a no-op
                // clearing an already-cleared key) for the two paths that already clear it themselves.
                ThreadContext.remove(JupiterTestExtension.TEST_CASE_MDC_KEY);
            }
        }
    }

    private static void setLoggerLevel(String logLevel) {
        if (logLevel != null) {
            int selectedLogLevel = Debug.getLevelFromString(logLevel);

            for (int level = Debug.ALWAYS; level <= Debug.FATAL; level++) {
                boolean isOn = level >= selectedLogLevel;
                Debug.set(level, isOn);
            }
        }
    }

    /**
     * Reports an exception that escaped runSuiteEntries() itself as a synthetic suite-level error
     * instead of letting it propagate out of start()'s for loop - see the try/catch around
     * runSuiteEntries() above for why that would otherwise abort every remaining testdef suite in the
     * run. Reported through the same sinks a normal test's testStarted()/testFinished() calls use, so
     * it shows up in the suite's report and a sink's own wasSuccessful()-equivalent state correctly
     * reflects it, rather than this suite silently contributing zero tests to the run.
     *
     * <p>Package-private rather than private so TestRunContainerTest can exercise it directly without
     * needing a full ofbiz --test container bootstrap.
     * @param suiteName the suite that failed to execute
     * @param throwable the exception that escaped
     * @param sinks where to report the synthetic failure
     */
    static void reportSuiteExecutionFailure(String suiteName, Throwable throwable, SuiteReportSink... sinks) {
        Debug.logError(throwable, "[JUNIT] Suite '" + suiteName + "' failed to execute: " + throwable, MODULE);
        String name = "suiteExecutionError";
        for (SuiteReportSink sink : sinks) {
            sink.testStarted(suiteName, name);
            sink.testFinished(suiteName, name, 0, Outcome.error(throwable));
        }
    }

    private static JunitSuiteWrapper prepareJunitSuiteWrapper(Map<String, String> testProps) throws ContainerException {
        String component = testProps.get("component");
        String suiteName = testProps.get("suitename");
        String testCase = testProps.get("case");

        JunitSuiteWrapper jsWrapper = new JunitSuiteWrapper(component, suiteName, testCase);
        if (jsWrapper.getAllTestList().isEmpty()) {
            throw new ContainerException("No tests found (" + component + " / " + suiteName + " / " + testCase + ")");
        }

        return jsWrapper;
    }

    /**
     * Normalizes a blank {@code --test method=} value (e.g. the trailing-{@code =} shape
     * {@code --test method=} produces) to null, so it's indistinguishable from method= not having
     * been given at all - a blank string would otherwise pass both validateMethodRequiresCase() and
     * validateMethodAppliesToSuite() (neither checks for blank, only null) and then fail deep inside
     * JUnit Platform's own precondition check as an opaque suiteExecutionError instead of this
     * feature's own clean ContainerException.
     *
     * <p>Package-private and static so TestRunContainerTest can exercise it directly without a full
     * ofbiz --test container bootstrap.
     * @param rawMethodName the raw --test method= value from the command line, or null if not given
     * @return rawMethodName unchanged if non-null and non-blank, otherwise null
     */
    static String normalizeMethodName(String rawMethodName) {
        return (rawMethodName == null || rawMethodName.isBlank()) ? null : rawMethodName;
    }

    /**
     * Validates that {@code --test method=} was not given without {@code --test case=} - method=
     * scopes a single case's resolved class down to one @Test method, so it's meaningless without
     * case= to say which class that is.
     *
     * <p>Package-private and static so TestRunContainerTest can exercise it directly without a full
     * ofbiz --test container bootstrap.
     * @param methodName the --test method= value, or null if not given
     * @param caseName the --test case= value, or null if not given
     * @throws ContainerException if methodName is non-null and caseName is null
     */
    static void validateMethodRequiresCase(String methodName, String caseName) throws ContainerException {
        if (methodName != null && caseName == null) {
            throw new ContainerException("--test method=" + methodName + " requires --test case=<case-name> to "
                    + "also be specified - method= scopes a single case's class down to one @Test method, so "
                    + "case= is needed to identify which class that is.");
        }
    }

    /**
     * Validates that {@code --test method=} (when given) has something to apply to - a resolved
     * suite with no JupiterEntry at all (a service-test/entity-xml/JUnit 3 case) means case= named
     * something method= can never apply to. The data-load prerequisite
     * ModelTestSuite.selectTestCaseElements() may have auto-included is always a Junit3Entry, so its
     * presence alone never satisfies this check.
     *
     * <p>Package-private and static so TestRunContainerTest can exercise it directly without a full
     * ofbiz --test container bootstrap.
     * @param methodName the --test method= value, or null if not given
     * @param suiteName the resolved suite's name, used only for the exception message
     * @param preparedTestList the resolved suite's prepared entries
     * @throws ContainerException if methodName is non-null and no entry in preparedTestList is a JupiterEntry
     */
    // This checks "at least one JupiterEntry", not "exactly one": every testdef file in this repo
    // resolves case= to at most one jupiter-test-suite entry today, but test-suite.xsd's test-group
    // element technically allows more than one jupiter-test-suite child - if a future testdef file
    // used that shape, method= would be applied to every one of them via runSuiteEntries(), silently
    // failing whichever one doesn't happen to declare the named method.
    static void validateMethodAppliesToSuite(String methodName, String suiteName, List<SuiteEntry> preparedTestList)
            throws ContainerException {
        if (methodName != null && preparedTestList.stream().noneMatch(JupiterEntry.class::isInstance)) {
            throw new ContainerException("--test method=" + methodName + " was given, but the resolved case= "
                    + "did not include a jupiter-test-suite entry in suite '" + suiteName + "' - method= only "
                    + "applies to Jupiter (JUnit 5) test classes.");
        }
    }

    private static SuiteXmlReportWriter createXmlReportWriter(String suiteName) throws ContainerException {
        try {
            return new SuiteXmlReportWriter(new FileOutputStream(LOG_DIR + suiteName + ".xml"));
        } catch (FileNotFoundException e) {
            throw new ContainerException(e);
        }
    }
}
