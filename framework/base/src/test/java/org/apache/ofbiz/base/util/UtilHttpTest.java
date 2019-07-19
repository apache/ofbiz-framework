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
package org.apache.ofbiz.base.util;

import static org.apache.ofbiz.base.util.UtilHttp.getPathInfoOnlyParameterMap;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Test;

public class UtilHttpTest {

    @Test
    public void basicGetPathInfoOnlyParameterMap() {
        assertThat(getPathInfoOnlyParameterMap("/~foo=1/~bar=2", null, false),
                allOf(hasEntry("foo", "1"), hasEntry("bar", "2")));

        assertThat(getPathInfoOnlyParameterMap("/~foo=1/~foo=2", null, false),
                hasEntry("foo", Arrays.asList("1", "2")));

        assertThat(getPathInfoOnlyParameterMap("/~foo=1/~foo=2/~foo=3/", null, false),
                hasEntry("foo", Arrays.asList("1", "2", "3")));

        assertThat(getPathInfoOnlyParameterMap("/~foo=1/~bar=2/~foo=3/", null, false),
                Matchers.<Map<String,Object>>allOf(
                        hasEntry("foo", Arrays.asList("1", "3")),
                        hasEntry("bar", "2")));
    }

    @Test
    public void emptyGetPathInfoOnlyParameterMap() {
        assertThat(getPathInfoOnlyParameterMap(null, null, false), is(anEmptyMap()));
    }

    @Test
    public void filteredGetPathInfoOnlyParameterMap() {
        assertThat(getPathInfoOnlyParameterMap("/~foo=1/~bar=2", UtilMisc.toSet("foo"), false),
                allOf(not(hasEntry("foo", "1")), hasEntry("bar", "2")));

        assertThat(getPathInfoOnlyParameterMap("/~foo=1/~bar=2", UtilMisc.toSet("foo"), true),
                allOf(hasEntry("foo", "1"), not(hasEntry("bar", "2"))));
    }
}
