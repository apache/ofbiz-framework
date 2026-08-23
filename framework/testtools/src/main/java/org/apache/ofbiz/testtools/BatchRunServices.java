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
import java.util.List;
import java.util.stream.Collectors;

import org.apache.ofbiz.base.component.ComponentConfig;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;

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
 * explicit {@code components} list, every named component must itself pass that same check or the
 * whole call is rejected before anything is queued - no partial batch is ever queued from a bad
 * list.
 *
 * <p>Each queued component always runs its whole suite - {@code runTestSuite} is called with only
 * {@code componentName} set, no {@code suiteName}, which {@link JunitSuiteWrapper} already treats
 * as "every testdef suite in this component" (see its {@code suiteName != null} filter). No
 * per-component {@code testCaseName}/{@code testMethodName}/{@code testParams} scoping in this
 * endpoint.
 *
 * <p>Exposed directly via the generic, framework-owned
 * {@code framework/testtools/api/testruns.rest.xml} endpoint
 * ({@code POST /rest/testtools/testruns/batch}, {@code GET /rest/testtools/testruns/batch/{batchId}}).
 * See {@link TestRunServices}' javadoc for the same "do not expose from a component's own
 * *.rest.xml" caution - it applies here identically.
 */
public final class BatchRunServices {

    private BatchRunServices() {
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
        boolean apiEnabled = "true".equalsIgnoreCase(TestRunServices.readStringProperty(delegator, "test.api.enabled", "false"));
        if (!apiEnabled) {
            return List.of();
        }
        return ComponentConfig.getAllTestSuiteInfos(null).stream()
                .map(info -> info.getComponentConfig().getComponentName())
                .distinct()
                .filter(name -> "true".equalsIgnoreCase(
                        TestRunServices.readStringProperty(delegator, "test.api.enabled." + name, "true")))
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
        boolean apiEnabled = "true".equalsIgnoreCase(TestRunServices.readStringProperty(delegator, "test.api.enabled", "false"));
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
            boolean componentEnabled = "true".equalsIgnoreCase(
                    TestRunServices.readStringProperty(delegator, "test.api.enabled." + name, "true"));
            if (!componentEnabled) {
                invalid.add(name + " (test execution API is disabled for this component)");
            }
        }
        return invalid;
    }
}
