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
}
