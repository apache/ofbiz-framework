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
}
