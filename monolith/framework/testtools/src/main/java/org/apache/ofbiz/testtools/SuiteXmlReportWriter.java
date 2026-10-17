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

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.ofbiz.base.util.Debug;

/**
 * Writes one {@code <test-suite>}'s worth of test-case results as JUnit-style XML, matching the exact
 * element/attribute shape Ant's {@code XMLJUnitResultFormatter} used to produce
 * ({@code <testsuite>/<testcase>/<failure>/<error>}), so {@code createTestReport}/
 * {@code createFramedTestReport} (test-reports.gradle's {@code parseSuiteXml()}) need zero changes.
 * Both JUnit 3 ({@link Junit3ResultBridge}) and Jupiter ({@link JupiterTestExtension.JupiterClassRunner})
 * entries feed the same instance, in real execution order, so a {@code <test-suite>} mixing both kinds
 * of entries produces one merged file with test-cases in declared order.
 *
 * <p>Deliberately writes no {@code <skipped/>} element for any test-case - see
 * {@link SuiteReportSink.Outcome}'s javadoc for why it has no SKIPPED variant.
 */
class SuiteXmlReportWriter implements SuiteReportSink {

    private static final String MODULE = SuiteXmlReportWriter.class.getName();
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final OutputStream out;
    private final List<TestCaseResult> testCases = new ArrayList<>();
    private String suiteName;
    private String timestamp;
    private long suiteStartMillis;
    private int failureCount;
    private int errorCount;

    SuiteXmlReportWriter(OutputStream out) {
        this.out = out;
    }

    @Override
    public void startSuite(String suiteName) {
        this.suiteName = suiteName;
        this.timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        this.suiteStartMillis = System.currentTimeMillis();
    }

    @Override
    public void testStarted(String classname, String name) {
        // Nothing to write yet: a <testcase> element only has enough information (elapsed time,
        // outcome) once testFinished() supplies it - matching Ant's own formatter, which only ever
        // appended a <testcase> from its endTest()/formatError() methods, never from startTest().
    }

    @Override
    public void testFinished(String classname, String name, long elapsedMillis, Outcome outcome) {
        testCases.add(new TestCaseResult(classname, name, elapsedMillis, outcome));
        if (outcome instanceof Outcome.Failure) {
            failureCount++;
        } else if (outcome instanceof Outcome.Error) {
            errorCount++;
        }
    }

    @Override
    public void endSuite() {
        double totalSeconds = (System.currentTimeMillis() - suiteStartMillis) / 1000.0;
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
        xml.append("<testsuite errors=\"").append(errorCount)
                .append("\" failures=\"").append(failureCount)
                .append("\" hostname=\"").append(escapeXml(hostname()))
                .append("\" name=\"").append(escapeXml(suiteName))
                .append("\" skipped=\"0\" tests=\"").append(testCases.size())
                .append("\" time=\"").append(totalSeconds)
                .append("\" timestamp=\"").append(timestamp)
                .append("\">\n");
        xml.append("  <properties />\n");
        for (TestCaseResult testCase : testCases) {
            appendTestCase(xml, testCase);
        }
        xml.append("</testsuite>\n");
        writeAndClose(xml.toString());
    }

    /**
     * Whether every test-case reported to this writer so far passed.
     * @return true if no failure or error has been recorded
     */
    boolean wasSuccessful() {
        return failureCount == 0 && errorCount == 0;
    }

    private void appendTestCase(StringBuilder xml, TestCaseResult testCase) {
        xml.append("  <testcase classname=\"").append(escapeXml(testCase.classname()))
                .append("\" name=\"").append(escapeXml(testCase.name()))
                .append("\" time=\"").append(testCase.elapsedMillis() / 1000.0).append("\"");
        if (testCase.outcome() instanceof Outcome.Passed) {
            xml.append(" />\n");
            return;
        }
        xml.append(">\n");
        if (testCase.outcome() instanceof Outcome.Failure failure) {
            appendFailureOrError(xml, "failure", failure.message(), failure.type(), failure.stackTrace());
        } else if (testCase.outcome() instanceof Outcome.Error error) {
            Throwable throwable = error.throwable();
            appendFailureOrError(xml, "error", throwable.getMessage(), throwable.getClass().getName(),
                    ReportingSupport.stackTraceOf(throwable));
        }
        xml.append("  </testcase>\n");
    }

    private void appendFailureOrError(StringBuilder xml, String tag, String message, String type, String stackTrace) {
        xml.append("    <").append(tag);
        if (message != null && !message.isEmpty()) {
            xml.append(" message=\"").append(escapeXml(message)).append("\"");
        }
        xml.append(" type=\"").append(escapeXml(type)).append("\">")
                .append(escapeXml(stackTrace))
                .append("</").append(tag).append(">\n");
    }

    private void writeAndClose(String xml) {
        try {
            out.write(xml.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            Debug.logError(e, "Unable to write suite XML report for '" + suiteName + "'", MODULE);
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                Debug.logError(e, "Unable to close suite XML report output stream for '" + suiteName + "'", MODULE);
            }
        }
    }

    private static String hostname() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            return localHost != null ? localHost.getHostName() : "localhost";
        } catch (UnknownHostException e) {
            return "localhost";
        }
    }

    private static String escapeXml(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
            case '&':
                sb.append("&amp;");
                break;
            case '<':
                sb.append("&lt;");
                break;
            case '>':
                sb.append("&gt;");
                break;
            case '"':
                sb.append("&quot;");
                break;
            default:
                if (isLegalXmlChar(c)) {
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    /**
     * Whether {@code c} is legal in an XML 1.0 document, matching Ant's own
     * {@code DOMElementWriter.isLegalCharacter(char)}. Illegal characters (e.g. most C0 control
     * characters, such as ESC/0x1B) are silently dropped by {@link #escapeXml(String)} rather than
     * escaped or replaced - the same per-{@code char} approach Ant's formatter used, deliberately not
     * attempting to special-case surrogate pairs.
     * @param c the character to check
     * @return true if {@code c} may appear in XML 1.0 content
     */
    private static boolean isLegalXmlChar(char c) {
        return c == 0x9 || c == 0xA || c == 0xD
                || (c >= 0x20 && c <= 0xD7FF)
                || (c >= 0xE000 && c <= 0xFFFD);
    }

    private record TestCaseResult(String classname, String name, long elapsedMillis, Outcome outcome) {
    }
}
