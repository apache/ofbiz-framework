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
package org.apache.ofbiz.widget.renderer.macro;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.webapp.control.RequestHandler;
import org.apache.ofbiz.widget.content.StaticContentUrlProvider;
import org.apache.ofbiz.widget.model.ModelFormField;
import org.apache.ofbiz.widget.model.ModelTheme;
import org.apache.ofbiz.widget.renderer.VisualTheme;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtl;
import org.junit.Test;

import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;

public class RenderableFtlFormElementsBuilderDatetimeTest {

    @Injectable
    private VisualTheme visualTheme;

    @Injectable
    private RequestHandler requestHandler;

    @Injectable
    private HttpServletRequest request;

    @Injectable
    private HttpServletResponse response;

    @Injectable
    private StaticContentUrlProvider staticContentUrlProvider;

    @Mocked
    private HttpSession httpSession;

    @Mocked
    private ModelTheme modelTheme;

    @Mocked
    private ModelFormField.ContainerField containerField;

    @Mocked
    private ModelFormField modelFormField;

    @Tested
    private RenderableFtlFormElementsBuilder renderableFtlFormElementsBuilder;

    @Test
    public void datetimeFieldSetsIdAndValue(@Mocked final ModelFormField.DateTimeField datetimeField) {
        final int maxLength = 22;
        new Expectations() {
            {
                modelFormField.getCurrentContainerId(withNotNull());
                result = "CurrentDatetimeId";

                modelFormField.getEntry(withNotNull(), anyString);
                result = "DATETIMEVALUE";
            }
        };

        final HashMap<String, Object> context = new HashMap<>();

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.dateTime(context, datetimeField);
        assertThat(renderableFtl, MacroCallMatcher.hasNameAndParameters("renderDateTimeField",
                MacroCallParameterMatcher.hasNameAndStringValue("id", "CurrentDatetimeId"),
                MacroCallParameterMatcher.hasNameAndStringValue("value", "DATETIMEVALUE")));
    }

    @Test
    public void datetimeFieldSetsDisabledParameters(@Mocked final ModelFormField.DateTimeField datetimeField) {
        new Expectations() {
            {
                modelFormField.getDisabled(withNotNull());
                result = true;

                modelFormField.getEntry(withNotNull(), anyString);
                result = "DATETIMEVALUE";
            }
        };

        final HashMap<String, Object> context = new HashMap<>();

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.dateTime(context, datetimeField);
        assertThat(renderableFtl, MacroCallMatcher.hasNameAndParameters("renderDateTimeField",
                MacroCallParameterMatcher.hasNameAndBooleanValue("disabled", true)));
    }

    @Test
    public void datetimeFieldSetsLengthAndMaskForDateType(@Mocked final ModelFormField.DateTimeField datetimeField) {
        new Expectations() {
            {
                datetimeField.isDateType();
                result = true;

                datetimeField.useMask();
                result = true;

                modelFormField.getEntry(withNotNull(), anyString);
                result = "DATETIMEVALUE";
            }
        };

        final HashMap<String, Object> context = new HashMap<>();

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.dateTime(context, datetimeField);
        assertThat(renderableFtl, MacroCallMatcher.hasNameAndParameters("renderDateTimeField",
                MacroCallParameterMatcher.hasNameAndStringValue("mask", "9999-99-99"),
                MacroCallParameterMatcher.hasNameAndIntegerValue("size", 10),
                MacroCallParameterMatcher.hasNameAndIntegerValue("maxlength", 10)));
    }

    @Test
    public void datetimeFieldSetsLengthForTimeType(@Mocked final ModelFormField.DateTimeField datetimeField) {
        new Expectations() {
            {
                datetimeField.isTimeType();
                result = true;

                datetimeField.useMask();
                result = true;

                modelFormField.getEntry(withNotNull(), anyString);
                result = "DATETIMEVALUE";
            }
        };

        final HashMap<String, Object> context = new HashMap<>();

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.dateTime(context, datetimeField);
        assertThat(renderableFtl, MacroCallMatcher.hasNameAndParameters("renderDateTimeField",
                MacroCallParameterMatcher.hasNameAndStringValue("mask", "99:99:99"),
                MacroCallParameterMatcher.hasNameAndIntegerValue("size", 8),
                MacroCallParameterMatcher.hasNameAndIntegerValue("maxlength", 8)));
    }

