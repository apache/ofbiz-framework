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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.ofbiz.base.container.ContainerException;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceContainer;
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
 * <p>runTestSuite also accepts an optional {@code testMethodName}, reusing the exact same
 * fail-closed validators and method-scoped selection the {@code ofbiz --test method=} CLI path
 * built (see {@link TestRunContainer#validateMethodRequiresCase},
 * {@link TestRunContainer#validateMethodAppliesToSuite}) - both run synchronously inside
 * runTestSuite itself, before a run is ever queued, so a bad testMethodName/testCaseName
 * combination never receives a runId.
 *
 * <p>Runs execute one at a time on a dedicated single-threaded executor: a second runTestSuite
 * call while one is in progress queues behind it rather than running concurrently.
 *
 * <p><b>Known POC limitation - dispatcher/delegator resource leak (one piece mitigated; the rest is
 * not).</b> {@code new JunitSuiteWrapper(...)} reuses {@code ModelTestSuite}'s constructor, which
 * unconditionally creates a fresh test {@code Delegator}/{@code LocalDispatcher} via
 * {@code ServiceContainer.getLocalDispatcher(...)} - the exact same construction path the
 * {@code ofbiz --test} CLI already uses once per process. For the CLI this is harmless: the JVM
 * exits right after. Here, every API-triggered {@code runTestSuite} call goes through that same
 * path inside a long-lived server process, so {@code executeRun} now removes each suite's
 * dispatcher entry from {@code ServiceContainer}'s static dispatcher cache
 * ({@code ServiceContainer.removeFromCache(name)}) once that suite is done, regardless of outcome -
 * including suites {@code JunitSuiteWrapper} itself discarded for having no matching tests (see
 * {@code JunitSuiteWrapper#getDiscardedModelTestSuites}), which {@code runTestSuite} also cleans up
 * before returning its "no tests found" error. <b>Deliberately {@code removeFromCache}, not
 * {@code ServiceContainer.deregister}</b>: {@code deregister} calls {@code LocalDispatcher.deregister()},
 * which shuts the backing {@code ServiceDispatcher} down as soon as its {@code localContext} empties
 * out - always true immediately for a dispatcher used by exactly one test run - and that shutdown
 * closes JMS listeners through a process-wide singleton ({@code JmsListenerFactory}) shared by every
 * dispatcher in the JVM, including the live server's real one. See
 * {@code deregisterTestDispatcher}'s own javadoc for the full chain.
 *
 * <p>What this fix does <b>not</b> touch, at all: the heavier {@code ServiceDispatcher} instance
 * backing each run - its {@code Security}, {@code JobManager} reference, now-empty
 * {@code localContext} - stays in {@code ServiceDispatcher}'s own separate static
 * {@code dispatchers} cache indefinitely, and so does the test {@code Delegator} itself, since
 * {@code ServiceDispatcher} pins its delegator reference for as long as that instance remains
 * cached. No public API anywhere in the framework removes an entry from that map, and adding one
 * would mean modifying {@code ServiceDispatcher} itself, which is out of scope for this fix.
 * Startup services also still re-run on every single {@code runTestSuite} call regardless of this
 * fix: {@code ModelTestSuite}'s constructor gives each run's delegator a unique name, so
 * {@code ServiceDispatcher.getInstance(delegator)} always misses that cache and initializes a new
 * {@code ServiceDispatcher} - removing the previous run's dispatcher cache entry does not change
 * that, since the next run's key never collided with it in the first place.
 *
 * <p><b>Runs execute against the live server's database.</b> An API-triggered run is not an isolated
 * sandbox: {@code modelSuite.getDelegator().rollback()} is called after each suite as a best-effort
 * cleanup, but there is no transactional isolation from the rest of the running server, and any
 * side effect a test performs outside that delegator's own transaction (e.g. a service that commits
 * independently) is not undone. Anyone deciding whether to enable {@code test.api.enabled} (see
 * testtools.properties) should weigh both of the limitations above.
 *
 * <p><b>Do not expose {@link #runTestSuite}/{@link #getTestRunStatus} directly in a component's own
 * {@code *.rest.xml}.</b> Both accept/report an arbitrary {@code componentName} and so can trigger or
 * poll any component's tests, not just the exposing component's own - a component-branded REST
 * endpoint must instead wrap {@link #runScopedTestSuite}/{@link #getScopedTestRunStatus} with its own
 * fixed component name, the way {@code plugins/example}'s {@code ExampleTestRunServices} does. Writing
 * {@code <service name="runTestSuite"/>} straight into a {@code *.rest.xml} reproduces the exact
 * cross-component-reach problem the scoped wrappers exist to close.
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
        String testMethodName = TestRunContainer.normalizeMethodName((String) context.get("testMethodName"));
        Map<String, Object> testParams = UtilGenerics.cast(context.get("testParams"));
        if (testParams == null) {
            testParams = Map.of();
        }
        // Normalize to an immutable, null-tolerant copy from here on: this prevents any mutation
        // by downstream code (executor/test/archiver) from corrupting the caller's map or causing
        // NPE inside Map.copyOf(...) if a test param value is null. LinkedHashMap+unmodifiableMap
        // tolerates null values, unlike Map.copyOf.
        testParams = Collections.unmodifiableMap(new LinkedHashMap<>(testParams));

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

        // testMethodName reuses the exact same fail-closed validators the ofbiz --test method=
        // CLI path already built (TestRunContainer, same package - see its javadoc for the full
        // rationale). This first check needs only the two raw strings, not any resolved suite, so
        // it runs before JunitSuiteWrapper is even constructed: it fails faster (no wasted
        // dispatcher/delegator construction for a malformed request) and mirrors how the CLI's own
        // init() validates before start() resolves anything. The returned error text is REST's own
        // wording (testMethodName/testCaseName), not e.getMessage()'s CLI phrasing (--test
        // method=/case=) - a REST caller never typed --test, so that vocabulary would be confusing
        // on this surface. The log line keeps e.getMessage() verbatim for CLI-consistent debugging.
        try {
            TestRunContainer.validateMethodRequiresCase(testMethodName, testCaseName);
        } catch (ContainerException e) {
            Debug.logWarning("runTestSuite: rejected for user '" + userLoginId + "', suite '" + suiteName + "'"
                    + " - " + e.getMessage(), MODULE);
            return ServiceUtil.returnError("testMethodName requires testCaseName to also be specified - "
                    + "testMethodName scopes a single case's class down to one @Test method, so testCaseName "
                    + "is needed to identify which class that is.");
        }

        // Initialized to null (not left blank): the catch (ContainerException e) block below reads
        // wrapper, and per Java's definite-assignment rules a catch block never sees an assignment
        // made inside its own try body - regardless of exception type or where in the block that
        // assignment sits - unless the variable was already assigned before the try started. The
        // pre-existing catch (Exception e) below compiled fine without this only because it never
        // dereferences wrapper.
        JunitSuiteWrapper wrapper = null;
        try {
            wrapper = new JunitSuiteWrapper(componentName, suiteName, testCaseName);
            if (wrapper.getAllTestList().isEmpty()) {
                // The wrapper's constructor still creates a dispatcher/delegator pair for every
                // <test-suite> element it discarded along the way (see
                // JunitSuiteWrapper#getDiscardedModelTestSuites) - unlike the per-suite loop in
                // executeRun(), nothing else will ever reach these, so deregister them here before
                // returning the error, or every "no tests found" request leaks one dispatcher.
                for (ModelTestSuite discarded : wrapper.getDiscardedModelTestSuites()) {
                    deregisterTestDispatcher(discarded);
                }
                return ServiceUtil.returnError("No tests found (component=" + componentName + ", suiteName=" + suiteName
                        + ", testCaseName=" + testCaseName + ")");
            }
            // Second half of the method= validation: this one needs each resolved suite's actual
            // entries, so it can only run once the wrapper above has resolved them. Still entirely
            // synchronous, before EXECUTOR.submit below - a bad testMethodName/testCaseName
            // combination must never reach a queued run, matching the CLI's "fails before anything
            // runs" guarantee. Guarded on testMethodName != null, mirroring the identical guard on
            // TestRunContainer.start()'s own pre-loop pass (CLI side): without it, every
            // testMethodName-less request - the overwhelming majority of callers - would still pay
            // for this pass, and a prepare-time throw from getPreparedTestList() would newly abort
            // the request before any run is queued instead of leaving today's behavior unchanged.
            if (testMethodName != null) {
                for (ModelTestSuite modelSuite : wrapper.getModelTestSuites()) {
                    TestRunContainer.validateMethodAppliesToSuite(testMethodName, modelSuite.getSuiteName(),
                            modelSuite.getPreparedTestList());
                }
            }
        } catch (ContainerException e) {
            // Only validateMethodAppliesToSuite (above) can reach this - JunitSuiteWrapper's own
            // constructor declares no checked exception, so wrapper is always assigned by the time
            // this catch block can run. Both getModelTestSuites() (real tests, just not the
            // requested method) and getDiscardedModelTestSuites() (zero-entry suites the wrapper's
            // constructor still built a dispatcher for) need cleanup here, the same as the "no tests
            // found" branch above gives its own discarded suites.
            for (ModelTestSuite modelSuite : wrapper.getModelTestSuites()) {
                deregisterTestDispatcher(modelSuite);
            }
            for (ModelTestSuite discarded : wrapper.getDiscardedModelTestSuites()) {
                deregisterTestDispatcher(discarded);
            }
            Debug.logWarning("runTestSuite: rejected for user '" + userLoginId + "', suite '" + suiteName + "'"
                    + " - " + e.getMessage(), MODULE);
            return ServiceUtil.returnError("testMethodName was given, but the resolved testCaseName did not "
                    + "include a jupiter-test-suite entry in suite '" + suiteName + "' - testMethodName only "
                    + "applies to Jupiter (JUnit 5) test classes.");
        } catch (Exception e) {
            // wrapper may or may not be assigned here (a throw from "new JunitSuiteWrapper(...)"
            // itself leaves it null; nothing else in this try block is a realistic throw source
            // today, but the null-check costs nothing and keeps this branch correct regardless).
            if (wrapper != null) {
                for (ModelTestSuite modelSuite : wrapper.getModelTestSuites()) {
                    deregisterTestDispatcher(modelSuite);
                }
                for (ModelTestSuite discarded : wrapper.getDiscardedModelTestSuites()) {
                    deregisterTestDispatcher(discarded);
                }
            }
            Debug.logError(e, "runTestSuite: failed to resolve suite '" + suiteName + "' (component=" + componentName
                    + ", testCaseName=" + testCaseName + ")", MODULE);
            return ServiceUtil.returnError("Unable to resolve the requested test suite (component=" + componentName
                    + ", suiteName=" + suiteName + ", testCaseName=" + testCaseName + "): " + e.getMessage());
        }

        String runId = UUID.randomUUID().toString();
        Map<String, Object> finalTestParams = testParams;
        // wrapper is only ever assigned once for real (the "= null" initializer above exists
        // solely so the catch (ContainerException e) block above compiles - see its comment); this
        // second, single-assignment copy is what the EXECUTOR.submit lambda below actually
        // captures, the same effectively-final-copy pattern finalTestParams uses above.
        JunitSuiteWrapper finalWrapper = wrapper;
        TRACKER.register(runId, suiteName, componentName, userLoginId, testParams);
        Debug.logInfo("runTestSuite: STARTED runId=" + runId + " user='" + userLoginId + "' suite='" + suiteName
                + "' testCaseName='" + testCaseName + "' testMethodName='" + testMethodName + "' testParams=" + testParams,
                MODULE);

        EXECUTOR.submit(() -> executeRun(runId, suiteName, finalTestParams, testMethodName, finalWrapper));

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("runId", runId);
        return result;
    }

    /**
     * Runs {@link #runTestSuite} with {@code componentName} forced to {@code fixedComponentName},
     * regardless of whatever value (if any) the caller's own context map contains - a caller-supplied
     * componentName is silently overwritten, never honored. Built for component-scoped wrapper
     * services (e.g. plugins/example's runExampleTestSuite): builds a new context map rather than
     * mutating the one it's given, the same defensive-copy discipline runTestSuite itself already
     * applies to testParams - a caller-controlled map must never be assumed safe to mutate in place.
     * @param dctx the dispatch context
     * @param context the caller's service context - not mutated
     * @param fixedComponentName the only component this call is allowed to resolve suites from
     * @return the runTestSuite result
     */
    public static Map<String, Object> runScopedTestSuite(DispatchContext dctx, Map<String, ?> context,
            String fixedComponentName) {
        // Fail closed, not open: an empty/null fixedComponentName must never reach runTestSuite's
        // context map. ComponentConfig.matchingComponentName treats a null cname as "match every
        // component" - if this guard were skipped, the entire scoping mechanism runScopedTestSuite
        // exists for would silently degrade to fully unscoped behavior instead of erroring out.
        if (UtilValidate.isEmpty(fixedComponentName)) {
            return ServiceUtil.returnError("runScopedTestSuite requires a fixed componentName");
        }
        Map<String, Object> scopedContext = new HashMap<>(context);
        scopedContext.put("componentName", fixedComponentName);
        return runTestSuite(dctx, scopedContext);
    }

    /**
     * Runs every ModelTestSuite the wrapper resolved (normally exactly one - see
     * JunitSuiteWrapper's suite-name filtering), reporting through a per-run SuiteXmlReportWriter
     * so JUnitXmlCounter/TestReportArchiver see only this run's results, then updates the tracker
     * and - when test.history is enabled - archives into the same manifest.json history
     * gradlew test/testIntegration already write to, tagged trigger="api".
     *
     * @param testMethodName when non-null (already validated against every suite by
     *     runTestSuite() before this was ever queued), scopes each suite's Jupiter entry to this
     *     one method - see TestRunContainer's 6-arg runSuiteEntries() overload
     */
    private static void executeRun(String runId, String suiteName, Map<String, Object> testParams, String testMethodName,
            JunitSuiteWrapper wrapper) {
        TRACKER.markRunning(runId);
        String ofbizHome = System.getProperty("ofbiz.home", ".");
        File runDir = new File(ofbizHome, "runtime/logs/test-results/api-runs/" + runId);
        runDir.mkdirs();

        try {
            boolean allPassed = true;
            List<ModelTestSuite> modelTestSuites = wrapper.getModelTestSuites();
            // Count of suites the loop below has entered (i.e. reached its own try/finally, which
            // deregisters that suite's dispatcher on any outcome). Used by the outer finally to
            // deregister whichever suites the loop never got to reach at all, in case some suite's
            // exception escaped its own try/finally and aborted the loop early.
            int startedCount = 0;
            try {
                for (ModelTestSuite modelSuite : modelTestSuites) {
                    startedCount++;
                    SuiteXmlReportWriter xmlSink = null;
                    try {
                        // FileOutputStream/startSuite are inside this try (not before it) so that a
                        // failure creating/starting the report for this suite still reaches the
                        // finally below and deregisters this suite's dispatcher, instead of leaking it
                        // and aborting the loop with every later suite's dispatcher still registered too.
                        File xmlFile = new File(runDir, modelSuite.getSuiteName() + ".xml");
                        xmlSink = new SuiteXmlReportWriter(new FileOutputStream(xmlFile));
                        xmlSink.startSuite(modelSuite.getSuiteName());
                        TestRunContainer.runSuiteEntries(modelSuite.getPreparedTestList(), modelSuite.getDelegator(),
                                modelSuite.getDispatcher(), testParams, testMethodName, xmlSink);
                        modelSuite.getDelegator().rollback();
                    } finally {
                        // endSuite() is the only place that actually flushes/writes/closes the underlying
                        // FileOutputStream (see SuiteXmlReportWriter#writeAndClose) - without this finally,
                        // an exception from runSuiteEntries()/rollback() would leak the stream (a real
                        // file-descriptor leak in this long-lived server) and leave a 0-byte XML on disk.
                        if (xmlSink != null) {
                            xmlSink.endSuite();
                        }
                        // Best-effort dispatcher cleanup - see this class's javadoc for exactly what this
                        // does and does not remove. Runs regardless of the suite's outcome, same as
                        // endSuite() above.
                        deregisterTestDispatcher(modelSuite);
                    }
                    allPassed = allPassed && xmlSink.wasSuccessful();
                }
            } finally {
                // If an exception unwound out of the loop above (e.g. FileOutputStream/startSuite
                // failing on a suite past the first), every suite at or after startedCount never got
                // a chance to run its own finally. Deregister those here so one bad suite doesn't
                // leave every suite queued after it permanently registered.
                for (int i = startedCount; i < modelTestSuites.size(); i++) {
                    deregisterTestDispatcher(modelTestSuites.get(i));
                }
            }

            Map<String, Object> resultSummary = archiveIfEnabled(runId, suiteName, testParams, runDir, allPassed);
            if (allPassed) {
                TRACKER.markPassed(runId, resultSummary);
            } else {
                TRACKER.markFailed(runId, resultSummary);
            }
            Debug.logInfo("runTestSuite: " + (allPassed ? "PASSED" : "FAILED") + " runId=" + runId, MODULE);
        } catch (Throwable t) {
            // Catches Throwable, not just Exception - matching TestRunContainer.start()'s own
            // last-resort net around runSuiteEntries() - so a test class's Error (NoClassDefFoundError,
            // StackOverflowError, OOM, ...) can't escape uncaught here. Left as Exception, such an Error
            // would bypass TRACKER.markError(...) entirely, leaving this run's tracked status stuck at
            // RUNNING forever - a polling caller would hang indefinitely with no timeout to save it.
            Debug.logError(t, "runTestSuite: ERROR runId=" + runId, MODULE);
            TRACKER.markError(runId, t);
        }
    }

    /**
     * Removes the {@code ServiceContainer.DISPATCHER_CACHE} entry for the test dispatcher
     * {@code ModelTestSuite}'s constructor created for this suite, via
     * {@code ServiceContainer.removeFromCache} - <b>not</b> {@code ServiceContainer.deregister}. This
     * distinction matters: {@code deregister} calls {@code LocalDispatcher.deregister()}, which (via
     * {@code ServiceDispatcher.deregister}) shuts the {@code ServiceDispatcher} down once its
     * {@code localContext} map empties out - and for a dispatcher used by exactly one test run, that is
     * always immediately. Shutdown calls {@code JmsListenerFactory.closeListeners()}, and
     * {@code JmsListenerFactory} is a process-wide singleton over a static listener map shared by every
     * dispatcher in the JVM - so {@code deregister} here would close the live server's real JMS listeners
     * on every single test run, not just this run's own (nonexistent) ones. {@code removeFromCache} only
     * removes the cache entry and touches none of that - see this class's javadoc for exactly what is and
     * is not cleaned up as a result.
     *
     * <p>Best-effort only: any failure or error here is logged, never rethrown - including an
     * {@code Error}, not just an {@code Exception} - since a cleanup failure must never turn an otherwise
     * passing test run into a reported failure/error.
     */
    private static void deregisterTestDispatcher(ModelTestSuite modelSuite) {
        try {
            String dispatcherName = modelSuite.getDispatcher().getName();
            ServiceContainer.removeFromCache(dispatcherName);
        } catch (Throwable t) {
            Debug.logWarning(t, "runTestSuite: failed to deregister test dispatcher for suite '" + modelSuite.getSuiteName()
                    + "' (best-effort cleanup only, run result is unaffected)", MODULE);
        }
    }

    private static Map<String, Object> archiveIfEnabled(String runId, String suiteName, Map<String, Object> testParams,
            File runDir, boolean allPassed) {
        // Reuses testtools.properties' existing test.history flag (same gate TestReportPurgeService
        // already checks) rather than introducing a second, separate toggle for the API-triggered
        // path - if you haven't opted into persisted history at all, an API-triggered run's tracker
        // entry (in-memory, for polling) is still fully functional, it's just not also archived.
        // Note for the implementer: unlike test.api.enabled above (read via EntityUtilProperties,
        // delegator-aware, so a live SystemProperty override applies), test.history here is read with
        // delegator=null - this method runs on the EXECUTOR's background thread, not a request thread
        // with a live Delegator in hand - so it is file-only: a SystemProperty override of test.history
        // will NOT apply to API-triggered runs. Intentional, but worth knowing before relying on it.
        String testHistory = readStringProperty(null, "test.history", "false");
        if (!"true".equalsIgnoreCase(testHistory)) {
            return UtilMisc.toMap("archived", false);
        }
        try {
            String ofbizHome = System.getProperty("ofbiz.home", ".");
            String integrationHistoryPath = readStringProperty(null, "test.history.integration.dir",
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
        result.put("componentName", record.componentName());
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

    /**
     * Runs {@link #getTestRunStatus} and, if the run exists, checks its recorded componentName
     * against {@code expectedComponentName} - on a mismatch, returns the exact same "No such runId"
     * error a genuinely unknown runId would produce (never a distinguishable "wrong component"
     * message), so polling can't be used to detect the mere existence of another component's runs. A
     * permission-denied result from the underlying call passes through unchanged - the permission
     * check still runs first, exactly as it does for the unscoped getTestRunStatus.
     * @param dctx the dispatch context
     * @param context the caller's service context
     * @param expectedComponentName the only component this call is allowed to report on
     * @return the getTestRunStatus result, or a masked "No such runId" error on a component mismatch
     */
    public static Map<String, Object> getScopedTestRunStatus(DispatchContext dctx, Map<String, ?> context,
            String expectedComponentName) {
        // Fail closed, not open: an empty/null expectedComponentName must never reach the
        // equality check below - unlike runScopedTestSuite's fixedComponentName (which fails open
        // by matching every component), a null here would instead throw a raw NullPointerException
        // out of expectedComponentName.equals(...), a different and equally unacceptable failure
        // mode. Guard against both up front so this helper always fails the same clean way.
        if (UtilValidate.isEmpty(expectedComponentName)) {
            return ServiceUtil.returnError("getScopedTestRunStatus requires an expectedComponentName");
        }
        Map<String, Object> result = getTestRunStatus(dctx, context);
        if (ServiceUtil.isError(result)) {
            return result;
        }
        if (!expectedComponentName.equals(result.get("componentName"))) {
            String runId = (String) context.get("runId");
            return ServiceUtil.returnError("No such runId: " + runId);
        }
        return result;
    }

    private static String readStringProperty(Delegator delegator, String propertyName, String defaultValue) {
        try {
            String value = delegator == null
                    ? UtilProperties.getPropertyValue(RESOURCE, propertyName, defaultValue)
                    : EntityUtilProperties.getPropertyValue(RESOURCE, propertyName, delegator);
            return UtilValidate.isNotEmpty(value) ? value.trim() : defaultValue;
        } catch (Exception e) {
            Debug.logWarning(e, "TestRunServices: could not read " + propertyName + ", using default '"
                    + defaultValue + "'", MODULE);
            return defaultValue;
        }
    }
}
