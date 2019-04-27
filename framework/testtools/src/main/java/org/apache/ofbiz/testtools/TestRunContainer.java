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

    public static final String module = TestRunContainer.class.getName();
    public static final String logDir = "runtime/logs/test-results/";

    private String name;
    private JunitSuiteWrapper jsWrapper;

    @Override
    public void init(List<StartupCommand> ofbizCommands, String name, String configFile) throws ContainerException {
        this.name = name;
        new File(logDir).mkdir();

        // get the test properties passed by the user in the command line
        Map<String, String> testProps = ofbizCommands.stream()
                .filter(command -> command.getName().equals(StartupCommandUtil.StartupOption.TEST.getName()))
                .map(command -> command.getProperties())
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
            JunitXmlListener xml = createJunitXmlListener(suite, logDir);
            TestResult results = new TestResult();
            results.addListener(new JunitListener());
            results.addListener(xml);

            // test
            xml.startTestSuite(test);
            suite.run(results);
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

    private void setLoggerLevel(String logLevel) {
        if (logLevel != null) {
            int selectedLogLevel = Debug.getLevelFromString(logLevel);

            for(int level = Debug.ALWAYS; level <= Debug.FATAL; level++) {
                boolean isOn = level >= selectedLogLevel;
                Debug.set(level, isOn);
            }
        }
    }

    private JunitSuiteWrapper prepareJunitSuiteWrapper(Map<String,String> testProps) throws ContainerException {
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

    private void logTestSuiteResults(TestSuite suite, TestResult results) {
        Debug.logInfo("[JUNIT] Results for test suite: " + suite.getName(), module);
        Debug.logInfo("[JUNIT] Pass: " + results.wasSuccessful() + " | # Tests: " + results.runCount() + " | # Failed: " +
                results.failureCount() + " # Errors: " + results.errorCount(), module);
        if (Debug.importantOn() && !results.wasSuccessful()) {
            Debug.logInfo("[JUNIT] ----------------------------- ERRORS ----------------------------- [JUNIT]", module);
            logErrorsOrFailures(results.errors());
            Debug.logInfo("[JUNIT] ------------------------------------------------------------------ [JUNIT]", module);

            Debug.logInfo("[JUNIT] ---------------------------- FAILURES ---------------------------- [JUNIT]", module);
            logErrorsOrFailures(results.failures());
            Debug.logInfo("[JUNIT] ------------------------------------------------------------------ [JUNIT]", module);
        }
    }

    private void logErrorsOrFailures(Enumeration<TestFailure> errorsOrFailures) {
        if (!errorsOrFailures.hasMoreElements()) {
            Debug.logInfo("None", module);
        } else {
            while (errorsOrFailures.hasMoreElements()) {
                TestFailure testFailure = errorsOrFailures.nextElement();
                Debug.logInfo("--> " + testFailure, module);
                Debug.logInfo(testFailure.trace(), module);
            }
        }
    }

    class JunitXmlListener extends XMLJUnitResultFormatter {

        Map<String, Long> startTimes = new HashMap<>();

        public JunitXmlListener(OutputStream out) {
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
            Debug.logWarning(throwable, "[JUNIT (error)] - " + getTestName(test) + " : " + throwable.toString(), module);
        }

        @Override
        public void addFailure(Test test, AssertionFailedError assertionFailedError) {
            Debug.logWarning("[JUNIT (failure)] - " + getTestName(test) + " : " + assertionFailedError.getMessage(), module);
        }

        @Override
        public void endTest(Test test) {
            Debug.logInfo("[JUNIT] : " + getTestName(test) + " finished.", module);
        }

        @Override
        public void startTest(Test test) {
           Debug.logInfo("[JUNIT] : " + getTestName(test) + " starting...", module);
        }

        private String getTestName(Test test) {
            if (test instanceof TestCase) {
                return ((TestCase)test).getName();
            } else {
                return test.getClass().getName();
            }

        }
    }
}