    @Test
    public void datetimeFieldSetsLengthForTimestampType(@Mocked final ModelFormField.DateTimeField datetimeField) {
        new Expectations() {
            {
                datetimeField.isTimestampType();
                result = true;
                minTimes = 0;

                datetimeField.useMask();
                result = true;

                modelFormField.getEntry(withNotNull(), anyString);
                result = "DATETIMEVALUE";
            }
        };

        final HashMap<String, Object> context = new HashMap<>();

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.dateTime(context, datetimeField);
        assertThat(renderableFtl, MacroCallMatcher.hasNameAndParameters("renderDateTimeField",
                MacroCallParameterMatcher.hasNameAndStringValue("mask", "9999-99-99 99:99:99"),
                MacroCallParameterMatcher.hasNameAndIntegerValue("size", 25),
                MacroCallParameterMatcher.hasNameAndIntegerValue("maxlength", 30)));
    }

    @Test
    public void datetimeFieldSetsTimeValuesForStepSize1(@Mocked final ModelFormField.DateTimeField datetimeField) {
        new Expectations() {
            {
                datetimeField.getStep();
                result = 1;

                datetimeField.getInputMethod();
                result = "time-dropdown";

                modelFormField.getEntry(withNotNull(), anyString);
                result = "DATETIMEVALUE";
            }
        };

        final HashMap<String, Object> context = new HashMap<>();

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.dateTime(context, datetimeField);
        assertThat(renderableFtl, MacroCallMatcher.hasNameAndParameters("renderDateTimeField",
                MacroCallParameterMatcher.hasNameAndStringValueStartsWith("timeValues", "[0, 1, 2, 3,"),
                MacroCallParameterMatcher.hasNameAndStringValueEndsWith("timeValues", "56, 57, 58, 59]")));
    }

    @Test
    public void datetimeFieldSetsTimeValuesForStepSize3(@Mocked final ModelFormField.DateTimeField datetimeField) {
        new Expectations() {
            {
                datetimeField.getStep();
                result = 3;

                datetimeField.getInputMethod();
                result = "time-dropdown";

                modelFormField.getEntry(withNotNull(), anyString);
                result = "DATETIMEVALUE";
            }
        };

        final HashMap<String, Object> context = new HashMap<>();

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.dateTime(context, datetimeField);
        assertThat(renderableFtl, MacroCallMatcher.hasNameAndParameters("renderDateTimeField",
                MacroCallParameterMatcher.hasNameAndStringValueStartsWith("timeValues", "[0, 3, 6, 9,"),
                MacroCallParameterMatcher.hasNameAndStringValueEndsWith("timeValues", "48, 51, 54, 57]")));
    }

    @Test
    public void datetimeFieldSetsValuesFor12HourClock(@Mocked final ModelFormField.DateTimeField datetimeField) {
        new Expectations() {
            {
                datetimeField.getStep();
                result = 1;

                datetimeField.getInputMethod();
                result = "time-dropdown";

                datetimeField.isTwelveHour();
                result = true;

                modelFormField.getEntry(withNotNull(), anyString);
                result = "2022-05-18 16:44:57";
            }
        };

        final HashMap<String, Object> context = new HashMap<>();

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.dateTime(context, datetimeField);
        assertThat(renderableFtl, MacroCallMatcher.hasNameAndParameters("renderDateTimeField",
                MacroCallParameterMatcher.hasNameAndIntegerValue("hour1", 4),
                MacroCallParameterMatcher.hasNameAndIntegerValue("hour2", 16),
                MacroCallParameterMatcher.hasNameAndBooleanValue("isTwelveHour", true),
                MacroCallParameterMatcher.hasNameAndBooleanValue("pmSelected", true)));
    }
}
