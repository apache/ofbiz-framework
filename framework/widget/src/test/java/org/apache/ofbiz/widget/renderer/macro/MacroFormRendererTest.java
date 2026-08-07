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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.template.FreeMarkerWorker;
import org.apache.ofbiz.webapp.control.ConfigXMLReader;
import org.apache.ofbiz.webapp.control.RequestHandler;
import org.apache.ofbiz.widget.model.FieldInfo;
import org.apache.ofbiz.widget.model.ModelForm;
import org.apache.ofbiz.widget.model.ModelFormField;
import org.apache.ofbiz.widget.model.ModelScreenWidget;
import org.apache.ofbiz.widget.model.ModelSingleForm;
import org.apache.ofbiz.widget.model.ThemeFactory;
import org.apache.ofbiz.widget.renderer.VisualTheme;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtl;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtlMacroCall;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import freemarker.core.Environment;
import freemarker.template.Template;

public final class MacroFormRendererTest {

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final FtlWriter ftlWriter = mock(FtlWriter.class);
    private final RenderableFtlFormElementsBuilder renderableFtlFormElementsBuilder = mock(RenderableFtlFormElementsBuilder.class);
    private final HttpSession httpSession = mock(HttpSession.class);
    private final Template template = mock(Template.class);
    private final Environment environment = mock(Environment.class);
    private final VisualTheme visualTheme = mock(VisualTheme.class);
    private final RequestHandler requestHandler = mock(RequestHandler.class);
    private final ModelFormField.ContainerField containerField = mock(ModelFormField.ContainerField.class);

    // Deep stubs: many renderXxxField() methods cascade through modelFormField.getModelForm() even
    // when a test doesn't care about the form itself - JMockit's @Mocked auto-cascaded that call to
    // a fresh mock; Mockito needs it asked for explicitly.
    private final ModelFormField modelFormField = mock(ModelFormField.class, RETURNS_DEEP_STUBS);

    private MacroFormRenderer macroFormRenderer;

    private MockedStatic<FreeMarkerWorker> freeMarkerWorkerMock;
    private MockedStatic<ThemeFactory> themeFactoryMock;
    private MockedStatic<RequestHandler> requestHandlerStaticMock;
    private MockedStatic<UtilHttp> utilHttpMock;
    private MockedStatic<UtilProperties> utilPropertiesMock;

    private final StringWriter appendable = new StringWriter();
    private final RenderableFtlMacroCall genericMacroCall = RenderableFtlMacroCall.builder()
            .name("genericTest")
            .build();
    private final RenderableFtlMacroCall genericHyperlinkMacroCall = RenderableFtlMacroCall.builder()
            .name("genericHyperlink")
            .build();
    private final RenderableFtlMacroCall genericTooltipMacroCall = RenderableFtlMacroCall.builder()
            .name("genericTooltip")
            .build();

    @BeforeEach
    public void setupMockups() throws IOException {
        // CALLS_REAL_METHODS on every static mock below: matches JMockit's MockUp<X>, which only
        // shadowed the one declared @Mock method on each class and left everything else on that
        // class real - the plain mockStatic() default would instead null out every static method.
        freeMarkerWorkerMock = mockStatic(FreeMarkerWorker.class, CALLS_REAL_METHODS);
        freeMarkerWorkerMock.when(() -> FreeMarkerWorker.getTemplate(anyString())).thenReturn(template);
        freeMarkerWorkerMock.when(() -> FreeMarkerWorker.renderTemplate(any(Template.class), anyMap(), any())).thenReturn(environment);

        themeFactoryMock = mockStatic(ThemeFactory.class, CALLS_REAL_METHODS);
        themeFactoryMock.when(() -> ThemeFactory.resolveVisualTheme(any())).thenReturn(visualTheme);

        requestHandlerStaticMock = mockStatic(RequestHandler.class, CALLS_REAL_METHODS);
        requestHandlerStaticMock.when(() -> RequestHandler.from(any())).thenReturn(requestHandler);

        utilHttpMock = mockStatic(UtilHttp.class, CALLS_REAL_METHODS);
        utilHttpMock.when(() -> UtilHttp.isJavaScriptEnabled(any())).thenReturn(true);

        utilPropertiesMock = mockStatic(UtilProperties.class, CALLS_REAL_METHODS);
        // nullable(), not any(): several renderXxxField() callers pass a null "locale" context
        // entry straight through, and any(Class) - unlike any() - does not match null here.
        utilPropertiesMock.when(() -> UtilProperties.getMessage(anyString(), anyString(), nullable(Locale.class)))
                .thenAnswer(invocation -> invocation.<String>getArgument(1) + "_MESSAGE");

        // makeHyperlinkString()'s underlying CsrfUtil call dereferences request.getSession() directly
        // with no null-check - JMockit's @Injectable request cascaded that call to a fresh mock
        // automatically; Mockito needs it stubbed explicitly. Harmless to wire even for tests that
        // never reach that path.
        when(request.getSession()).thenReturn(httpSession);

        // MacroFormRenderer's constructor itself calls ThemeFactory.resolveVisualTheme()/
        // RequestHandler.from()/UtilHttp.isJavaScriptEnabled() - the static mocks above must already
        // be armed before this call.
        macroFormRenderer = new MacroFormRenderer(null, request, response, ftlWriter, renderableFtlFormElementsBuilder);
    }

