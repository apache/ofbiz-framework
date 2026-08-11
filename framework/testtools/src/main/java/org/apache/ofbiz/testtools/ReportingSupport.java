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
import java.util.List;
import java.util.function.Consumer;

/**
 * Small helpers shared by the SuiteReportSink producers/callers - {@link Junit3ResultBridge},
 * {@link SuiteXmlReportWriter}, and {@link JupiterTestExtension.JupiterClassRunner} - extracted here
 * once all three were near-verbatim duplicates of the same two methods.
 */
final class ReportingSupport {

    private ReportingSupport() {
    }

    /**
     * Renders a throwable's full stack trace as text, the same shape {@code Throwable.printStackTrace()}
     * writes to a stream.
     * @param throwable the throwable to render
     * @return the throwable's stack trace text
     */
    static String stackTraceOf(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    /**
     * Invokes {@code action} against every sink in {@code sinks}, in order.
     * @param sinks the sinks to fan out to
     * @param action the per-sink action to invoke
     */
    static void dispatch(List<SuiteReportSink> sinks, Consumer<SuiteReportSink> action) {
        sinks.forEach(action);
    }
}
