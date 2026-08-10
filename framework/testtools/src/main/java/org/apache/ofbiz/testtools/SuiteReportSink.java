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

/**
 * Destination for one {@code <test-suite>}'s worth of test execution events, fed synchronously, in
 * real execution order, by whichever engine is currently executing a {@link SuiteEntry} -
 * {@link Junit3ResultBridge} for {@link SuiteEntry.Junit3Entry}, {@link
 * JupiterTestExtension.JupiterClassRunner} for {@link SuiteEntry.JupiterEntry}. {@link TestRunContainer}
 * feeds the same sink instance(s) to both engines for a given {@code <test-suite>}, in the order its
 * entries are declared in testdef XML, so a suite mixing {@code <junit-test-suite>} and
 * {@code <jupiter-test-suite>} entries produces one merged report with no separate merge step.
 *
 * <p>Two implementations exist: {@link SuiteXmlReportWriter} (replaces Ant's
 * {@code XMLJUnitResultFormatter}) and {@link SuiteReportLogger} (renamed from the old
 * {@code JunitListener}).
 */
interface SuiteReportSink {

    /**
     * Called once, before any test in the suite runs.
     * @param suiteName the testdef {@code suite-name}
     */
    void startSuite(String suiteName);

    /**
     * Called when a single test-case begins executing.
     * @param classname the test's declaring class name
     * @param name the test's method/case name (or a display-name-derived variant - see
     *     {@link JupiterTestExtension.JupiterClassRunner}'s reporting logic)
     */
    void testStarted(String classname, String name);

    /**
     * Called when a single test-case finishes, with its outcome.
     * @param classname the test's declaring class name
     * @param name the test's method/case name
     * @param elapsedMillis how long the test took to run
     * @param outcome the result
     */
    void testFinished(String classname, String name, long elapsedMillis, Outcome outcome);

    /**
     * Called once, after every test in the suite has finished (or after a suite/class-level failure
     * that prevented some of them from running).
     */
    void endSuite();

    /**
     * A single test-case's result. No SKIPPED variant: a JUnit 5 {@code Assumptions.assumeTrue}/
     * {@code assumeFalse}-aborted test is reported as {@link Passed} (logged separately by whichever
     * sink cares to, not distinguished in the XML/HTML report) - the same behavior the JUnit 3
     * impersonation this replaces already had, kept unchanged deliberately (see the 2026-08-10
     * design's Non-goals).
     */
    sealed interface Outcome permits Outcome.Passed, Outcome.Failure, Outcome.Error {

        /**
         * A passed outcome.
         * @return a Passed instance
         */
        static Outcome passed() {
            return new Passed();
        }

        /**
         * A failed-assertion outcome.
         * @param message the assertion message, may be null
         * @param type the failure's throwable class name
         * @param stackTrace the failure's full stack trace text
         * @return a Failure instance
         */
        static Outcome failure(String message, String type, String stackTrace) {
            return new Failure(message, type, stackTrace);
        }

        /**
         * An unexpected-exception outcome.
         * @param throwable the throwable that escaped the test
         * @return an Error instance
         */
        static Outcome error(Throwable throwable) {
            return new Error(throwable);
        }

        /** A test that passed. */
        record Passed() implements Outcome {
        }

        /** A test that failed an assertion. */
        record Failure(String message, String type, String stackTrace) implements Outcome {
        }

        /** A test that threw an unexpected exception. */
        record Error(Throwable throwable) implements Outcome {
        }
    }
}
