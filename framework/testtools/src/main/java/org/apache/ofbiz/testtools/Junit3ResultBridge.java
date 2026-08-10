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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

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
 */
final class Junit3ResultBridge implements TestListener {

    private final List<SuiteReportSink> sinks;
    private final Map<Test, Long> startTimes = new IdentityHashMap<>();
    private final Set<Test> finished = new HashSet<>();

    Junit3ResultBridge(SuiteReportSink... sinks) {
        this.sinks = List.of(sinks);
    }

    @Override
    public void startTest(Test test) {
        startTimes.put(test, System.currentTimeMillis());
        dispatch(sink -> sink.testStarted(classnameOf(test), nameOf(test)));
    }

    @Override
    public void addFailure(Test test, AssertionFailedError error) {
        report(test, Outcome.failure(error.getMessage(), error.getClass().getName(), stackTraceOf(error)));
    }

    @Override
    public void addError(Test test, Throwable error) {
        report(test, Outcome.error(error));
    }

    @Override
    public void endTest(Test test) {
        // JUnit 3's dispatch order is always startTest -> [addFailure|addError] -> endTest, so a test
        // reaching here already marked finished was a failure/error already reported by one of the two
        // methods above - only a still-unmarked test here is a genuine pass.
        if (!finished.contains(test)) {
            report(test, Outcome.passed());
        }
    }

    private void report(Test test, Outcome outcome) {
        finished.add(test);
        long elapsed = System.currentTimeMillis() - startTimes.getOrDefault(test, System.currentTimeMillis());
        String classname = classnameOf(test);
        String name = nameOf(test);
        dispatch(sink -> sink.testFinished(classname, name, elapsed, outcome));
    }

    private void dispatch(Consumer<SuiteReportSink> action) {
        sinks.forEach(action);
    }

    private static String classnameOf(Test test) {
        return test.getClass().getName();
    }

    private static String nameOf(Test test) {
        return test instanceof TestCase ? ((TestCase) test).getName() : "unknown";
    }

    private static String stackTraceOf(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }
}
