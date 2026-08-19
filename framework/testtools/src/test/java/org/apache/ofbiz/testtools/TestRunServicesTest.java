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

import java.util.Map;

import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.DispatchContext;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TestRunServicesTest {

    @Test
    void runTestSuiteReturnsErrorWhenPermissionDenied() {
        DispatchContext dctx = mock(DispatchContext.class);
        Security security = mock(Security.class);
        Delegator delegator = mock(Delegator.class);
        GenericValue userLogin = mock(GenericValue.class);
        when(dctx.getSecurity()).thenReturn(security);
        when(dctx.getDelegator()).thenReturn(delegator);
        when(userLogin.getString("userLoginId")).thenReturn("nobody");
        when(security.hasPermission("TESTEXEC_ADMIN", userLogin)).thenReturn(false);

        Map<String, Object> result = TestRunServices.runTestSuite(dctx,
                Map.of("suiteName", "example-tests", "userLogin", userLogin));

        assertThat(result.get("responseMessage"), is("error"));
        assertThat(result.get("runId"), nullValue());
    }

    @Test
    void runTestSuiteReturnsErrorWhenApiDisabled() {
        // No stubbing of testtools.properties overrides: the real classpath resource
        // framework/testtools/config/testtools.properties ships test.api.enabled=false (Task 3),
        // and EntityUtilProperties falls through to it when delegator.findOne("SystemProperty", ...)
        // - unstubbed on this mock - returns null.
        DispatchContext dctx = mock(DispatchContext.class);
        Security security = mock(Security.class);
        Delegator delegator = mock(Delegator.class);
        GenericValue userLogin = mock(GenericValue.class);
        when(dctx.getSecurity()).thenReturn(security);
        when(dctx.getDelegator()).thenReturn(delegator);
        when(userLogin.getString("userLoginId")).thenReturn("admin");
        when(security.hasPermission("TESTEXEC_ADMIN", userLogin)).thenReturn(true);

        Map<String, Object> result = TestRunServices.runTestSuite(dctx,
                Map.of("suiteName", "example-tests", "userLogin", userLogin));

        assertThat(result.get("responseMessage"), is("error"));
        assertThat(result.get("runId"), nullValue());
    }

    @Test
    void getTestRunStatusReturnsErrorWhenPermissionDenied() {
        DispatchContext dctx = mock(DispatchContext.class);
        Security security = mock(Security.class);
        GenericValue userLogin = mock(GenericValue.class);
        when(dctx.getSecurity()).thenReturn(security);
        when(userLogin.getString("userLoginId")).thenReturn("nobody");
        when(security.hasPermission("TESTEXEC_ADMIN", userLogin)).thenReturn(false);

        Map<String, Object> result = TestRunServices.getTestRunStatus(dctx,
                Map.of("runId", "run-1", "userLogin", userLogin));

        assertThat(result.get("responseMessage"), is("error"));
    }

    @Test
    void getTestRunStatusReturnsErrorForAnUnknownRunId() {
        DispatchContext dctx = mock(DispatchContext.class);
        Security security = mock(Security.class);
        GenericValue userLogin = mock(GenericValue.class);
        when(dctx.getSecurity()).thenReturn(security);
        when(userLogin.getString("userLoginId")).thenReturn("admin");
        when(security.hasPermission("TESTEXEC_ADMIN", userLogin)).thenReturn(true);

        Map<String, Object> result = TestRunServices.getTestRunStatus(dctx,
                Map.of("runId", "no-such-run", "userLogin", userLogin));

        assertThat(result.get("responseMessage"), is("error"));
    }

    @Test
    void getTestRunStatusIncludesComponentName() {
        DispatchContext dctx = mock(DispatchContext.class);
        Security security = mock(Security.class);
        GenericValue userLogin = mock(GenericValue.class);
        when(dctx.getSecurity()).thenReturn(security);
        when(userLogin.getString("userLoginId")).thenReturn("admin");
        when(security.hasPermission("TESTEXEC_ADMIN", userLogin)).thenReturn(true);
        TestRunServices.TRACKER.register("component-check-run", "example-tests", "example", "admin", Map.of());

        Map<String, Object> result = TestRunServices.getTestRunStatus(dctx,
                Map.of("runId", "component-check-run", "userLogin", userLogin));

        assertThat(result.get("componentName"), is("example"));
    }

    @Test
    void getScopedTestRunStatusReturnsRealDataWhenComponentMatches() {
        DispatchContext dctx = mock(DispatchContext.class);
        Security security = mock(Security.class);
        GenericValue userLogin = mock(GenericValue.class);
        when(dctx.getSecurity()).thenReturn(security);
        when(userLogin.getString("userLoginId")).thenReturn("admin");
        when(security.hasPermission("TESTEXEC_ADMIN", userLogin)).thenReturn(true);
        TestRunServices.TRACKER.register("scoped-run-match", "example-tests", "example", "admin", Map.of());

        Map<String, Object> result = TestRunServices.getScopedTestRunStatus(dctx,
                Map.of("runId", "scoped-run-match", "userLogin", userLogin), "example");

        assertThat(result.get("responseMessage"), is("success"));
        assertThat(result.get("componentName"), is("example"));
    }

    @Test
    void getScopedTestRunStatusMasksAMismatchedComponentAsUnknownRunId() {
        DispatchContext dctx = mock(DispatchContext.class);
        Security security = mock(Security.class);
        GenericValue userLogin = mock(GenericValue.class);
        when(dctx.getSecurity()).thenReturn(security);
        when(userLogin.getString("userLoginId")).thenReturn("admin");
        when(security.hasPermission("TESTEXEC_ADMIN", userLogin)).thenReturn(true);
        TestRunServices.TRACKER.register("scoped-run-mismatch", "content-tests", "content", "admin", Map.of());

        Map<String, Object> result = TestRunServices.getScopedTestRunStatus(dctx,
                Map.of("runId", "scoped-run-mismatch", "userLogin", userLogin), "example");

        assertThat(result.get("responseMessage"), is("error"));
        assertThat(result.get("errorMessage"), is("No such runId: scoped-run-mismatch"));
    }

    @Test
    void getScopedTestRunStatusPassesThroughPermissionDenialUnchanged() {
        DispatchContext dctx = mock(DispatchContext.class);
        Security security = mock(Security.class);
        GenericValue userLogin = mock(GenericValue.class);
        when(dctx.getSecurity()).thenReturn(security);
        when(userLogin.getString("userLoginId")).thenReturn("nobody");
        when(security.hasPermission("TESTEXEC_ADMIN", userLogin)).thenReturn(false);

        Map<String, Object> result = TestRunServices.getScopedTestRunStatus(dctx,
                Map.of("runId", "any-run", "userLogin", userLogin), "example");

        assertThat(result.get("responseMessage"), is("error"));
        assertThat(result.get("errorMessage"), is("You do not have permission to view test run status (TESTEXEC_ADMIN)"));
    }

    @Test
    void runScopedTestSuitePassesThroughPermissionDenialUnchanged() {
        // Cannot unit-test the componentName-forcing behavior itself in isolation - like
        // runTestSuite's own suite-resolution path, that needs a real bootstrapped ComponentConfig
        // (see this file's existing tests' pattern, and TestRunServices' own "Design note on
        // testability"). This test only confirms the delegation is wired correctly: a permission
        // denial passes straight through, and passing a deliberately mismatched componentName
        // ("content") in the caller's context doesn't cause a crash before the permission check
        // - proving nothing about whether the override happens, only that the wrapper doesn't
        // reject/mangle the call. The override itself is verified by manual/live validation (Task 5).
        DispatchContext dctx = mock(DispatchContext.class);
        Security security = mock(Security.class);
        Delegator delegator = mock(Delegator.class);
        GenericValue userLogin = mock(GenericValue.class);
        when(dctx.getSecurity()).thenReturn(security);
        when(dctx.getDelegator()).thenReturn(delegator);
        when(userLogin.getString("userLoginId")).thenReturn("nobody");
        when(security.hasPermission("TESTEXEC_ADMIN", userLogin)).thenReturn(false);

        Map<String, Object> result = TestRunServices.runScopedTestSuite(dctx,
                Map.of("suiteName", "example-tests", "componentName", "content", "userLogin", userLogin), "example");

        assertThat(result.get("responseMessage"), is("error"));
        assertThat(result.get("runId"), nullValue());
    }

    @Test
    void runScopedTestSuiteReturnsErrorForANullFixedComponentName() {
        // Must fail closed, not open: ComponentConfig.matchingComponentName treats a null cname as
        // "match every component", so skipping this guard would silently turn a null
        // fixedComponentName into fully unscoped behavior instead of an error. Uses a permissive
        // security mock so the guard - not the permission check - is what's actually exercised.
        DispatchContext dctx = mock(DispatchContext.class);
        Security security = mock(Security.class);
        Delegator delegator = mock(Delegator.class);
        GenericValue userLogin = mock(GenericValue.class);
        when(dctx.getSecurity()).thenReturn(security);
        when(dctx.getDelegator()).thenReturn(delegator);
        when(userLogin.getString("userLoginId")).thenReturn("admin");
        when(security.hasPermission("TESTEXEC_ADMIN", userLogin)).thenReturn(true);

        Map<String, Object> result = TestRunServices.runScopedTestSuite(dctx,
                Map.of("suiteName", "example-tests", "userLogin", userLogin), null);

        assertThat(result.get("responseMessage"), is("error"));
        assertThat(result.get("errorMessage"), is("runScopedTestSuite requires a fixed componentName"));
        assertThat(result.get("runId"), nullValue());
    }

    @Test
    void runScopedTestSuiteReturnsErrorForAnEmptyFixedComponentName() {
        DispatchContext dctx = mock(DispatchContext.class);
        Security security = mock(Security.class);
        Delegator delegator = mock(Delegator.class);
        GenericValue userLogin = mock(GenericValue.class);
        when(dctx.getSecurity()).thenReturn(security);
        when(dctx.getDelegator()).thenReturn(delegator);
        when(userLogin.getString("userLoginId")).thenReturn("admin");
        when(security.hasPermission("TESTEXEC_ADMIN", userLogin)).thenReturn(true);

        Map<String, Object> result = TestRunServices.runScopedTestSuite(dctx,
                Map.of("suiteName", "example-tests", "userLogin", userLogin), "");

        assertThat(result.get("responseMessage"), is("error"));
        assertThat(result.get("errorMessage"), is("runScopedTestSuite requires a fixed componentName"));
        assertThat(result.get("runId"), nullValue());
    }

    @Test
    void getScopedTestRunStatusReturnsErrorForANullExpectedComponentName() {
        // Without this guard, expectedComponentName.equals(...) would throw a raw
        // NullPointerException instead of returning a clean error - a different (and equally
        // unacceptable) failure mode than runScopedTestSuite's fail-open risk.
        DispatchContext dctx = mock(DispatchContext.class);
        Security security = mock(Security.class);
        GenericValue userLogin = mock(GenericValue.class);
        when(dctx.getSecurity()).thenReturn(security);
        when(userLogin.getString("userLoginId")).thenReturn("admin");
        when(security.hasPermission("TESTEXEC_ADMIN", userLogin)).thenReturn(true);

        Map<String, Object> result = TestRunServices.getScopedTestRunStatus(dctx,
                Map.of("runId", "any-run", "userLogin", userLogin), null);

        assertThat(result.get("responseMessage"), is("error"));
        assertThat(result.get("errorMessage"), is("getScopedTestRunStatus requires an expectedComponentName"));
    }

    @Test
    void getScopedTestRunStatusReturnsErrorForAnEmptyExpectedComponentName() {
        DispatchContext dctx = mock(DispatchContext.class);
        Security security = mock(Security.class);
        GenericValue userLogin = mock(GenericValue.class);
        when(dctx.getSecurity()).thenReturn(security);
        when(userLogin.getString("userLoginId")).thenReturn("admin");
        when(security.hasPermission("TESTEXEC_ADMIN", userLogin)).thenReturn(true);

        Map<String, Object> result = TestRunServices.getScopedTestRunStatus(dctx,
                Map.of("runId", "any-run", "userLogin", userLogin), "");

        assertThat(result.get("responseMessage"), is("error"));
        assertThat(result.get("errorMessage"), is("getScopedTestRunStatus requires an expectedComponentName"));
    }

    @Test
    void getScopedTestRunStatusMasksANullComponentNameRunAsUnknownRunId() {
        // Proves the fail-closed guarantee for a run registered with componentName=null (e.g. a
        // hypothetical future unscoped internal registration path): the scoped wrapper's equality
        // check must still deny it, returning the same masked "No such runId" response a genuine
        // mismatch gets - never a crash, never real data.
        DispatchContext dctx = mock(DispatchContext.class);
        Security security = mock(Security.class);
        GenericValue userLogin = mock(GenericValue.class);
        when(dctx.getSecurity()).thenReturn(security);
        when(userLogin.getString("userLoginId")).thenReturn("admin");
        when(security.hasPermission("TESTEXEC_ADMIN", userLogin)).thenReturn(true);
        TestRunServices.TRACKER.register("scoped-run-null-component", "content-tests", null, "admin", Map.of());

        Map<String, Object> result = TestRunServices.getScopedTestRunStatus(dctx,
                Map.of("runId", "scoped-run-null-component", "userLogin", userLogin), "example");

        assertThat(result.get("responseMessage"), is("error"));
        assertThat(result.get("errorMessage"), is("No such runId: scoped-run-null-component"));
    }
}
