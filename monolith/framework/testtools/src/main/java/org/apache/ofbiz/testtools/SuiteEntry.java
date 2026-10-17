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

import junit.framework.Test;

/**
 * One test-case/test-group child parsed from a {@code <test-suite>}'s testdef XML, ready to execute.
 * {@link Junit3Entry} wraps a real {@code junit.framework.Test}, built the same way it always has been
 * for {@code junit-test-suite}/{@code service-test}/{@code simple-method-test}/
 * {@code entity-xml(-assert)}/{@code webdriver-test} elements. {@link JupiterEntry} carries just the
 * Jupiter test class itself, with no eager JUnit Platform discovery -
 * {@link JupiterTestExtension.JupiterClassRunner} discovers and executes it directly when
 * {@link TestRunContainer} reaches it in {@link ModelTestSuite#getPreparedTestList()}'s declared order.
 */
sealed interface SuiteEntry permits SuiteEntry.Junit3Entry, SuiteEntry.JupiterEntry {

    /**
     * A JUnit 3 test-case, executed via {@code junit.framework.TestResult} through {@link Junit3ResultBridge}.
     * @param test the JUnit 3 test
     */
    record Junit3Entry(Test test) implements SuiteEntry {
    }

    /**
     * A Jupiter test class, executed via the real JUnit Platform {@code Launcher} through
     * {@link JupiterTestExtension.JupiterClassRunner}.
     * @param testClass the Jupiter test class
     */
    record JupiterEntry(Class<?> testClass) implements SuiteEntry {
    }
}
