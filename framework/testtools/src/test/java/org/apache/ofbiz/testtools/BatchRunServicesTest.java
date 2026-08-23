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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
    void runBatchTestSuiteOmitsAComponentWhoseRunTestSuiteCallErrors() {
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

            assertThat(result.get("responseMessage"), is("success"));
            String batchId = (String) result.get("batchId");
            assertThat(BatchRunServices.BATCH_TRACKER.get(batchId), is(List.of()));
        }
    }
}
