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

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestFailure;
import junit.framework.TestListener;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;
import org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter;
import org.apache.ofbiz.base.container.Container;
import org.apache.ofbiz.base.container.ContainerException;
import org.apache.ofbiz.base.container.StartupCommandToArgsAdapter;
import org.apache.ofbiz.base.start.StartupCommand;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.Delegator;

/**
 * A Container implementation to run the tests configured through this testtools stuff.
 */
public class TestRunContainer implements Container {

    public static final String module = TestRunContainer.class.getName();
    public static final String logDir = "runtime/logs/test-results/";

    protected String configFile = null;
    protected String component = null;
    protected String suiteName = null;
    protected String testCase = null;
    protected String logLevel = null;

    private String name;

    @Override
    public void init(List<StartupCommand> ofbizCommands, String name, String configFile) {
        // TODO: remove this hack and provide clean implementation
        String[] args = StartupCommandToArgsAdapter.adaptStartupCommandsToLoaderArgs(ofbizCommands);

        this.name = name;
        this.configFile = configFile;
        if (args != null) {
            for (String argument : args) {
                // arguments can prefix w/ a '-'. Just strip them off
                if (argument.startsWith("-")) {
                    int subIdx = 1;
                    if (argument.startsWith("--")) {
                        subIdx = 2;
                    }
                    argument = argument.substring(subIdx);
                }

                // parse the arguments
                if (argument.indexOf("=") != -1) {
                    String argumentName = argument.substring(0, argument.indexOf("="));
                    String argumentVal = argument.substring(argument.indexOf("=") + 1);

                    if ("component".equalsIgnoreCase(argumentName)) {
                        this.component = argumentVal;
                    }
                    if ("suitename".equalsIgnoreCase(argumentName)) {
                        this.suiteName = argumentVal;
                    }
                    if ("case".equalsIgnoreCase(argumentName)) {
                        this.testCase = argumentVal;
                    }
                    if ("loglevel".equalsIgnoreCase(argumentName)) {
                        this.logLevel = argumentVal;
                    }
                }
            }
        }

        // make sure the log dir exists
        File dir = new File(logDir);
        if (!dir.exists())
            dir.mkdir();
    }

    public boolean start() throws ContainerException {
        // configure log4j output logging
        if (logLevel != null) {
            int llevel = Debug.getLevelFromString(logLevel);

            for (int v = 0; v < 9; v++) {
                if (v < llevel) {
                    Debug.set(v, false);
                } else {
                    Debug.set(v, true);
                }
            }
        }

        // get the tests to run
        JunitSuiteWrapper jsWrapper = new JunitSuiteWrapper(component, suiteName, testCase);
        if (jsWrapper.getAllTestList().size() == 0) {
            throw new ContainerException("No tests found (" + component + " / " + suiteName + " / " + testCase + ")");
        }

        boolean failedRun = false;
        for (ModelTestSuite modelSuite: jsWrapper.getModelTestSuites()) {
            Delegator testDelegator = modelSuite.getDelegator();
            TestSuite suite = modelSuite.makeTestSuite();
            JUnitTest test = new JUnitTest();
            test.setName(suite.getName());

            // create the XML logger
            JunitXmlListener xml;
            try {
                xml = new JunitXmlListener(new FileOutputStream(logDir + suite.getName() + ".xml"));
            } catch (FileNotFoundException e) {
                throw new ContainerException(e);
            }

            // per-suite results
            TestResult results = new TestResult();
            results.addListener(new JunitListener());
            results.addListener(xml);

            // add the suite to the xml listener
            xml.startTestSuite(test);
            // run the tests
            suite.run(results);
            test.setCounts(results.runCount(), results.failureCount(), results.errorCount());
            // rollback all entity operations performed by the delegator
            testDelegator.rollback();
            xml.endTestSuite(test);

            if (!results.wasSuccessful()) {
                failedRun = true;
            }

            // display the results
            Debug.logInfo("[JUNIT] Results for test suite: " + suite.getName(), module);
            Debug.logInfo("[JUNIT] Pass: " + results.wasSuccessful() + " | # Tests: " + results.runCount() + " | # Failed: " +
                    results.failureCount() + " # Errors: " + results.errorCount(), module);
            if (Debug.importantOn() && !results.wasSuccessful()) {
                Debug.logInfo("[JUNIT] ----------------------------- ERRORS ----------------------------- [JUNIT]", module);
                Enumeration<?> err = results.errors();
                if (!err.hasMoreElements()) {
                    Debug.logInfo("None", module);
                } else {
                    while (err.hasMoreElements()) {
                        Object error = err.nextElement();
                        Debug.logInfo("--> " + error, module);
                        if (error instanceof TestFailure) {
                            Debug.logInfo(((TestFailure) error).trace(), module);
                        }
                    }
                }
                Debug.logInfo("[JUNIT] ------------------------------------------------------------------ [JUNIT]", module);
                Debug.logInfo("[JUNIT] ---------------------------- FAILURES ---------------------------- [JUNIT]", module);
                Enumeration<?> fail = results.failures();
                if (!fail.hasMoreElements()) {
                    Debug.logInfo("None", module);
                } else {
                    while (fail.hasMoreElements()) {
                        Object failure = fail.nextElement();
                        Debug.logInfo("--> " + failure, module);
                        if (failure instanceof TestFailure) {
                            Debug.logInfo(((TestFailure) failure).trace(), module);
                        }
                    }
                }
                Debug.logInfo("[JUNIT] ------------------------------------------------------------------ [JUNIT]", module);
            }
        }

        if (failedRun) {
            throw new ContainerException("Test run was unsuccessful");
        }
        return true;
    }

    public void stop() throws ContainerException {
    }

    public String getName() {
        return name;
    }

    class JunitXmlListener extends XMLJUnitResultFormatter {

        Map<String, Long> startTimes = new HashMap<String, Long>();

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

        public void addError(Test test, Throwable throwable) {
            Debug.logWarning(throwable, "[JUNIT (error)] - " + getTestName(test) + " : " + throwable.toString(), module);
        }

        public void addFailure(Test test, AssertionFailedError assertionFailedError) {
            Debug.logWarning("[JUNIT (failure)] - " + getTestName(test) + " : " + assertionFailedError.getMessage(), module);
        }

        public void endTest(Test test) {
            Debug.logInfo("[JUNIT] : " + getTestName(test) + " finished.", module);
        }

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
