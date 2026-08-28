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
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.DispatchContext;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
        // Mocks EntityUtilProperties directly (rather than relying on the classpath testtools.properties
        // file's actual value, which may legitimately vary between environments) so this test asserts
        // the exact global-disabled error message, not just a generic error that could equally be the
        // unrelated suite-resolution failure path. Same pattern as
        // runTestSuiteReturnsErrorWhenComponentApiDisabled below.
        DispatchContext dctx = mock(DispatchContext.class);
        Security security = mock(Security.class);
        Delegator delegator = mock(Delegator.class);
        GenericValue userLogin = mock(GenericValue.class);
        when(dctx.getSecurity()).thenReturn(security);
        when(dctx.getDelegator()).thenReturn(delegator);
        when(userLogin.getString("userLoginId")).thenReturn("admin");
        when(security.hasPermission("TESTEXEC_ADMIN", userLogin)).thenReturn(true);

        try (MockedStatic<EntityUtilProperties> entityUtilProperties =
                Mockito.mockStatic(EntityUtilProperties.class, Mockito.CALLS_REAL_METHODS)) {
            entityUtilProperties.when(() -> EntityUtilProperties.getPropertyValue("testtools", "test.api.enabled", delegator))
                    .thenReturn("false");

            Map<String, Object> result = TestRunServices.runTestSuite(dctx,
                    Map.of("suiteName", "example-tests", "userLogin", userLogin));

            assertThat(result.get("responseMessage"), is("error"));
            assertThat(result.get("errorMessage"), is("The test execution API is disabled in this environment."));
            assertThat(result.get("runId"), nullValue());
        }
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
    void runTestSuiteReturnsErrorWhenComponentApiDisabled() {
        // Mocks EntityUtilProperties directly (rather than relying on the classpath testtools.properties
        // file, the way runTestSuiteReturnsErrorWhenApiDisabled does for the global flag) because this
        // test needs two different property values at once - the global flag true, the per-component
        // override false - which a single checked-in file can't express for an arbitrary test-only
        // component name. CALLS_REAL_METHODS means every other EntityUtilProperties call not explicitly
        // stubbed below still behaves normally.
        DispatchContext dctx = mock(DispatchContext.class);
        Security security = mock(Security.class);
        Delegator delegator = mock(Delegator.class);
        GenericValue userLogin = mock(GenericValue.class);
        when(dctx.getSecurity()).thenReturn(security);
        when(dctx.getDelegator()).thenReturn(delegator);
        when(userLogin.getString("userLoginId")).thenReturn("admin");
        when(security.hasPermission("TESTEXEC_ADMIN", userLogin)).thenReturn(true);

        try (MockedStatic<EntityUtilProperties> entityUtilProperties =
                Mockito.mockStatic(EntityUtilProperties.class, Mockito.CALLS_REAL_METHODS)) {
            entityUtilProperties.when(() -> EntityUtilProperties.getPropertyValue("testtools", "test.api.enabled", delegator))
                    .thenReturn("true");
            entityUtilProperties.when(() -> EntityUtilProperties.getPropertyValue("testtools", "test.api.enabled.example", delegator))
                    .thenReturn("false");

            Map<String, Object> result = TestRunServices.runTestSuite(dctx,
                    Map.of("suiteName", "example-tests", "componentName", "example", "userLogin", userLogin));

            assertThat(result.get("responseMessage"), is("error"));
            assertThat(result.get("errorMessage"), is("The test execution API is disabled for component 'example' "
                    + "in this environment."));
            assertThat(result.get("runId"), nullValue());
        }
    }

    @Test
    void runTestSuiteDoesNotRejectWhenComponentApiIsNotDisabled() {
        // Cannot verify a full successful run here - resolving a
        // real suite needs a bootstrapped ComponentConfig this test module doesn't have, so
        // JunitSuiteWrapper's constructor still throws and this call still ends in error - just not
        // the new component-disabled error this test exists to rule out (proving the gate was passed,
        // not that a run actually completed). Full enabled-path behavior is verified by manual/live
        // validation (Task 3). Only the absence of the component-disabled error is asserted below -
        // asserting the error response itself would couple this test to the unrelated
        // un-bootstrapped-ComponentConfig failure mode, not the gate this test exists to verify.
        DispatchContext dctx = mock(DispatchContext.class);
        Security security = mock(Security.class);
        Delegator delegator = mock(Delegator.class);
        GenericValue userLogin = mock(GenericValue.class);
        when(dctx.getSecurity()).thenReturn(security);
        when(dctx.getDelegator()).thenReturn(delegator);
        when(userLogin.getString("userLoginId")).thenReturn("admin");
        when(security.hasPermission("TESTEXEC_ADMIN", userLogin)).thenReturn(true);

        try (MockedStatic<EntityUtilProperties> entityUtilProperties =
                Mockito.mockStatic(EntityUtilProperties.class, Mockito.CALLS_REAL_METHODS)) {
            entityUtilProperties.when(() -> EntityUtilProperties.getPropertyValue("testtools", "test.api.enabled", delegator))
                    .thenReturn("true");
            // test.api.enabled.example deliberately left unstubbed: CALLS_REAL_METHODS falls through to
            // the real EntityUtilProperties -> mock delegator (findList unstubbed, returns null) ->
            // properties-file fallback, which has no such key either, landing on readStringProperty's own
            // default ("true") - proving the opt-out default, not an explicit override.

            Map<String, Object> result = TestRunServices.runTestSuite(dctx,
                    Map.of("suiteName", "example-tests", "componentName", "example", "userLogin", userLogin));

            assertThat((String) result.get("errorMessage"), not(containsString("is disabled for component")));
        }
    }

    @Test
    void runTestSuiteRejectsBlankComponentName() {
        // No componentName in context at all - mirrors runTestSuiteReturnsErrorWhenApiDisabled's own
        // context map. Fail closed, not open: this is the only REST route to runTestSuite (the generic
        // framework/testtools/api/testruns.rest.xml endpoint), and REST attribute binding merges
        // body/path/query/header sources onto the same context map, so a caller can still send an
        // empty componentName (e.g. an empty query parameter) despite the URL's path parameter
        // normally supplying a real one. Without this guard, componentName would reach
        // ComponentConfig.matchingComponentName as null/blank - which matches every component -
        // silently turning the request into an unscoped sweep across every component's tests. Also
        // proves the per-component override is never even consulted for a rejected call: verifies
        // EntityUtilProperties.getPropertyValue is never invoked with a "test.api.enabled."-prefixed
        // property name (the global "test.api.enabled" key itself does not match that prefix, since
        // it has no trailing dot).
        DispatchContext dctx = mock(DispatchContext.class);
        Security security = mock(Security.class);
        Delegator delegator = mock(Delegator.class);
        GenericValue userLogin = mock(GenericValue.class);
        when(dctx.getSecurity()).thenReturn(security);
        when(dctx.getDelegator()).thenReturn(delegator);
        when(userLogin.getString("userLoginId")).thenReturn("admin");
        when(security.hasPermission("TESTEXEC_ADMIN", userLogin)).thenReturn(true);

        try (MockedStatic<EntityUtilProperties> entityUtilProperties =
                Mockito.mockStatic(EntityUtilProperties.class, Mockito.CALLS_REAL_METHODS)) {
            entityUtilProperties.when(() -> EntityUtilProperties.getPropertyValue("testtools", "test.api.enabled", delegator))
                    .thenReturn("true");

            Map<String, Object> result = TestRunServices.runTestSuite(dctx,
                    Map.of("suiteName", "example-tests", "userLogin", userLogin));

            assertThat(result.get("responseMessage"), is("error"));
            assertThat(result.get("errorMessage"), is("runTestSuite requires a componentName"));
            assertThat(result.get("runId"), nullValue());
            entityUtilProperties.verify(() -> EntityUtilProperties.getPropertyValue(eq("testtools"),
                    startsWith("test.api.enabled."), eq(delegator)), never());
        }
    }

    @Test
    void runTestSuiteRejectsAnEmptyStringComponentName() {
        // Distinct from the null/absent case above: an explicitly empty string (e.g. what a REST
        // caller sending "?componentName=" produces) must be rejected the same way, not treated as
        // "present" merely because the key exists in the context map.
        DispatchContext dctx = mock(DispatchContext.class);
        Security security = mock(Security.class);
        Delegator delegator = mock(Delegator.class);
        GenericValue userLogin = mock(GenericValue.class);
        when(dctx.getSecurity()).thenReturn(security);
        when(dctx.getDelegator()).thenReturn(delegator);
        when(userLogin.getString("userLoginId")).thenReturn("admin");
        when(security.hasPermission("TESTEXEC_ADMIN", userLogin)).thenReturn(true);

        try (MockedStatic<EntityUtilProperties> entityUtilProperties =
                Mockito.mockStatic(EntityUtilProperties.class, Mockito.CALLS_REAL_METHODS)) {
            entityUtilProperties.when(() -> EntityUtilProperties.getPropertyValue("testtools", "test.api.enabled", delegator))
                    .thenReturn("true");

            Map<String, Object> result = TestRunServices.runTestSuite(dctx,
                    Map.of("suiteName", "example-tests", "componentName", "", "userLogin", userLogin));

            assertThat(result.get("responseMessage"), is("error"));
            assertThat(result.get("errorMessage"), is("runTestSuite requires a componentName"));
            assertThat(result.get("runId"), nullValue());
        }
    }

    @Test
    void describeRunIncludesRunIdComponentNameStatusAndResultSummary() {
        TestRunRecord record = TestRunRecord.queued("run-9", "example-tests", "example", "admin", Map.of())
                .passed(Map.of("total", 2, "passed", 2, "failed", 0));

        Map<String, Object> described = TestRunServices.describeRun(record);

        assertThat(described.get("runId"), is("run-9"));
        assertThat(described.get("componentName"), is("example"));
        assertThat(described.get("status"), is("PASSED"));
        assertThat(described.get("resultSummary"), is(Map.of("total", 2, "passed", 2, "failed", 0)));
    }

    @Test
    void describeRunAddsErrorMessageIntoResultSummaryWhenPresent() {
        TestRunRecord record = TestRunRecord.queued("run-9", "example-tests", "example", "admin", Map.of())
                .error(new RuntimeException("boom"));

        Map<String, Object> described = TestRunServices.describeRun(record);

        assertThat(described.get("status"), is("ERROR"));
        assertThat(((Map<?, ?>) described.get("resultSummary")).get("errorMessage"), is("boom"));
    }

    @Test
    void getTestRunStatusIncludesRunId() {
        DispatchContext dctx = mock(DispatchContext.class);
        Security security = mock(Security.class);
        GenericValue userLogin = mock(GenericValue.class);
        when(dctx.getSecurity()).thenReturn(security);
        when(userLogin.getString("userLoginId")).thenReturn("admin");
        when(security.hasPermission("TESTEXEC_ADMIN", userLogin)).thenReturn(true);
        TestRunServices.TRACKER.register("run-id-check", "example-tests", "example", "admin", Map.of());

        Map<String, Object> result = TestRunServices.getTestRunStatus(dctx,
                Map.of("runId", "run-id-check", "userLogin", userLogin));

        assertThat(result.get("runId"), is("run-id-check"));
    }

    @Test
    void isTestApiGloballyEnabledReadsTheGlobalFlag() {
        Delegator delegator = mock(Delegator.class);
        try (MockedStatic<EntityUtilProperties> entityUtilProperties =
                Mockito.mockStatic(EntityUtilProperties.class, Mockito.CALLS_REAL_METHODS)) {
            entityUtilProperties.when(() -> EntityUtilProperties.getPropertyValue("testtools", "test.api.enabled", delegator))
                    .thenReturn("true");

            assertThat(TestRunServices.isTestApiGloballyEnabled(delegator), is(true));
        }
    }

    @Test
    void isTestApiEnabledForComponentDefaultsToTrueWhenUnset() {
        Delegator delegator = mock(Delegator.class);
        assertThat(TestRunServices.isTestApiEnabledForComponent(delegator, "example"), is(true));
    }

    @Test
    void isTestApiEnabledForComponentReflectsAnExplicitOverride() {
        Delegator delegator = mock(Delegator.class);
        try (MockedStatic<EntityUtilProperties> entityUtilProperties =
                Mockito.mockStatic(EntityUtilProperties.class, Mockito.CALLS_REAL_METHODS)) {
            entityUtilProperties.when(() -> EntityUtilProperties.getPropertyValue("testtools", "test.api.enabled.example", delegator))
                    .thenReturn("false");

            assertThat(TestRunServices.isTestApiEnabledForComponent(delegator, "example"), is(false));
        }
    }
}
