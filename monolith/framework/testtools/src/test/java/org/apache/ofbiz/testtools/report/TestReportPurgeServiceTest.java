/*
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
 */
package org.apache.ofbiz.testtools.report;

import java.io.File;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Covers just {@link TestReportPurgeService#resolveBaseDir(String, String)}: the rest of
 * {@code purgeOldTestReports} needs a running DispatchContext/Delegator, so it's out of scope here
 * by design.
 */
class TestReportPurgeServiceTest {

    @Test
    void resolvesARelativePathAgainstOfbizHome() {
        File resolved = TestReportPurgeService.resolveBaseDir("runtime/test-reports", "/opt/ofbiz");

        assertThat(resolved, is(new File("/opt/ofbiz", "runtime/test-reports")));
    }

    @Test
    void leavesAnAbsolutePathAlone() {
        File resolved = TestReportPurgeService.resolveBaseDir("/var/ofbiz-reports", "/opt/ofbiz");

        assertThat(resolved, is(new File("/var/ofbiz-reports")));
    }

    @Test
    void fallsBackToTheConfiguredPathAsIsWhenOfbizHomeIsUnset() {
        File resolved = TestReportPurgeService.resolveBaseDir("runtime/test-reports", null);

        assertThat(resolved, is(new File("runtime/test-reports")));
    }
}
