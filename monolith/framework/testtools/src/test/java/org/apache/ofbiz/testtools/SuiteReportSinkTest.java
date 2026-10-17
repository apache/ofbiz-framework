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

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

class SuiteReportSinkTest {

    @Test
    void passedFactoryProducesAPassedOutcome() {
        assertThat(SuiteReportSink.Outcome.passed(), instanceOf(SuiteReportSink.Outcome.Passed.class));
    }

    @Test
    void failureFactoryCarriesItsFields() {
        SuiteReportSink.Outcome.Failure failure =
                (SuiteReportSink.Outcome.Failure) SuiteReportSink.Outcome.failure("msg", "java.lang.AssertionError", "trace");

        assertThat(failure.message(), is("msg"));
        assertThat(failure.type(), is("java.lang.AssertionError"));
        assertThat(failure.stackTrace(), is("trace"));
    }

    @Test
    void errorFactoryCarriesTheThrowable() {
        RuntimeException boom = new RuntimeException("boom");

        SuiteReportSink.Outcome.Error error = (SuiteReportSink.Outcome.Error) SuiteReportSink.Outcome.error(boom);

        assertThat(error.throwable(), sameInstance(boom));
    }
}
