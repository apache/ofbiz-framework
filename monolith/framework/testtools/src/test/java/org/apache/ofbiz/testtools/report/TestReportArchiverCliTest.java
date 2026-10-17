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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class TestReportArchiverCliTest {

    private static final String[] PROPERTY_NAMES = {
            "test.report.base.dir", "test.report.project.dir", "test.report.suite.name",
            "test.report.source.task", "test.report.task.outcome", "test.report.results.dir",
            "test.report.html.dir"
    };

    @AfterEach
    void clearSystemProperties() {
        for (String name : PROPERTY_NAMES) {
            System.clearProperty(name);
        }
    }

    @Test
    void writesAManifestWhenAllRequiredPropertiesAreSet(@TempDir File tmp) throws IOException {
        File baseDir = new File(tmp, "runtime/test-reports");
        File resultsDir = new File(tmp, "results");
        resultsDir.mkdirs();
        Files.writeString(new File(resultsDir, "S.xml").toPath(),
                "<testsuite name=\"x\" tests=\"1\" failures=\"0\" errors=\"0\" skipped=\"0\"></testsuite>");

        System.setProperty("test.report.base.dir", baseDir.getAbsolutePath());
        System.setProperty("test.report.project.dir", tmp.getAbsolutePath());
        System.setProperty("test.report.suite.name", "unit");
        System.setProperty("test.report.source.task", "test");
        System.setProperty("test.report.task.outcome", "PASSED");
        System.setProperty("test.report.results.dir", resultsDir.getAbsolutePath());

        TestReportArchiverCli.main(new String[0]);

        File[] dateDirs = baseDir.listFiles();
        assertThat(dateDirs != null && dateDirs.length == 1, is(true));
        File[] runDirs = dateDirs[0].listFiles();
        assertThat(runDirs != null && runDirs.length == 1, is(true));
        assertThat(new File(runDirs[0], "manifest.json").exists(), is(true));
    }

    @Test
    void doesNotThrowWhenARequiredPropertyIsMissing() {
        System.setProperty("test.report.base.dir", "/tmp/whatever");
        // test.report.results.dir and test.report.suite.name intentionally left unset.

        TestReportArchiverCli.main(new String[0]); // must not throw
    }
}
