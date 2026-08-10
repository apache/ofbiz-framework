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

import junit.framework.TestCase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

class SuiteEntryTest {

    @Test
    void junit3EntryCarriesItsTest() {
        junit.framework.Test fixture = new TestCase("dummy") {
            @Override
            public void runTest() {
            }
        };

        SuiteEntry entry = new SuiteEntry.Junit3Entry(fixture);

        assertThat(entry, instanceOf(SuiteEntry.Junit3Entry.class));
        assertThat(((SuiteEntry.Junit3Entry) entry).test(), sameInstance(fixture));
    }

    @Test
    void jupiterEntryCarriesItsTestClass() {
        SuiteEntry entry = new SuiteEntry.JupiterEntry(SuiteEntryTest.class);

        assertThat(entry, instanceOf(SuiteEntry.JupiterEntry.class));
        assertThat(((SuiteEntry.JupiterEntry) entry).testClass(), is(SuiteEntryTest.class));
    }
}
