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
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ofbiz.base.lang.JSON;
import org.apache.ofbiz.base.util.Debug;

/**
 * Pure decision logic for {@link TestReportPurgeService}: given the run folders under {@code
 * runtime/test-reports/<date>/}, decides which ones are older than the retention window and safe
 * to delete - "safe" meaning not one of the last N fully-passing runs for its suite, which stay
 * protected regardless of age so a known-good baseline is never lost.
 */
public final class TestReportPurgePlanner {

    private static final String MODULE = TestReportPurgePlanner.class.getName();
    private static final DateTimeFormatter DATE_FOLDER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private TestReportPurgePlanner() {
    }

    /** One discovered run folder and the manifest fields the planner needs. */
    public static final class RunFolder {
        private final File dir;
        private final LocalDate date;
        private final String suiteName;
        private final boolean green;

        public RunFolder(File dir, LocalDate date, String suiteName, boolean green) {
            this.dir = dir;
            this.date = date;
            this.suiteName = suiteName;
            this.green = green;
        }

        public File getDir() {
            return dir;
        }

        public LocalDate getDate() {
            return date;
        }

        public String getSuiteName() {
            return suiteName;
        }

        public boolean isGreen() {
            return green;
        }
    }

    /** Walks {@code baseDir/<yyyy-MM-dd>/<run>/manifest.json} and builds the RunFolder list. */
    public static List<RunFolder> discoverRunFolders(File baseDir) {
        List<RunFolder> runFolders = new ArrayList<>();
        File[] dateDirs = baseDir.listFiles(File::isDirectory);
        if (dateDirs == null) {
            return runFolders;
        }
        for (File dateDir : dateDirs) {
            LocalDate date;
            try {
                date = LocalDate.parse(dateDir.getName(), DATE_FOLDER);
            } catch (DateTimeParseException e) {
                continue; // not a date-named folder, skip
            }
            File[] runDirs = dateDir.listFiles(File::isDirectory);
            if (runDirs == null) {
                continue;
            }
            for (File runDir : runDirs) {
                File manifestFile = new File(runDir, "manifest.json");
                if (!manifestFile.isFile()) {
                    continue;
                }
                try (FileInputStream in = new FileInputStream(manifestFile)) {
                    TestRunManifest manifest = JSON.from(in).toObject(TestRunManifest.class);
                    // A run only counts as green when it actually tested something (total > 0) and
                    // both the counts and the recorded outcome agree it passed - otherwise an empty
                    // or failed-before-any-XML-was-produced run (failed == 0 by default) would be
                    // wrongly treated as a protected baseline.
                    boolean green = manifest.getCounts() != null && manifest.getCounts().getTotal() > 0
                            && manifest.getCounts().getFailed() == 0 && "PASSED".equals(manifest.getOutcome());
                    runFolders.add(new RunFolder(runDir, date, manifest.getSuiteName(), green));
                } catch (IOException e) {
                    // Unreadable/corrupt manifest - leave the folder alone, don't guess.
                    Debug.logWarning(e, "TestReportPurgePlanner: skipping unreadable " + manifestFile, MODULE);
                }
            }
        }
        return runFolders;
    }

    /**
     * Returns the subset of {@code runFolders} that should be deleted: older than {@code
     * retentionDays} relative to {@code today}, excluding each suite's last {@code
     * keepLastGreenPerSuite} green runs.
     */
    public static List<File> planDeletions(List<RunFolder> runFolders, int retentionDays,
            int keepLastGreenPerSuite, LocalDate today) {
        LocalDate cutoff = today.minusDays(retentionDays);

        Map<String, List<RunFolder>> bySuite = new HashMap<>();
        for (RunFolder runFolder : runFolders) {
            bySuite.computeIfAbsent(runFolder.getSuiteName(), key -> new ArrayList<>()).add(runFolder);
        }

        Set<File> protectedDirs = new LinkedHashSet<>();
        for (List<RunFolder> suiteRuns : bySuite.values()) {
            suiteRuns.stream()
                    .filter(RunFolder::isGreen)
                    .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                    .limit(keepLastGreenPerSuite)
                    .forEach(runFolder -> protectedDirs.add(runFolder.getDir()));
        }

        List<File> toDelete = new ArrayList<>();
        for (RunFolder runFolder : runFolders) {
            if (runFolder.getDate().isBefore(cutoff) && !protectedDirs.contains(runFolder.getDir())) {
                toDelete.add(runFolder.getDir());
            }
        }
        return toDelete;
    }
}
