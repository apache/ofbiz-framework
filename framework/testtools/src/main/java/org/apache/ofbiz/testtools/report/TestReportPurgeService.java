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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * Deletes dated test-run history folders older than the configured retention window, always
 * keeping each suite's last N green (fully-passing) runs regardless of age. Covers both history
 * roots the archiver writes (see {@code test-report-archive.gradle}), defaulting to
 * {@code build/test-reports-history/} (unit) and {@code runtime/logs/test-reports-history/}
 * (integration) but following {@code testtools.properties}' {@code test.history.unit.dir}/
 * {@code test.history.integration.dir} overrides if either is set, so purge always targets
 * wherever the archiver is actually writing.
 *
 * <p>Entirely opt-in, driven by {@code framework/testtools/config/testtools.properties}: no-ops
 * unless {@code test.history=true} is set there, and skips purging (but not archiving) if
 * {@code test.history=true} but {@code test.history.days} is left unset/commented - history then
 * just accumulates until a retention window is explicitly configured. Scheduled daily regardless
 * (visible/manageable from the admin Scheduler screen) via the TESTREPORT_PURGE JobSandbox entry
 * seeded in TestReportsScheduledServiceData.xml, mirroring how autoSyncRotatedSecrets is seeded
 * but no-ops unless secret.rotation.autosync.enabled=true (see
 * SecretManagerScheduledServiceData.xml).
 */
public final class TestReportPurgeService {

    private static final String MODULE = TestReportPurgeService.class.getName();
    private static final String RESOURCE = "testtools";
    private static final String DEFAULT_UNIT_HISTORY_PATH = "build/test-reports-history";
    private static final String DEFAULT_INTEGRATION_HISTORY_PATH = "runtime/logs/test-reports-history";
    private static final int DEFAULT_KEEP_LAST_GREEN = 5;

    private TestReportPurgeService() {
    }

    public static Map<String, Object> purgeOldTestReports(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();

        String testHistory = readStringProperty(delegator, "test.history", "false");
        if (!"true".equalsIgnoreCase(testHistory)) {
            return ServiceUtil.returnSuccess("test.history is not enabled in general.properties, nothing to purge");
        }

        String retentionDaysValue = readStringProperty(delegator, "test.history.days", null);
        if (UtilValidate.isEmpty(retentionDaysValue)) {
            return ServiceUtil.returnSuccess(
                    "test.history.days is not configured in general.properties, skipping purge (history retained indefinitely)");
        }
        int retentionDays;
        try {
            retentionDays = Integer.parseInt(retentionDaysValue.trim());
        } catch (NumberFormatException e) {
            return ServiceUtil.returnError("test.history.days is not a valid integer: '" + retentionDaysValue + "'");
        }

        String unitHistoryPath = readStringProperty(delegator, "test.history.unit.dir", DEFAULT_UNIT_HISTORY_PATH);
        String integrationHistoryPath = readStringProperty(delegator, "test.history.integration.dir",
                DEFAULT_INTEGRATION_HISTORY_PATH);

        String ofbizHome = System.getProperty("ofbiz.home");
        File unitHistoryDir = resolveBaseDir(unitHistoryPath, ofbizHome);
        File integrationHistoryDir = resolveBaseDir(integrationHistoryPath, ofbizHome);

        long deletedCount = purgeOneHistoryDir(unitHistoryDir, retentionDays, DEFAULT_KEEP_LAST_GREEN)
                + purgeOneHistoryDir(integrationHistoryDir, retentionDays, DEFAULT_KEEP_LAST_GREEN);

        String message = "Purged " + deletedCount + " test report run(s) older than " + retentionDays
                + " days across " + unitHistoryDir + " and " + integrationHistoryDir
                + " (kept last " + DEFAULT_KEEP_LAST_GREEN + " green run(s) per suite)";
        Debug.logInfo("purgeOldTestReports: " + message, MODULE);
        Map<String, Object> result = ServiceUtil.returnSuccess(message);
        result.put("deletedCount", deletedCount);
        return result;
    }

    private static long purgeOneHistoryDir(File baseDir, int retentionDays, int keepLastGreen) {
        if (!baseDir.isDirectory()) {
            return 0;
        }

        List<TestReportPurgePlanner.RunFolder> runFolders = TestReportPurgePlanner.discoverRunFolders(baseDir);
        List<File> toDelete = TestReportPurgePlanner.planDeletions(runFolders, retentionDays, keepLastGreen, LocalDate.now());

        long deletedCount = 0;
        for (File dir : toDelete) {
            try {
                deleteRecursive(dir.toPath());
                deletedCount++;
            } catch (IOException e) {
                Debug.logError(e, "purgeOldTestReports: failed to delete " + dir, MODULE);
            }
        }
        deleteEmptyDateFolders(baseDir);
        return deletedCount;
    }

    /**
     * Resolves {@code configuredPath} against {@code ofbizHome} when it's a relative path, matching
     * the pattern used elsewhere in this codebase for {@code runtime/...} paths (see
     * {@code UtilURL.fromOfbizHomePath} and {@code CatalinaContainer}) rather than leaving it to
     * resolve against whatever the JVM's working directory happens to be. Falls back to resolving
     * as-is (relative to JVM cwd) when {@code ofbizHome} is unset.
     */
    static File resolveBaseDir(String configuredPath, String ofbizHome) {
        File configured = new File(configuredPath);
        if (configured.isAbsolute() || UtilValidate.isEmpty(ofbizHome)) {
            return configured;
        }
        return new File(ofbizHome, configuredPath);
    }

    private static String readStringProperty(Delegator delegator, String propertyName, String defaultValue) {
        try {
            String value = EntityUtilProperties.getPropertyValue(RESOURCE, propertyName, delegator);
            return UtilValidate.isNotEmpty(value) ? value.trim() : defaultValue;
        } catch (Exception e) {
            Debug.logWarning(e, "purgeOldTestReports: could not read " + propertyName + ", using default '"
                    + defaultValue + "'", MODULE);
            return defaultValue;
        }
    }

    private static void deleteRecursive(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var children = Files.list(path)) {
                for (Path child : (Iterable<Path>) children::iterator) {
                    deleteRecursive(child);
                }
            }
        }
        Files.deleteIfExists(path);
    }

    private static void deleteEmptyDateFolders(File baseDir) {
        File[] dateDirs = baseDir.listFiles(File::isDirectory);
        if (dateDirs == null) {
            return;
        }
        for (File dateDir : dateDirs) {
            File[] children = dateDir.listFiles();
            if (children != null && children.length == 0) {
                dateDir.delete();
            }
        }
    }
}
