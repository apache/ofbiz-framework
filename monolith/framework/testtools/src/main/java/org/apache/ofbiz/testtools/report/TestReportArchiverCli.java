/*
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
 */
package org.apache.ofbiz.testtools.report;

import java.io.File;

/**
 * Entry point invoked by the Gradle {@code archiveUnitTestReport} / {@code
 * archiveIntegrationTestReport} tasks (see {@code test-report-archive.gradle}) via {@code
 * finalizedBy} on {@code test} and on {@code ofbiz --test}/{@code testIntegration}. Archiving
 * must never fail the build - every exception is caught, logged to stderr, and swallowed.
 *
 * <p>System properties consumed:
 * <ul>
 *   <li>{@code test.report.base.dir} - runtime/test-reports (or configured override), required</li>
 *   <li>{@code test.report.project.dir} - project rootDir, used for git commit/branch lookup</li>
 *   <li>{@code test.report.suite.name} - e.g. "unit", "testIntegration", required</li>
 *   <li>{@code test.report.source.task} - gradle task name that just ran</li>
 *   <li>{@code test.report.task.outcome} - "PASSED" | "FAILED"</li>
 *   <li>{@code test.report.results.dir} - directory holding this run's JUnit XML, required</li>
 *   <li>{@code test.report.html.dir} - separate HTML report directory, only set when the HTML
 *       report does not already live inside test.report.results.dir</li>
 * </ul>
 */
public final class TestReportArchiverCli {

    private TestReportArchiverCli() {
    }

    public static void main(String[] args) {
        try {
            String baseDir = require("test.report.base.dir");
            String resultsDir = require("test.report.results.dir");
            String suiteName = require("test.report.suite.name");

            String htmlDirProperty = System.getProperty("test.report.html.dir");
            File htmlDir = (htmlDirProperty == null || htmlDirProperty.isBlank())
                    ? null : new File(htmlDirProperty);

            TestReportArchiver.ArchiveRequest request = new TestReportArchiver.ArchiveRequest(
                    new File(baseDir),
                    new File(System.getProperty("test.report.project.dir", ".")),
                    suiteName,
                    System.getProperty("test.report.source.task", "unknown"),
                    System.getProperty("test.report.task.outcome", "UNKNOWN"),
                    new File(resultsDir),
                    htmlDir);

            TestRunManifest manifest = TestReportArchiver.archive(request);
            System.out.println("TestReportArchiverCli: archived '" + suiteName + "' run to "
                    + manifest.getResultsLocation());
        } catch (Exception e) {
            System.err.println("TestReportArchiverCli: failed to archive test report: " + e.getMessage());
        }
    }

    private static String require(String propertyName) {
        String value = System.getProperty(propertyName);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("missing required system property: " + propertyName);
        }
        return value;
    }
}
