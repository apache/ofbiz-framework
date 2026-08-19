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
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.ofbiz.base.lang.JSON;

/**
 * Archives one test run's output into a dated folder under {@code runtime/test-reports/}, and
 * writes a {@code manifest.json} describing it. Both {@code test} and {@code testIntegration}
 * runs are handled by copying whatever result/report directories the caller resolved (see
 * {@link ArchiveRequest}) - copying, not just recording a path, because both real source
 * locations ({@code build/test-results/test} and {@code runtime/logs/test-results}) are
 * overwritten by the very next run.
 */
public final class TestReportArchiver {

    private static final DateTimeFormatter DATE_FOLDER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);
    // Unit-suffixed (04h16m23s) rather than plain-hyphenated (04-16-23) so the segment reads as
    // a time-of-day on its own, without relying on its position under the date folder for
    // context. Still filesystem-safe (no colons) and sorts identically to the hyphenated form,
    // since every field stays zero-padded and the literal h/m/s separators are constant.
    private static final DateTimeFormatter TIME_FOLDER =
            DateTimeFormatter.ofPattern("HH'h'mm'm'ss's'").withZone(ZoneOffset.UTC);

    private TestReportArchiver() {
    }

    /**
     * Performs the archive: copies result/report directories into a new dated run folder and
     * writes manifest.json there. Returns the manifest that was written.
     */
    public static TestRunManifest archive(ArchiveRequest request) throws IOException {
        Instant now = Instant.now();
        String dateFolder = DATE_FOLDER.format(now);
        String timeFolder = TIME_FOLDER.format(now);
        File runFolder = new File(request.getBaseDir(),
                dateFolder + File.separator + timeFolder + "_" + request.getSuiteName());
        Files.createDirectories(runFolder.toPath());

        TestRunManifest.Counts counts = JUnitXmlCounter.count(request.getResultsDir());

        Map<String, String> artifacts = new LinkedHashMap<>();
        File resultsDest = new File(runFolder, "results");
        if (copyIfExists(request.getResultsDir(), resultsDest)) {
            artifacts.put("junitXml", "results/");
            File innerHtmlReport = new File(resultsDest, "test-report.html");
            if (innerHtmlReport.exists()) {
                artifacts.put("htmlReport", "results/test-report.html");
            }
        }
        if (request.getHtmlReportDir() != null) {
            File htmlDest = new File(runFolder, "html-report");
            if (copyIfExists(request.getHtmlReportDir(), htmlDest)) {
                artifacts.put("htmlReport", "html-report/index.html");
            }
        }

        TestRunManifest manifest = new TestRunManifest();
        manifest.setRunId(dateFolder + "_" + timeFolder + "_" + request.getSuiteName());
        manifest.setSuiteName(request.getSuiteName());
        manifest.setArchivedAt(DateTimeFormatter.ISO_INSTANT.format(now));
        manifest.setGradleTask(request.getSourceTask());
        manifest.setOutcome(request.getOutcome());
        manifest.setGitCommit(GitInfo.currentCommit(request.getProjectDir()));
        manifest.setGitBranch(GitInfo.currentBranch(request.getProjectDir()));
        manifest.setCounts(counts);
        manifest.setResultsLocation(runFolder.getAbsolutePath());
        manifest.setArtifacts(artifacts);
        manifest.setTrigger(request.getTrigger());
        manifest.setParamsUsed(request.getParamsUsed());

        writeManifest(runFolder, manifest);
        return manifest;
    }

    private static void writeManifest(File runFolder, TestRunManifest manifest) throws IOException {
        String json = JSON.from(manifest).toString();
        Files.writeString(new File(runFolder, "manifest.json").toPath(), json);
    }

    private static boolean copyIfExists(File source, File dest) throws IOException {
        if (source == null || !source.exists()) {
            return false;
        }
        copyRecursive(source.toPath(), dest.toPath());
        return true;
    }

    private static void copyRecursive(Path source, Path dest) throws IOException {
        if (Files.isDirectory(source)) {
            Files.createDirectories(dest);
            try (var children = Files.list(source)) {
                for (Path child : (Iterable<Path>) children::iterator) {
                    copyRecursive(child, dest.resolve(child.getFileName().toString()));
                }
            }
        } else {
            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Immutable parameters for one {@link TestReportArchiver#archive} call. */
    public static final class ArchiveRequest {

        private final File baseDir;
        private final File projectDir;
        private final String suiteName;
        private final String sourceTask;
        private final String outcome;
        private final File resultsDir;
        private final File htmlReportDir;
        private final String trigger;
        private final Map<String, String> paramsUsed;

        public ArchiveRequest(File baseDir, File projectDir, String suiteName, String sourceTask, String outcome,
                File resultsDir, File htmlReportDir) {
            this(baseDir, projectDir, suiteName, sourceTask, outcome, resultsDir, htmlReportDir, "gradle", Map.of());
        }

        public ArchiveRequest(File baseDir, File projectDir, String suiteName, String sourceTask, String outcome,
                File resultsDir, File htmlReportDir, String trigger, Map<String, String> paramsUsed) {
            this.baseDir = baseDir;
            this.projectDir = projectDir;
            this.suiteName = suiteName;
            this.sourceTask = sourceTask;
            this.outcome = outcome;
            this.resultsDir = resultsDir;
            this.htmlReportDir = htmlReportDir;
            this.trigger = trigger;
            this.paramsUsed = paramsUsed;
        }

        public File getBaseDir() {
            return baseDir;
        }

        public File getProjectDir() {
            return projectDir;
        }

        public String getSuiteName() {
            return suiteName;
        }

        public String getSourceTask() {
            return sourceTask;
        }

        public String getOutcome() {
            return outcome;
        }

        public File getResultsDir() {
            return resultsDir;
        }

        public File getHtmlReportDir() {
            return htmlReportDir;
        }

        public String getTrigger() {
            return trigger;
        }

        public Map<String, String> getParamsUsed() {
            return paramsUsed;
        }
    }
}
