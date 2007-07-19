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
package org.ofbiz.testtools;

import java.util.Enumeration;

import junit.framework.*;

import org.ofbiz.base.container.Container;
import org.ofbiz.base.container.ContainerException;
import org.ofbiz.base.util.Debug;

/**
 * A Container implementation to run the tests configured through this testtools stuff.
 */
public class TestRunContainer implements Container {

    public static final String module = TestRunContainer.class.getName();
    protected TestResult results = null;
    protected String configFile = null;
    protected String component = null;
    protected String testCase = null;

    /**
     * @see org.ofbiz.base.container.Container#init(java.lang.String[], java.lang.String)
     */
    public void init(String[] args, String configFile) {
        this.configFile = configFile;
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                String argument = args[i];
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
                    if ("case".equalsIgnoreCase(argumentName)) {
                        this.testCase = argumentVal;
                    }
                }
            }
        }
    }

    public boolean start() throws ContainerException {
        //ContainerConfig.Container jc = ContainerConfig.getContainer("junit-container", configFile);

        // get the tests to run
        JunitSuiteWrapper jsWrapper = new JunitSuiteWrapper(component, testCase);
        if (jsWrapper.getAllTestList().size() == 0) {
            throw new ContainerException("No tests found (" + component + " / " + testCase + ")");
        }

        // load the tests into the suite
        TestSuite suite = new TestSuite();
        jsWrapper.populateTestSuite(suite);

        // holder for the results
        results = new TestResult();
        results.addListener(new JunitListener());

        // run the tests
        suite.run(results);

        // dispay the results
        Debug.log("[JUNIT] Pass: " + results.wasSuccessful() + " | # Tests: " + results.runCount() + " | # Failed: " +
                results.failureCount() + " # Errors: " + results.errorCount(), module);
        if (Debug.importantOn()) {
            Debug.log("[JUNIT] ----------------------------- ERRORS ----------------------------- [JUNIT]", module);
            Enumeration err = results.errors();
            if (!err.hasMoreElements()) {
                Debug.log("None");
            } else {
                while (err.hasMoreElements()) {
                    Debug.log("--> " + err.nextElement(), module);
                }
            }
            Debug.log("[JUNIT] ------------------------------------------------------------------ [JUNIT]", module);
            Debug.log("[JUNIT] ---------------------------- FAILURES ---------------------------- [JUNIT]", module);
            Enumeration fail = results.failures();
            if (!fail.hasMoreElements()) {
                Debug.log("None");
            } else {
                while (fail.hasMoreElements()) {
                    Debug.log("--> " + fail.nextElement(), module);
                }
            }
            Debug.log("[JUNIT] ------------------------------------------------------------------ [JUNIT]", module);
        }

        return true;
    }

    public void stop() throws ContainerException {
        try {
            Thread.sleep(2000);
        } catch (Exception e) {
            throw new ContainerException(e);
        }
    }

    class JunitListener implements TestListener {

        public void addError(Test test, Throwable throwable) {
            Debug.logWarning(throwable, "[JUNIT (error)] - " + test.getClass().getName() + " : " + throwable.toString(), module);
        }

        public void addFailure(Test test, AssertionFailedError assertionFailedError) {
            Debug.logWarning("[JUNIT (failure)] - " + test.getClass().getName() + " : " + assertionFailedError.getMessage(), module);
        }

        public void endTest(Test test) {
            Debug.logInfo("[JUNIT] : " + test.getClass().getName() + " finished.", module);
        }

        public void startTest(Test test) {
           Debug.logInfo("[JUNIT] : " + test.getClass().getName() + " starting...", module);
        }
    }
}
