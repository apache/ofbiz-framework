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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Plain data holder serialized to {@code manifest.json} by {@link TestReportArchiver} and read
 * back by {@link TestReportPurgePlanner}, via {@link org.apache.ofbiz.base.lang.JSON}'s
 * Jackson-backed bean (de)serialization. Field/getter names form the manifest schema.
 */
public final class TestRunManifest {

    private String runId;
    private String suiteName;
    private String archivedAt;
    private String gradleTask;
    private String outcome;
    private String gitCommit;
    private String gitBranch;
    private Counts counts;
    private String resultsLocation;
    private Map<String, String> artifacts = new LinkedHashMap<>();
    private String trigger = "gradle";
    private Map<String, String> paramsUsed = new LinkedHashMap<>();

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getSuiteName() {
        return suiteName;
    }

    public void setSuiteName(String suiteName) {
        this.suiteName = suiteName;
    }

    public String getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(String archivedAt) {
        this.archivedAt = archivedAt;
    }

    public String getGradleTask() {
        return gradleTask;
    }

    public void setGradleTask(String gradleTask) {
        this.gradleTask = gradleTask;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String getGitCommit() {
        return gitCommit;
    }

    public void setGitCommit(String gitCommit) {
        this.gitCommit = gitCommit;
    }

    public String getGitBranch() {
        return gitBranch;
    }

    public void setGitBranch(String gitBranch) {
        this.gitBranch = gitBranch;
    }

    public Counts getCounts() {
        return counts;
    }

    public void setCounts(Counts counts) {
        this.counts = counts;
    }

    public String getResultsLocation() {
        return resultsLocation;
    }

    public void setResultsLocation(String resultsLocation) {
        this.resultsLocation = resultsLocation;
    }

    public Map<String, String> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(Map<String, String> artifacts) {
        this.artifacts = artifacts;
    }

    public String getTrigger() {
        return trigger;
    }

    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }

    public Map<String, String> getParamsUsed() {
        return paramsUsed;
    }

    public void setParamsUsed(Map<String, String> paramsUsed) {
        this.paramsUsed = paramsUsed;
    }

    /** Pass/fail/skip totals for one archived run. */
    public static final class Counts {
        private int total;
        private int passed;
        private int failed;
        private int skipped;

        public Counts() {
        }

        public Counts(int total, int passed, int failed, int skipped) {
            this.total = total;
            this.passed = passed;
            this.failed = failed;
            this.skipped = skipped;
        }

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }

        public int getPassed() {
            return passed;
        }

        public void setPassed(int passed) {
            this.passed = passed;
        }

        public int getFailed() {
            return failed;
        }

        public void setFailed(int failed) {
            this.failed = failed;
        }

        public int getSkipped() {
            return skipped;
        }

        public void setSkipped(int skipped) {
            this.skipped = skipped;
        }
    }
}
