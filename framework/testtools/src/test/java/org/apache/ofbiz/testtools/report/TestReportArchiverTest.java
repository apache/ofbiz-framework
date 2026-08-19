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
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import org.apache.ofbiz.base.lang.JSON;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;

class TestReportArchiverTest {

    @Test
    void archivesASeparateResultsAndHtmlDirLikeThePlainTestTask(@TempDir File tmp) throws IOException {
        File baseDir = new File(tmp, "runtime/test-reports");
        File resultsDir = new File(tmp, "build/test-results/test");
        File htmlDir = new File(tmp, "build/reports/tests/test");
        resultsDir.mkdirs();
        htmlDir.mkdirs();
        Files.writeString(new File(resultsDir, "org.example.SomeTest.xml").toPath(),
                "<testsuite name=\"x\" tests=\"2\" failures=\"1\" errors=\"0\" skipped=\"0\"></testsuite>");
        Files.writeString(new File(htmlDir, "index.html").toPath(), "<html>report</html>");

        TestReportArchiver.ArchiveRequest request = new TestReportArchiver.ArchiveRequest(
                baseDir, tmp, "unit", "test", "FAILED", resultsDir, htmlDir);
        TestRunManifest manifest = TestReportArchiver.archive(request);

        assertThat(manifest.getSuiteName(), is("unit"));
        assertThat(manifest.getOutcome(), is("FAILED"));
        assertThat(manifest.getCounts().getTotal(), is(2));
        assertThat(manifest.getCounts().getFailed(), is(1));
        assertThat(manifest.getArtifacts().get("junitXml"), is("results/"));
        assertThat(manifest.getArtifacts().get("htmlReport"), is("html-report/index.html"));

        File runFolder = new File(manifest.getResultsLocation());
        assertThat(new File(runFolder, "results/org.example.SomeTest.xml").exists(), is(true));
        assertThat(new File(runFolder, "html-report/index.html").exists(), is(true));

        File manifestFile = new File(runFolder, "manifest.json");
        assertThat(manifestFile.exists(), is(true));
        TestRunManifest roundTripped = JSON.from(Files.newInputStream(manifestFile.toPath())).toObject(TestRunManifest.class);
        assertThat(roundTripped.getSuiteName(), is("unit"));
        assertThat(roundTripped.getCounts().getFailed(), is(1));
    }

    @Test
    void archivesACombinedResultsDirLikeTestIntegration(@TempDir File tmp) throws IOException {
        File baseDir = new File(tmp, "runtime/test-reports");
        File resultsDir = new File(tmp, "runtime/logs/test-results");
        resultsDir.mkdirs();
        Files.writeString(new File(resultsDir, "SomeSuite.xml").toPath(),
                "<testsuite name=\"x\" tests=\"3\" failures=\"0\" errors=\"0\" skipped=\"1\"></testsuite>");
        Files.writeString(new File(resultsDir, "test-report.html").toPath(), "<html>combined</html>");

        TestReportArchiver.ArchiveRequest request = new TestReportArchiver.ArchiveRequest(
                baseDir, tmp, "testIntegration", "testIntegration", "PASSED", resultsDir, null);
        TestRunManifest manifest = TestReportArchiver.archive(request);

        assertThat(manifest.getArtifacts().get("junitXml"), is("results/"));
        assertThat(manifest.getArtifacts().get("htmlReport"), is("results/test-report.html"));

        File runFolder = new File(manifest.getResultsLocation());
        assertThat(new File(runFolder, "results/test-report.html").exists(), is(true));
    }

    @Test
    void runFolderNameFollowsDateTimeSuiteConvention(@TempDir File tmp) throws IOException {
        File baseDir = new File(tmp, "runtime/test-reports");
        File resultsDir = new File(tmp, "empty-results");
        resultsDir.mkdirs();

        TestReportArchiver.ArchiveRequest request = new TestReportArchiver.ArchiveRequest(baseDir, tmp, "unit", "test", "PASSED", resultsDir, null);
        TestRunManifest manifest = TestReportArchiver.archive(request);

        assertThat(manifest.getRunId(), notNullValue());
        File runFolder = new File(manifest.getResultsLocation());
        assertThat(runFolder.getParentFile().getParentFile(), equalTo(baseDir));
        assertThat(runFolder.getName(), matchesPattern("\\d{2}h\\d{2}m\\d{2}s_unit"));
        assertThat(runFolder.getParentFile().getName(), matchesPattern("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    void archiveRecordsTriggerAndParamsUsedWhenSupplied(@TempDir File tmp) throws IOException {
        File baseDir = new File(tmp, "runtime/test-reports");
        File resultsDir = new File(tmp, "runtime/logs/test-results");
        resultsDir.mkdirs();
        Files.writeString(new File(resultsDir, "example-tests.xml").toPath(),
                "<testsuite name=\"x\" tests=\"1\" failures=\"0\" errors=\"0\" skipped=\"0\"></testsuite>");

        TestRunManifest manifest = TestReportArchiver.archive(new TestReportArchiver.ArchiveRequest(
                baseDir, tmp, "example-tests", "api", "PASSED", resultsDir, null,
                "api", Map.of("exampleName", "Caller Supplied Name")));

        assertThat(manifest.getTrigger(), is("api"));
        assertThat(manifest.getParamsUsed(), is(Map.of("exampleName", "Caller Supplied Name")));
    }

    @Test
    void archiveDefaultsTriggerToGradleWhenNotSupplied(@TempDir File tmp) throws IOException {
        File baseDir = new File(tmp, "runtime/test-reports");
        File resultsDir = new File(tmp, "runtime/logs/test-results");
        resultsDir.mkdirs();
        Files.writeString(new File(resultsDir, "example-tests.xml").toPath(),
                "<testsuite name=\"x\" tests=\"1\" failures=\"0\" errors=\"0\" skipped=\"0\"></testsuite>");

        TestRunManifest manifest = TestReportArchiver.archive(new TestReportArchiver.ArchiveRequest(
                baseDir, tmp, "example-tests", "testIntegration", "PASSED", resultsDir, null));

        assertThat(manifest.getTrigger(), is("gradle"));
        assertThat(manifest.getParamsUsed(), is(Map.of()));
    }
}
