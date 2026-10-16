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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.ofbiz.webapp.control.ConfigXMLReader;
import org.apache.ofbiz.webapp.control.RequestHandler;
import org.apache.ofbiz.widget.content.StaticContentUrlProvider;
import org.apache.ofbiz.widget.model.FieldInfo;
import org.apache.ofbiz.widget.model.ModelForm;
import org.apache.ofbiz.widget.model.ModelFormField;
import org.apache.ofbiz.widget.model.ModelScreenWidget;
import org.apache.ofbiz.widget.renderer.VisualTheme;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtlMacroCall;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtl;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtlNoop;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtlString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasKey;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class RenderableFtlFormElementsBuilderTest {

    private final VisualTheme visualTheme = mock(VisualTheme.class);
    private final RequestHandler requestHandler = mock(RequestHandler.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final StaticContentUrlProvider staticContentUrlProvider = mock(StaticContentUrlProvider.class);
    private final HttpSession httpSession = mock(HttpSession.class);
    private final ModelFormField.ContainerField containerField = mock(ModelFormField.ContainerField.class);

    // Deep stubs: RenderableFtlFormElementsBuilder methods cascade through modelFormField.getModelForm()
    // (via FormRenderer.getCurrentFormName()) even when a test doesn't care about the form itself -
    // JMockit's @Mocked auto-cascaded that call to a fresh mock; Mockito needs it asked for explicitly.
    private final ModelFormField modelFormField = mock(ModelFormField.class, RETURNS_DEEP_STUBS);

    private RenderableFtlFormElementsBuilder renderableFtlFormElementsBuilder;

    @BeforeEach
    public void createBuilder() {
        // Every field-type mock below (displayField, textField, ...) routes its own
        // getModelFormField() back to this same shared modelFormField mock - JMockit's @Mocked
        // cascading did this automatically for same-typed mocks; Mockito needs it stubbed per mock.
        when(containerField.getModelFormField()).thenReturn(modelFormField);
        // makeHyperlinkString()'s underlying CsrfUtil.generateTokenForNonAjax() call dereferences
        // request.getSession() directly with no null-check - JMockit's @Injectable request cascaded
        // that call to a fresh mock automatically; Mockito needs it stubbed explicitly.
        when(request.getSession()).thenReturn(httpSession);
        renderableFtlFormElementsBuilder = new RenderableFtlFormElementsBuilder(
                visualTheme, requestHandler, request, response, staticContentUrlProvider);
    }

    @Test
    public void emptyLabelUsesNoopMacro() {
        final ModelScreenWidget.Label label = mock(ModelScreenWidget.Label.class);
        when(label.getText(notNull())).thenReturn("");

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.label(ImmutableMap.of(), label);
        assertThat(renderableFtl, equalTo(RenderableFtlNoop.INSTANCE));
    }

    @Test
    public void labelMacroCallUsesText() {
        final ModelScreenWidget.Label label = mock(ModelScreenWidget.Label.class);
        when(label.getText(notNull())).thenReturn("TEXT");

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.label(ImmutableMap.of(), label);
        assertThat(renderableFtl,
                MacroCallMatcher.hasNameAndParameters("renderLabel",
                        MacroCallParameterMatcher.hasNameAndStringValue("text", "TEXT")));
    }

    @Test
    public void displayFieldMacroUsesType() {
        final ModelFormField.DisplayField displayField = mock(ModelFormField.DisplayField.class);
        when(displayField.getModelFormField()).thenReturn(modelFormField);
        when(displayField.getType()).thenReturn("TYPE");
        when(displayField.getDescription(notNull())).thenReturn("DESCRIPTION");

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.displayField(ImmutableMap.of(),
                displayField, false);
        assertThat(renderableFtl,
                MacroCallMatcher.hasNameAndParameters("renderDisplayField",
                        MacroCallParameterMatcher.hasNameAndStringValue("type", "TYPE")));
    }

    @Test
    public void containerMacroCallUsesContainerId() {
        when(modelFormField.getCurrentContainerId(notNull())).thenReturn("CurrentContainerId");

        final RenderableFtlMacroCall macroCall =
                renderableFtlFormElementsBuilder.containerMacroCall(ImmutableMap.of(), containerField);
        assertThat(macroCall,
                MacroCallMatcher.hasNameAndParameters("renderContainerField",
                        MacroCallParameterMatcher.hasNameAndStringValue("id", "CurrentContainerId")));
    }

    @Test
    public void basicAnchorLinkCreatesMacroCall() {
        final ModelFormField.SubHyperlink subHyperlink = mock(ModelFormField.SubHyperlink.class);
        when(subHyperlink.getModelFormField()).thenReturn(modelFormField);

        final Map<String, ConfigXMLReader.RequestMap> requestMapMap = new HashMap<>();

        when(subHyperlink.getStyle(notNull())).thenReturn("TestLinkStyle");
        when(subHyperlink.getUrlMode()).thenReturn("url-mode");
        when(subHyperlink.shouldUse(notNull())).thenReturn(true);
        when(subHyperlink.getDescription(notNull())).thenReturn("LinkDescription");
        when(subHyperlink.getTarget(notNull())).thenReturn("/link/target/path");
        when(request.getAttribute("requestMapMap")).thenReturn(requestMapMap);

        final RenderableFtl linkElement =
                renderableFtlFormElementsBuilder.makeHyperlinkString(subHyperlink, new HashMap<>());
        assertThat(linkElement,
                MacroCallMatcher.hasNameAndParameters("makeHyperlinkString",
                        MacroCallParameterMatcher.hasNameAndStringValue("linkStyle", "TestLinkStyle"),
                        MacroCallParameterMatcher.hasNameAndStringValue("linkUrl", "/link/target/path")));
    }

    @Test
    public void textFieldSetsIdValueAndLength() {
        final int maxLength = 42;
        final ModelFormField.TextField textField = mock(ModelFormField.TextField.class);
        when(textField.getModelFormField()).thenReturn(modelFormField);
        when(modelFormField.getCurrentContainerId(notNull())).thenReturn("CurrentTextId");
        when(modelFormField.getEntry(notNull(), nullable(String.class))).thenReturn("TEXTVALUE");
        when(textField.getMaxlength()).thenReturn(maxLength);
        when(httpSession.getAttribute("delegatorName")).thenReturn("DelegatorName");

        final HashMap<String, Object> context = new HashMap<>();
        context.put("session", httpSession);

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.textField(context, textField, true);
        assertThat(renderableFtl, MacroCallMatcher.hasNameAndParameters("renderTextField",
                MacroCallParameterMatcher.hasNameAndStringValue("id", "CurrentTextId"),
                MacroCallParameterMatcher.hasNameAndStringValue("value", "TEXTVALUE"),
                MacroCallParameterMatcher.hasNameAndStringValue("maxlength", Integer.toString(maxLength))));

    }

    @Test
    public void textFieldCreatesAjaxUrl() {
        final ModelFormField.TextField textField = mock(ModelFormField.TextField.class);
        when(textField.getModelFormField()).thenReturn(modelFormField);

        final List<ModelForm.UpdateArea> updateAreas = ImmutableList.of(
                new ModelForm.UpdateArea("change", "areaId1", "target1?param1=${param1}&param2=ThisIsParam2"),
                new ModelForm.UpdateArea("change", "areaId2", "target2"));
        when(modelFormField.getOnChangeUpdateAreas()).thenReturn(updateAreas);
        when(requestHandler.makeLink(request, response, "target1")).thenReturn("http://host.domain/target1");
        when(requestHandler.makeLink(request, response, "target2")).thenReturn("http://host.domain/target2");
        when(httpSession.getAttribute("delegatorName")).thenReturn("DelegatorName");

        final HashMap<String, Object> context = new HashMap<>();
        context.put("param1", "ThisIsParam1");
        context.put("session", httpSession);

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.textField(context, textField, true);
        assertThat(renderableFtl, MacroCallMatcher.hasName("renderTextField"));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(
                MacroCallParameterMatcher.hasNameAndStringValue("ajaxUrl",
                        "areaId1,http://host.domain/target1,param1=ThisIsParam1&param2=ThisIsParam2,"
                                + "areaId2,http://host.domain/target2,")));
    }

    @Test
    public void textFieldDefaultTypeIsText() {
        final ModelFormField.TextField textField = mock(ModelFormField.TextField.class);
        when(textField.getModelFormField()).thenReturn(modelFormField);
        when(httpSession.getAttribute("delegatorName")).thenReturn("DelegatorName");

        final RenderableFtl renderableFtl =
                renderableFtlFormElementsBuilder.textField(Map.of("session", httpSession), textField, true);
        assertThat(renderableFtl, MacroCallMatcher.hasName("renderTextField"));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("type", "text")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("step", "")));
    }

    @Test
    public void textFieldTypeNumber() {
        final ModelFormField.TextField textField = mock(ModelFormField.TextField.class);
        when(textField.getModelFormField()).thenReturn(modelFormField);
        when(textField.getType()).thenReturn("number");
        when(textField.getMin()).thenReturn("2");
        when(textField.getMax()).thenReturn("8");
        when(httpSession.getAttribute("delegatorName")).thenReturn("DelegatorName");

        final RenderableFtl renderableFtl =
                renderableFtlFormElementsBuilder.textField(Map.of("session", httpSession), textField, true);
        assertThat(renderableFtl, MacroCallMatcher.hasName("renderTextField"));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("type", "number")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("step", "any")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("min", "2")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("max", "8")));
    }

    @Test
    public void textFieldTypeNumberCustomStep() {
        final ModelFormField.TextField textField = mock(ModelFormField.TextField.class);
        when(textField.getModelFormField()).thenReturn(modelFormField);
        when(textField.getType()).thenReturn("number");
        when(textField.getStep()).thenReturn("0.1");
        when(httpSession.getAttribute("delegatorName")).thenReturn("DelegatorName");

        final RenderableFtl renderableFtl =
                renderableFtlFormElementsBuilder.textField(Map.of("session", httpSession), textField, true);
        assertThat(renderableFtl, MacroCallMatcher.hasName("renderTextField"));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("type", "number")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("step", "0.1")));
    }

    @Test
    public void textFieldTypeEmail() {
        final ModelFormField.TextField textField = mock(ModelFormField.TextField.class);
        when(textField.getModelFormField()).thenReturn(modelFormField);
        when(textField.getType()).thenReturn("email");
        when(httpSession.getAttribute("delegatorName")).thenReturn("DelegatorName");

        final RenderableFtl renderableFtl =
                renderableFtlFormElementsBuilder.textField(Map.of("session", httpSession), textField, true);
        assertThat(renderableFtl, MacroCallMatcher.hasName("renderTextField"));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("type", "email")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("step", "")));
    }

    @Test
    public void textFieldTypeUrl() {
        final ModelFormField.TextField textField = mock(ModelFormField.TextField.class);
        when(textField.getModelFormField()).thenReturn(modelFormField);
        when(textField.getType()).thenReturn("url");
        when(httpSession.getAttribute("delegatorName")).thenReturn("DelegatorName");

        final RenderableFtl renderableFtl =
                renderableFtlFormElementsBuilder.textField(Map.of("session", httpSession), textField, true);
        assertThat(renderableFtl, MacroCallMatcher.hasName("renderTextField"));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("type", "url")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("step", "")));
    }

    @Test
    public void textFieldTypeTel() {
        final ModelFormField.TextField textField = mock(ModelFormField.TextField.class);
        when(textField.getModelFormField()).thenReturn(modelFormField);
        when(textField.getType()).thenReturn("tel");
        when(httpSession.getAttribute("delegatorName")).thenReturn("DelegatorName");

        final RenderableFtl renderableFtl =
                renderableFtlFormElementsBuilder.textField(Map.of("session", httpSession), textField, true);
        assertThat(renderableFtl, MacroCallMatcher.hasName("renderTextField"));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("type", "tel")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("step", "")));
    }

    @Test
    public void textFieldTypeRange() {
        final ModelFormField.TextField textField = mock(ModelFormField.TextField.class);
        when(textField.getModelFormField()).thenReturn(modelFormField);
        when(textField.getType()).thenReturn("range");
        when(textField.getMin()).thenReturn("15");
        when(textField.getMax()).thenReturn("30");
        when(textField.getStep()).thenReturn("3");
        when(httpSession.getAttribute("delegatorName")).thenReturn("DelegatorName");

        final RenderableFtl renderableFtl =
                renderableFtlFormElementsBuilder.textField(Map.of("session", httpSession), textField, true);
        assertThat(renderableFtl, MacroCallMatcher.hasName("renderTextField"));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("type", "range")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("min", "15")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("max", "30")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("step", "3")));
    }

    @Test
    public void textFieldNotRequired() {
        final ModelFormField.TextField textField = mock(ModelFormField.TextField.class);
        when(textField.getModelFormField()).thenReturn(modelFormField);
        when(modelFormField.getRequiredField()).thenReturn(false);
        when(httpSession.getAttribute("delegatorName")).thenReturn("DelegatorName");

        final RenderableFtl renderableFtl =
                renderableFtlFormElementsBuilder.textField(Map.of("session", httpSession), textField, true);
        assertThat(renderableFtl, MacroCallMatcher.hasName("renderTextField"));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndBooleanValue("required", false)));
    }

    @Test
    public void textFieldRequiredWithoutRequiredStyle() {
        final ModelFormField.TextField textField = mock(ModelFormField.TextField.class);
        when(textField.getModelFormField()).thenReturn(modelFormField);
        when(modelFormField.getModelForm().getType()).thenReturn("single");
        when(modelFormField.getRequiredField()).thenReturn(true);
        when(httpSession.getAttribute("delegatorName")).thenReturn("DelegatorName");

        final RenderableFtl renderableFtl =
                renderableFtlFormElementsBuilder.textField(Map.of("session", httpSession), textField, true);
        assertThat(renderableFtl, MacroCallMatcher.hasName("renderTextField"));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndBooleanValue("required", true)));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("className", "required")));
    }

    @Test
    public void textFieldRequiredWithRequiredStyle() {
        final ModelFormField.TextField textField = mock(ModelFormField.TextField.class);
        when(textField.getModelFormField()).thenReturn(modelFormField);
        when(modelFormField.getModelForm().getType()).thenReturn("single");
        when(modelFormField.getRequiredField()).thenReturn(true);
        when(modelFormField.getRequiredFieldStyle()).thenReturn("someCssClass");
        when(httpSession.getAttribute("delegatorName")).thenReturn("DelegatorName");

        final RenderableFtl renderableFtl =
                renderableFtlFormElementsBuilder.textField(Map.of("session", httpSession), textField, true);
        assertThat(renderableFtl, MacroCallMatcher.hasName("renderTextField"));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndBooleanValue("required", true)));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(
                MacroCallParameterMatcher.hasNameAndStringValue("className", "required someCssClass")));
    }

    @Test
    public void textFieldPattern() {
        final ModelFormField.TextField textField = mock(ModelFormField.TextField.class);
        when(textField.getModelFormField()).thenReturn(modelFormField);
        when(textField.getPattern()).thenReturn("\\d{4,4}");
        when(httpSession.getAttribute("delegatorName")).thenReturn("DelegatorName");

        final RenderableFtl renderableFtl =
                renderableFtlFormElementsBuilder.textField(Map.of("session", httpSession), textField, true);
        assertThat(renderableFtl, MacroCallMatcher.hasName("renderTextField"));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("pattern", "\\d{4,4}")));
    }

    @Test
    public void textFieldPatternOnInvalidType() {
        final ModelFormField.TextField textField = mock(ModelFormField.TextField.class);
        when(textField.getModelFormField()).thenReturn(modelFormField);
        when(textField.getPattern()).thenReturn("\\d{4,4}");
        when(textField.getType()).thenReturn("number");
        when(httpSession.getAttribute("delegatorName")).thenReturn("DelegatorName");

        final RenderableFtl renderableFtl =
                renderableFtlFormElementsBuilder.textField(Map.of("session", httpSession), textField, true);

        assertThat(renderableFtl, MacroCallMatcher.hasName("renderTextField"));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("pattern", "")));
    }

    @Test
    public void textareaFieldSetsIdValueLengthAndSize() {
        final int maxLength = 142;
        final int cols = 80;
        final int rows = 5;
        final ModelFormField.TextareaField textareaField = mock(ModelFormField.TextareaField.class);
        when(textareaField.getModelFormField()).thenReturn(modelFormField);
        when(modelFormField.getCurrentContainerId(notNull())).thenReturn("CurrentTextareaId");
        when(modelFormField.getEntry(notNull(), nullable(String.class))).thenReturn("TEXTAREAVALUE");
        when(textareaField.getMaxlength()).thenReturn(maxLength);
        when(textareaField.getCols()).thenReturn(cols);
        when(textareaField.getRows()).thenReturn(rows);

        final HashMap<String, Object> context = new HashMap<>();

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.textArea(context, textareaField);
        assertThat(renderableFtl, MacroCallMatcher.hasNameAndParameters("renderTextareaField",
                MacroCallParameterMatcher.hasNameAndStringValue("id", "CurrentTextareaId"),
                MacroCallParameterMatcher.hasNameAndStringValue("value", "TEXTAREAVALUE"),
                MacroCallParameterMatcher.hasNameAndIntegerValue("cols", cols),
                MacroCallParameterMatcher.hasNameAndIntegerValue("rows", rows),
                MacroCallParameterMatcher.hasNameAndIntegerValue("maxlength", maxLength)));
    }

    @Test
    public void textareaFieldSetsDisabledParameters() {
        final ModelFormField.TextareaField textareaField = mock(ModelFormField.TextareaField.class);
        when(textareaField.getModelFormField()).thenReturn(modelFormField);
        when(modelFormField.getDisabled(notNull())).thenReturn(true);

        final HashMap<String, Object> context = new HashMap<>();

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.textArea(context, textareaField);
        assertThat(renderableFtl, MacroCallMatcher.hasNameAndParameters("renderTextareaField",
                MacroCallParameterMatcher.hasNameAndBooleanValue("disabled", true)));
    }

    @Test
    public void textareaFieldVisualEditorEnabledNoButtons() {
        final ModelFormField.TextareaField textareaField = mock(ModelFormField.TextareaField.class);
        when(textareaField.getModelFormField()).thenReturn(modelFormField);
        when(modelFormField.getDisabled(notNull())).thenReturn(true);
        when(textareaField.getVisualEditorEnable()).thenReturn(true);
        when(textareaField.getVisualEditorButtons(notNull())).thenReturn("");

        final HashMap<String, Object> context = new HashMap<>();

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.textArea(context, textareaField);
        assertThat(renderableFtl, MacroCallMatcher.hasNameAndParameters("renderTextareaField",
                MacroCallParameterMatcher.hasNameAndBooleanValue("visualEditorEnable", true)));
    }

    @Test
    public void textareaFieldVisualEditorEnabledButtons() {
        String editorConfiguration = "[['formatting'],['strong','em','del'],['link'],['unorderedList','orderedList'],"
                + "['horizontalRule'],['removeformat'],['indent','outdent'],['fullscreen']]";

        final ModelFormField.TextareaField textareaField = mock(ModelFormField.TextareaField.class);
        when(textareaField.getModelFormField()).thenReturn(modelFormField);
        when(modelFormField.getDisabled(notNull())).thenReturn(true);
        when(textareaField.getVisualEditorEnable()).thenReturn(true);
        when(textareaField.getVisualEditorButtons(notNull())).thenReturn(editorConfiguration);

        final HashMap<String, Object> context = new HashMap<>();

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.textArea(context, textareaField);
        assertThat(renderableFtl, MacroCallMatcher.hasNameAndParameters("renderTextareaField",
                MacroCallParameterMatcher.hasNameAndBooleanValue("visualEditorEnable", true)));
        assertThat(renderableFtl, MacroCallMatcher.hasNameAndParameters(
                "renderTextareaField",
                MacroCallParameterMatcher.hasNameAndStringValue("buttons", editorConfiguration)));
    }

    @Test
    public void fieldGroupOpenRendersCollapsibleAreaId() {
        final ModelForm.FieldGroup fieldGroup = mock(ModelForm.FieldGroup.class);
        when(fieldGroup.getStyle()).thenReturn("GROUPSTYLE");
        when(fieldGroup.getTitle()).thenReturn("TITLE${title}");
        when(fieldGroup.getId()).thenReturn("FIELDGROUPID");
        when(fieldGroup.initiallyCollapsed()).thenReturn(true);

        final Map<String, Object> context = new HashMap<>();
        context.put("title", "ABC");

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.fieldGroupOpen(context, fieldGroup);
        assertThat(renderableFtl, MacroCallMatcher.hasName("renderFieldGroupOpen"));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("title", "TITLEABC")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("style", "GROUPSTYLE")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue(
                "collapsibleAreaId", "FIELDGROUPID_body")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndBooleanValue("collapsed", true)));
    }

    @Test
    public void fieldGroupCloseRendersStyle() {
        final ModelForm.FieldGroup fieldGroup = mock(ModelForm.FieldGroup.class);
        when(fieldGroup.getStyle()).thenReturn("GROUPSTYLE");

        final Map<String, Object> context = new HashMap<>();
        context.put("title", "ABC");

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.fieldGroupClose(context, fieldGroup);
        assertThat(renderableFtl, MacroCallMatcher.hasName("renderFieldGroupClose"));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("style", "GROUPSTYLE")));
    }

    @Test
    public void dateRangePickerField() {
        final ModelFormField.DateRangePickerField dateRangePickerField = mock(ModelFormField.DateRangePickerField.class);
        when(dateRangePickerField.getModelFormField()).thenReturn(modelFormField);
        when(dateRangePickerField.getAlwaysShowCalendars()).thenReturn(true);
        when(dateRangePickerField.getApplyButtonClasses(notNull())).thenReturn("abc");
        when(dateRangePickerField.getApplyLabel(notNull())).thenReturn("al");
        when(dateRangePickerField.getAutoApply()).thenReturn(false);
        when(dateRangePickerField.getButtonClasses(notNull())).thenReturn("bc");
        when(dateRangePickerField.getCancelButtonClasses(notNull())).thenReturn("cbc");
        when(dateRangePickerField.getCancelLabel(notNull())).thenReturn("cl");
        when(dateRangePickerField.getClearTitle(notNull())).thenReturn("ct");
        when(dateRangePickerField.getDrops()).thenReturn("down");
        when(dateRangePickerField.getLinkedCalendars()).thenReturn(false);
        when(dateRangePickerField.getMaxSpan()).thenReturn(9);
        when(dateRangePickerField.getMaxYear()).thenReturn(2100);
        when(dateRangePickerField.getMinYear()).thenReturn(1900);
        when(dateRangePickerField.getOpens()).thenReturn("right");
        when(dateRangePickerField.getShowDropdowns()).thenReturn(true);
        when(dateRangePickerField.getShowIsoWeekNumbers()).thenReturn(true);
        when(dateRangePickerField.getShowRanges()).thenReturn(true);
        when(dateRangePickerField.getShowWeekNumbers()).thenReturn(true);
        when(dateRangePickerField.getSingleDatePicker()).thenReturn(false);
        when(dateRangePickerField.getTimePicker()).thenReturn(true);
        when(dateRangePickerField.getTimePicker24Hour()).thenReturn(true);
        when(dateRangePickerField.getTimePickerIncrement()).thenReturn(5);
        when(dateRangePickerField.getTimePickerSeconds()).thenReturn(true);

        final Map<String, Object> context = new HashMap<>();
        @SuppressWarnings("deprecation") Locale locale = new Locale("fr");
        context.put("locale", locale);

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.dateRangePicker(context, dateRangePickerField);
        assertThat(renderableFtl, MacroCallMatcher.hasName("renderDateRangePicker"));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndBooleanValue("alwaysShowCalendars", true)));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("applyButtonClasses", "abc")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("applyLabel", "al")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndBooleanValue("autoApply", false)));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("buttonClasses", "bc")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("cancelButtonClasses", "cbc")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("cancelLabel", "cl")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("clearTitle", "ct")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("drops", "down")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndBooleanValue("linkedCalendars", false)));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("maxSpan", "9")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("maxYear", "2100")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("minYear", "1900")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("opens", "right")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndBooleanValue("showDropdowns", true)));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndBooleanValue("showIsoWeekNumbers", true)));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndBooleanValue("showRanges", true)));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndBooleanValue("showWeekNumbers", true)));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndBooleanValue("singleDatePicker", false)));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndBooleanValue("timePicker", true)));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndBooleanValue("timePicker24Hour", true)));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("timePickerIncrement", "5")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndBooleanValue("timePickerSeconds", true)));
    }

    private static List<ModelFormField.OptionValue> threeOptions() {
        final List<ModelFormField.OptionValue> optionValues = new ArrayList<>();
        optionValues.add(new ModelFormField.OptionValue("KEY1", "DESC1"));
        optionValues.add(new ModelFormField.OptionValue("KEY2", "DESC2"));
        optionValues.add(new ModelFormField.OptionValue("KEY3", "DESC3"));
        return optionValues;
    }

    private static Map<String, String> checkItem(final String key, final String description, final boolean checked) {
        return Map.of("value", key, "description", description, "checked", Boolean.toString(checked));
    }

    @Test
    public void checkFieldChecksTheCurrentValue() {
        final ModelFormField.CheckField checkField = mock(ModelFormField.CheckField.class);
        when(checkField.getModelFormField()).thenReturn(modelFormField);
        when(checkField.isAllChecked(notNull())).thenReturn(false);
        when(checkField.getAllOptionValues(notNull(), any())).thenReturn(threeOptions());
        when(modelFormField.getEntry(notNull())).thenReturn("KEY2");

        final RenderableFtlMacroCall macroCall = (RenderableFtlMacroCall) renderableFtlFormElementsBuilder.checkField(new HashMap<>(), checkField);
        assertThat(macroCall, MacroCallMatcher.hasNameAndParameters("renderCheckField",
                MacroCallParameterMatcher.hasNameAndStringValue("currentValue", "KEY2")));
        assertThat(macroCall.getParameters().get("items"), equalTo(List.of(
                checkItem("KEY1", "DESC1", false), checkItem("KEY2", "DESC2", true), checkItem("KEY3", "DESC3", false))));
    }

    @Test
    public void checkFieldChecksTheValuesOfTheContextWhenThereIsNoCurrentValue() {
        final ModelFormField.CheckField checkField = mock(ModelFormField.CheckField.class);
        when(checkField.getModelFormField()).thenReturn(modelFormField);
        when(checkField.isAllChecked(notNull())).thenReturn(false);
        when(checkField.getAllOptionValues(notNull(), any())).thenReturn(threeOptions());
        when(modelFormField.getEntry(notNull())).thenReturn("");
        when(modelFormField.getAttributeName()).thenReturn("FieldName");

        final Map<String, Object> context = new HashMap<>();
        context.put("FieldName", List.of("KEY1", "KEY3"));

        final RenderableFtlMacroCall macroCall = (RenderableFtlMacroCall) renderableFtlFormElementsBuilder.checkField(context, checkField);
        assertThat(macroCall.getParameters().get("items"), equalTo(List.of(
                checkItem("KEY1", "DESC1", true), checkItem("KEY2", "DESC2", false), checkItem("KEY3", "DESC3", true))));
    }

    @Test
    public void checkFieldPassesKeysAndDescriptionsAsTheyAre() {
        final ModelFormField.CheckField checkField = mock(ModelFormField.CheckField.class);
        when(checkField.getModelFormField()).thenReturn(modelFormField);
        when(checkField.isAllChecked(notNull())).thenReturn(false);
        final List<ModelFormField.OptionValue> optionValues = new ArrayList<>();
        optionValues.add(new ModelFormField.OptionValue("it's ${1+1}", "say \"hi\" \\ #{2}"));
        when(checkField.getAllOptionValues(notNull(), any())).thenReturn(optionValues);
        when(modelFormField.getEntry(notNull())).thenReturn("");

        final RenderableFtlMacroCall macroCall = (RenderableFtlMacroCall) renderableFtlFormElementsBuilder.checkField(new HashMap<>(), checkField);
        assertThat(macroCall.getParameters().get("items"), equalTo(List.of(
                checkItem("it's ${1+1}", "say \"hi\" \\ #{2}", false))));
    }

    @Test
    public void radioFieldSetsItemsCurrentValueAndNoCurrentSelectedKey() {
        final ModelFormField.RadioField radioField = mock(ModelFormField.RadioField.class);
        when(radioField.getModelFormField()).thenReturn(modelFormField);
        when(radioField.getAllOptionValues(notNull(), any())).thenReturn(threeOptions());
        when(radioField.getNoCurrentSelectedKey(notNull())).thenReturn("NOKEY");
        when(modelFormField.getEntry(notNull())).thenReturn("KEY2");

        final RenderableFtlMacroCall macroCall = (RenderableFtlMacroCall) renderableFtlFormElementsBuilder.radioField(new HashMap<>(), radioField);
        assertThat(macroCall, MacroCallMatcher.hasNameAndParameters("renderRadioField",
                MacroCallParameterMatcher.hasNameAndStringValue("currentValue", "KEY2"),
                MacroCallParameterMatcher.hasNameAndStringValue("noCurrentSelectedKey", "NOKEY")));
        assertThat(macroCall.getParameters().get("items"), equalTo(List.of(
                Map.of("key", "KEY1", "description", "DESC1"),
                Map.of("key", "KEY2", "description", "DESC2"),
                Map.of("key", "KEY3", "description", "DESC3"))));
    }

    @Test
    public void submitFieldSetsTitleAndDoesNotUseAjaxWithoutUpdateAreas() {
        final ModelFormField.SubmitField submitField = mock(ModelFormField.SubmitField.class);
        when(submitField.getModelFormField()).thenReturn(modelFormField);
        when(modelFormField.getTitle(notNull())).thenReturn("BUTTONTITLE");
        when(modelFormField.getModelForm().getOnSubmitUpdateAreas()).thenReturn(new ArrayList<>());
        when(modelFormField.getOnClickUpdateAreas()).thenReturn(new ArrayList<>());

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.submitField(new HashMap<>(), submitField, true);
        assertThat(renderableFtl, MacroCallMatcher.hasNameAndParameters("renderSubmitField",
                MacroCallParameterMatcher.hasNameAndStringValue("title", "BUTTONTITLE"),
                MacroCallParameterMatcher.hasNameAndStringValue("containerId", ""),
                MacroCallParameterMatcher.hasNameAndStringValue("ajaxUrl", ""),
                MacroCallParameterMatcher.hasNameAndStringValue("closeOnSubmit", "true")));
    }

    @Test
    public void resetFieldSetsTitleAndName() {
        final ModelFormField.ResetField resetField = mock(ModelFormField.ResetField.class);
        when(resetField.getModelFormField()).thenReturn(modelFormField);
        when(modelFormField.getTitle(notNull())).thenReturn("BUTTONTITLE");
        when(modelFormField.getParameterName(notNull())).thenReturn("PARAMNAME");

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.resetField(new HashMap<>(), resetField);
        assertThat(renderableFtl, MacroCallMatcher.hasNameAndParameters("renderResetField",
                MacroCallParameterMatcher.hasNameAndStringValue("title", "BUTTONTITLE"),
                MacroCallParameterMatcher.hasNameAndStringValue("name", "PARAMNAME")));
    }

    @Test
    public void hiddenFieldSetsTheValue() {
        when(modelFormField.getParameterName(notNull())).thenReturn("PARAMNAME");

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.hiddenField(new HashMap<>(), modelFormField, "HIDDENVALUE");
        assertThat(renderableFtl, MacroCallMatcher.hasNameAndParameters("renderHiddenField",
                MacroCallParameterMatcher.hasNameAndStringValue("name", "PARAMNAME"),
                MacroCallParameterMatcher.hasNameAndStringValue("value", "HIDDENVALUE")));
    }

    @Test
    public void hiddenFieldRemovesTheBackslashOfTheEscapedQuotes() {
        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.hiddenField(new HashMap<>(), modelFormField, "say \\\"hi\\\"");
        assertThat(renderableFtl, MacroCallMatcher.hasNameAndParameters("renderHiddenField",
                MacroCallParameterMatcher.hasNameAndStringValue("value", "say \"hi\"")));
    }

    @Test
    public void formatEmptySpaceHasNoParameters() {
        final RenderableFtlMacroCall macroCall = (RenderableFtlMacroCall) renderableFtlFormElementsBuilder.formatEmptySpace();
        assertThat(macroCall.getName(), equalTo("renderFormatEmptySpace"));
        assertThat(macroCall.getParameters().isEmpty(), equalTo(true));
    }

    @Test
    public void hyperlinkTitleShowsTheSelectAllOnlyWhenAsked() {
        final RenderableFtlMacroCall withSelectAll = (RenderableFtlMacroCall) renderableFtlFormElementsBuilder.hyperlinkTitle("FORM", "TITLE", true);
        assertThat(withSelectAll, MacroCallMatcher.hasNameAndParameters("renderHyperlinkTitle",
                MacroCallParameterMatcher.hasNameAndStringValue("name", "FORM"),
                MacroCallParameterMatcher.hasNameAndStringValue("title", "TITLE"),
                MacroCallParameterMatcher.hasNameAndStringValue("showSelectAll", "Y")));

        final RenderableFtlMacroCall withoutSelectAll =
                (RenderableFtlMacroCall) renderableFtlFormElementsBuilder.hyperlinkTitle("FORM", "TITLE", false);
        assertThat(withoutSelectAll.getParameters(), not(hasKey("showSelectAll")));
    }

    @Test
    public void fieldTitleSetsIdAndForOnlyWithAnId() {
        final RenderableFtlMacroCall withId = (RenderableFtlMacroCall) renderableFtlFormElementsBuilder.fieldTitle("STYLE", "TITLE", "FIELDID", null);
        assertThat(withId, MacroCallMatcher.hasNameAndParameters("renderFieldTitle",
                MacroCallParameterMatcher.hasNameAndStringValue("style", "STYLE"),
                MacroCallParameterMatcher.hasNameAndStringValue("title", "TITLE"),
                MacroCallParameterMatcher.hasNameAndStringValue("id", "FIELDID_title"),
                MacroCallParameterMatcher.hasNameAndStringValue("for", "FIELDID")));
        assertThat(withId.getParameters(), not(hasKey("fieldHelpText")));

        final RenderableFtlMacroCall withoutId = (RenderableFtlMacroCall) renderableFtlFormElementsBuilder.fieldTitle("STYLE", "TITLE", "", "HELP");
        assertThat(withoutId.getParameters(), not(hasKey("id")));
        assertThat(withoutId.getParameters(), not(hasKey("for")));
        assertThat(withoutId, MacroCallMatcher.hasNameAndParameters("renderFieldTitle",
                MacroCallParameterMatcher.hasNameAndStringValue("fieldHelpText", "HELP")));
    }

    @Test
    public void sortFieldSetsTheLinkAndOmitsAnEmptyTooltip() {
        final RenderableFtlMacroCall macroCall =
                (RenderableFtlMacroCall) renderableFtlFormElementsBuilder.sortField("STYLE", "TITLE", "/target?x=1", true, "");
        assertThat(macroCall, MacroCallMatcher.hasNameAndParameters("renderSortField",
                MacroCallParameterMatcher.hasNameAndStringValue("style", "STYLE"),
                MacroCallParameterMatcher.hasNameAndStringValue("title", "TITLE"),
                MacroCallParameterMatcher.hasNameAndStringValue("linkUrl", "/target?x=1"),
                MacroCallParameterMatcher.hasNameAndBooleanValue("ajaxEnabled", true)));
        assertThat(macroCall.getParameters(), not(hasKey("tooltip")));

        final RenderableFtlMacroCall withTooltip =
                (RenderableFtlMacroCall) renderableFtlFormElementsBuilder.sortField("STYLE", "TITLE", "/target", false, "TIP");
        assertThat(withTooltip, MacroCallMatcher.hasNameAndParameters("renderSortField",
                MacroCallParameterMatcher.hasNameAndStringValue("tooltip", "TIP")));
    }

    private static RenderableFtlMacroCall macroCall(final RenderableFtl renderableFtl) {
        return (RenderableFtlMacroCall) renderableFtl;
    }

    private ModelFormField columnField(final String name, final int fieldType, final String titleAreaStyle) {
        final ModelFormField columnField = mock(ModelFormField.class, RETURNS_DEEP_STUBS);
        when(columnField.getName()).thenReturn(name);
        when(columnField.getFieldInfo().getFieldType()).thenReturn(fieldType);
        when(columnField.getTitleAreaStyle()).thenReturn(titleAreaStyle);
        return columnField;
    }

    @Test
    public void formOpenSetsTheFormTypeAndTheViewParameters() {
        final ModelForm modelForm = mock(ModelForm.class);
        when(modelForm.getType()).thenReturn("single");
        when(modelForm.getUseRowSubmit()).thenReturn(true);

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.formOpen(new HashMap<>(), modelForm);
        assertThat(renderableFtl, MacroCallMatcher.hasNameAndParameters("renderFormOpen",
                MacroCallParameterMatcher.hasNameAndStringValue("formType", "single"),
                MacroCallParameterMatcher.hasNameAndStringValue("autocomplete", "off"),
                MacroCallParameterMatcher.hasNameAndStringValue("hasRequiredField", ""),
                MacroCallParameterMatcher.hasNameAndBooleanValue("useRowSubmit", true),
                MacroCallParameterMatcher.hasName("viewIndex"),
                MacroCallParameterMatcher.hasName("viewSize"),
                MacroCallParameterMatcher.hasName("csrfNameValue")));
    }

    @Test
    public void formOpenFlagsTheFormWithARequiredField() {
        final ModelForm modelForm = mock(ModelForm.class);
        when(modelForm.getClientAutocompleteFields()).thenReturn(true);
        final ModelFormField requiredField = mock(ModelFormField.class);
        when(requiredField.getRequiredField()).thenReturn(true);
        when(modelForm.getFieldList()).thenReturn(List.of(requiredField));

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.formOpen(new HashMap<>(), modelForm);
        assertThat(renderableFtl, MacroCallMatcher.hasNameAndParameters("renderFormOpen",
                MacroCallParameterMatcher.hasNameAndStringValue("autocomplete", ""),
                MacroCallParameterMatcher.hasNameAndStringValue("hasRequiredField", "Y")));
    }

    @Test
    public void closingMacrosHaveNoParameters() {
        assertThat(macroCall(renderableFtlFormElementsBuilder.formClose()).getName(), equalTo("renderFormClose"));
        assertThat(macroCall(renderableFtlFormElementsBuilder.multiFormClose()).getName(), equalTo("renderMultiFormClose"));
        assertThat(macroCall(renderableFtlFormElementsBuilder.formatHeaderOpen()).getName(), equalTo("renderFormatHeaderOpen"));
        assertThat(macroCall(renderableFtlFormElementsBuilder.formatHeaderClose()).getName(), equalTo("renderFormatHeaderClose"));
        assertThat(macroCall(renderableFtlFormElementsBuilder.formatHeaderRowClose()).getName(), equalTo("renderFormatHeaderRowClose"));
        assertThat(macroCall(renderableFtlFormElementsBuilder.formatHeaderRowCellClose()).getName(),
                equalTo("renderFormatHeaderRowCellClose"));
        assertThat(macroCall(renderableFtlFormElementsBuilder.formatHeaderRowFormCellClose()).getName(),
                equalTo("renderFormatHeaderRowFormCellClose"));
        assertThat(macroCall(renderableFtlFormElementsBuilder.formatItemRowFormCellClose()).getName(),
                equalTo("renderFormatItemRowFormCellClose"));
        assertThat(macroCall(renderableFtlFormElementsBuilder.formatFieldRowOpen()).getName(), equalTo("renderFormatFieldRowOpen"));
        assertThat(macroCall(renderableFtlFormElementsBuilder.formatFieldRowClose()).getName(), equalTo("renderFormatFieldRowClose"));
        assertThat(macroCall(renderableFtlFormElementsBuilder.formatFieldRowTitleCellClose()).getName(),
                equalTo("renderFormatFieldRowTitleCellClose"));
        assertThat(macroCall(renderableFtlFormElementsBuilder.formatFieldRowWidgetCellClose()).getName(),
                equalTo("renderFormatFieldRowWidgetCellClose"));
        assertThat(macroCall(renderableFtlFormElementsBuilder.formatHeaderRowClose()).getParameters().isEmpty(), equalTo(true));
    }

    @Test
    public void postMultiFormContentIsNothingWhenThereIsNone() {
        assertThat(renderableFtlFormElementsBuilder.postMultiFormContent(new HashMap<>()), equalTo(RenderableFtlNoop.INSTANCE));

        final Map<String, Object> context = new HashMap<>();
        context.put("wholeFormContext", new HashMap<String, Object>());
        assertThat(renderableFtlFormElementsBuilder.postMultiFormContent(context), equalTo(RenderableFtlNoop.INSTANCE));
    }

    @Test
    public void postMultiFormContentIsWhatTheHiddenFormLinksStored() {
        final RenderableFtlMacroCall stored = RenderableFtlMacroCall.builder().name("stored").build();
        final Map<String, Object> wholeFormContext = new HashMap<>();
        wholeFormContext.put("postMultiFormRenderableFtl", stored);
        final Map<String, Object> context = new HashMap<>();
        context.put("wholeFormContext", wholeFormContext);

        assertThat(renderableFtlFormElementsBuilder.postMultiFormContent(context), equalTo(stored));
    }

    @Test
    public void formatListWrapperOpenCollectsOneStylePerVisibleColumn() {
        final ModelForm modelForm = mock(ModelForm.class);
        when(modelForm.getName()).thenReturn("FORMNAME");
        when(modelForm.getDefaultTableStyle()).thenReturn("STYLE${styleParam}");
        final List<ModelFormField> fields = List.of(
                columnField("first", FieldInfo.DISPLAY, "FIRSTSTYLE"),
                columnField("hidden", FieldInfo.HIDDEN, "HIDDENSTYLE"),
                columnField("second", FieldInfo.TEXT, "it's a style"),
                // a second field with the name of the first one only replaces the style
                columnField("first", FieldInfo.TEXT, "REPLACEDSTYLE"),
                columnField("third", FieldInfo.TEXT, null));
        when(modelForm.getFieldList()).thenReturn(fields);

        final RenderableFtlMacroCall macroCall = macroCall(
                renderableFtlFormElementsBuilder.formatListWrapperOpen(ImmutableMap.of("styleParam", "ABCD"), modelForm));
        assertThat(macroCall, MacroCallMatcher.hasNameAndParameters("renderFormatListWrapperOpen",
                MacroCallParameterMatcher.hasNameAndStringValue("formName", "FORMNAME"),
                MacroCallParameterMatcher.hasNameAndStringValue("style", "STYLEABCD")));
        assertThat(macroCall.getParameters().get("columnStyles"), equalTo(List.of("REPLACEDSTYLE", "it's a style", "")));
    }

    @Test
    public void formatListWrapperOpenWithoutFieldsHasNoColumnStyles() {
        final ModelForm modelForm = mock(ModelForm.class);

        final RenderableFtlMacroCall macroCall = macroCall(renderableFtlFormElementsBuilder.formatListWrapperOpen(new HashMap<>(), modelForm));
        assertThat(macroCall.getParameters().get("columnStyles"), equalTo(List.of()));
    }

    @Test
    public void emptyFormDataMessageSetsTheMessage() {
        final ModelForm modelForm = mock(ModelForm.class);
        when(modelForm.getEmptyFormDataMessage(notNull())).thenReturn("EMPTY ${notEvaluated}");

        assertThat(renderableFtlFormElementsBuilder.emptyFormDataMessage(new HashMap<>(), modelForm),
                MacroCallMatcher.hasNameAndParameters("renderEmptyFormDataMessage",
                        MacroCallParameterMatcher.hasNameAndStringValue("message", "EMPTY ${notEvaluated}")));
    }

    @Test
    public void formatListWrapperCloseSetsTheFormName() {
        final ModelForm modelForm = mock(ModelForm.class);
        when(modelForm.getName()).thenReturn("FORMNAME");

        assertThat(renderableFtlFormElementsBuilder.formatListWrapperClose(modelForm),
                MacroCallMatcher.hasNameAndParameters("renderFormatListWrapperClose",
                        MacroCallParameterMatcher.hasNameAndStringValue("formName", "FORMNAME")));
    }

    @Test
    public void formatHeaderRowOpenExpandsTheStyle() {
        final ModelForm modelForm = mock(ModelForm.class);
        when(modelForm.getHeaderRowStyle()).thenReturn("HEADER${styleParam}");

        assertThat(renderableFtlFormElementsBuilder.formatHeaderRowOpen(ImmutableMap.of("styleParam", "ABCD"), modelForm),
                MacroCallMatcher.hasNameAndParameters("renderFormatHeaderRowOpen",
                        MacroCallParameterMatcher.hasNameAndStringValue("style", "HEADERABCD")));
    }

    @Test
    public void formatHeaderCellsSetStyleAndPositionSpan() {
        final ModelFormField localModelFormField = mock(ModelFormField.class);
        when(localModelFormField.getTitleAreaStyle()).thenReturn("TITLEAREASTYLE");
        when(localModelFormField.getTitleStyle()).thenReturn("TITLESTYLE");
        final ModelForm modelForm = mock(ModelForm.class);
        when(modelForm.getFormTitleAreaStyle()).thenReturn("FORMTITLEAREASTYLE");

        assertThat(renderableFtlFormElementsBuilder.formatHeaderRowCellOpen(localModelFormField, 3),
                MacroCallMatcher.hasNameAndParameters("renderFormatHeaderRowCellOpen",
                        MacroCallParameterMatcher.hasNameAndStringValue("style", "TITLEAREASTYLE"),
                        MacroCallParameterMatcher.hasNameAndIntegerValue("positionSpan", 3)));
        assertThat(renderableFtlFormElementsBuilder.formatHeaderRowFormCellOpen(modelForm),
                MacroCallMatcher.hasNameAndParameters("renderFormatHeaderRowFormCellOpen",
                        MacroCallParameterMatcher.hasNameAndStringValue("style", "FORMTITLEAREASTYLE")));
        assertThat(renderableFtlFormElementsBuilder.formatHeaderRowFormCellTitleSeparator(localModelFormField, true),
                MacroCallMatcher.hasNameAndParameters("renderFormatHeaderRowFormCellTitleSeparator",
                        MacroCallParameterMatcher.hasNameAndStringValue("style", "TITLESTYLE"),
                        MacroCallParameterMatcher.hasNameAndBooleanValue("isLast", true)));
    }

    @Test
    public void formatItemRowOpenUsesTheEvenRowStyleForAnEvenIndex() {
        final ModelForm modelForm = mock(ModelForm.class);
        when(modelForm.getName()).thenReturn("FORMNAME");
        when(modelForm.getEvenRowStyle()).thenReturn("EVENSTYLE");
        when(modelForm.getOddRowStyle()).thenReturn("ODDSTYLE");

        assertThat(renderableFtlFormElementsBuilder.formatItemRowOpen(ImmutableMap.of("itemIndex", 2), modelForm),
                MacroCallMatcher.hasNameAndParameters("renderFormatItemRowOpen",
                        MacroCallParameterMatcher.hasNameAndStringValue("formName", "FORMNAME"),
                        MacroCallParameterMatcher.hasNameAndIntegerValue("itemIndex", 2),
                        MacroCallParameterMatcher.hasNameAndStringValue("evenRowStyle", "EVENSTYLE"),
                        MacroCallParameterMatcher.hasNameAndStringValue("oddRowStyle", "")));
    }

    @Test
    public void formatItemRowOpenUsesTheExpandedOddRowStyleForAnOddIndex() {
        final ModelForm modelForm = mock(ModelForm.class);
        when(modelForm.getEvenRowStyle()).thenReturn("EVENSTYLE");
        when(modelForm.getOddRowStyle()).thenReturn("ODD${styleParam}");

        assertThat(renderableFtlFormElementsBuilder.formatItemRowOpen(ImmutableMap.of("itemIndex", 3, "styleParam", "ABCD"), modelForm),
                MacroCallMatcher.hasNameAndParameters("renderFormatItemRowOpen",
                        MacroCallParameterMatcher.hasNameAndStringValue("evenRowStyle", ""),
                        MacroCallParameterMatcher.hasNameAndStringValue("oddRowStyle", "ODDABCD")));
    }

    @Test
    public void formatItemRowOpenWithoutAnIndexLeavesTheIndexOut() {
        final ModelForm modelForm = mock(ModelForm.class);

        final RenderableFtlMacroCall macroCall = macroCall(renderableFtlFormElementsBuilder.formatItemRowOpen(new HashMap<>(), modelForm));
        assertThat(macroCall.getParameters(), not(hasKey("itemIndex")));
    }

    @Test
    public void formatItemRowCellsSetFieldNameStyleAndPositionSpan() {
        final ModelFormField localModelFormField = mock(ModelFormField.class);
        when(localModelFormField.getName()).thenReturn("FIELDNAME");
        when(localModelFormField.getWidgetAreaStyle()).thenReturn("AREASTYLE");
        final ModelForm modelForm = mock(ModelForm.class);
        when(modelForm.getName()).thenReturn("FORMNAME");
        when(modelForm.getFormTitleAreaStyle()).thenReturn("FORMTITLEAREASTYLE");

        assertThat(renderableFtlFormElementsBuilder.formatItemRowCellOpen(localModelFormField, 2),
                MacroCallMatcher.hasNameAndParameters("renderFormatItemRowCellOpen",
                        MacroCallParameterMatcher.hasNameAndStringValue("fieldName", "FIELDNAME"),
                        MacroCallParameterMatcher.hasNameAndIntegerValue("positionSpan", 2),
                        MacroCallParameterMatcher.hasNameAndStringValue("style", "AREASTYLE")));
        assertThat(renderableFtlFormElementsBuilder.formatItemRowCellClose(localModelFormField),
                MacroCallMatcher.hasNameAndParameters("renderFormatItemRowCellClose",
                        MacroCallParameterMatcher.hasNameAndStringValue("fieldName", "FIELDNAME")));
        assertThat(renderableFtlFormElementsBuilder.formatItemRowFormCellOpen(modelForm),
                MacroCallMatcher.hasNameAndParameters("renderFormatItemRowFormCellOpen",
                        MacroCallParameterMatcher.hasNameAndStringValue("style", "FORMTITLEAREASTYLE")));
        assertThat(renderableFtlFormElementsBuilder.formatItemRowClose(modelForm),
                MacroCallMatcher.hasNameAndParameters("renderFormatItemRowClose",
                        MacroCallParameterMatcher.hasNameAndStringValue("formName", "FORMNAME")));
    }

    @Test
    public void formatSingleWrapperSetsTheFormNameAndTheExpandedStyle() {
        final ModelForm modelForm = mock(ModelForm.class);
        when(modelForm.getName()).thenReturn("FORMNAME");
        when(modelForm.getDefaultTableStyle()).thenReturn("STYLE${styleParam}");

        assertThat(renderableFtlFormElementsBuilder.formatSingleWrapperOpen(ImmutableMap.of("styleParam", "ABCD"), modelForm),
                MacroCallMatcher.hasNameAndParameters("renderFormatSingleWrapperOpen",
                        MacroCallParameterMatcher.hasNameAndStringValue("formName", "FORMNAME"),
                        MacroCallParameterMatcher.hasNameAndStringValue("style", "STYLEABCD")));
        assertThat(renderableFtlFormElementsBuilder.formatSingleWrapperClose(modelForm),
                MacroCallMatcher.hasNameAndParameters("renderFormatSingleWrapperClose",
                        MacroCallParameterMatcher.hasNameAndStringValue("formName", "FORMNAME")));
    }

    @Test
    public void formatFieldRowCellsSetStyleAndPositionSpan() {
        final ModelFormField localModelFormField = mock(ModelFormField.class);
        when(localModelFormField.getTitleAreaStyle()).thenReturn("TITLEAREASTYLE");
        when(localModelFormField.getWidgetAreaStyle()).thenReturn("AREASTYLE");

        assertThat(renderableFtlFormElementsBuilder.formatFieldRowTitleCellOpen(localModelFormField),
                MacroCallMatcher.hasNameAndParameters("renderFormatFieldRowTitleCellOpen",
                        MacroCallParameterMatcher.hasNameAndStringValue("style", "TITLEAREASTYLE")));
        assertThat(renderableFtlFormElementsBuilder.formatFieldRowWidgetCellOpen(localModelFormField, 1),
                MacroCallMatcher.hasNameAndParameters("renderFormatFieldRowWidgetCellOpen",
                        MacroCallParameterMatcher.hasNameAndIntegerValue("positionSpan", 1),
                        MacroCallParameterMatcher.hasNameAndStringValue("style", "AREASTYLE")));
    }

    @Test
    public void boundaryCommentSetsTheTypeAndTheName() {
        assertThat(renderableFtlFormElementsBuilder.boundaryComment("Begin", "Form Widget - Form Element", "FORMNAME"),
                MacroCallMatcher.hasNameAndParameters("formatBoundaryComment",
                        MacroCallParameterMatcher.hasNameAndStringValue("boundaryType", "Begin"),
                        MacroCallParameterMatcher.hasNameAndStringValue("widgetType", "Form Widget - Form Element"),
                        MacroCallParameterMatcher.hasNameAndStringValue("widgetName", "FORMNAME")));
    }

    private RenderableFtlMacroCall hyperlink(final String description, final Map<String, String> parameterMap) {
        return macroCall(renderableFtlFormElementsBuilder.makeHyperlinkByType(null, null, null, "#", parameterMap, description, "", "",
                modelFormField, request, response, new HashMap<>()));
    }

    @Test
    public void hyperlinkUsesTheTitleOfTheRequestWhenTheDescriptionIsNotTruncated() {
        when(request.getAttribute("title")).thenReturn("TITLE");

        assertThat(hyperlink("DESCRIPTION", new HashMap<>()), MacroCallMatcher.hasNameAndParameters("makeHyperlinkString",
                MacroCallParameterMatcher.hasNameAndStringValue("description", "DESCRIPTION"),
                MacroCallParameterMatcher.hasNameAndStringValue("title", "TITLE")));
    }

    @Test
    public void hyperlinkUsesTheDescriptionAsTitleWhenItIsTruncated() {
        when(request.getAttribute("descriptionSize")).thenReturn(5);
        when(request.getAttribute("title")).thenReturn("TITLE");

        assertThat(hyperlink("DESCRIPTION", new HashMap<>()), MacroCallMatcher.hasNameAndParameters("makeHyperlinkString",
                MacroCallParameterMatcher.hasNameAndStringValue("description", "DESCR…"),
                MacroCallParameterMatcher.hasNameAndStringValue("title", "DESCRIPTION")));
    }

    @Test
    public void hyperlinkPassesTheDescriptionAsItIs() {
        final String description = "say \"hi\" \\ ${1+1} #{2} <#stop>";

        assertThat(hyperlink(description, new HashMap<>()), MacroCallMatcher.hasNameAndParameters("makeHyperlinkString",
                MacroCallParameterMatcher.hasNameAndStringValue("description", description)));
    }

    @Test
    public void layeredModalHyperlinkPassesTheParametersAsJsonText() {
        final Map<String, String> parameterMap = new HashMap<>();
        parameterMap.put("k1", "v1");
        parameterMap.put("k2", "v2");
        when(request.getAttribute("title")).thenReturn("TitleValue");
        when(request.getAttribute("text")).thenReturn("TextValue");
        when(request.getAttribute("id")).thenReturn("IdValue");
        when(request.getAttribute("uniqueItemName")).thenReturn("UniqueItemName");
        when(request.getAttribute("width")).thenReturn("650");
        when(request.getAttribute("height")).thenReturn("150");

        assertThat(hyperlink("DescriptionValue", parameterMap), MacroCallMatcher.hasNameAndParameters("makeHyperlinkString",
                MacroCallParameterMatcher.hasNameAndStringValue("description", "DescriptionValue"),
                MacroCallParameterMatcher.hasNameAndStringValue("title", "TitleValue"),
                MacroCallParameterMatcher.hasNameAndStringValue("text", "TextValue"),
                MacroCallParameterMatcher.hasNameAndStringValue("id", "IdValue"),
                MacroCallParameterMatcher.hasNameAndStringValue("uniqueItemName", "UniqueItemName"),
                MacroCallParameterMatcher.hasNameAndStringValue("width", "650"),
                MacroCallParameterMatcher.hasNameAndStringValue("height", "150"),
                MacroCallParameterMatcher.hasNameAndStringValue("targetParameters",
                        "{\"k1\":\"v1\",\"k2\":\"v2\",\"presentation\":\"layer\"}")));
    }

    @Test
    public void hyperlinkWithoutParametersHasNoTargetParameters() {
        assertThat(hyperlink("DESCRIPTION", new HashMap<>()), MacroCallMatcher.hasNameAndParameters("makeHyperlinkString",
                MacroCallParameterMatcher.hasNameAndStringValue("targetParameters", "")));
    }

    @Test
    public void hiddenFormHyperlinkInAMultiFormKeepsTheFormForAfterTheClosingOfTheForm() {
        final ModelForm modelForm = mock(ModelForm.class);
        when(modelForm.getType()).thenReturn("multi");
        when(modelFormField.getModelForm()).thenReturn(modelForm);
        when(request.getAttribute("title")).thenReturn("");
        final Map<String, Object> context = new HashMap<>();
        context.put("wholeFormContext", new HashMap<String, Object>());

        final RenderableFtl link = renderableFtlFormElementsBuilder.makeHyperlinkByType("hidden-form", "STYLE", null, "#",
                new HashMap<>(), "A ${description}", "", "", modelFormField, request, response, context);

        assertThat(link instanceof RenderableFtlString, equalTo(true));
        assertThat(((RenderableFtlString) link).getFtlString().contains("A ${description}"), equalTo(true));
        assertThat(renderableFtlFormElementsBuilder.postMultiFormContent(context) instanceof RenderableFtlString, equalTo(true));
    }
}
