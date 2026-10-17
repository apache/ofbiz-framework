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
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.ofbiz.base.util.Debug;

/** Looks up the current git commit and branch via the {@code git} CLI. Never throws. */
public final class GitInfo {

    private static final String MODULE = GitInfo.class.getName();
    private static final String UNKNOWN = "unknown";
    private static final long TIMEOUT_SECONDS = 5;

    private GitInfo() {
    }

    public static String currentCommit(File workingDir) {
        return run(workingDir, "rev-parse", "--short", "HEAD");
    }

    public static String currentBranch(File workingDir) {
        return run(workingDir, "rev-parse", "--abbrev-ref", "HEAD");
    }

    private static String run(File workingDir, String... gitArgs) {
        try {
            String[] command = new String[gitArgs.length + 1];
            command[0] = "git";
            System.arraycopy(gitArgs, 0, command, 1, gitArgs.length);
            Process process = new ProcessBuilder(command)
                    .directory(workingDir)
                    .redirectErrorStream(true)
                    .start();
            AtomicReference<String> outputRef = new AtomicReference<>("");
            Thread reader = new Thread(() -> {
                try {
                    outputRef.set(new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
                } catch (IOException e) {
                    Debug.logInfo(e, "GitInfo: error reading git output, using '" + UNKNOWN + "'", MODULE);
                }
            }, "GitInfo-stdout-reader");
            reader.setDaemon(true);
            reader.start();
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return UNKNOWN;
            }
            reader.join(1000);
            String output = outputRef.get();
            if (process.exitValue() != 0 || output.trim().isEmpty()) {
                return UNKNOWN;
            }
            return output.trim();
        } catch (IOException e) {
            Debug.logWarning(e, "GitInfo: git lookup failed, using '" + UNKNOWN + "'", MODULE);
            return UNKNOWN;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Debug.logWarning(e, "GitInfo: git lookup interrupted, using '" + UNKNOWN + "'", MODULE);
            return UNKNOWN;
        }
    }
}
