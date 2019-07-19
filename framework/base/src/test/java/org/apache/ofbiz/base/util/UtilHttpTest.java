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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Predicate;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HttpMethod;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class UtilHttpTest {
    private HttpServletRequest req;

    @Before
    public void setup() {
        req = Mockito.mock(HttpServletRequest.class);
    }

    @Test
    public void basicGetPathInfoOnlyParameterMap() {
        assertThat(getPathInfoOnlyParameterMap("/~foo=1/~bar=2", x -> true),
                allOf(hasEntry("foo", "1"), hasEntry("bar", "2")));

        assertThat(getPathInfoOnlyParameterMap("/~foo=1/~foo=2", x -> true),
                hasEntry("foo", Arrays.asList("1", "2")));

        assertThat(getPathInfoOnlyParameterMap("/~foo=1/~foo=2/~foo=3/", x -> true),
                hasEntry("foo", Arrays.asList("1", "2", "3")));

        assertThat(getPathInfoOnlyParameterMap("/~foo=1/~bar=2/~foo=3/", x -> true),
                Matchers.<Map<String,Object>>allOf(
                        hasEntry("foo", Arrays.asList("1", "3")),
                        hasEntry("bar", "2")));
    }

    @Test
    public void emptyGetPathInfoOnlyParameterMap() {
        assertThat(getPathInfoOnlyParameterMap(null, x -> true), is(anEmptyMap()));
    }

    @Test
    public void filteredGetPathInfoOnlyParameterMap() {
        assertThat(getPathInfoOnlyParameterMap("/~foo=1/~bar=2", name -> !"foo".equals(name)),
                allOf(not(hasEntry("foo", "1")), hasEntry("bar", "2")));

        assertThat(getPathInfoOnlyParameterMap("/~foo=1/~bar=2", "foo"::equals),
                allOf(hasEntry("foo", "1"), not(hasEntry("bar", "2"))));
    }

    @Test
    public void basicGetParameterMap() {
        when(req.getParameterMap()).thenReturn(UtilMisc.toMap(
                "foo", new String[] {"1"},
                "bar", new String[] {"2", "3"}));
        when(req.getPathInfo()).thenReturn("/foo");
        assertThat(UtilHttp.getParameterMap(req), Matchers.<Map<String, Object>>allOf(
                hasEntry("foo", "1"),
                hasEntry("bar", Arrays.asList("2", "3"))));
    }

    @Test
    public void pathInfoOverrideGetParameterMap() {
        when(req.getParameterMap()).thenReturn(UtilMisc.toMap(
                "foo", new String[] {"1"},
                "bar", new String[] {"2"}));
        when(req.getPathInfo()).thenReturn("/foo/~bar=3");
        assertThat(UtilHttp.getParameterMap(req), Matchers.<Map<String, Object>>allOf(
                hasEntry("foo", "1"),
                hasEntry("bar", "3")));
    }

    @Test
    public void emptyParameterMap() {
        when(req.getParameterMap()).thenReturn(Collections.emptyMap());
        when(req.getPathInfo()).thenReturn("/foo/bar");
        when(req.getMethod()).thenReturn(HttpMethod.POST);
        UtilHttp.getParameterMap(req);
        // Check that multi-part arguments are looked up
        Mockito.verify(req).getContentType();
    }

    @Test
    public void filteredGetParameterMap() {
        when(req.getParameterMap()).thenReturn(UtilMisc.toMap(
                "foo", new String[] {"1"},
                "bar", new String[] {"2", "3"}));
        when(req.getPathInfo()).thenReturn("/foo");
        Predicate<String> equalsBar = "bar"::equals;
        assertThat(UtilHttp.getParameterMap(req, equalsBar.negate()), Matchers.<Map<String, Object>>allOf(
                hasEntry("foo", "1"),
                not(hasEntry("bar", Arrays.asList("2", "3")))));
        assertThat(UtilHttp.getParameterMap(req, equalsBar), Matchers.<Map<String, Object>>allOf(
                not(hasEntry("foo", "1")),
                hasEntry("bar", Arrays.asList("2", "3"))));
    }

    @Test
    public void basicMakeParamValueFromComposite() {
        when(req.getParameter("meetingDate_c_compositeType")).thenReturn("Timestamp");
        when(req.getParameterMap()).thenReturn(UtilMisc.toMap(
                "meetingDate_c_date", new String[] {"2019-07-14"},
                "meetingDate_c_hour", new String[] {"13"},
                "meetingDate_c_minutes", new String[] {"8"}));
        assertThat(UtilHttp.makeParamValueFromComposite(req, "meetingDate"),
                equalTo(Timestamp.valueOf(LocalDateTime.of(2019, Month.JULY, 14, 13, 8))));
    }

    @Test
    public void emptyTypeMakeParamValueFromComposite() {
        when(req.getParameter("meetingDate_c_compositeType")).thenReturn(null);
        when(req.getParameterMap()).thenReturn(UtilMisc.toMap(
                "meetingDate_c_date", new String[] {"2019-07-14"},
                "meetingDate_c_hour", new String[] {"13"},
                "meetingDate_c_minutes", new String[] {"8"}));
        assertNull(UtilHttp.makeParamValueFromComposite(req, "meetingDate"));
    }

    @Test
    public void ampmMakeParamValueFromComposite() {
        when(req.getParameter("meetingDate_c_compositeType")).thenReturn("Timestamp");

        when(req.getParameterMap()).thenReturn(UtilMisc.toMap(
                "meetingDate_c_date", new String[] {"2019-07-14"},
                "meetingDate_c_hour", new String[] {"12"},
                "meetingDate_c_minutes", new String[] {"8"},
                "meetingDate_c_ampm", new String[] {"AM"}));
        assertThat(UtilHttp.makeParamValueFromComposite(req, "meetingDate"),
                equalTo(Timestamp.valueOf(LocalDateTime.of(2019, Month.JULY, 14, 0, 8))));

        when(req.getParameterMap()).thenReturn(UtilMisc.toMap(
                "meetingDate_c_date", new String[] {"2019-07-14"},
                "meetingDate_c_hour", new String[] {"8"},
                "meetingDate_c_minutes", new String[] {"8"},
                "meetingDate_c_ampm", new String[] {"PM"}));
        assertThat(UtilHttp.makeParamValueFromComposite(req, "meetingDate"),
                equalTo(Timestamp.valueOf(LocalDateTime.of(2019, Month.JULY, 14, 20, 8))));

        when(req.getParameterMap()).thenReturn(UtilMisc.toMap(
                "meetingDate_c_date", new String[] {"2019-07-14"},
                "meetingDate_c_hour", new String[] {"18"},
                "meetingDate_c_minutes", new String[] {"8"},
                "meetingDate_c_ampm", new String[] {"PM"}));
        assertThat(UtilHttp.makeParamValueFromComposite(req, "meetingDate"),
                equalTo(Timestamp.valueOf(LocalDateTime.of(2019, Month.JULY, 14, 18, 8))));
    }
}