    @AfterEach
    public void tearDownMockups() {
        freeMarkerWorkerMock.close();
        themeFactoryMock.close();
        requestHandlerStaticMock.close();
        utilHttpMock.close();
        utilPropertiesMock.close();
    }

    @Test
    public void labelRenderedAsSingleMacro() {
        final ModelScreenWidget.Label label = mock(ModelScreenWidget.Label.class);
        when(renderableFtlFormElementsBuilder.label(notNull(), notNull())).thenReturn(genericMacroCall);

        macroFormRenderer.renderLabel(appendable, ImmutableMap.of(), label);
        genericSingleMacroRenderedVerification();
    }

    @Test
    public void displayFieldRendersFieldWithTooltip() throws IOException {
        final ModelFormField.DisplayField displayField = mock(ModelFormField.DisplayField.class);
        when(renderableFtlFormElementsBuilder.displayField(notNull(), notNull(), anyBoolean())).thenReturn(genericMacroCall);
        genericTooltipRenderedExpectation(displayField);

        macroFormRenderer.renderDisplayField(appendable, ImmutableMap.of(), displayField);

        genericSingleMacroRenderedVerification();
        genericTooltipRenderedVerification();
    }

    @Test
    public void displayEntityFieldRendersFieldWithLinkAndTooltip() throws IOException {
        final ModelFormField.DisplayEntityField displayEntityField = mock(ModelFormField.DisplayEntityField.class);
        final ModelFormField.SubHyperlink subHyperlink = mock(ModelFormField.SubHyperlink.class);
        when(renderableFtlFormElementsBuilder.displayField(notNull(), notNull(), anyBoolean())).thenReturn(genericMacroCall);
        when(displayEntityField.getSubHyperlink()).thenReturn(subHyperlink);
        when(renderableFtlFormElementsBuilder.makeHyperlinkString(eq(subHyperlink), notNull())).thenReturn(genericHyperlinkMacroCall);
        genericTooltipRenderedExpectation(displayEntityField);

        macroFormRenderer.renderDisplayField(appendable, ImmutableMap.of(), displayEntityField);

        genericSingleMacroRenderedVerification();
        genericSubHyperlinkRenderedVerification();
        genericTooltipRenderedVerification();
    }

    @Test
    public void textFieldRendersFieldWithLinkAndTooltip() {
        final ModelFormField.TextField textField = mock(ModelFormField.TextField.class);
        final ModelFormField.SubHyperlink subHyperlink = mock(ModelFormField.SubHyperlink.class);
        final RenderableFtl renderableFtlAsterisk = RenderableFtlMacroCall.builder()
                .name("asterisks")
                .build();
        when(renderableFtlFormElementsBuilder.textField(notNull(), eq(textField), anyBoolean()))
                .thenReturn(genericMacroCall);
        when(textField.getSubHyperlink()).thenReturn(subHyperlink);
        when(renderableFtlFormElementsBuilder.makeHyperlinkString(eq(subHyperlink), notNull())).thenReturn(genericHyperlinkMacroCall);
        when(renderableFtlFormElementsBuilder.asterisks(notNull(), notNull())).thenReturn(renderableFtlAsterisk);

        genericTooltipRenderedExpectation(textField);

        macroFormRenderer.renderTextField(appendable, ImmutableMap.of("session", httpSession), textField);
        genericSingleMacroRenderedVerification();
        genericSubHyperlinkRenderedVerification();
        genericTooltipRenderedVerification();

        verify(ftlWriter).processFtl(appendable, null, renderableFtlAsterisk);
    }

    @Test
    public void textAreaMacroRendered() {
        final ModelFormField.TextareaField textareaField = mock(ModelFormField.TextareaField.class);
        when(renderableFtlFormElementsBuilder.textArea(notNull(), eq(textareaField)))
                .thenReturn(genericMacroCall);

        genericTooltipRenderedExpectation(textareaField);

        macroFormRenderer.renderTextareaField(appendable, ImmutableMap.of(), textareaField);

        genericSingleMacroRenderedVerification();
        genericTooltipRenderedVerification();
    }

    @Test
    public void dateTimeMacroRendered() {
        final ModelFormField.DateTimeField dateTimeField = mock(ModelFormField.DateTimeField.class);
        when(renderableFtlFormElementsBuilder.dateTime(notNull(), eq(dateTimeField)))
                .thenReturn(genericMacroCall);

        genericTooltipRenderedExpectation(dateTimeField);

        macroFormRenderer.renderDateTimeField(appendable, ImmutableMap.of(), dateTimeField);

        genericSingleMacroRenderedVerification();
        genericTooltipRenderedVerification();
    }

    @Test
    public void dateRangePickerFieldMacroRendered() throws IOException {
        final ModelFormField.DateRangePickerField dateRangePickerField = mock(ModelFormField.DateRangePickerField.class);
        when(renderableFtlFormElementsBuilder.dateRangePicker(notNull(), eq(dateRangePickerField)))
                .thenReturn(genericMacroCall);

        genericTooltipRenderedExpectation(dateRangePickerField);
        macroFormRenderer.renderDateRangePickerField(appendable, ImmutableMap.of(), dateRangePickerField);
        genericSingleMacroRenderedVerification();
        genericTooltipRenderedVerification();
    }

