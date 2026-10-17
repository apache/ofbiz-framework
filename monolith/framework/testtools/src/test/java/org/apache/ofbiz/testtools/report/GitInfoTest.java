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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;

class GitInfoTest {

    @Test
    void returnsUnknownForADirectoryWithNoGitRepo(@TempDir File notARepo) {
        assertThat(GitInfo.currentCommit(notARepo), is("unknown"));
        assertThat(GitInfo.currentBranch(notARepo), is("unknown"));
    }

    @Test
    void returnsARealShortCommitHashInsideThisRepo() {
        // build.gradle's own workingDir default for JavaExec is the project root, which is
        // exactly what test.report.project.dir will be set to in production - use it here too.
        String commit = GitInfo.currentCommit(new File("."));

        assertThat(commit, not(is("unknown")));
        assertThat(commit, matchesPattern("[0-9a-f]{7,40}"));
    }
}
