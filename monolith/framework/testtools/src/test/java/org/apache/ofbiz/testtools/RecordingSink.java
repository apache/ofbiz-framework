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

/**
 * A simple in-memory SuiteReportSink that records every call it receives, for assertions in
 * Junit3ResultBridgeTest, JupiterClassRunnerTest, and TestRunContainerTest. Not itself a test class.
 */
final class RecordingSink implements SuiteReportSink {

    //ALLOW PUBLIC FIELDS
    final List<String> startSuiteCalls = new ArrayList<>();
    final List<String> testStartedCalls = new ArrayList<>();
    final List<FinishedCall> testFinishedCalls = new ArrayList<>();
    int endSuiteCallCount;
    //FORBID PUBLIC FIELDS

    @Override
    public void startSuite(String suiteName) {
        startSuiteCalls.add(suiteName);
    }

    @Override
    public void testStarted(String classname, String name) {
        testStartedCalls.add(classname + "#" + name);
    }

    @Override
    public void testFinished(String classname, String name, long elapsedMillis, Outcome outcome) {
        testFinishedCalls.add(new FinishedCall(classname, name, outcome));
    }

    @Override
    public void endSuite() {
        endSuiteCallCount++;
    }

    record FinishedCall(String classname, String name, Outcome outcome) {
        String key() {
            return classname + "#" + name;
        }
    }
}