    @Test
    public void checkFieldMacroRendered() throws IOException {
        final ModelFormField.CheckField checkField = mock(ModelFormField.CheckField.class);
        when(checkField.getModelFormField()).thenReturn(modelFormField);
        final List<ModelFormField.OptionValue> optionValues = ImmutableList.of(
                new ModelFormField.OptionValue("KEY1", "DESC1"),
                new ModelFormField.OptionValue("KEY2", "DESC2"),
                new ModelFormField.OptionValue("KEY3", "DESC3"),
                new ModelFormField.OptionValue("KEY4", "DESC4"));

        when(modelFormField.getEntry(notNull())).thenReturn("KEY2");
        when(checkField.getAllOptionValues(notNull(), any())).thenReturn(optionValues);

        macroFormRenderer.renderCheckField(appendable, ImmutableMap.of(), checkField);
        assertAndGetMacroString("renderCheckField", ImmutableMap.of(
                "currentValue", "KEY2",
                "items", ImmutableList.of(
                        "{'value':'KEY1', 'description':'DESC1', 'checked':'false'}",
                        "{'value':'KEY2', 'description':'DESC2', 'checked':'true'}",
                        "{'value':'KEY3', 'description':'DESC3', 'checked':'false'}",
                        "{'value':'KEY4', 'description':'DESC4', 'checked':'false'}")));

        when(modelFormField.getEntry(notNull())).thenReturn("");
        when(checkField.getModelFormField().getAttributeName()).thenReturn("FieldName");

        StringWriter writer = new StringWriter();
        Map<String, Object> context = new HashMap<>();
        List<String> fieldName = new ArrayList<>();
        fieldName.add("KEY1");
        fieldName.add("KEY3");
        context.put("FieldName", fieldName);

        try {
            macroFormRenderer.renderCheckField(writer, context, checkField);
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertAndGetMacroString("renderCheckField", ImmutableMap.of(
                "items", ImmutableList.of(
                        "{'value':'KEY1', 'description':'DESC1', 'checked':'true'}",
                        "{'value':'KEY2', 'description':'DESC2', 'checked':'false'}",
                        "{'value':'KEY3', 'description':'DESC3', 'checked':'true'}",
                        "{'value':'KEY4', 'description':'DESC4', 'checked':'false'}")));

    }

    @Test
    public void radioFieldMacroRendered() throws IOException {
        final ModelFormField.RadioField radioField = mock(ModelFormField.RadioField.class);
        when(radioField.getModelFormField()).thenReturn(modelFormField);
        final List<ModelFormField.OptionValue> optionValues = ImmutableList.of(
                new ModelFormField.OptionValue("KEY1", "DESC1"),
                new ModelFormField.OptionValue("KEY2", "DESC2"));

        when(modelFormField.getEntry(notNull())).thenReturn("KEY2");
        when(radioField.getAllOptionValues(notNull(), any())).thenReturn(optionValues);

        macroFormRenderer.renderRadioField(appendable, ImmutableMap.of(), radioField);
        assertAndGetMacroString("renderRadioField", ImmutableMap.of(
                "currentValue", "KEY2",
                "items", ImmutableList.of("{'key':'KEY1', 'description':'DESC1'}",
                        "{'key':'KEY2', 'description':'DESC2'}")));
    }

    @Test
    public void submitFieldMacroRendered() throws IOException {
        final ModelFormField.SubmitField submitField = mock(ModelFormField.SubmitField.class);
        when(submitField.getModelFormField()).thenReturn(modelFormField);
        when(modelFormField.getTitle(notNull())).thenReturn("BUTTONTITLE");
        // renderSubmitField() does updateAreas.addAll(modelForm.getOnSubmitUpdateAreas()) unconditionally
        // once the isNotEmpty() guard passes. JMockit's cascading defaults an unstubbed array-returning
        // call to an empty array; Mockito's deep-stub default is null, and AbstractCollection.addAll()
        // dereferences Collection.toArray() without a null-check - needs an explicit non-null list here.
        when(modelFormField.getModelForm().getOnSubmitUpdateAreas()).thenReturn(new ArrayList<>());
        when(modelFormField.getOnClickUpdateAreas()).thenReturn(new ArrayList<>());

        macroFormRenderer.renderSubmitField(appendable, ImmutableMap.of(), submitField);
        assertAndGetMacroString("renderSubmitField", ImmutableMap.of("title", "BUTTONTITLE"));
    }

    @Test
    public void resetFieldMacroRendered() throws IOException {
        final ModelFormField.ResetField resetField = mock(ModelFormField.ResetField.class);
        when(resetField.getModelFormField()).thenReturn(modelFormField);
        when(modelFormField.getTitle(notNull())).thenReturn("BUTTONTITLE");

        macroFormRenderer.renderResetField(appendable, ImmutableMap.of(), resetField);
        assertAndGetMacroString("renderResetField", ImmutableMap.of("title", "BUTTONTITLE"));
    }

    @Test
    public void hiddenFieldMacroRendered() throws IOException {
        final ModelFormField.HiddenField hiddenField = mock(ModelFormField.HiddenField.class);
        when(hiddenField.getModelFormField()).thenReturn(modelFormField);
        when(hiddenField.getValue(notNull())).thenReturn("HIDDENVALUE");

        macroFormRenderer.renderHiddenField(appendable, ImmutableMap.of(), hiddenField);
        assertAndGetMacroString("renderHiddenField", ImmutableMap.of("value", "HIDDENVALUE"));
    }

    @Test
    public void emptyFieldTitleMacroRendered() throws IOException {
        when(modelFormField.getTitle(notNull())).thenReturn(" ");

        macroFormRenderer.renderFieldTitle(appendable, ImmutableMap.of(), modelFormField);
        assertAndGetMacroString("renderFormatEmptySpace");
    }

    @Test
    public void fieldTitleMacroRendered() throws IOException {
        when(modelFormField.getTitle(notNull())).thenReturn("FIELDTITLE");

        macroFormRenderer.renderFieldTitle(appendable, ImmutableMap.of(), modelFormField);
        assertAndGetMacroString("renderFieldTitle", ImmutableMap.of("title", "FIELDTITLE"));
    }

    @Test
    public void formOpenedMacroRendered() throws IOException {
        final ModelSingleForm modelSingleForm = mock(ModelSingleForm.class);
        when(modelSingleForm.getType()).thenReturn("single");

        macroFormRenderer.renderFormOpen(appendable, ImmutableMap.of(), modelSingleForm);
        assertAndGetMacroString("renderFormOpen", ImmutableMap.of("formType", "single"));
    }

    @Test
    public void formClosedMacroRendered() throws IOException {
        final ModelSingleForm modelSingleForm = mock(ModelSingleForm.class);
        macroFormRenderer.renderFormClose(appendable, ImmutableMap.of(), modelSingleForm);
        assertAndGetMacroString("renderFormClose");
    }

    @Test
    public void multiFormClosedMacroRendered() throws IOException {
        final ModelForm modelForm = mock(ModelForm.class);
        macroFormRenderer.renderMultiFormClose(appendable, ImmutableMap.of(), modelForm);
        assertAndGetMacroString("renderMultiFormClose");
    }

    @Test
    public void listWrapperOpenMacroRendered() throws IOException {
        final ModelSingleForm modelSingleForm = mock(ModelSingleForm.class);
        macroFormRenderer.setRenderPagination(false);
        macroFormRenderer.renderFormatListWrapperOpen(appendable, new HashMap<>(), modelSingleForm);
        assertAndGetMacroString("renderFormatListWrapperOpen");
    }

    @Test
    public void emptyFormDataMacroRendered() throws IOException {
        final ModelSingleForm modelSingleForm = mock(ModelSingleForm.class);
        when(modelSingleForm.getEmptyFormDataMessage(notNull())).thenReturn("EMPTY");

        macroFormRenderer.renderEmptyFormDataMessage(appendable, new HashMap<>(), modelSingleForm);
        assertAndGetMacroString("renderEmptyFormDataMessage", ImmutableMap.of("message", "EMPTY"));
    }

    @Test
    public void listWrapperCloseMacroRendered() throws IOException {
        final ModelSingleForm modelSingleForm = mock(ModelSingleForm.class);
        macroFormRenderer.setRenderPagination(false);
        macroFormRenderer.renderFormatListWrapperClose(appendable, new HashMap<>(), modelSingleForm);
        assertAndGetMacroString("renderFormatListWrapperClose");
    }

    @Test
    public void itemRowOpenMacroRendered() throws IOException {
        final ModelForm modelForm = mock(ModelForm.class);
        when(modelForm.getName()).thenReturn("FORMNAME");
        when(modelForm.getEvenRowStyle()).thenReturn("EVENSTYLE");

        macroFormRenderer.renderFormatItemRowOpen(appendable, ImmutableMap.of("itemIndex", 2), modelForm);
        assertAndGetMacroString("renderFormatItemRowOpen", ImmutableMap.of(
                "formName", "FORMNAME",
                "itemIndex", 2,
                "evenRowStyle", "EVENSTYLE"));
    }

    @Test
    public void itemRowCellOpenMacroRendered() throws IOException {
        final ModelForm modelForm = mock(ModelForm.class);
        final ModelFormField localModelFormField = mock(ModelFormField.class);
        when(localModelFormField.getWidgetAreaStyle()).thenReturn("AREASTYLE");
        when(localModelFormField.getName()).thenReturn("FIELDNAME");

        macroFormRenderer.renderFormatItemRowCellOpen(appendable, ImmutableMap.of(), modelForm, localModelFormField, 2);
        assertAndGetMacroString("renderFormatItemRowCellOpen", ImmutableMap.of(
                "fieldName", "FIELDNAME",
                "positionSpan", 2,
                "style", "AREASTYLE"));
    }

    @Test
    public void itemRowFormCellOpenMacroRendered() throws IOException {
        final ModelForm modelForm = mock(ModelForm.class);
        when(modelForm.getFormTitleAreaStyle()).thenReturn("AREASTYLE");

        macroFormRenderer.renderFormatItemRowFormCellOpen(appendable, ImmutableMap.of(), modelForm);
        assertAndGetMacroString("renderFormatItemRowFormCellOpen", ImmutableMap.of("style", "AREASTYLE"));
    }

    @Test
    public void singleWrapperOpenMacroRendered() throws IOException {
        final ModelForm modelForm = mock(ModelForm.class);
        when(modelForm.getDefaultTableStyle()).thenReturn("STYLE${styleParam}");
        when(modelForm.getName()).thenReturn("FORMNAME");

        macroFormRenderer.renderFormatSingleWrapperOpen(appendable, ImmutableMap.of("styleParam", "ABCD"), modelForm);
        assertAndGetMacroString("renderFormatSingleWrapperOpen", ImmutableMap.of(
                "formName", "FORMNAME",
                "style", "STYLEABCD"));
    }

    @Test
    public void fieldRowWidgetCellOpenMacroRendered() throws IOException {
        final ModelFormField localModelFormField = mock(ModelFormField.class);
        when(localModelFormField.getWidgetAreaStyle()).thenReturn("AREASTYLE");

        macroFormRenderer.renderFormatFieldRowWidgetCellOpen(appendable, ImmutableMap.of(), localModelFormField, 1, 1, null);
        assertAndGetMacroString("renderFormatFieldRowWidgetCellOpen", ImmutableMap.of(
                "positionSpan", 1,
                "style", "AREASTYLE"));
    }

    @Test
    public void textFindFieldMacroRendered() throws IOException {
        final ModelFormField localModelFormField = mock(ModelFormField.class);
        final ModelFormField.TextFindField textFindField = mock(ModelFormField.TextFindField.class);
        when(textFindField.getModelFormField()).thenReturn(localModelFormField);
        when(textFindField.getHideOptions()).thenReturn(true);
        when(localModelFormField.getWidgetStyle()).thenReturn("WIDGETSTYLE");
        when(localModelFormField.shouldBeRed(notNull())).thenReturn(true);
        when(localModelFormField.getParameterName(notNull())).thenReturn("FIELDNAME");

        ImmutableMap<String, Object> context = ImmutableMap.of();
        macroFormRenderer.renderTextFindField(appendable, context, textFindField);
        assertAndGetMacroString("renderTextFindField", ImmutableMap.of(
                "name", "FIELDNAME",
                "className", "WIDGETSTYLE",
                "alert", "true"));
    }

    @Test
    public void rangeFindFieldMacroRendered() throws IOException {
        final ModelFormField localModelFormField = mock(ModelFormField.class);
        final ModelFormField.RangeFindField rangeFindField = mock(ModelFormField.RangeFindField.class);
        when(rangeFindField.getModelFormField()).thenReturn(localModelFormField);
        when(localModelFormField.getWidgetStyle()).thenReturn("WIDGETSTYLE");
        when(rangeFindField.getDefaultValue(notNull())).thenReturn("AAA");
        when(localModelFormField.getEntry(notNull(), eq("AAA"))).thenReturn("AAA");
        when(localModelFormField.getEntry(notNull())).thenReturn("BBB");
        when(localModelFormField.shouldBeRed(notNull())).thenReturn(true);
        when(localModelFormField.getParameterName(notNull())).thenReturn("FIELDNAME");

        ImmutableMap<String, Object> context = ImmutableMap.of();
        macroFormRenderer.renderRangeFindField(appendable, context, rangeFindField);
        assertAndGetMacroString("renderRangeFindField", ImmutableMap.of(
                "name", "FIELDNAME",
                "className", "WIDGETSTYLE",
                "alert", "true",
                "value", "AAA",
                "value2", "BBB"));
    }

    @Test
    public void lookupFieldMacroRendered() throws IOException {
        // Deep stubs: renderLookupField()'s shouldApplyRequiredField() call cascades through
        // localModelFormField.getModelForm().getType() unconditionally.
        final ModelFormField localModelFormField = mock(ModelFormField.class, RETURNS_DEEP_STUBS);
        final ModelFormField.LookupField lookupField = mock(ModelFormField.LookupField.class);
        when(httpSession.getAttribute("delegatorName")).thenReturn("delegator");

        when(lookupField.getModelFormField()).thenReturn(localModelFormField);
        when(localModelFormField.getEntry(notNull(), isNull())).thenReturn("VALUE");
        when(localModelFormField.getParameterName(notNull())).thenReturn("FIELDNAME");
        when(localModelFormField.getCurrentContainerId(notNull())).thenReturn("CONTAINERID");

        ImmutableMap<String, Object> context = ImmutableMap.of("session", httpSession);
        macroFormRenderer.renderLookupField(appendable, context, lookupField);
        assertAndGetMacroString("renderLookupField", ImmutableMap.of(
                "name", "FIELDNAME",
                "value", "VALUE",
                "id", "CONTAINERID"));
    }

    @Test
    public void renderNextPrevMacroRendered() throws IOException {
        final ModelForm modelForm = mock(ModelForm.class);
        final String targetService = ""; // Leave empty to avoid CSRF token generation.
        final String paginateIndexField = "PAGINATE_INDEX";
        final String paginateSizeField = "PAGINATE_SIZE";

        when(modelForm.getPaginateTarget(notNull())).thenReturn(targetService);
        when(modelForm.getMultiPaginateIndexField(notNull())).thenReturn(paginateIndexField);

        Map<String, Object> context = new HashMap<>();
        context.put("session", httpSession);
        context.put(paginateIndexField, 0);
        context.put(paginateSizeField, 30);
        macroFormRenderer.renderNextPrev(appendable, context, modelForm);

        assertAndGetMacroString("renderNextPrev");
    }

    @Test
    public void fileFieldMacroRendered() throws IOException {
        final ModelFormField.FileField fileField = mock(ModelFormField.FileField.class);
        when(fileField.getModelFormField()).thenReturn(modelFormField);
        when(modelFormField.getParameterName(notNull())).thenReturn("FIELDNAME");
        when(modelFormField.getEntry(notNull(), isNull())).thenReturn("VALUE");
        when(modelFormField.getWidgetStyle()).thenReturn("WIDGETSTYLE");

        macroFormRenderer.renderFileField(appendable, ImmutableMap.of(), fileField);

        assertAndGetMacroString("renderFileField", ImmutableMap.of(
                "name", "FIELDNAME",
                "value", "VALUE",
                "className", "WIDGETSTYLE"));
    }

    @Test
    public void passwordFieldMacroRendered() throws IOException {
        final ModelFormField.PasswordField passwordField = mock(ModelFormField.PasswordField.class);
        when(passwordField.getModelFormField()).thenReturn(modelFormField);
        when(modelFormField.getParameterName(notNull())).thenReturn("FIELDNAME");
        when(modelFormField.getEntry(notNull(), isNull())).thenReturn("VALUE");
        when(modelFormField.getWidgetStyle()).thenReturn("WIDGETSTYLE");

        macroFormRenderer.renderPasswordField(appendable, ImmutableMap.of(), passwordField);

        assertAndGetMacroString("renderPasswordField", ImmutableMap.of(
                "name", "FIELDNAME",
                "value", "VALUE",
                "className", "WIDGETSTYLE"));
    }

    @Test
    public void imageFieldMacroRendered() throws IOException {
        final ModelFormField.ImageField imageField = mock(ModelFormField.ImageField.class);
        when(imageField.getModelFormField()).thenReturn(modelFormField);
        when(modelFormField.getEntry(notNull(), isNull())).thenReturn("VALUE");

        macroFormRenderer.renderImageField(appendable, ImmutableMap.of(), imageField);

        assertAndGetMacroString("renderImageField", ImmutableMap.of("value", "VALUE"));
    }

    @Test
    public void fieldGroupOpenMacroRendered() throws IOException {
        final ModelForm.FieldGroup fieldGroup = mock(ModelForm.FieldGroup.class);
        when(renderableFtlFormElementsBuilder.fieldGroupOpen(notNull(), notNull())).thenReturn(genericMacroCall);

        macroFormRenderer.renderFieldGroupOpen(appendable, ImmutableMap.of(), fieldGroup);
        genericSingleMacroRenderedVerification();
    }

    @Test
    public void fieldGroupCloseMacroRendered() throws IOException {
        final ModelForm.FieldGroup fieldGroup = mock(ModelForm.FieldGroup.class);
        when(renderableFtlFormElementsBuilder.fieldGroupClose(notNull(), notNull())).thenReturn(genericMacroCall);

        macroFormRenderer.renderFieldGroupClose(appendable, ImmutableMap.of(), fieldGroup);
        genericSingleMacroRenderedVerification();
    }

    @Test
    public void sortFieldMacroRendered() throws IOException {
        final ModelForm modelForm = mock(ModelForm.class);
        final String paginateTarget = "TARGET";

        when(modelFormField.getModelForm()).thenReturn(modelForm);
        when(modelForm.getPaginateTarget(notNull())).thenReturn(paginateTarget);
        when(modelFormField.getSortFieldHelpText(notNull())).thenReturn("HELPTEXT");

        final Map<String, Object> context = new HashMap<>();
        macroFormRenderer.renderSortField(appendable, context, modelFormField, "TITLE");

        assertAndGetMacroString("renderSortField", ImmutableMap.of("title", "TITLE"));
    }

    @Test
    public void containerRendererAsSingleMacro() throws IOException {
        when(renderableFtlFormElementsBuilder.containerMacroCall(notNull(), notNull())).thenReturn(genericMacroCall);

        macroFormRenderer.renderContainerFindField(appendable, ImmutableMap.of(), containerField);
        genericSingleMacroRenderedVerification();
    }

    /**
     * Ensures that {@link MacroFormRenderer#renderFormatListWrapperOpen(Appendable, Map, ModelForm)} populates the
     * context with the _QBESTRING_ entry representing a query string.
     * <p>
     * This check exists as the presence of _QBESTRING_ in the context is depended on by
     * {@link MacroFormRenderer#renderNextPrev(Appendable, Map, ModelForm)} and
     * {@link MacroFormRenderer#renderSortField(Appendable, Map, ModelFormField, String)}.
     */
    @Test
    public void renderFormatListWrapperOpenPopulatesQueryString() throws IOException {
        final ModelSingleForm modelSingleForm = mock(ModelSingleForm.class);
        macroFormRenderer.setRenderPagination(false);

        Map<String, Object> requestParameters = new HashMap<>();
        requestParameters.put("field1", "value1");
        requestParameters.put("field2", "value2 with spaces");

        HashMap<String, Object> context = new HashMap<>();
        context.put("requestParameters", requestParameters);

        macroFormRenderer.renderFormatListWrapperOpen(appendable, context, modelSingleForm);

        assertThat(context, Matchers.hasEntry("_QBESTRING_", "field1=value1&amp;field2=value2+with+spaces"));
    }

    @Test
    public void renderNextPrevUsesQueryString() throws IOException {
        final ModelForm modelForm = mock(ModelForm.class);
        final String targetService = ""; // Leave empty to avoid CSRF token generation.
        final String qbeString = "field1=value1&amp;field2=value2+with+spaces";
        final String linkFromQbeString = "LinkFromQBEString";

        when(modelForm.getPaginateTarget(notNull())).thenReturn(targetService);
        when(requestHandler.makeLink(notNull(), notNull(), contains(qbeString))).thenReturn(linkFromQbeString);

        final Map<String, Object> context = new HashMap<>();
        context.put("_QBESTRING_", qbeString);
        context.put("listSize", 100);
        macroFormRenderer.renderNextPrev(appendable, context, modelForm);

        assertAndGetMacroString("renderNextPrev", ImmutableMap.of("nextUrl", linkFromQbeString));
    }

    @Test
    public void renderSortFieldUsesQueryString() throws IOException {
        final ModelForm modelForm = mock(ModelForm.class);
        final String paginateTarget = "TARGET";
        final String qbeString = "field2=value2 with spaces";
        final String linkFromQbeString = "LinkFromQBEString";

        when(modelFormField.getModelForm()).thenReturn(modelForm);
        when(modelForm.getPaginateTarget(notNull())).thenReturn(paginateTarget);
        when(requestHandler.makeLink(notNull(), notNull(), contains(qbeString))).thenReturn(linkFromQbeString);
        when(modelFormField.getSortFieldHelpText(notNull())).thenReturn("HELPTEXT");

        final Map<String, Object> context = new HashMap<>();
        context.put("_QBESTRING_", qbeString);
        context.put("listSize", 100);
        macroFormRenderer.renderSortField(appendable, context, modelFormField, "");

        assertAndGetMacroString("renderSortField", ImmutableMap.of(
                "linkUrl", new FreemarkerRawString(linkFromQbeString)));
    }

    @Test
    public void hyperlinkFieldMacroRenderedTitleNotTruncated() throws IOException {
        final ModelFormField.HyperlinkField hyperlinkField = mock(ModelFormField.HyperlinkField.class);
        when(hyperlinkField.getModelFormField()).thenReturn(modelFormField);
        final String description = "DESCRIPTION";
        final String title = "TITLE";

        when(hyperlinkField.getDescription(notNull())).thenReturn(description);
        when(hyperlinkField.getTarget(notNull())).thenReturn("#");
        when(request.getAttribute("title")).thenReturn(title);

        macroFormRenderer.renderHyperlinkField(appendable, new HashMap<>(), hyperlinkField);
        assertAndGetMacroString("makeHyperlinkString", ImmutableMap.of("description", description, "title", title));
    }

    @Test
    public void hyperlinkFieldMacroRenderedTruncatedNoTitle() throws IOException {
        final ModelFormField.HyperlinkField hyperlinkField = mock(ModelFormField.HyperlinkField.class);
        when(hyperlinkField.getModelFormField()).thenReturn(modelFormField);
        final String description = "DESCRIPTION";

        when(hyperlinkField.getDescription(notNull())).thenReturn(description);
        when(hyperlinkField.getTarget(notNull())).thenReturn("#");
        when(request.getAttribute("descriptionSize")).thenReturn(5);

        macroFormRenderer.renderHyperlinkField(appendable, new HashMap<>(), hyperlinkField);
        assertAndGetMacroString("makeHyperlinkString", ImmutableMap.of("description", "DESCR…", "title", description));
    }

    @Test
    public void hyperlinkFieldMacroRenderedTruncatedWithTitle() throws IOException {
        final ModelFormField.HyperlinkField hyperlinkField = mock(ModelFormField.HyperlinkField.class);
        when(hyperlinkField.getModelFormField()).thenReturn(modelFormField);
        final String description = "DESCRIPTION";
        final String title = "TITLE";

        when(hyperlinkField.getDescription(notNull())).thenReturn(description);
        when(hyperlinkField.getTarget(notNull())).thenReturn("#");
        when(hyperlinkField.getTitle()).thenReturn(title);
        when(request.getAttribute("descriptionSize")).thenReturn(5);

        macroFormRenderer.renderHyperlinkField(appendable, new HashMap<>(), hyperlinkField);
        assertAndGetMacroString("makeHyperlinkString", ImmutableMap.of("description", "DESCR…", "title", description));
    }

    @Test
    public void hyperlinkFieldMacroRenderedModalParameters() throws IOException {
        final ModelFormField.HyperlinkField hyperlinkField = mock(ModelFormField.HyperlinkField.class);
        when(hyperlinkField.getModelFormField()).thenReturn(modelFormField);
        final String title = "TitleValue";
        final String text = "TextValue";
        final String description = "DescriptionValue";
        final String target = "Encoded Target";
        final String id = "IdValue";
        final String uniqueItemName = "UniqueItemName";
        final String width = "650";
        final String height = "150";
        final String confirmation = "Are you sure ?";
        final String targetWindow = "_blank";
        final Map<String, ConfigXMLReader.RequestMap> requestMapMap = new HashMap<>();
        final Map<String, String> parameterMap = new HashMap<>();
        parameterMap.put("k1", "v1");
        parameterMap.put("k2", "v2");

        when(hyperlinkField.getDescription(notNull())).thenReturn(description);
        when(hyperlinkField.getTarget(notNull())).thenReturn(target);
        when(hyperlinkField.getParameterMap(notNull(), isNull(), isNull())).thenReturn(parameterMap);
        when(hyperlinkField.getConfirmation(notNull())).thenReturn(confirmation);
        when(hyperlinkField.getTargetWindow(notNull())).thenReturn(targetWindow);
        when(request.getAttribute("title")).thenReturn(title);
        when(request.getAttribute("text")).thenReturn(text);
        when(request.getAttribute("requestMapMap")).thenReturn(requestMapMap);
        when(request.getAttribute("id")).thenReturn(id);
        when(request.getAttribute("uniqueItemName")).thenReturn(uniqueItemName);
        when(request.getAttribute("width")).thenReturn(width);
        when(request.getAttribute("height")).thenReturn(height);

        macroFormRenderer.renderHyperlinkField(appendable, new HashMap<>(), hyperlinkField);
        ImmutableMap<String, Object> result = ImmutableMap.<String, Object>builder()
                .put("title", title)
                .put("description", description)
                .put("linkUrl", "Encoded%20Target")
                .put("id", id)
                .put("targetParameters", "{\\\"k1\\\":\\\"v1\\\",\\\"k2\\\":\\\"v2\\\",\\\"presentation\\\":\\\"layer\\\"}")
                .put("width", width)
                .put("confirmation", confirmation)
                .put("targetWindow", targetWindow)
                .build();
        assertAndGetMacroString("makeHyperlinkString", result);
    }

    private String assertAndGetMacroString(final String expectedName) {
        return assertAndGetMacroString(expectedName, ImmutableMap.of());
    }

    private String assertAndGetMacroString(final String expectedName, final Map<String, Object> expectedAttributes) {
        ArgumentCaptor<String> macroCaptor = ArgumentCaptor.forClass(String.class);
        verify(ftlWriter, atLeastOnce()).processFtlString(notNull(), isNull(), macroCaptor.capture());

        // The captor accumulates every processFtlString() call made on this mock since the test
        // began, not just those since the last check - unlike JMockit's per-block capture, so a test
        // that renders more than one macro (or calls a render method twice) must look at the most
        // recent invocation, not the first, to see the one this specific call actually produced.
        List<String> macros = macroCaptor.getAllValues();
        assertThat(macros, not(empty()));
        final String macro = macros.get(macros.size() - 1);
        assertThat(macro, startsWith("<@" + expectedName));

        expectedAttributes.forEach((name, value) -> assertMacroAttribute(macro, name, value));

        return macro;
    }

    private void assertMacroAttribute(final String macro, final String attributeName, final Object attributeValue) {
        if (attributeValue instanceof Number) {
            assertThat(macro, containsString(attributeName + "=" + attributeValue));
        } else if (attributeValue instanceof List<?>) {
            final String valueString = ((List<?>) attributeValue).stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(",", "[", "]"));
            assertThat(macro, containsString(attributeName + "=" + valueString));
        } else if (attributeValue instanceof Boolean) {
            assertThat(macro, containsString(attributeName + "=" + attributeValue));
        } else if (attributeValue instanceof FreemarkerRawString) {
            final String valueString = ((FreemarkerRawString) attributeValue).getRawString();
            assertThat(macro, containsString(attributeName + "=\"" + valueString + "\""));
        } else {
            assertThat(macro, containsString(attributeName + "=\"" + attributeValue + "\""));
        }
    }

    /**
     * Assert that the generic MacroCall instance is passed to the macro executor. This is used for simple renderings
     * where MacroFormRenderer has FormMacroCallBuilder to construct a MacroCall and then passes it straight to the
     * MacroCall executor.
     */
    private void genericSingleMacroRenderedVerification() {
        verify(ftlWriter).processFtl(appendable, null, genericMacroCall);
    }

    private void genericTooltipRenderedExpectation(final FieldInfo fieldInfo) {
        when(fieldInfo.getModelFormField()).thenReturn(modelFormField);
        when(renderableFtlFormElementsBuilder.tooltip(notNull(), eq(modelFormField))).thenReturn(genericTooltipMacroCall);
    }

    private void genericTooltipRenderedVerification() {
        verify(ftlWriter).processFtl(appendable, null, genericTooltipMacroCall);
    }

    private void genericSubHyperlinkRenderedVerification() {
        verify(ftlWriter).processFtl(appendable, null, genericHyperlinkMacroCall);
    }

    static class FreemarkerRawString {
        private final String rawString;

        FreemarkerRawString(final String rawString) {
            this.rawString = rawString;
        }

        public String getRawString() {
            return rawString;
        }
    }
}
