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
 * Deletes {@code runtime/test-reports/<date>/<run>} folders older than the configured retention
 * window, always keeping each suite's last N green (fully-passing) runs regardless of age.
 * Scheduled daily via the TESTREPORT_PURGE JobSandbox entry seeded in
 * TestReportsScheduledServiceData.xml.
 */
public final class TestReportPurgeService {

    private static final String MODULE = TestReportPurgeService.class.getName();
    private static final String RESOURCE = "testtools";

    private TestReportPurgeService() {
    }

    public static Map<String, Object> purgeOldTestReports(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();

        String baseDirPath = readStringProperty(delegator, "test.report.base.dir", "runtime/test-reports");
        int retentionDays = readIntProperty(delegator, "test.report.retention.days", 30);
        int keepLastGreen = readIntProperty(delegator, "test.report.retention.keep.last.n.green", 5);

        File baseDir = new File(baseDirPath);
        if (!baseDir.isDirectory()) {
            return ServiceUtil.returnSuccess("test report base dir '" + baseDirPath + "' does not exist yet, nothing to purge");
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

        String message = "Purged " + deletedCount + " test report run(s) older than " + retentionDays
                + " days (kept last " + keepLastGreen + " green run(s) per suite)";
        Debug.logInfo("purgeOldTestReports: " + message, MODULE);
        Map<String, Object> result = ServiceUtil.returnSuccess(message);
        result.put("deletedCount", deletedCount);
        return result;
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

    private static int readIntProperty(Delegator delegator, String propertyName, int defaultValue) {
        try {
            String value = EntityUtilProperties.getPropertyValue(RESOURCE, propertyName, delegator);
            if (UtilValidate.isNotEmpty(value)) {
                return Integer.parseInt(value.trim());
            }
        } catch (NumberFormatException e) {
            Debug.logWarning(propertyName + " is not a valid integer - using default " + defaultValue, MODULE);
        } catch (Exception e) {
            Debug.logWarning(e, "purgeOldTestReports: could not read " + propertyName + ", using default "
                    + defaultValue, MODULE);
        }
        return defaultValue;
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
