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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.ofbiz.base.component.ComponentConfig;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * REST-triggered batch test execution: runBatchTestSuite fans a full-suite
 * {@link TestRunServices#runTestSuite} call out to multiple components in one call, tracked under
 * one batchId; getBatchTestRunStatus polls the aggregate. Layered entirely on top of
 * TestRunServices/TestRunTracker - no change to the existing single-component
 * runTestSuite/getTestRunStatus contract.
 *
 * <p>Two modes: with no {@code components} list, every component with a testdef where both the
 * global {@code test.api.enabled} and its own {@code test.api.enabled.<componentName>} (default
 * {@code true}) resolve {@code true} is queued (see {@link #discoverEligibleComponents}); with an
 * explicit {@code components} list, an empty list is rejected outright, and every named component
 * must itself pass that same check or the whole call is rejected before anything is queued - no
 * batch is ever queued from an invalid or empty list. This upfront rejection is distinct from a
 * component that passes it but still fails its actual {@link TestRunServices#runTestSuite} call
 * (e.g. a testdef that resolves to zero tests): that component is simply omitted from the batch,
 * the same as an auto-discovered one would be - see {@link #runBatchTestSuite}.
 *
 * <p>Each queued component always runs its whole suite - {@code runTestSuite} is called with only
 * {@code componentName} set, no {@code suiteName}, which {@link JunitSuiteWrapper} already treats
 * as "every testdef suite in this component" (see its {@code suiteName != null} filter). No
 * per-component {@code testCaseName}/{@code testMethodName}/{@code testParams} scoping in this
 * endpoint.
 *
 * <p>{@code runBatchTestSuite} fans a component list out with a plain sequential loop, each
 * iteration calling {@code runTestSuite} synchronously before moving to the next - per the "no new
 * concurrency" constraint, nothing here runs in parallel or off the caller's thread. Each
 * {@code runTestSuite} call itself constructs a fresh test {@code Delegator}/{@code
 * LocalDispatcher} and re-runs startup services (see {@link TestRunServices}' own javadoc), so a
 * large auto-discovered component set can make the POST block for a while before a {@code batchId}
 * comes back - this is a known characteristic of the current POC-level implementation, not a bug.
 *
 * <p>Exposed directly via the generic, framework-owned
 * {@code framework/testtools/api/testruns.rest.xml} endpoint
 * ({@code POST /rest/testtools/testruns/batch}, {@code GET /rest/testtools/testruns/batch/{batchId}}).
 * See {@link TestRunServices}' javadoc for the same "do not expose from a component's own
 * *.rest.xml" caution - it applies here identically.
 */
public final class BatchRunServices {

    private static final String MODULE = BatchRunServices.class.getName();
    private static final String TESTEXEC_PERMISSION = "TESTEXEC_ADMIN";

    static final BatchRunTracker BATCH_TRACKER = new BatchRunTracker();

    private BatchRunServices() {
    }

    public static Map<String, Object> runBatchTestSuite(DispatchContext dctx, Map<String, ?> context) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String userLoginId = userLogin == null ? "unknown" : userLogin.getString("userLoginId");

        if (!dctx.getSecurity().hasPermission(TESTEXEC_PERMISSION, userLogin)) {
            Debug.logWarning("runBatchTestSuite: DENIED for user '" + userLoginId + "' - missing "
                    + TESTEXEC_PERMISSION, MODULE);
            return ServiceUtil.returnError("You do not have permission to trigger test runs (" + TESTEXEC_PERMISSION + ")");
        }

        List<String> requestedComponents = UtilGenerics.cast(context.get("components"));
        List<String> componentNames;
        if (requestedComponents == null) {
            componentNames = discoverEligibleComponents(dctx.getDelegator());
            if (componentNames.isEmpty()) {
                Debug.logWarning("runBatchTestSuite: rejected for user '" + userLoginId
                        + "' - no components are eligible", MODULE);
                return ServiceUtil.returnError("No components are eligible for a batch test run - none have "
                        + "the test execution API enabled, or none have a testdef.");
            }
        } else if (requestedComponents.isEmpty()) {
            Debug.logWarning("runBatchTestSuite: rejected for user '" + userLoginId
                    + "' - components list was explicitly empty", MODULE);
            return ServiceUtil.returnError("The components list cannot be empty - omit the field entirely to "
                    + "auto-discover eligible components, or name at least one component.");
        } else {
            List<String> invalid = validateRequestedComponents(requestedComponents, dctx.getDelegator());
            if (!invalid.isEmpty()) {
                Debug.logWarning("runBatchTestSuite: rejected for user '" + userLoginId + "' - invalid components: "
                        + invalid, MODULE);
                return ServiceUtil.returnError("The following components cannot be included in this batch run: "
                        + String.join("; ", invalid));
            }
            componentNames = requestedComponents.stream().distinct().toList();
        }

        String batchId = UUID.randomUUID().toString();
        List<BatchRunTracker.BatchChildRef> children = new ArrayList<>();
        for (String componentName : componentNames) {
            Map<String, Object> childContext = new LinkedHashMap<>();
            childContext.put("componentName", componentName);
            childContext.put("userLogin", userLogin);
            Map<String, Object> result = TestRunServices.runTestSuite(dctx, childContext);
            if (ServiceUtil.isError(result)) {
                // Silently omitted, matching the "auto-discovered component has no testdef tests"
                // edge case this same rule already covers - the only realistic ways an
                // already-eligibility-checked component can still fail here are a genuinely empty
                // testdef (registered but resolves to zero tests) or a live config change racing
                // between this batch's own eligibility check above and this call, both of which
                // mean there is no runId to show for this component either way.
                Debug.logWarning("runBatchTestSuite: batchId=" + batchId + " skipped component '" + componentName
                        + "' - " + ServiceUtil.getErrorMessage(result), MODULE);
                continue;
            }
            children.add(new BatchRunTracker.BatchChildRef(componentName, (String) result.get("runId")));
        }
        if (children.isEmpty()) {
            Debug.logWarning("runBatchTestSuite: rejected for user '" + userLoginId
                    + "' - every requested component's runTestSuite call failed, batchId=" + batchId, MODULE);
            return ServiceUtil.returnError("No components were successfully queued for this batch - every "
                    + "requested component's runTestSuite call failed.");
        }
        BATCH_TRACKER.register(batchId, children);

        List<String> queuedComponentNames = children.stream().map(BatchRunTracker.BatchChildRef::componentName).toList();
        Debug.logInfo("runBatchTestSuite: STARTED batchId=" + batchId + " user='" + userLoginId + "' components="
                + queuedComponentNames, MODULE);

        Map<String, Object> response = ServiceUtil.returnSuccess();
        response.put("batchId", batchId);
        return response;
    }

    public static Map<String, Object> getBatchTestRunStatus(DispatchContext dctx, Map<String, ?> context) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String userLoginId = userLogin == null ? "unknown" : userLogin.getString("userLoginId");
        if (!dctx.getSecurity().hasPermission(TESTEXEC_PERMISSION, userLogin)) {
            Debug.logWarning("getBatchTestRunStatus: DENIED for user '" + userLoginId + "' - missing "
                    + TESTEXEC_PERMISSION, MODULE);
            return ServiceUtil.returnError("You do not have permission to view test run status (" + TESTEXEC_PERMISSION + ")");
        }

        String batchId = (String) context.get("batchId");
        List<BatchRunTracker.BatchChildRef> children = BATCH_TRACKER.get(batchId);
        if (children == null) {
            return ServiceUtil.returnError("No such batchId: " + batchId);
        }

        List<Map<String, Object>> componentResults = new ArrayList<>();
        int passed = 0;
        int failed = 0;
        int running = 0;
        int queued = 0;
        int errored = 0;
        for (BatchRunTracker.BatchChildRef child : children) {
            // TestRunServices.TRACKER always has an entry for this runId by construction: it was
            // registered synchronously (TestRunServices.runTestSuite's own TRACKER.register call)
            // before that call ever returned the runId this child was built from, and nothing ever
            // removes an entry from that tracker.
            TestRunRecord record = TestRunServices.TRACKER.get(child.runId());
            componentResults.add(TestRunServices.describeRun(record));
            switch (record.status()) {
            case PASSED -> passed++;
            case FAILED -> failed++;
            case ERROR -> errored++;
            case RUNNING -> running++;
            case QUEUED -> queued++;
            }
        }

        String batchStatus;
        if (errored > 0) {
            batchStatus = "ERROR";
        } else if (failed > 0) {
            batchStatus = "FAILED";
        } else if (running > 0 || queued > 0) {
            batchStatus = "RUNNING";
        } else {
            batchStatus = "PASSED";
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", children.size());
        summary.put("passed", passed);
        summary.put("failed", failed);
        summary.put("running", running);
        summary.put("queued", queued);
        summary.put("error", errored);

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("status", batchStatus);
        result.put("summary", summary);
        result.put("components", componentResults);
        return result;
    }

    /**
     * Every component with a testdef where both the global {@code test.api.enabled} and its own
     * {@code test.api.enabled.<componentName>} (default {@code true}) resolve {@code true} -
     * short-circuits to an empty list without even consulting {@link ComponentConfig} when the
     * global flag alone is off.
     * @param delegator the calling request's Delegator, for the same live-overridable property
     *     read {@link TestRunServices#readStringProperty} already gives a single-component call
     * @return every eligible component's name, in {@link ComponentConfig}'s own declared order,
     *     each name appearing at most once even if it registers more than one testdef file
     */
    static List<String> discoverEligibleComponents(Delegator delegator) {
        if (!TestRunServices.isTestApiGloballyEnabled(delegator)) {
            return List.of();
        }
        return ComponentConfig.getAllTestSuiteInfos(null).stream()
                .map(info -> info.getComponentConfig().getComponentName())
                .distinct()
                .filter(name -> TestRunServices.isTestApiEnabledForComponent(delegator, name))
                .collect(Collectors.toList());
    }

    /**
     * Validates every caller-named component up front, before any of them is queued - a bad entry
     * must reject the whole call rather than silently running only the good ones (see this class's
     * javadoc).
     * @param requested the caller-supplied {@code components} list, as given (not yet validated)
     * @param delegator the calling request's Delegator
     * @return one human-readable reason string per invalid entry, empty if every entry is valid
     */
    static List<String> validateRequestedComponents(List<String> requested, Delegator delegator) {
        boolean apiEnabled = TestRunServices.isTestApiGloballyEnabled(delegator);
        List<String> invalid = new ArrayList<>();
        for (String name : requested) {
            if (UtilValidate.isEmpty(name)) {
                invalid.add(name + " (blank component name)");
                continue;
            }
            if (!apiEnabled) {
                invalid.add(name + " (test execution API is disabled)");
                continue;
            }
            if (!Boolean.TRUE.equals(ComponentConfig.componentExists(name))) {
                invalid.add(name + " (unknown component)");
                continue;
            }
            if (ComponentConfig.getAllTestSuiteInfos(name).isEmpty()) {
                invalid.add(name + " (no testdef found)");
                continue;
            }
            boolean componentEnabled = TestRunServices.isTestApiEnabledForComponent(delegator, name);
            if (!componentEnabled) {
                invalid.add(name + " (test execution API is disabled for this component)");
            }
        }
        return invalid;
    }
}
