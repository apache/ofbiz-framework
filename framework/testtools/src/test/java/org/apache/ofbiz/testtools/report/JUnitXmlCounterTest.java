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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class JUnitXmlCounterTest {

    @Test
    void sumsCountsAcrossMultipleXmlFilesRecursively(@TempDir File resultsDir) throws IOException {
        // Matches the <testsuite errors= failures= skipped= tests=> shape
        // org.apache.ofbiz.testtools.SuiteXmlReportWriter and Gradle's own JUnit Platform
        // listener both produce.
        writeSuiteXml(new File(resultsDir, "SuiteA.xml"), 10, 1, 1, 0);
        File nested = new File(resultsDir, "sub");
        nested.mkdirs();
        writeSuiteXml(new File(nested, "SuiteB.xml"), 5, 0, 0, 2);

        TestRunManifest.Counts counts = JUnitXmlCounter.count(resultsDir);

        assertThat(counts.getTotal(), is(15));
        assertThat(counts.getFailed(), is(2));
        assertThat(counts.getSkipped(), is(2));
        assertThat(counts.getPassed(), is(11));
    }

    @Test
    void skipsUnparsableFilesInsteadOfThrowing(@TempDir File resultsDir) throws IOException {
        Files.writeString(new File(resultsDir, "broken.xml").toPath(), "<not-xml");
        writeSuiteXml(new File(resultsDir, "Good.xml"), 3, 0, 0, 0);

        TestRunManifest.Counts counts = JUnitXmlCounter.count(resultsDir);

        assertThat(counts.getTotal(), is(3));
        assertThat(counts.getFailed(), is(0));
    }

    @Test
    void returnsAllZeroesWhenDirectoryDoesNotExist() {
        TestRunManifest.Counts counts = JUnitXmlCounter.count(new File("/no/such/dir"));

        assertThat(counts.getTotal(), is(0));
        assertThat(counts.getPassed(), is(0));
        assertThat(counts.getFailed(), is(0));
        assertThat(counts.getSkipped(), is(0));
    }

    private static void writeSuiteXml(File file, int tests, int failures, int errors, int skipped) throws IOException {
        String xml = "<testsuite name=\"x\" tests=\"" + tests + "\" failures=\"" + failures
                + "\" errors=\"" + errors + "\" skipped=\"" + skipped + "\"></testsuite>";
        Files.writeString(file.toPath(), xml);
    }
}
