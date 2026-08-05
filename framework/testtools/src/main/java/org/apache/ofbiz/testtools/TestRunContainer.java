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
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.container.Container;
import org.apache.ofbiz.base.container.ContainerException;
import org.apache.ofbiz.base.start.StartupCommand;
import org.apache.ofbiz.base.start.StartupCommandUtil;
import org.apache.ofbiz.base.util.Debug;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;
import org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestFailure;
import junit.framework.TestListener;
import junit.framework.TestResult;
import junit.framework.TestSuite;

/**
 * A Container implementation to run the tests configured through this testtools stuff.
 */
public class TestRunContainer implements Container {

    private static final String MODULE = TestRunContainer.class.getName();
    public static final String LOG_DIR = "runtime/logs/test-results/";

    private String name;
    private JunitSuiteWrapper jsWrapper;

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

        this.jsWrapper = prepareJunitSuiteWrapper(testProps);
    }

    @Override
    public boolean start() throws ContainerException {
        boolean failedRun = false;
        for (ModelTestSuite modelSuite: jsWrapper.getModelTestSuites()) {

            // prepare
            TestSuite suite = modelSuite.makeTestSuite();
            JUnitTest test = new JUnitTest(suite.getName());
            JunitXmlListener xml = createJunitXmlListener(suite, LOG_DIR);
            TestResult results = new TestResult();
            results.addListener(new JunitListener());
            results.addListener(xml);

            // test
            xml.startTestSuite(test);
            try {
                suite.run(results);
            } catch (Throwable t) {
                // An individual test's exception is always caught by TestCase.runBare()/TestResult's
                // own protected-invocation machinery and reported as that one test's error - it can
                // never take down sibling suites. JupiterTestSuite.run() (the JUnit 3 bridge for
                // Jupiter classes) doesn't have that same guarantee: anything escaping its
                // launcher.execute() call - a JUnitException from a discovery/engine-registration problem,
                // a PreconditionViolationException, or a bug in its own TestExecutionListener callback code
                // - propagates straight out of suite.run() here. Without this catch, that would abort every
                // remaining testdef suite in this loop, not just the one that hit the problem.
                reportSuiteExecutionFailure(suite, results, t);
            }
            test.setCounts(results.runCount(), results.failureCount(), results.errorCount());
            modelSuite.getDelegator().rollback(); // rollback all entity operations
            xml.endTestSuite(test);

            logTestSuiteResults(suite, results);

            failedRun = !results.wasSuccessful() ? true : failedRun;
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
     * Reports an exception that escaped suite.run() itself as a synthetic suite-level error instead of
     * letting it propagate out of start()'s for loop - see the try/catch around suite.run() above for
     * why that would otherwise abort every remaining testdef suite in the run, not just this one.
     * Reported through the same TestResult/listener pipeline (JunitListener, the XML formatter) a normal
     * addError() would use, so it shows up in the suite's report and results.wasSuccessful() correctly
     * flips to false, rather than this suite silently contributing zero tests to the run.
     *
     * <p>Package-private rather than private so TestRunContainerTest can exercise it directly without
     * needing a full ofbiz --test container bootstrap.
     */
    static void reportSuiteExecutionFailure(TestSuite suite, TestResult results, Throwable throwable) {
        Debug.logError(throwable, "[JUNIT] Suite '" + suite.getName() + "' failed to execute: " + throwable, MODULE);
        Test failureMarker = new TestCase(suite.getName() + ".suiteExecutionError") {
            @Override
            public void run(TestResult result) {
                throw new UnsupportedOperationException("reporting handle only, cannot be run directly");
            }
        };
        results.startTest(failureMarker);
        results.addError(failureMarker, throwable);
        results.endTest(failureMarker);
    }

    private static JunitSuiteWrapper prepareJunitSuiteWrapper(Map<String, String> testProps) throws ContainerException {
        String component = testProps.get("component");
        String suiteName = testProps.get("suitename");
        String testCase = testProps.get("case");

        JunitSuiteWrapper jsWrapper = new JunitSuiteWrapper(component, suiteName, testCase);
        if (jsWrapper.getAllTestList().size() == 0) {
            throw new ContainerException("No tests found (" + component + " / " + suiteName + " / " + testCase + ")");
        }

        return jsWrapper;
    }

    private JunitXmlListener createJunitXmlListener(TestSuite suite, String logDir) throws ContainerException {
        try {
            return new JunitXmlListener(new FileOutputStream(logDir + suite.getName() + ".xml"));
        } catch (FileNotFoundException e) {
            throw new ContainerException(e);
        }
    }

    private static void logTestSuiteResults(TestSuite suite, TestResult results) {
        Debug.logInfo("[JUNIT] Results for test suite: " + suite.getName(), MODULE);
        Debug.logInfo("[JUNIT] Pass: " + results.wasSuccessful() + " | # Tests: " + results.runCount() + " | # Failed: "
                + results.failureCount() + " # Errors: " + results.errorCount(), MODULE);
        if (Debug.importantOn() && !results.wasSuccessful()) {
            Debug.logInfo("[JUNIT] ----------------------------- ERRORS ----------------------------- [JUNIT]", MODULE);
            logErrorsOrFailures(results.errors());
            Debug.logInfo("[JUNIT] ------------------------------------------------------------------ [JUNIT]", MODULE);

            Debug.logInfo("[JUNIT] ---------------------------- FAILURES ---------------------------- [JUNIT]", MODULE);
            logErrorsOrFailures(results.failures());
            Debug.logInfo("[JUNIT] ------------------------------------------------------------------ [JUNIT]", MODULE);
        }
    }

    private static void logErrorsOrFailures(Enumeration<TestFailure> errorsOrFailures) {
        if (!errorsOrFailures.hasMoreElements()) {
            Debug.logInfo("None", MODULE);
        } else {
            while (errorsOrFailures.hasMoreElements()) {
                TestFailure testFailure = errorsOrFailures.nextElement();
                Debug.logInfo("--> " + testFailure, MODULE);
                Debug.logInfo(testFailure.trace(), MODULE);
            }
        }
    }

    class JunitXmlListener extends XMLJUnitResultFormatter {

        private Map<String, Long> startTimes = new HashMap<>();

        JunitXmlListener(OutputStream out) {
            this.setOutput(out);
        }

        @Override
        public void startTestSuite(JUnitTest suite) {
            startTimes.put(suite.getName(), System.currentTimeMillis());
            super.startTestSuite(suite);
        }

        @Override
        public void endTestSuite(JUnitTest suite) throws BuildException {
            long startTime = startTimes.get(suite.getName());
            suite.setRunTime((System.currentTimeMillis() - startTime));
            super.endTestSuite(suite);
        }
    }

    class JunitListener implements TestListener {

        @Override
        public void addError(Test test, Throwable throwable) {
            Debug.logWarning(throwable, "[JUNIT (error)] - " + getTestName(test) + " : " + throwable.toString(), MODULE);
        }

        @Override
        public void addFailure(Test test, AssertionFailedError assertionFailedError) {
            Debug.logWarning("[JUNIT (failure)] - " + getTestName(test) + " : " + assertionFailedError.getMessage(), MODULE);
        }

        @Override
        public void endTest(Test test) {
            Debug.logInfo("[JUNIT] : " + getTestName(test) + " finished.", MODULE);
        }

        @Override
        public void startTest(Test test) {
            Debug.logInfo("[JUNIT] : " + getTestName(test) + " starting...", MODULE);
        }

        private String getTestName(Test test) {
            if (test instanceof TestCase) {
                return ((TestCase) test).getName();
            } else {
                return test.getClass().getName();
            }

        }
    }
}
