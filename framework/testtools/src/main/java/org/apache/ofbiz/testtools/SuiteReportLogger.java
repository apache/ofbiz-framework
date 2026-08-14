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

import java.util.ArrayList;
import java.util.List;

import org.apache.ofbiz.base.util.Debug;

/**
 * Logs each test-case's start/finish and an end-of-suite pass/fail summary via Debug.log*, exactly as
 * the old TestRunContainer.JunitListener inner class plus its logTestSuiteResults()/
 * logErrorsOrFailures() static helper methods did. Renamed and absorbed both responsibilities into one
 * SuiteReportSink implementation since both now key off the same per-test events.
 *
 * <p>One deliberate, log-only behavior change from the original: the end-of-suite recap lists each
 * failure/error's message only, avoiding redundant stack trace logging - the full stack trace is
 * already logged once in real time by testFinished() when each failure/error first happens.
 */
final class SuiteReportLogger implements SuiteReportSink {

    private static final String MODULE = SuiteReportLogger.class.getName();

    private String suiteName;
    private int testCount;
    private int failureCount;
    private int errorCount;
    private final List<String> failureRecap = new ArrayList<>();
    private final List<String> errorRecap = new ArrayList<>();

    @Override
    public void startSuite(String suiteName) {
        this.suiteName = suiteName;
    }

    @Override
    public void testStarted(String classname, String name) {
        Debug.logInfo("[JUNIT] : " + label(classname, name) + " starting...", MODULE);
    }

    @Override
    public void testFinished(String classname, String name, long elapsedMillis, Outcome outcome) {
        testCount++;
        String label = label(classname, name);
        if (outcome instanceof Outcome.Failure failure) {
            failureCount++;
            Debug.logWarning("[JUNIT (failure)] - " + label + " : " + failure.message(), MODULE);
            Debug.logWarning("[JUNIT (failure)] - " + label + " stack trace:\n" + failure.stackTrace(), MODULE);
            failureRecap.add(label + " : " + failure.message());
        } else if (outcome instanceof Outcome.Error error) {
            errorCount++;
            Debug.logWarning(error.throwable(), "[JUNIT (error)] - " + label + " : " + error.throwable(), MODULE);
            errorRecap.add(label + " : " + error.throwable());
        }
        Debug.logInfo("[JUNIT] : " + label + " finished.", MODULE);
    }

    @Override
    public void endSuite() {
        boolean wasSuccessful = failureCount == 0 && errorCount == 0;
        Debug.logInfo("[JUNIT] Results for test suite: " + suiteName, MODULE);
        Debug.logInfo("[JUNIT] Pass: " + wasSuccessful + " | # Tests: " + testCount + " | # Failed: "
                + failureCount + " # Errors: " + errorCount, MODULE);
        if (Debug.importantOn() && !wasSuccessful) {
            Debug.logInfo("[JUNIT] ----------------------------- ERRORS ----------------------------- [JUNIT]", MODULE);
            logRecap(errorRecap);
            Debug.logInfo("[JUNIT] ------------------------------------------------------------------ [JUNIT]", MODULE);

            Debug.logInfo("[JUNIT] ---------------------------- FAILURES ---------------------------- [JUNIT]", MODULE);
            logRecap(failureRecap);
            Debug.logInfo("[JUNIT] ------------------------------------------------------------------ [JUNIT]", MODULE);
        }
    }

    private void logRecap(List<String> recap) {
        if (recap.isEmpty()) {
            Debug.logInfo("None", MODULE);
        } else {
            recap.forEach(line -> Debug.logInfo("--> " + line, MODULE));
        }
    }

    /**
     * Derives a "SimpleClassName.name" label from a fully-qualified classname and a test name, the
     * same idea test-reports.gradle's {@code simpleClassName} helper applies on the HTML side, so two
     * same-named methods in different classes in the same suite remain distinguishable in the Debug
     * log too.
     * @param classname the test's declaring class name, possibly fully-qualified
     * @param name the test's method/case name
     * @return "{@code SimpleClassName.name}"
     */
    private static String label(String classname, String name) {
        int lastDot = classname.lastIndexOf('.');
        String simpleClassName = lastDot >= 0 ? classname.substring(lastDot + 1) : classname;
        return simpleClassName + "." + name;
    }
}
