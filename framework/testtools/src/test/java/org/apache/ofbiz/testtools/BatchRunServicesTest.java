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

import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.component.ComponentConfig;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class BatchRunServicesTest {

    private static ComponentConfig.TestSuiteInfo testSuiteInfoFor(String componentName) {
        ComponentConfig componentConfig = mock(ComponentConfig.class);
        when(componentConfig.getComponentName()).thenReturn(componentName);
        ComponentConfig.TestSuiteInfo testSuiteInfo = mock(ComponentConfig.TestSuiteInfo.class);
        when(testSuiteInfo.getComponentConfig()).thenReturn(componentConfig);
        return testSuiteInfo;
    }

    @Test
    void discoverEligibleComponentsReturnsEveryComponentWithATestdefWhenBothFlagsAreOn() {
        Delegator delegator = mock(Delegator.class);

        var example = testSuiteInfoFor("example");
        var party = testSuiteInfoFor("party");
        try (MockedStatic<ComponentConfig> componentConfig = Mockito.mockStatic(ComponentConfig.class);
                MockedStatic<EntityUtilProperties> entityUtilProperties =
                        Mockito.mockStatic(EntityUtilProperties.class, Mockito.CALLS_REAL_METHODS)) {
            componentConfig.when(() -> ComponentConfig.getAllTestSuiteInfos(null))
                    .thenReturn(List.of(example, party));
            entityUtilProperties.when(() -> EntityUtilProperties.getPropertyValue("testtools", "test.api.enabled", delegator))
                    .thenReturn("true");
            entityUtilProperties.when(() -> EntityUtilProperties.getPropertyValue("testtools", "test.api.enabled.example", delegator))
                    .thenReturn("true");
            entityUtilProperties.when(() -> EntityUtilProperties.getPropertyValue("testtools", "test.api.enabled.party", delegator))
                    .thenReturn("true");

            List<String> discovered = BatchRunServices.discoverEligibleComponents(delegator);

            assertThat(discovered, is(List.of("example", "party")));
        }
    }

    @Test
    void discoverEligibleComponentsExcludesAComponentWithItsOwnFlagOff() {
        Delegator delegator = mock(Delegator.class);

        var example = testSuiteInfoFor("example");
        var party = testSuiteInfoFor("party");
        try (MockedStatic<ComponentConfig> componentConfig = Mockito.mockStatic(ComponentConfig.class);
                MockedStatic<EntityUtilProperties> entityUtilProperties =
                        Mockito.mockStatic(EntityUtilProperties.class, Mockito.CALLS_REAL_METHODS)) {
            componentConfig.when(() -> ComponentConfig.getAllTestSuiteInfos(null))
                    .thenReturn(List.of(example, party));
            entityUtilProperties.when(() -> EntityUtilProperties.getPropertyValue("testtools", "test.api.enabled", delegator))
                    .thenReturn("true");
            entityUtilProperties.when(() -> EntityUtilProperties.getPropertyValue("testtools", "test.api.enabled.example", delegator))
                    .thenReturn("true");
            entityUtilProperties.when(() -> EntityUtilProperties.getPropertyValue("testtools", "test.api.enabled.party", delegator))
                    .thenReturn("false");

            List<String> discovered = BatchRunServices.discoverEligibleComponents(delegator);

            assertThat(discovered, is(List.of("example")));
        }
    }

    @Test
    void discoverEligibleComponentsDeduplicatesMultipleTestSuitesInOneComponent() {
        Delegator delegator = mock(Delegator.class);

        var accounting1 = testSuiteInfoFor("accounting");
        var accounting2 = testSuiteInfoFor("accounting");
        var accounting3 = testSuiteInfoFor("accounting");
        try (MockedStatic<ComponentConfig> componentConfig = Mockito.mockStatic(ComponentConfig.class);
                MockedStatic<EntityUtilProperties> entityUtilProperties =
                        Mockito.mockStatic(EntityUtilProperties.class, Mockito.CALLS_REAL_METHODS)) {
            componentConfig.when(() -> ComponentConfig.getAllTestSuiteInfos(null))
                    .thenReturn(List.of(accounting1, accounting2, accounting3));
            entityUtilProperties.when(() -> EntityUtilProperties.getPropertyValue("testtools", "test.api.enabled", delegator))
                    .thenReturn("true");
            entityUtilProperties.when(() -> EntityUtilProperties.getPropertyValue("testtools", "test.api.enabled.accounting", delegator))
                    .thenReturn("true");

            List<String> discovered = BatchRunServices.discoverEligibleComponents(delegator);

            assertThat(discovered, is(List.of("accounting")));
        }
    }

    @Test
    void discoverEligibleComponentsReturnsEmptyListWhenGlobalFlagIsOff() {
        Delegator delegator = mock(Delegator.class);

        try (MockedStatic<ComponentConfig> componentConfig = Mockito.mockStatic(ComponentConfig.class);
                MockedStatic<EntityUtilProperties> entityUtilProperties =
                        Mockito.mockStatic(EntityUtilProperties.class, Mockito.CALLS_REAL_METHODS)) {
            entityUtilProperties.when(() -> EntityUtilProperties.getPropertyValue("testtools", "test.api.enabled", delegator))
                    .thenReturn("false");

            List<String> discovered = BatchRunServices.discoverEligibleComponents(delegator);

            assertThat(discovered, is(List.of()));
            componentConfig.verifyNoInteractions();
        }
    }

    @Test
    void validateRequestedComponentsReturnsEmptyListWhenAllAreValid() {
        Delegator delegator = mock(Delegator.class);
        var party = testSuiteInfoFor("party");
        try (MockedStatic<ComponentConfig> componentConfig = Mockito.mockStatic(ComponentConfig.class);
                MockedStatic<EntityUtilProperties> entityUtilProperties =
                        Mockito.mockStatic(EntityUtilProperties.class, Mockito.CALLS_REAL_METHODS)) {
            componentConfig.when(() -> ComponentConfig.componentExists("party")).thenReturn(true);
            componentConfig.when(() -> ComponentConfig.getAllTestSuiteInfos("party"))
                    .thenReturn(List.of(party));
            entityUtilProperties.when(() -> EntityUtilProperties.getPropertyValue("testtools", "test.api.enabled", delegator))
                    .thenReturn("true");
            entityUtilProperties.when(() -> EntityUtilProperties.getPropertyValue("testtools", "test.api.enabled.party", delegator))
                    .thenReturn("true");

            List<String> invalid = BatchRunServices.validateRequestedComponents(List.of("party"), delegator);

            assertThat(invalid, is(List.of()));
        }
    }

    @Test
    void validateRequestedComponentsFlagsAnUnknownComponent() {
        Delegator delegator = mock(Delegator.class);
        try (MockedStatic<ComponentConfig> componentConfig = Mockito.mockStatic(ComponentConfig.class);
                MockedStatic<EntityUtilProperties> entityUtilProperties =
                        Mockito.mockStatic(EntityUtilProperties.class, Mockito.CALLS_REAL_METHODS)) {
            componentConfig.when(() -> ComponentConfig.componentExists("nosuch")).thenReturn(false);
            entityUtilProperties.when(() -> EntityUtilProperties.getPropertyValue("testtools", "test.api.enabled", delegator))
                    .thenReturn("true");

            List<String> invalid = BatchRunServices.validateRequestedComponents(List.of("nosuch"), delegator);

            assertThat(invalid, contains("nosuch (unknown component)"));
        }
    }

    @Test
    void validateRequestedComponentsFlagsAComponentWithNoTestdef() {
        Delegator delegator = mock(Delegator.class);
        try (MockedStatic<ComponentConfig> componentConfig = Mockito.mockStatic(ComponentConfig.class);
                MockedStatic<EntityUtilProperties> entityUtilProperties =
                        Mockito.mockStatic(EntityUtilProperties.class, Mockito.CALLS_REAL_METHODS)) {
            componentConfig.when(() -> ComponentConfig.componentExists("birt")).thenReturn(true);
            componentConfig.when(() -> ComponentConfig.getAllTestSuiteInfos("birt")).thenReturn(List.of());
            entityUtilProperties.when(() -> EntityUtilProperties.getPropertyValue("testtools", "test.api.enabled", delegator))
                    .thenReturn("true");

            List<String> invalid = BatchRunServices.validateRequestedComponents(List.of("birt"), delegator);

            assertThat(invalid, contains("birt (no testdef found)"));
        }
    }

    @Test
    void validateRequestedComponentsFlagsADisabledComponent() {
        Delegator delegator = mock(Delegator.class);
        var accounting = testSuiteInfoFor("accounting");
        try (MockedStatic<ComponentConfig> componentConfig = Mockito.mockStatic(ComponentConfig.class);
                MockedStatic<EntityUtilProperties> entityUtilProperties =
                        Mockito.mockStatic(EntityUtilProperties.class, Mockito.CALLS_REAL_METHODS)) {
            componentConfig.when(() -> ComponentConfig.componentExists("accounting")).thenReturn(true);
            componentConfig.when(() -> ComponentConfig.getAllTestSuiteInfos("accounting"))
                    .thenReturn(List.of(accounting));
            entityUtilProperties.when(() -> EntityUtilProperties.getPropertyValue("testtools", "test.api.enabled", delegator))
                    .thenReturn("true");
            entityUtilProperties.when(() -> EntityUtilProperties.getPropertyValue("testtools", "test.api.enabled.accounting", delegator))
                    .thenReturn("false");

            List<String> invalid = BatchRunServices.validateRequestedComponents(List.of("accounting"), delegator);

            assertThat(invalid, contains("accounting (test execution API is disabled for this component)"));
        }
    }

    @Test
    void validateRequestedComponentsFlagsABlankEntryWithoutCallingComponentConfig() {
        Delegator delegator = mock(Delegator.class);
        try (MockedStatic<ComponentConfig> componentConfig = Mockito.mockStatic(ComponentConfig.class);
                MockedStatic<EntityUtilProperties> entityUtilProperties =
                        Mockito.mockStatic(EntityUtilProperties.class, Mockito.CALLS_REAL_METHODS)) {
            entityUtilProperties.when(() -> EntityUtilProperties.getPropertyValue("testtools", "test.api.enabled", delegator))
                    .thenReturn("true");

            List<String> invalid = BatchRunServices.validateRequestedComponents(List.of(""), delegator);

            assertThat(invalid, contains(" (blank component name)"));
            componentConfig.verifyNoInteractions();
        }
    }

    @Test
    void validateRequestedComponentsFlagsEveryEntryWhenGlobalFlagIsOff() {
        Delegator delegator = mock(Delegator.class);
        try (MockedStatic<ComponentConfig> componentConfig = Mockito.mockStatic(ComponentConfig.class);
                MockedStatic<EntityUtilProperties> entityUtilProperties =
                        Mockito.mockStatic(EntityUtilProperties.class, Mockito.CALLS_REAL_METHODS)) {
            entityUtilProperties.when(() -> EntityUtilProperties.getPropertyValue("testtools", "test.api.enabled", delegator))
                    .thenReturn("false");

            List<String> invalid = BatchRunServices.validateRequestedComponents(List.of("party"), delegator);

            assertThat(invalid, contains("party (test execution API is disabled)"));
            componentConfig.verifyNoInteractions();
        }
    }

    @Test
    void runBatchTestSuiteReturnsErrorWhenPermissionDenied() {
        DispatchContext dctx = mock(DispatchContext.class);
        Security security = mock(Security.class);
        GenericValue userLogin = mock(GenericValue.class);
        when(dctx.getSecurity()).thenReturn(security);
        when(userLogin.getString("userLoginId")).thenReturn("nobody");
        when(security.hasPermission("TESTEXEC_ADMIN", userLogin)).thenReturn(false);

        Map<String, Object> result = BatchRunServices.runBatchTestSuite(dctx, Map.of("userLogin", userLogin));

        assertThat(result.get("responseMessage"), is("error"));
        assertThat(result.get("batchId"), nullValue());
    }

    @Test
    void runBatchTestSuiteRejectsWhenNoComponentsAreEligible() {
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

            Map<String, Object> result = BatchRunServices.runBatchTestSuite(dctx, Map.of("userLogin", userLogin));

            assertThat(result.get("responseMessage"), is("error"));
            assertThat((String) result.get("errorMessage"), containsString("No components are eligible"));
            assertThat(result.get("batchId"), nullValue());
        }
    }

    @Test
    void runBatchTestSuiteRejectsAnExplicitlyEmptyComponentsList() {
        DispatchContext dctx = mock(DispatchContext.class);
        Security security = mock(Security.class);
        GenericValue userLogin = mock(GenericValue.class);
        when(dctx.getSecurity()).thenReturn(security);
        when(userLogin.getString("userLoginId")).thenReturn("admin");
        when(security.hasPermission("TESTEXEC_ADMIN", userLogin)).thenReturn(true);

        Map<String, Object> result = BatchRunServices.runBatchTestSuite(dctx,
                Map.of("userLogin", userLogin, "components", List.of()));

        assertThat(result.get("responseMessage"), is("error"));
        assertThat((String) result.get("errorMessage"), containsString("cannot be empty"));
        assertThat(result.get("batchId"), nullValue());
    }

    @Test
    void runBatchTestSuiteRejectsWholeBatchWhenAnExplicitComponentIsInvalid() {
        DispatchContext dctx = mock(DispatchContext.class);
        Security security = mock(Security.class);
        Delegator delegator = mock(Delegator.class);
        GenericValue userLogin = mock(GenericValue.class);
        when(dctx.getSecurity()).thenReturn(security);
        when(dctx.getDelegator()).thenReturn(delegator);
        when(userLogin.getString("userLoginId")).thenReturn("admin");
        when(security.hasPermission("TESTEXEC_ADMIN", userLogin)).thenReturn(true);

        try (MockedStatic<ComponentConfig> componentConfig = Mockito.mockStatic(ComponentConfig.class);
                MockedStatic<EntityUtilProperties> entityUtilProperties =
                        Mockito.mockStatic(EntityUtilProperties.class, Mockito.CALLS_REAL_METHODS)) {
            componentConfig.when(() -> ComponentConfig.componentExists("nosuch")).thenReturn(false);
            entityUtilProperties.when(() -> EntityUtilProperties.getPropertyValue("testtools", "test.api.enabled", delegator))
                    .thenReturn("true");

            Map<String, Object> result = BatchRunServices.runBatchTestSuite(dctx,
                    Map.of("userLogin", userLogin, "components", List.of("nosuch")));

            assertThat(result.get("responseMessage"), is("error"));
            assertThat((String) result.get("errorMessage"), containsString("nosuch (unknown component)"));
            assertThat(result.get("batchId"), nullValue());
        }
    }

    private static Map<String, Object> successResult(String runId) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("runId", runId);
        return result;
    }

    private static Map<String, Object> errorResult(String message) {
        return ServiceUtil.returnError(message);
    }

    @Test
    void runBatchTestSuiteTracksASuccessfullyQueuedComponent() {
        DispatchContext dctx = mock(DispatchContext.class);
        Security security = mock(Security.class);
        Delegator delegator = mock(Delegator.class);
        GenericValue userLogin = mock(GenericValue.class);
        when(dctx.getSecurity()).thenReturn(security);
        when(dctx.getDelegator()).thenReturn(delegator);
        when(userLogin.getString("userLoginId")).thenReturn("admin");
        when(security.hasPermission("TESTEXEC_ADMIN", userLogin)).thenReturn(true);

        var exampleSuiteInfo = testSuiteInfoFor("example");
        try (MockedStatic<ComponentConfig> componentConfig = Mockito.mockStatic(ComponentConfig.class);
                MockedStatic<EntityUtilProperties> entityUtilProperties =
                        Mockito.mockStatic(EntityUtilProperties.class, Mockito.CALLS_REAL_METHODS);
                MockedStatic<TestRunServices> testRunServices =
                        Mockito.mockStatic(TestRunServices.class, Mockito.CALLS_REAL_METHODS)) {
            componentConfig.when(() -> ComponentConfig.componentExists("example")).thenReturn(true);
            componentConfig.when(() -> ComponentConfig.getAllTestSuiteInfos("example"))
                    .thenReturn(List.of(exampleSuiteInfo));
            entityUtilProperties.when(() -> EntityUtilProperties.getPropertyValue("testtools", "test.api.enabled", delegator))
                    .thenReturn("true");
            entityUtilProperties.when(() -> EntityUtilProperties.getPropertyValue("testtools", "test.api.enabled.example", delegator))
                    .thenReturn("true");
            testRunServices.when(() -> TestRunServices.runTestSuite(eq(dctx), Mockito.<Map<String, ?>>any()))
                    .thenReturn(successResult("run-example"));

            Map<String, Object> result = BatchRunServices.runBatchTestSuite(dctx,
                    Map.of("userLogin", userLogin, "components", List.of("example")));

            assertThat(result.get("responseMessage"), is("success"));
            String batchId = (String) result.get("batchId");
            assertThat(batchId, notNullValue());
            assertThat(BatchRunServices.BATCH_TRACKER.get(batchId),
                    is(List.of(new BatchRunTracker.BatchChildRef("example", "run-example"))));
        }
    }

    @Test
    void runBatchTestSuiteRejectsTheWholeBatchWhenEveryComponentsRunTestSuiteCallErrors() {
        DispatchContext dctx = mock(DispatchContext.class);
        Security security = mock(Security.class);
        Delegator delegator = mock(Delegator.class);
        GenericValue userLogin = mock(GenericValue.class);
        when(dctx.getSecurity()).thenReturn(security);
        when(dctx.getDelegator()).thenReturn(delegator);
        when(userLogin.getString("userLoginId")).thenReturn("admin");
        when(security.hasPermission("TESTEXEC_ADMIN", userLogin)).thenReturn(true);

        var flakySuiteInfo = testSuiteInfoFor("flaky");
        try (MockedStatic<ComponentConfig> componentConfig = Mockito.mockStatic(ComponentConfig.class);
                MockedStatic<EntityUtilProperties> entityUtilProperties =
                        Mockito.mockStatic(EntityUtilProperties.class, Mockito.CALLS_REAL_METHODS);
                MockedStatic<TestRunServices> testRunServices =
                        Mockito.mockStatic(TestRunServices.class, Mockito.CALLS_REAL_METHODS)) {
            componentConfig.when(() -> ComponentConfig.componentExists("flaky")).thenReturn(true);
            componentConfig.when(() -> ComponentConfig.getAllTestSuiteInfos("flaky"))
                    .thenReturn(List.of(flakySuiteInfo));
            entityUtilProperties.when(() -> EntityUtilProperties.getPropertyValue("testtools", "test.api.enabled", delegator))
                    .thenReturn("true");
            entityUtilProperties.when(() -> EntityUtilProperties.getPropertyValue("testtools", "test.api.enabled.flaky", delegator))
                    .thenReturn("true");
            testRunServices.when(() -> TestRunServices.runTestSuite(eq(dctx), Mockito.<Map<String, ?>>any()))
                    .thenReturn(errorResult("No tests found"));

            Map<String, Object> result = BatchRunServices.runBatchTestSuite(dctx,
                    Map.of("userLogin", userLogin, "components", List.of("flaky")));

            assertThat(result.get("responseMessage"), is("error"));
            assertThat(result.get("batchId"), nullValue());
        }
    }

    @Test
    void runBatchTestSuiteOmitsAFailingComponentButKeepsGoingWhenAtLeastOneSucceeds() {
        DispatchContext dctx = mock(DispatchContext.class);
        Security security = mock(Security.class);
        Delegator delegator = mock(Delegator.class);
        GenericValue userLogin = mock(GenericValue.class);
        when(dctx.getSecurity()).thenReturn(security);
        when(dctx.getDelegator()).thenReturn(delegator);
        when(userLogin.getString("userLoginId")).thenReturn("admin");
        when(security.hasPermission("TESTEXEC_ADMIN", userLogin)).thenReturn(true);

        var exampleSuiteInfo = testSuiteInfoFor("example");
        var flakySuiteInfo = testSuiteInfoFor("flaky");
        try (MockedStatic<ComponentConfig> componentConfig = Mockito.mockStatic(ComponentConfig.class);
                MockedStatic<EntityUtilProperties> entityUtilProperties =
                        Mockito.mockStatic(EntityUtilProperties.class, Mockito.CALLS_REAL_METHODS);
                MockedStatic<TestRunServices> testRunServices =
                        Mockito.mockStatic(TestRunServices.class, Mockito.CALLS_REAL_METHODS)) {
            componentConfig.when(() -> ComponentConfig.componentExists("example")).thenReturn(true);
            componentConfig.when(() -> ComponentConfig.getAllTestSuiteInfos("example"))
                    .thenReturn(List.of(exampleSuiteInfo));
            componentConfig.when(() -> ComponentConfig.componentExists("flaky")).thenReturn(true);
            componentConfig.when(() -> ComponentConfig.getAllTestSuiteInfos("flaky"))
                    .thenReturn(List.of(flakySuiteInfo));
            entityUtilProperties.when(() -> EntityUtilProperties.getPropertyValue("testtools", "test.api.enabled", delegator))
                    .thenReturn("true");
            entityUtilProperties.when(() -> EntityUtilProperties.getPropertyValue("testtools", "test.api.enabled.example", delegator))
                    .thenReturn("true");
            entityUtilProperties.when(() -> EntityUtilProperties.getPropertyValue("testtools", "test.api.enabled.flaky", delegator))
                    .thenReturn("true");
            testRunServices.when(() -> TestRunServices.runTestSuite(eq(dctx), argThat(ctx ->
                    "example".equals(((Map<?, ?>) ctx).get("componentName")))))
                    .thenReturn(successResult("run-example"));
            testRunServices.when(() -> TestRunServices.runTestSuite(eq(dctx), argThat(ctx ->
                    "flaky".equals(((Map<?, ?>) ctx).get("componentName")))))
                    .thenReturn(errorResult("No tests found"));

            Map<String, Object> result = BatchRunServices.runBatchTestSuite(dctx,
                    Map.of("userLogin", userLogin, "components", List.of("example", "flaky")));

            assertThat(result.get("responseMessage"), is("success"));
            String batchId = (String) result.get("batchId");
            assertThat(BatchRunServices.BATCH_TRACKER.get(batchId),
                    is(List.of(new BatchRunTracker.BatchChildRef("example", "run-example"))));
        }
    }

    @Test
    void runBatchTestSuiteDedupesDuplicateComponentNamesInAnExplicitList() {
        DispatchContext dctx = mock(DispatchContext.class);
        Security security = mock(Security.class);
        Delegator delegator = mock(Delegator.class);
        GenericValue userLogin = mock(GenericValue.class);
        when(dctx.getSecurity()).thenReturn(security);
        when(dctx.getDelegator()).thenReturn(delegator);
        when(userLogin.getString("userLoginId")).thenReturn("admin");
        when(security.hasPermission("TESTEXEC_ADMIN", userLogin)).thenReturn(true);

        var partySuiteInfo = testSuiteInfoFor("party");
        try (MockedStatic<ComponentConfig> componentConfig = Mockito.mockStatic(ComponentConfig.class);
                MockedStatic<EntityUtilProperties> entityUtilProperties =
                        Mockito.mockStatic(EntityUtilProperties.class, Mockito.CALLS_REAL_METHODS);
                MockedStatic<TestRunServices> testRunServices =
                        Mockito.mockStatic(TestRunServices.class, Mockito.CALLS_REAL_METHODS)) {
            componentConfig.when(() -> ComponentConfig.componentExists("party")).thenReturn(true);
            componentConfig.when(() -> ComponentConfig.getAllTestSuiteInfos("party"))
                    .thenReturn(List.of(partySuiteInfo));
            entityUtilProperties.when(() -> EntityUtilProperties.getPropertyValue("testtools", "test.api.enabled", delegator))
                    .thenReturn("true");
            entityUtilProperties.when(() -> EntityUtilProperties.getPropertyValue("testtools", "test.api.enabled.party", delegator))
                    .thenReturn("true");
            testRunServices.when(() -> TestRunServices.runTestSuite(eq(dctx), Mockito.<Map<String, ?>>any()))
                    .thenReturn(successResult("run-party"));

            Map<String, Object> result = BatchRunServices.runBatchTestSuite(dctx,
                    Map.of("userLogin", userLogin, "components", List.of("party", "party")));

            assertThat(result.get("responseMessage"), is("success"));
            String batchId = (String) result.get("batchId");
            assertThat(BatchRunServices.BATCH_TRACKER.get(batchId),
                    is(List.of(new BatchRunTracker.BatchChildRef("party", "run-party"))));
            testRunServices.verify(() -> TestRunServices.runTestSuite(eq(dctx), Mockito.<Map<String, ?>>any()),
                    times(1));
        }
    }

    @Test
    void getBatchTestRunStatusReturnsErrorWhenPermissionDenied() {
        DispatchContext dctx = mock(DispatchContext.class);
        Security security = mock(Security.class);
        GenericValue userLogin = mock(GenericValue.class);
        when(dctx.getSecurity()).thenReturn(security);
        when(userLogin.getString("userLoginId")).thenReturn("nobody");
        when(security.hasPermission("TESTEXEC_ADMIN", userLogin)).thenReturn(false);

        Map<String, Object> result = BatchRunServices.getBatchTestRunStatus(dctx,
                Map.of("batchId", "batch-1", "userLogin", userLogin));

        assertThat(result.get("responseMessage"), is("error"));
    }

    @Test
    void getBatchTestRunStatusReturnsErrorForAnUnknownBatchId() {
        DispatchContext dctx = mock(DispatchContext.class);
        Security security = mock(Security.class);
        GenericValue userLogin = mock(GenericValue.class);
        when(dctx.getSecurity()).thenReturn(security);
        when(userLogin.getString("userLoginId")).thenReturn("admin");
        when(security.hasPermission("TESTEXEC_ADMIN", userLogin)).thenReturn(true);

        Map<String, Object> result = BatchRunServices.getBatchTestRunStatus(dctx,
                Map.of("batchId", "no-such-batch", "userLogin", userLogin));

        assertThat(result.get("responseMessage"), is("error"));
    }

    @Test
    void getBatchTestRunStatusIsRunningWhileAnyChildIsQueuedOrRunning() {
        DispatchContext dctx = mock(DispatchContext.class);
        Security security = mock(Security.class);
        GenericValue userLogin = mock(GenericValue.class);
        when(dctx.getSecurity()).thenReturn(security);
        when(userLogin.getString("userLoginId")).thenReturn("admin");
        when(security.hasPermission("TESTEXEC_ADMIN", userLogin)).thenReturn(true);
        TestRunServices.TRACKER.register("run-passed", "example-tests", "example", "admin", Map.of());
        TestRunServices.TRACKER.markRunning("run-passed");
        TestRunServices.TRACKER.markPassed("run-passed", Map.of("total", 1, "passed", 1, "failed", 0));
        TestRunServices.TRACKER.register("run-running", "party-tests", "party", "admin", Map.of());
        TestRunServices.TRACKER.markRunning("run-running");
        BatchRunServices.BATCH_TRACKER.register("batch-running", List.of(
                new BatchRunTracker.BatchChildRef("example", "run-passed"),
                new BatchRunTracker.BatchChildRef("party", "run-running")));

        Map<String, Object> result = BatchRunServices.getBatchTestRunStatus(dctx,
                Map.of("batchId", "batch-running", "userLogin", userLogin));

        assertThat(result.get("status"), is("RUNNING"));
        Map<?, ?> summary = (Map<?, ?>) result.get("summary");
        assertThat(summary.get("total"), is(2));
        assertThat(summary.get("passed"), is(1));
        assertThat(summary.get("running"), is(1));
    }

    @Test
    void getBatchTestRunStatusIsPassedWhenEveryChildPassed() {
        DispatchContext dctx = mock(DispatchContext.class);
        Security security = mock(Security.class);
        GenericValue userLogin = mock(GenericValue.class);
        when(dctx.getSecurity()).thenReturn(security);
        when(userLogin.getString("userLoginId")).thenReturn("admin");
        when(security.hasPermission("TESTEXEC_ADMIN", userLogin)).thenReturn(true);
        TestRunServices.TRACKER.register("run-a", "example-tests", "example", "admin", Map.of());
        TestRunServices.TRACKER.markPassed("run-a", Map.of("total", 1, "passed", 1, "failed", 0));
        BatchRunServices.BATCH_TRACKER.register("batch-passed",
                List.of(new BatchRunTracker.BatchChildRef("example", "run-a")));

        Map<String, Object> result = BatchRunServices.getBatchTestRunStatus(dctx,
                Map.of("batchId", "batch-passed", "userLogin", userLogin));

        assertThat(result.get("status"), is("PASSED"));
    }

    @Test
    void getBatchTestRunStatusIsFailedWhenAnyChildFailed() {
        DispatchContext dctx = mock(DispatchContext.class);
        Security security = mock(Security.class);
        GenericValue userLogin = mock(GenericValue.class);
        when(dctx.getSecurity()).thenReturn(security);
        when(userLogin.getString("userLoginId")).thenReturn("admin");
        when(security.hasPermission("TESTEXEC_ADMIN", userLogin)).thenReturn(true);
        TestRunServices.TRACKER.register("run-a2", "example-tests", "example", "admin", Map.of());
        TestRunServices.TRACKER.markPassed("run-a2", Map.of("total", 1, "passed", 1, "failed", 0));
        TestRunServices.TRACKER.register("run-b2", "party-tests", "party", "admin", Map.of());
        TestRunServices.TRACKER.markFailed("run-b2", Map.of("total", 1, "passed", 0, "failed", 1));
        BatchRunServices.BATCH_TRACKER.register("batch-failed", List.of(
                new BatchRunTracker.BatchChildRef("example", "run-a2"),
                new BatchRunTracker.BatchChildRef("party", "run-b2")));

        Map<String, Object> result = BatchRunServices.getBatchTestRunStatus(dctx,
                Map.of("batchId", "batch-failed", "userLogin", userLogin));

        assertThat(result.get("status"), is("FAILED"));
    }

    @Test
    void getBatchTestRunStatusIsErrorWhenAnyChildErrored() {
        DispatchContext dctx = mock(DispatchContext.class);
        Security security = mock(Security.class);
        GenericValue userLogin = mock(GenericValue.class);
        when(dctx.getSecurity()).thenReturn(security);
        when(userLogin.getString("userLoginId")).thenReturn("admin");
        when(security.hasPermission("TESTEXEC_ADMIN", userLogin)).thenReturn(true);
        TestRunServices.TRACKER.register("run-a3", "example-tests", "example", "admin", Map.of());
        TestRunServices.TRACKER.markPassed("run-a3", Map.of("total", 1, "passed", 1, "failed", 0));
        TestRunServices.TRACKER.register("run-b3", "party-tests", "party", "admin", Map.of());
        TestRunServices.TRACKER.markError("run-b3", new RuntimeException("suite blew up"));
        BatchRunServices.BATCH_TRACKER.register("batch-error", List.of(
                new BatchRunTracker.BatchChildRef("example", "run-a3"),
                new BatchRunTracker.BatchChildRef("party", "run-b3")));

        Map<String, Object> result = BatchRunServices.getBatchTestRunStatus(dctx,
                Map.of("batchId", "batch-error", "userLogin", userLogin));

        assertThat(result.get("status"), is("ERROR"));
    }

    @Test
    void getBatchTestRunStatusIncludesPerComponentResults() {
        DispatchContext dctx = mock(DispatchContext.class);
        Security security = mock(Security.class);
        GenericValue userLogin = mock(GenericValue.class);
        when(dctx.getSecurity()).thenReturn(security);
        when(userLogin.getString("userLoginId")).thenReturn("admin");
        when(security.hasPermission("TESTEXEC_ADMIN", userLogin)).thenReturn(true);
        TestRunServices.TRACKER.register("run-a4", "example-tests", "example", "admin", Map.of());
        TestRunServices.TRACKER.markPassed("run-a4", Map.of("total", 1, "passed", 1, "failed", 0));
        BatchRunServices.BATCH_TRACKER.register("batch-components",
                List.of(new BatchRunTracker.BatchChildRef("example", "run-a4")));

        Map<String, Object> result = BatchRunServices.getBatchTestRunStatus(dctx,
                Map.of("batchId", "batch-components", "userLogin", userLogin));

        List<?> components = (List<?>) result.get("components");
        assertThat(components.size(), is(1));
        Map<?, ?> componentResult = (Map<?, ?>) components.get(0);
        assertThat(componentResult.get("componentName"), is("example"));
        assertThat(componentResult.get("runId"), is("run-a4"));
        assertThat(componentResult.get("status"), is("PASSED"));
    }

    @Test
    void getBatchTestRunStatusPrefersErrorOverFailedWhenBothArePresent() {
        DispatchContext dctx = mock(DispatchContext.class);
        Security security = mock(Security.class);
        GenericValue userLogin = mock(GenericValue.class);
        when(dctx.getSecurity()).thenReturn(security);
        when(userLogin.getString("userLoginId")).thenReturn("admin");
        when(security.hasPermission("TESTEXEC_ADMIN", userLogin)).thenReturn(true);
        TestRunServices.TRACKER.register("run-p1-err", "example-tests", "example", "admin", Map.of());
        TestRunServices.TRACKER.markError("run-p1-err", new RuntimeException("example error"));
        TestRunServices.TRACKER.register("run-p1-fail", "party-tests", "party", "admin", Map.of());
        TestRunServices.TRACKER.markFailed("run-p1-fail", Map.of("total", 1, "passed", 0, "failed", 1));
        BatchRunServices.BATCH_TRACKER.register("batch-p1", List.of(
                new BatchRunTracker.BatchChildRef("example", "run-p1-err"),
                new BatchRunTracker.BatchChildRef("party", "run-p1-fail")));

        Map<String, Object> result = BatchRunServices.getBatchTestRunStatus(dctx,
                Map.of("batchId", "batch-p1", "userLogin", userLogin));

        assertThat(result.get("status"), is("ERROR"));
    }

    @Test
    void getBatchTestRunStatusPrefersFailedOverRunningWhenBothArePresent() {
        DispatchContext dctx = mock(DispatchContext.class);
        Security security = mock(Security.class);
        GenericValue userLogin = mock(GenericValue.class);
        when(dctx.getSecurity()).thenReturn(security);
        when(userLogin.getString("userLoginId")).thenReturn("admin");
        when(security.hasPermission("TESTEXEC_ADMIN", userLogin)).thenReturn(true);
        TestRunServices.TRACKER.register("run-p2-fail", "example-tests", "example", "admin", Map.of());
        TestRunServices.TRACKER.markFailed("run-p2-fail", Map.of("total", 1, "passed", 0, "failed", 1));
        TestRunServices.TRACKER.register("run-p2-run", "party-tests", "party", "admin", Map.of());
        TestRunServices.TRACKER.markRunning("run-p2-run");
        BatchRunServices.BATCH_TRACKER.register("batch-p2", List.of(
                new BatchRunTracker.BatchChildRef("example", "run-p2-fail"),
                new BatchRunTracker.BatchChildRef("party", "run-p2-run")));

        Map<String, Object> result = BatchRunServices.getBatchTestRunStatus(dctx,
                Map.of("batchId", "batch-p2", "userLogin", userLogin));

        assertThat(result.get("status"), is("FAILED"));
    }

    @Test
    void getBatchTestRunStatusIsRunningForAGenuinelyQueuedChildThatNeverStarted() {
        DispatchContext dctx = mock(DispatchContext.class);
        Security security = mock(Security.class);
        GenericValue userLogin = mock(GenericValue.class);
        when(dctx.getSecurity()).thenReturn(security);
        when(userLogin.getString("userLoginId")).thenReturn("admin");
        when(security.hasPermission("TESTEXEC_ADMIN", userLogin)).thenReturn(true);
        TestRunServices.TRACKER.register("run-p3-queued", "example-tests", "example", "admin", Map.of());
        BatchRunServices.BATCH_TRACKER.register("batch-p3",
                List.of(new BatchRunTracker.BatchChildRef("example", "run-p3-queued")));

        Map<String, Object> result = BatchRunServices.getBatchTestRunStatus(dctx,
                Map.of("batchId", "batch-p3", "userLogin", userLogin));

        assertThat(result.get("status"), is("RUNNING"));
        Map<?, ?> summary = (Map<?, ?>) result.get("summary");
        assertThat(summary.get("queued"), is(1));
    }
}
