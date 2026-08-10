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
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.ofbiz.webapp.control.RequestHandler;
import org.apache.ofbiz.widget.content.StaticContentUrlProvider;
import org.apache.ofbiz.widget.model.ModelFormField;
import org.apache.ofbiz.widget.renderer.VisualTheme;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public final class RenderableFtlFormElementsBuilderDatetimeTest {

    // Deep stubs: RenderableFtlFormElementsBuilder methods cascade through modelFormField.getModelForm()
    // (via FormRenderer.getCurrentFormName()) even when a test doesn't care about the form itself -
    // JMockit's @Mocked auto-cascaded that call to a fresh mock; Mockito needs it asked for explicitly.
    private final ModelFormField modelFormField = mock(ModelFormField.class, RETURNS_DEEP_STUBS);

    private RenderableFtlFormElementsBuilder renderableFtlFormElementsBuilder;

    @BeforeEach
    public void createBuilder() {
        renderableFtlFormElementsBuilder = new RenderableFtlFormElementsBuilder(
                mock(VisualTheme.class), mock(RequestHandler.class),
                mock(HttpServletRequest.class), mock(HttpServletResponse.class),
                mock(StaticContentUrlProvider.class));
    }

    @Test
    public void datetimeFieldSetsIdAndValue() {
        final int maxLength = 22;
        final ModelFormField.DateTimeField datetimeField = mock(ModelFormField.DateTimeField.class);
        when(datetimeField.getModelFormField()).thenReturn(modelFormField);
        when(modelFormField.getCurrentContainerId(notNull())).thenReturn("CurrentDatetimeId");
        when(modelFormField.getEntry(notNull(), nullable(String.class))).thenReturn("DATETIMEVALUE");

        final HashMap<String, Object> context = new HashMap<>();

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.dateTime(context, datetimeField);
        assertThat(renderableFtl, MacroCallMatcher.hasNameAndParameters("renderDateTimeField",
                MacroCallParameterMatcher.hasNameAndStringValue("id", "CurrentDatetimeId"),
                MacroCallParameterMatcher.hasNameAndStringValue("value", "DATETIMEVALUE")));
    }

    @Test
    public void datetimeFieldSetsDisabledParameters() {
        final ModelFormField.DateTimeField datetimeField = mock(ModelFormField.DateTimeField.class);
        when(datetimeField.getModelFormField()).thenReturn(modelFormField);
        when(modelFormField.getDisabled(notNull())).thenReturn(true);
        when(modelFormField.getEntry(notNull(), nullable(String.class))).thenReturn("DATETIMEVALUE");

        final HashMap<String, Object> context = new HashMap<>();

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.dateTime(context, datetimeField);
        assertThat(renderableFtl, MacroCallMatcher.hasNameAndParameters("renderDateTimeField",
                MacroCallParameterMatcher.hasNameAndBooleanValue("disabled", true)));
    }

    @Test
    public void datetimeFieldSetsLengthAndMaskForDateType() {
        final ModelFormField.DateTimeField datetimeField = mock(ModelFormField.DateTimeField.class);
        when(datetimeField.getModelFormField()).thenReturn(modelFormField);
        when(datetimeField.isDateType()).thenReturn(true);
        when(datetimeField.useMask()).thenReturn(true);
        when(modelFormField.getEntry(notNull(), nullable(String.class))).thenReturn("DATETIMEVALUE");

        final HashMap<String, Object> context = new HashMap<>();

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.dateTime(context, datetimeField);
        assertThat(renderableFtl, MacroCallMatcher.hasNameAndParameters("renderDateTimeField",
                MacroCallParameterMatcher.hasNameAndStringValue("mask", "9999-99-99"),
                MacroCallParameterMatcher.hasNameAndIntegerValue("size", 10),
                MacroCallParameterMatcher.hasNameAndIntegerValue("maxlength", 10)));
    }

    @Test
    public void datetimeFieldSetsLengthForTimeType() {
        final ModelFormField.DateTimeField datetimeField = mock(ModelFormField.DateTimeField.class);
        when(datetimeField.getModelFormField()).thenReturn(modelFormField);
        when(datetimeField.isTimeType()).thenReturn(true);
        when(datetimeField.useMask()).thenReturn(true);
        when(modelFormField.getEntry(notNull(), nullable(String.class))).thenReturn("DATETIMEVALUE");

        final HashMap<String, Object> context = new HashMap<>();

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.dateTime(context, datetimeField);
        assertThat(renderableFtl, MacroCallMatcher.hasNameAndParameters("renderDateTimeField",
                MacroCallParameterMatcher.hasNameAndStringValue("mask", "99:99:99"),
                MacroCallParameterMatcher.hasNameAndIntegerValue("size", 8),
                MacroCallParameterMatcher.hasNameAndIntegerValue("maxlength", 8)));
    }

    @Test
    public void datetimeFieldSetsLengthForTimestampType() {
        final ModelFormField.DateTimeField datetimeField = mock(ModelFormField.DateTimeField.class);
        when(datetimeField.getModelFormField()).thenReturn(modelFormField);
        when(datetimeField.isTimestampType()).thenReturn(true);
        when(datetimeField.useMask()).thenReturn(true);
        when(modelFormField.getEntry(notNull(), nullable(String.class))).thenReturn("DATETIMEVALUE");

        final HashMap<String, Object> context = new HashMap<>();

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.dateTime(context, datetimeField);
        assertThat(renderableFtl, MacroCallMatcher.hasNameAndParameters("renderDateTimeField",
                MacroCallParameterMatcher.hasNameAndStringValue("mask", "9999-99-99 99:99:99"),
                MacroCallParameterMatcher.hasNameAndIntegerValue("size", 25),
                MacroCallParameterMatcher.hasNameAndIntegerValue("maxlength", 30)));
    }

    @Test
    public void datetimeFieldSetsTimeValuesForStepSize1() {
        final ModelFormField.DateTimeField datetimeField = mock(ModelFormField.DateTimeField.class);
        when(datetimeField.getModelFormField()).thenReturn(modelFormField);
        when(datetimeField.getStep()).thenReturn(1);
        when(datetimeField.getInputMethod()).thenReturn("time-dropdown");
        when(modelFormField.getEntry(notNull(), nullable(String.class))).thenReturn("DATETIMEVALUE");

        final HashMap<String, Object> context = new HashMap<>();

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.dateTime(context, datetimeField);
        assertThat(renderableFtl, MacroCallMatcher.hasNameAndParameters("renderDateTimeField",
                MacroCallParameterMatcher.hasNameAndStringValueStartsWith("timeValues", "[0, 1, 2, 3,"),
                MacroCallParameterMatcher.hasNameAndStringValueEndsWith("timeValues", "56, 57, 58, 59]")));
    }

    @Test
    public void datetimeFieldSetsTimeValuesForStepSize3() {
        final ModelFormField.DateTimeField datetimeField = mock(ModelFormField.DateTimeField.class);
        when(datetimeField.getModelFormField()).thenReturn(modelFormField);
        when(datetimeField.getStep()).thenReturn(3);
        when(datetimeField.getInputMethod()).thenReturn("time-dropdown");
        when(modelFormField.getEntry(notNull(), nullable(String.class))).thenReturn("DATETIMEVALUE");

        final HashMap<String, Object> context = new HashMap<>();

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.dateTime(context, datetimeField);
        assertThat(renderableFtl, MacroCallMatcher.hasNameAndParameters("renderDateTimeField",
                MacroCallParameterMatcher.hasNameAndStringValueStartsWith("timeValues", "[0, 3, 6, 9,"),
                MacroCallParameterMatcher.hasNameAndStringValueEndsWith("timeValues", "48, 51, 54, 57]")));
    }

    @Test
    public void datetimeFieldSetsValuesFor12HourClock() {
        final ModelFormField.DateTimeField datetimeField = mock(ModelFormField.DateTimeField.class);
        when(datetimeField.getModelFormField()).thenReturn(modelFormField);
        when(datetimeField.getStep()).thenReturn(1);
        when(datetimeField.getInputMethod()).thenReturn("time-dropdown");
        when(datetimeField.isTwelveHour()).thenReturn(true);
        when(modelFormField.getEntry(notNull(), nullable(String.class))).thenReturn("2022-05-18 16:44:57");

        final HashMap<String, Object> context = new HashMap<>();

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.dateTime(context, datetimeField);
        assertThat(renderableFtl, MacroCallMatcher.hasNameAndParameters("renderDateTimeField",
                MacroCallParameterMatcher.hasNameAndIntegerValue("hour1", 4),
                MacroCallParameterMatcher.hasNameAndIntegerValue("hour2", 16),
                MacroCallParameterMatcher.hasNameAndBooleanValue("isTwelveHour", true),
                MacroCallParameterMatcher.hasNameAndBooleanValue("pmSelected", true)));
    }
}
