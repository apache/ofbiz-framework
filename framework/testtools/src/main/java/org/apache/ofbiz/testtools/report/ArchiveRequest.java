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

/** Immutable parameters for one {@link TestReportArchiver#archive} call. */
public final class ArchiveRequest {

    private final File baseDir;
    private final File projectDir;
    private final String suiteName;
    private final String sourceTask;
    private final String outcome;
    private final File resultsDir;
    private final File htmlReportDir;

    public ArchiveRequest(File baseDir, File projectDir, String suiteName, String sourceTask, String outcome,
            File resultsDir, File htmlReportDir) {
        this.baseDir = baseDir;
        this.projectDir = projectDir;
        this.suiteName = suiteName;
        this.sourceTask = sourceTask;
        this.outcome = outcome;
        this.resultsDir = resultsDir;
        this.htmlReportDir = htmlReportDir;
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
}
