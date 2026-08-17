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
    private static final DateTimeFormatter TIME_FOLDER =
            DateTimeFormatter.ofPattern("HH-mm-ss").withZone(ZoneOffset.UTC);

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
        java.io.File runFolder = new java.io.File(request.getBaseDir(),
                dateFolder + java.io.File.separator + timeFolder + "_" + request.getSuiteName());
        Files.createDirectories(runFolder.toPath());

        TestRunManifest.Counts counts = JUnitXmlCounter.count(request.getResultsDir());

        Map<String, String> artifacts = new LinkedHashMap<>();
        java.io.File resultsDest = new java.io.File(runFolder, "results");
        if (copyIfExists(request.getResultsDir(), resultsDest)) {
            artifacts.put("junitXml", "results/");
            java.io.File innerHtmlReport = new java.io.File(resultsDest, "test-report.html");
            if (innerHtmlReport.exists()) {
                artifacts.put("htmlReport", "results/test-report.html");
            }
        }
        if (request.getHtmlReportDir() != null) {
            java.io.File htmlDest = new java.io.File(runFolder, "html-report");
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

        writeManifest(runFolder, manifest);
        return manifest;
    }

    private static void writeManifest(java.io.File runFolder, TestRunManifest manifest) throws IOException {
        String json = JSON.from(manifest).toString();
        Files.writeString(new java.io.File(runFolder, "manifest.json").toPath(), json);
    }

    private static boolean copyIfExists(java.io.File source, java.io.File dest) throws IOException {
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
}
