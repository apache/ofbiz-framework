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

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.ThreadContext;
import org.apache.ofbiz.testtools.SuiteReportSink.Outcome;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestListener;

/**
 * Adapts junit.framework.TestListener callbacks - fired by TestResult while a
 * {@link SuiteEntry.Junit3Entry} runs - into {@link SuiteReportSink} calls. This is the only place in
 * the JUnit-3-independence design that still implements a junit.framework.* interface: JUnit 3
 * execution itself is unchanged (still {@code junit.framework.TestSuite}/{@code TestResult}), only its
 * reporting output is redirected here, into the same sink(s) {@link JupiterTestExtension.JupiterClassRunner}
 * reports Jupiter entries to.
 *
 * <p>Some OFBiz JUnit-3 test engines - {@link ServiceTest}, {@link SimpleMethodTest},
 * {@link EntityXmlAssertTest} - override {@code run(TestResult)} and can call
 * {@code result.addFailure(this, ...)}/{@code result.addError(this, ...)} more than once for the same
 * {@code Test} object (once per accumulated error message), before calling {@code result.endTest(this)}
 * exactly once. {@code testFinished()} must therefore be dispatched exactly once per test, at
 * {@code endTest()} time, not from addFailure()/addError() directly - {@link #outcomes} records at most
 * one {@link Outcome} per test (the first reported failure/error; later ones for the same
 * already-recorded test are deliberately dropped, since {@link Outcome} carries only one
 * failure/error per test by design) and {@link #endTest(Test)} is the single place that reads it back
 * out and reports.
 */
final class Junit3ResultBridge implements TestListener {

    private final List<SuiteReportSink> sinks;
    private final Map<Test, Long> startTimes = new IdentityHashMap<>();
    private final Map<Test, Outcome> outcomes = new IdentityHashMap<>();

    // Must match the %X{testCase} reference in framework/base/config/log4j2.xml's logPattern.
    private static final String TEST_CASE_MDC_KEY = "testCase";

    Junit3ResultBridge(SuiteReportSink... sinks) {
        this.sinks = List.of(sinks);
    }

    @Override
    public void startTest(Test test) {
        startTimes.put(test, System.currentTimeMillis());
        ThreadContext.put(TEST_CASE_MDC_KEY, test.getClass().getSimpleName() + "#" + nameOf(test));
        ReportingSupport.dispatch(sinks, sink -> sink.testStarted(classnameOf(test), nameOf(test)));
    }

    @Override
    public void addFailure(Test test, AssertionFailedError error) {
        outcomes.putIfAbsent(test,
                Outcome.failure(error.getMessage(), error.getClass().getName(), ReportingSupport.stackTraceOf(error)));
    }

    @Override
    public void addError(Test test, Throwable error) {
        outcomes.putIfAbsent(test, Outcome.error(error));
    }

    @Override
    public void endTest(Test test) {
        // JUnit 3's dispatch order is always startTest -> [addFailure|addError]* -> endTest, with
        // endTest() called exactly once per test regardless of how many addFailure()/addError() calls
        // preceded it - so this is the single dispatch point for testFinished().
        try {
            Outcome outcome = outcomes.remove(test);
            report(test, outcome != null ? outcome : Outcome.passed());
        } finally {
            ThreadContext.remove(TEST_CASE_MDC_KEY);
        }
    }

    private void report(Test test, Outcome outcome) {
        long elapsed = System.currentTimeMillis() - startTimes.getOrDefault(test, System.currentTimeMillis());
        String classname = classnameOf(test);
        String name = nameOf(test);
        ReportingSupport.dispatch(sinks, sink -> sink.testFinished(classname, name, elapsed, outcome));
    }

    private static String classnameOf(Test test) {
        return test.getClass().getName();
    }

    private static String nameOf(Test test) {
        return test instanceof TestCase ? ((TestCase) test).getName() : "unknown";
    }
}
