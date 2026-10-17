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
import org.apache.ofbiz.widget.model.ModelForm;
import org.apache.ofbiz.widget.model.ModelFormField;
import org.apache.ofbiz.widget.model.ModelScreenWidget;
import org.apache.ofbiz.widget.renderer.VisualTheme;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtlMacroCall;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtl;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtlNoop;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
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
}
