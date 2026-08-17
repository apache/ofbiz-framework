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
import java.time.LocalDate;
import java.util.List;

import org.apache.ofbiz.base.lang.JSON;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

class TestReportPurgePlannerTest {

    private static TestReportPurgePlanner.RunFolder runFolder(String path, String date, String suite, boolean green) {
        return new TestReportPurgePlanner.RunFolder(new File(path), LocalDate.parse(date), suite, green);
    }

    private static void writeManifest(File runDir, String suiteName, String outcome, int total, int failed)
            throws IOException {
        Files.createDirectories(runDir.toPath());
        TestRunManifest manifest = new TestRunManifest();
        manifest.setRunId(runDir.getName());
        manifest.setSuiteName(suiteName);
        manifest.setOutcome(outcome);
        manifest.setCounts(new TestRunManifest.Counts(total, total - failed, failed, 0));
        Files.writeString(new File(runDir, "manifest.json").toPath(), JSON.from(manifest).toString());
    }

    @Test
    void deletesOnlyRunsOlderThanTheRetentionWindow() {
        List<TestReportPurgePlanner.RunFolder> runFolders = List.of(
                runFolder("/base/2026-01-01/a_unit", "2026-01-01", "unit", true),
                runFolder("/base/2026-08-01/b_unit", "2026-08-01", "unit", true));
        LocalDate today = LocalDate.parse("2026-08-17");

        List<File> toDelete = TestReportPurgePlanner.planDeletions(runFolders, 30, 0, today);

        assertThat(toDelete, contains(new File("/base/2026-01-01/a_unit")));
    }

    @Test
    void protectsTheLastNGreenRunsPerSuiteEvenIfOld() {
        List<TestReportPurgePlanner.RunFolder> runFolders = List.of(
                runFolder("/base/2026-01-01/a_unit", "2026-01-01", "unit", true),
                runFolder("/base/2026-01-02/b_unit", "2026-01-02", "unit", true),
                runFolder("/base/2026-01-03/c_unit", "2026-01-03", "unit", false));
        LocalDate today = LocalDate.parse("2026-08-17");

        List<File> toDelete = TestReportPurgePlanner.planDeletions(runFolders, 30, 1, today);

        // b_unit (2026-01-02) is the most recent green run for "unit" - protected.
        // a_unit (older green) and c_unit (red, never protected) are both fair game.
        assertThat(toDelete, containsInAnyOrder(new File("/base/2026-01-01/a_unit"), new File("/base/2026-01-03/c_unit")));
    }

    @Test
    void protectionIsPerSuiteNotGlobal() {
        List<TestReportPurgePlanner.RunFolder> runFolders = List.of(
                runFolder("/base/2026-01-01/a_unit", "2026-01-01", "unit", true),
                runFolder("/base/2026-01-01/a_testIntegration", "2026-01-01", "testIntegration", true));
        LocalDate today = LocalDate.parse("2026-08-17");

        List<File> toDelete = TestReportPurgePlanner.planDeletions(runFolders, 30, 1, today);

        assertThat(toDelete, empty());
    }

    @Test
    void keepsRunsWithinTheRetentionWindowRegardlessOfGreen() {
        List<TestReportPurgePlanner.RunFolder> runFolders = List.of(
                runFolder("/base/2026-08-16/a_unit", "2026-08-16", "unit", false));
        LocalDate today = LocalDate.parse("2026-08-17");

        List<File> toDelete = TestReportPurgePlanner.planDeletions(runFolders, 30, 0, today);

        assertThat(toDelete, hasSize(0));
    }

    @Test
    void anEmptyRunWithZeroTotalIsNotTreatedAsGreenEvenThoughFailedIsZero(@TempDir File tmp) throws IOException {
        File dateDir = new File(tmp, "2026-01-01");
        File emptyRunDir = new File(dateDir, "00-00-00_unit");
        writeManifest(emptyRunDir, "unit", "PASSED", 0, 0);

        List<TestReportPurgePlanner.RunFolder> runFolders = TestReportPurgePlanner.discoverRunFolders(tmp);

        assertThat(runFolders, hasSize(1));
        assertThat(runFolders.get(0).isGreen(), is(false));
    }

    @Test
    void aRunWithOutcomeFailedIsNotTreatedAsGreenEvenThoughFailedCountIsZero(@TempDir File tmp) throws IOException {
        File dateDir = new File(tmp, "2026-01-01");
        File failedRunDir = new File(dateDir, "00-00-00_unit");
        writeManifest(failedRunDir, "unit", "FAILED", 5, 0);

        List<TestReportPurgePlanner.RunFolder> runFolders = TestReportPurgePlanner.discoverRunFolders(tmp);

        assertThat(runFolders, hasSize(1));
        assertThat(runFolders.get(0).isGreen(), is(false));
    }

    @Test
    void aGenuinePassingRunIsStillTreatedAsGreen(@TempDir File tmp) throws IOException {
        File dateDir = new File(tmp, "2026-01-01");
        File passingRunDir = new File(dateDir, "00-00-00_unit");
        writeManifest(passingRunDir, "unit", "PASSED", 5, 0);

        List<TestReportPurgePlanner.RunFolder> runFolders = TestReportPurgePlanner.discoverRunFolders(tmp);

        assertThat(runFolders, hasSize(1));
        assertThat(runFolders.get(0).isGreen(), is(true));
    }
}
