/*******************************************************************************
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
 *******************************************************************************/
package org.apache.ofbiz.testtools;

import java.io.File;
import java.io.FileOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.testtools.report.TestReportArchiver;
import org.apache.ofbiz.testtools.report.TestRunManifest;

/**
 * REST-triggered test execution: runTestSuite kicks off a testdef {@code <test-suite>} (optionally
 * one {@code case-name} within it) asynchronously and returns a runId; getTestRunStatus polls it.
 * Reuses the exact same in-JVM engine {@code ofbiz --test} uses - JunitSuiteWrapper/ModelTestSuite/
 * TestRunContainer.runSuiteEntries() - unchanged; the only new execution-side behavior is arming
 * JupiterTestExtension.CURRENT_TEST_PARAMS with the caller's testParams map (see
 * TestRunContainer's new runSuiteEntries() overload).
 *
 * <p>Runs execute one at a time on a dedicated single-threaded executor: a second runTestSuite
 * call while one is in progress queues behind it rather than running concurrently.
 */
public final class TestRunServices {

    private static final String MODULE = TestRunServices.class.getName();
    private static final String RESOURCE = "testtools";
    private static final String TESTEXEC_PERMISSION = "TESTEXEC_ADMIN";

    static final TestRunTracker TRACKER = new TestRunTracker();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "TestRunServices-worker");
        thread.setDaemon(true);
        return thread;
    });

    private TestRunServices() {
    }

    public static Map<String, Object> runTestSuite(DispatchContext dctx, Map<String, ?> context) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String userLoginId = userLogin == null ? "unknown" : userLogin.getString("userLoginId");
        String suiteName = (String) context.get("suiteName");
        String componentName = (String) context.get("componentName");
        String testCaseName = (String) context.get("testCaseName");
        Map<String, Object> testParams = UtilGenerics.cast(context.get("testParams"));
        if (testParams == null) {
            testParams = Map.of();
        }

        if (!dctx.getSecurity().hasPermission(TESTEXEC_PERMISSION, userLogin)) {
            Debug.logWarning("runTestSuite: DENIED for user '" + userLoginId + "', suite '" + suiteName + "'"
                    + " - missing " + TESTEXEC_PERMISSION, MODULE);
            return ServiceUtil.returnError("You do not have permission to trigger test runs (" + TESTEXEC_PERMISSION + ")");
        }

        boolean apiEnabled = "true".equalsIgnoreCase(readStringProperty(dctx.getDelegator(), "test.api.enabled", "false"));
        if (!apiEnabled) {
            Debug.logWarning("runTestSuite: rejected for user '" + userLoginId + "', suite '" + suiteName + "'"
                    + " - test.api.enabled is false", MODULE);
            return ServiceUtil.returnError("The test execution API is disabled in this environment (test.api.enabled=false)");
        }

        JunitSuiteWrapper wrapper = new JunitSuiteWrapper(componentName, suiteName, testCaseName);
        if (wrapper.getAllTestList().isEmpty()) {
            return ServiceUtil.returnError("No tests found (component=" + componentName + ", suiteName=" + suiteName
                    + ", testCaseName=" + testCaseName + ")");
        }

        String runId = UUID.randomUUID().toString();
        Map<String, Object> finalTestParams = testParams;
        TRACKER.register(runId, suiteName, userLoginId, testParams);
        Debug.logInfo("runTestSuite: STARTED runId=" + runId + " user='" + userLoginId + "' suite='" + suiteName
                + "' testCaseName='" + testCaseName + "' testParams=" + testParams, MODULE);

        EXECUTOR.submit(() -> executeRun(runId, suiteName, finalTestParams, wrapper));

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("runId", runId);
        return result;
    }

    /**
     * Runs every ModelTestSuite the wrapper resolved (normally exactly one - see
     * JunitSuiteWrapper's suite-name filtering), reporting through a per-run SuiteXmlReportWriter
     * so JUnitXmlCounter/TestReportArchiver see only this run's results, then updates the tracker
     * and - when test.history is enabled - archives into the same manifest.json history
     * gradlew test/testIntegration already write to, tagged trigger="api".
     */
    private static void executeRun(String runId, String suiteName, Map<String, Object> testParams, JunitSuiteWrapper wrapper) {
        TRACKER.markRunning(runId);
        String ofbizHome = System.getProperty("ofbiz.home", ".");
        File runDir = new File(ofbizHome, "runtime/logs/test-results/api-runs/" + runId);
        runDir.mkdirs();

        try {
            boolean allPassed = true;
            for (ModelTestSuite modelSuite : wrapper.getModelTestSuites()) {
                File xmlFile = new File(runDir, modelSuite.getSuiteName() + ".xml");
                SuiteXmlReportWriter xmlSink = new SuiteXmlReportWriter(new FileOutputStream(xmlFile));
                xmlSink.startSuite(modelSuite.getSuiteName());
                TestRunContainer.runSuiteEntries(modelSuite.getPreparedTestList(), modelSuite.getDelegator(),
                        modelSuite.getDispatcher(), testParams, xmlSink);
                modelSuite.getDelegator().rollback();
                xmlSink.endSuite();
                allPassed = allPassed && xmlSink.wasSuccessful();
            }

            Map<String, Object> resultSummary = archiveIfEnabled(runId, suiteName, testParams, runDir, allPassed);
            if (allPassed) {
                TRACKER.markPassed(runId, resultSummary);
            } else {
                TRACKER.markFailed(runId, resultSummary);
            }
            Debug.logInfo("runTestSuite: " + (allPassed ? "PASSED" : "FAILED") + " runId=" + runId, MODULE);
        } catch (Exception e) {
            Debug.logError(e, "runTestSuite: ERROR runId=" + runId, MODULE);
            TRACKER.markError(runId, e);
        }
    }

    private static Map<String, Object> archiveIfEnabled(String runId, String suiteName, Map<String, Object> testParams,
            File runDir, boolean allPassed) {
        // Reuses testtools.properties' existing test.history flag (same gate TestReportPurgeService
        // already checks) rather than introducing a second, separate toggle for the API-triggered
        // path - if you haven't opted into persisted history at all, an API-triggered run's tracker
        // entry (in-memory, for polling) is still fully functional, it's just not also archived.
        Delegator delegator = null;
        String testHistory = readStringProperty(delegator, "test.history", "false");
        if (!"true".equalsIgnoreCase(testHistory)) {
            return UtilMisc.toMap("archived", false);
        }
        try {
            String ofbizHome = System.getProperty("ofbiz.home", ".");
            String integrationHistoryPath = readStringProperty(delegator, "test.history.integration.dir",
                    "runtime/logs/test-reports-history");
            File baseDir = new File(integrationHistoryPath);
            if (!baseDir.isAbsolute()) {
                baseDir = new File(ofbizHome, integrationHistoryPath);
            }
            Map<String, String> paramsUsed = new LinkedHashMap<>();
            testParams.forEach((key, value) -> paramsUsed.put(key, String.valueOf(value)));

            TestRunManifest manifest = TestReportArchiver.archive(new TestReportArchiver.ArchiveRequest(
                    baseDir, new File(ofbizHome), suiteName, "api", allPassed ? "PASSED" : "FAILED",
                    runDir, null, "api", paramsUsed));

            return UtilMisc.toMap("archived", true, "total", manifest.getCounts().getTotal(),
                    "passed", manifest.getCounts().getPassed(), "failed", manifest.getCounts().getFailed(),
                    "skipped", manifest.getCounts().getSkipped());
        } catch (Exception e) {
            Debug.logWarning(e, "runTestSuite: runId=" + runId + " archiving failed (run itself still succeeded/failed"
                    + " as reported above)", MODULE);
            return UtilMisc.toMap("archived", false);
        }
    }

    public static Map<String, Object> getTestRunStatus(DispatchContext dctx, Map<String, ?> context) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String userLoginId = userLogin == null ? "unknown" : userLogin.getString("userLoginId");
        if (!dctx.getSecurity().hasPermission(TESTEXEC_PERMISSION, userLogin)) {
            Debug.logWarning("getTestRunStatus: DENIED for user '" + userLoginId + "' - missing " + TESTEXEC_PERMISSION, MODULE);
            return ServiceUtil.returnError("You do not have permission to view test run status (" + TESTEXEC_PERMISSION + ")");
        }

        String runId = (String) context.get("runId");
        TestRunRecord record = TRACKER.get(runId);
        if (record == null) {
            return ServiceUtil.returnError("No such runId: " + runId);
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("status", record.status().name());
        Map<String, Object> resultSummary = new LinkedHashMap<>();
        if (record.resultSummary() != null) {
            resultSummary.putAll(record.resultSummary());
        }
        if (record.errorMessage() != null) {
            resultSummary.put("errorMessage", record.errorMessage());
        }
        result.put("resultSummary", resultSummary);
        return result;
    }

    private static String readStringProperty(Delegator delegator, String propertyName, String defaultValue) {
        try {
            String value = delegator == null
                    ? org.apache.ofbiz.base.util.UtilProperties.getPropertyValue(RESOURCE, propertyName, defaultValue)
                    : EntityUtilProperties.getPropertyValue(RESOURCE, propertyName, delegator);
            return UtilValidate.isNotEmpty(value) ? value.trim() : defaultValue;
        } catch (Exception e) {
            Debug.logWarning(e, "TestRunServices: could not read " + propertyName + ", using default '"
                    + defaultValue + "'", MODULE);
            return defaultValue;
        }
    }
}
