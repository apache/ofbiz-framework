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
            String suiteName = modelSuite.getSuiteName();
            SuiteXmlReportWriter xmlSink = createXmlReportWriter(suiteName);
            SuiteReportLogger logSink = new SuiteReportLogger();
            xmlSink.startSuite(suiteName);
            logSink.startSuite(suiteName);

            try {
                runSuiteEntries(modelSuite.getPreparedTestList(), modelSuite.getDelegator(),
                        modelSuite.getDispatcher(), xmlSink, logSink);
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
     * @param sinks where to report results
     */
    static void runSuiteEntries(List<SuiteEntry> entries, Delegator delegator, LocalDispatcher dispatcher,
            SuiteReportSink... sinks) {
        TestResult junit3Result = new TestResult();
        junit3Result.addListener(new Junit3ResultBridge(sinks));
        for (SuiteEntry entry : entries) {
            if (entry instanceof Junit3Entry junit3Entry) {
                junit3Entry.test().run(junit3Result);
            } else if (entry instanceof JupiterEntry jupiterEntry) {
                new JupiterTestExtension.JupiterClassRunner(jupiterEntry.testClass(), delegator, dispatcher, sinks).run();
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

    private static SuiteXmlReportWriter createXmlReportWriter(String suiteName) throws ContainerException {
        try {
            return new SuiteXmlReportWriter(new FileOutputStream(LOG_DIR + suiteName + ".xml"));
        } catch (FileNotFoundException e) {
            throw new ContainerException(e);
        }
    }
}
