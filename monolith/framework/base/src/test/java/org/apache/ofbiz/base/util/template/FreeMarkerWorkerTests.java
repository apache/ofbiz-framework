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
package org.apache.ofbiz.base.util.template;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public final class FreeMarkerWorkerTests {
    @BeforeEach
    public void initialize() {
        System.setProperty("ofbiz.home", System.getProperty("user.dir"));
    }

    @Test
    public void renderTemplateFromString() throws Exception {
        StringWriter out = new StringWriter();
        Map<String, Object> context = new HashMap<>();
        context.put("name", "World!");
        FreeMarkerWorker.renderTemplateFromString("template1", "Hello ${name}", context, out, 0, false);
        assertEquals("Hello World!", out.toString());
    }

    /**
     * OFBIZ-13504: report templates format dates through
     * {@code Static["...UtilDateTime"].toDateString(date, format)}. FreeMarker resolves
     * {@code Static[...]} members reflectively against public methods only, so the two-argument
     * overload must stay publicly visible for those templates to render.
     *
     * <p>The date below is deliberately the 9th of the 3rd month: {@code dd/MM/yyyy} yields
     * 09/03/2026 while {@code MM/dd/yyyy} yields 03/09/2026. The assertion therefore also fails if
     * the explicit format is dropped in favour of the single-argument overload, which silently
     * switches the reports to US date order.</p>
     */
    @Test
    public void renderTemplateCallingToDateStringWithExplicitFormat() throws Exception {
        StringWriter out = new StringWriter();
        Map<String, Object> context = new HashMap<>();
        context.put("aDate", Timestamp.valueOf("2026-03-09 14:30:00"));
        FreeMarkerWorker.renderTemplateFromString("templateToDateString",
                "${Static[\"org.apache.ofbiz.base.util.UtilDateTime\"].toDateString(aDate, \"dd/MM/yyyy\")}",
                context, out, 0, false);
        assertEquals("09/03/2026", out.toString());
    }
}
