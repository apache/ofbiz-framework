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
 *******************************************************************************/
package org.apache.ofbiz.widget.renderer.macro;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.template.FreeMarkerWorker;
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
import org.mockito.InOrder;
import org.mockito.MockedStatic;

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
        when(renderableFtlFormElementsBuilder.checkField(notNull(), eq(checkField))).thenReturn(genericMacroCall);
        genericTooltipRenderedExpectation(checkField);

        macroFormRenderer.renderCheckField(appendable, ImmutableMap.of(), checkField);

        genericSingleMacroRenderedVerification();
        genericTooltipRenderedVerification();
    }

    @Test
    public void radioFieldMacroRendered() throws IOException {
        final ModelFormField.RadioField radioField = mock(ModelFormField.RadioField.class);
        when(renderableFtlFormElementsBuilder.radioField(notNull(), eq(radioField))).thenReturn(genericMacroCall);
        genericTooltipRenderedExpectation(radioField);

        macroFormRenderer.renderRadioField(appendable, ImmutableMap.of(), radioField);

        genericSingleMacroRenderedVerification();
        genericTooltipRenderedVerification();
    }

    @Test
    public void submitFieldMacroRendered() throws IOException {
        final ModelFormField.SubmitField submitField = mock(ModelFormField.SubmitField.class);
        when(renderableFtlFormElementsBuilder.submitField(notNull(), eq(submitField), anyBoolean())).thenReturn(genericMacroCall);
        genericTooltipRenderedExpectation(submitField);

        macroFormRenderer.renderSubmitField(appendable, ImmutableMap.of(), submitField);

        genericSingleMacroRenderedVerification();
        genericTooltipRenderedVerification();
    }

    @Test
    public void resetFieldMacroRendered() throws IOException {
        final ModelFormField.ResetField resetField = mock(ModelFormField.ResetField.class);
        when(renderableFtlFormElementsBuilder.resetField(notNull(), eq(resetField))).thenReturn(genericMacroCall);
        genericTooltipRenderedExpectation(resetField);

        macroFormRenderer.renderResetField(appendable, ImmutableMap.of(), resetField);

        genericSingleMacroRenderedVerification();
        genericTooltipRenderedVerification();
    }

    @Test
    public void hiddenFieldMacroRendered() throws IOException {
        final ModelFormField.HiddenField hiddenField = mock(ModelFormField.HiddenField.class);
        when(hiddenField.getModelFormField()).thenReturn(modelFormField);
        when(hiddenField.getValue(notNull())).thenReturn("HIDDENVALUE");
        when(renderableFtlFormElementsBuilder.hiddenField(notNull(), eq(modelFormField), eq("HIDDENVALUE"))).thenReturn(genericMacroCall);

        macroFormRenderer.renderHiddenField(appendable, ImmutableMap.of(), hiddenField);

        genericSingleMacroRenderedVerification();
    }

    @Test
    public void emptyFieldTitleMacroRendered() throws IOException {
        when(modelFormField.getTitle(notNull())).thenReturn(" ");
        when(renderableFtlFormElementsBuilder.formatEmptySpace()).thenReturn(genericMacroCall);

        macroFormRenderer.renderFieldTitle(appendable, ImmutableMap.of(), modelFormField);

        genericSingleMacroRenderedVerification();
    }

    @Test
    public void fieldTitleMacroRendered() throws IOException {
        when(modelFormField.getTitle(notNull())).thenReturn("FIELDTITLE");
        when(renderableFtlFormElementsBuilder.fieldTitle(nullable(String.class), eq("FIELDTITLE"), nullable(String.class),
                nullable(String.class))).thenReturn(genericMacroCall);

        macroFormRenderer.renderFieldTitle(appendable, ImmutableMap.of(), modelFormField);

        genericSingleMacroRenderedVerification();
    }

    @Test
    public void rowSubmitFieldTitleMacroRendered() throws IOException {
        when(modelFormField.getTitle(notNull())).thenReturn("FIELDTITLE");
        when(modelFormField.isRowSubmit()).thenReturn(true);
        when(modelFormField.getModelForm().getName()).thenReturn("FORMNAME");
        when(renderableFtlFormElementsBuilder.hyperlinkTitle("FORMNAME", "FIELDTITLE", true)).thenReturn(genericMacroCall);

        macroFormRenderer.renderFieldTitle(appendable, ImmutableMap.of(), modelFormField);

        genericSingleMacroRenderedVerification();
    }

    @Test
    public void formOpenedMacroRendered() throws IOException {
        final ModelSingleForm modelSingleForm = mock(ModelSingleForm.class);
        when(renderableFtlFormElementsBuilder.formOpen(notNull(), eq(modelSingleForm))).thenReturn(genericMacroCall);

        macroFormRenderer.renderFormOpen(appendable, ImmutableMap.of(), modelSingleForm);

        genericSingleMacroRenderedVerification();
    }

    @Test
    public void formClosedMacroRendered() throws IOException {
        final ModelSingleForm modelSingleForm = mock(ModelSingleForm.class);
        when(renderableFtlFormElementsBuilder.formClose()).thenReturn(genericMacroCall);

        macroFormRenderer.renderFormClose(appendable, ImmutableMap.of(), modelSingleForm);

        genericSingleMacroRenderedVerification();
    }

    @Test
    public void multiFormClosedMacroRenderedBeforeThePostFormContent() throws IOException {
        final ModelForm modelForm = mock(ModelForm.class);
        when(renderableFtlFormElementsBuilder.multiFormClose()).thenReturn(genericMacroCall);
        when(renderableFtlFormElementsBuilder.postMultiFormContent(notNull())).thenReturn(genericHyperlinkMacroCall);

        macroFormRenderer.renderMultiFormClose(appendable, ImmutableMap.of(), modelForm);

        final InOrder inOrder = inOrder(ftlWriter);
        inOrder.verify(ftlWriter).processFtl(appendable, null, genericMacroCall);
        inOrder.verify(ftlWriter).processFtl(appendable, null, genericHyperlinkMacroCall);
    }

    @Test
    public void listWrapperOpenMacroRendered() throws IOException {
        final ModelSingleForm modelSingleForm = mock(ModelSingleForm.class);
        when(renderableFtlFormElementsBuilder.formatListWrapperOpen(notNull(), eq(modelSingleForm))).thenReturn(genericMacroCall);

        macroFormRenderer.setRenderPagination(false);
        macroFormRenderer.renderFormatListWrapperOpen(appendable, new HashMap<>(), modelSingleForm);

        genericSingleMacroRenderedVerification();
    }

    @Test
    public void emptyFormDataMacroRendered() throws IOException {
        final ModelSingleForm modelSingleForm = mock(ModelSingleForm.class);
        when(renderableFtlFormElementsBuilder.emptyFormDataMessage(notNull(), eq(modelSingleForm))).thenReturn(genericMacroCall);

        macroFormRenderer.renderEmptyFormDataMessage(appendable, new HashMap<>(), modelSingleForm);

        genericSingleMacroRenderedVerification();
    }

    @Test
    public void listWrapperCloseMacroRendered() throws IOException {
        final ModelSingleForm modelSingleForm = mock(ModelSingleForm.class);
        when(renderableFtlFormElementsBuilder.formatListWrapperClose(modelSingleForm)).thenReturn(genericMacroCall);

        macroFormRenderer.setRenderPagination(false);
        macroFormRenderer.renderFormatListWrapperClose(appendable, new HashMap<>(), modelSingleForm);

        genericSingleMacroRenderedVerification();
    }

    @Test
    public void itemRowOpenMacroRendered() throws IOException {
        final ModelForm modelForm = mock(ModelForm.class);
        when(renderableFtlFormElementsBuilder.formatItemRowOpen(notNull(), eq(modelForm))).thenReturn(genericMacroCall);

        macroFormRenderer.renderFormatItemRowOpen(appendable, ImmutableMap.of("itemIndex", 2), modelForm);

        genericSingleMacroRenderedVerification();
    }

    @Test
    public void itemRowCellOpenMacroRendered() throws IOException {
        final ModelForm modelForm = mock(ModelForm.class);
        final ModelFormField localModelFormField = mock(ModelFormField.class);
        when(renderableFtlFormElementsBuilder.formatItemRowCellOpen(localModelFormField, 2)).thenReturn(genericMacroCall);

        macroFormRenderer.renderFormatItemRowCellOpen(appendable, ImmutableMap.of(), modelForm, localModelFormField, 2);

        genericSingleMacroRenderedVerification();
    }

    @Test
    public void itemRowFormCellOpenMacroRendered() throws IOException {
        final ModelForm modelForm = mock(ModelForm.class);
        when(renderableFtlFormElementsBuilder.formatItemRowFormCellOpen(modelForm)).thenReturn(genericMacroCall);

        macroFormRenderer.renderFormatItemRowFormCellOpen(appendable, ImmutableMap.of(), modelForm);

        genericSingleMacroRenderedVerification();
    }

    @Test
    public void singleWrapperOpenMacroRendered() throws IOException {
        final ModelForm modelForm = mock(ModelForm.class);
        when(renderableFtlFormElementsBuilder.formatSingleWrapperOpen(notNull(), eq(modelForm))).thenReturn(genericMacroCall);

        macroFormRenderer.renderFormatSingleWrapperOpen(appendable, ImmutableMap.of(), modelForm);

        genericSingleMacroRenderedVerification();
    }

    @Test
    public void fieldRowWidgetCellOpenMacroRendered() throws IOException {
        final ModelFormField localModelFormField = mock(ModelFormField.class);
        when(renderableFtlFormElementsBuilder.formatFieldRowWidgetCellOpen(localModelFormField, 1)).thenReturn(genericMacroCall);

        macroFormRenderer.renderFormatFieldRowWidgetCellOpen(appendable, ImmutableMap.of(), localModelFormField, 1, 1, null);

        genericSingleMacroRenderedVerification();
    }

    @Test
    public void boundaryCommentsRenderedOnlyWhenEnabled() throws IOException {
        final ModelSingleForm modelSingleForm = mock(ModelSingleForm.class);
        when(modelSingleForm.getBoundaryCommentName()).thenReturn("FORMNAME");
        when(renderableFtlFormElementsBuilder.formOpen(notNull(), eq(modelSingleForm))).thenReturn(genericMacroCall);
        when(renderableFtlFormElementsBuilder.boundaryComment("Begin", "Form Widget - Form Element", "FORMNAME"))
                .thenReturn(genericTooltipMacroCall);

        utilPropertiesMock.when(() -> UtilProperties.getPropertyValue("widget", "widget.verbose")).thenReturn("false");
        macroFormRenderer.renderFormOpen(appendable, ImmutableMap.of(), modelSingleForm);
        verify(ftlWriter, never()).processFtl(appendable, null, genericTooltipMacroCall);

        utilPropertiesMock.when(() -> UtilProperties.getPropertyValue("widget", "widget.verbose")).thenReturn("true");
        macroFormRenderer.renderFormOpen(appendable, ImmutableMap.of(), modelSingleForm);
        verify(ftlWriter).processFtl(appendable, null, genericTooltipMacroCall);
    }

    @Test
    public void textFindFieldMacroRendered() throws IOException {
        final ModelFormField.TextFindField textFindField = mock(ModelFormField.TextFindField.class);
        when(renderableFtlFormElementsBuilder.textFindField(notNull(), eq(textFindField))).thenReturn(genericMacroCall);
        genericTooltipRenderedExpectation(textFindField);

        macroFormRenderer.renderTextFindField(appendable, ImmutableMap.of(), textFindField);

        genericSingleMacroRenderedVerification();
        genericTooltipRenderedVerification();
    }

    @Test
    public void rangeFindFieldMacroRendered() throws IOException {
        final ModelFormField.RangeFindField rangeFindField = mock(ModelFormField.RangeFindField.class);
        when(renderableFtlFormElementsBuilder.rangeFindField(notNull(), eq(rangeFindField))).thenReturn(genericMacroCall);
        genericTooltipRenderedExpectation(rangeFindField);

        macroFormRenderer.renderRangeFindField(appendable, ImmutableMap.of(), rangeFindField);

        genericSingleMacroRenderedVerification();
        genericTooltipRenderedVerification();
    }

    @Test
    public void lookupFieldMacroRendered() throws IOException {
        final ModelFormField.LookupField lookupField = mock(ModelFormField.LookupField.class);
        when(renderableFtlFormElementsBuilder.lookupField(notNull(), eq(lookupField), eq(true))).thenReturn(genericMacroCall);
        genericTooltipRenderedExpectation(lookupField);

        macroFormRenderer.renderLookupField(appendable, ImmutableMap.of(), lookupField);

        genericSingleMacroRenderedVerification();
        genericTooltipRenderedVerification();
    }

    @Test
    public void renderNextPrevMacroRendered() throws IOException {
        final ModelForm modelForm = mock(ModelForm.class);
        when(renderableFtlFormElementsBuilder.nextPrev(notNull(), eq(modelForm), eq(true))).thenReturn(genericMacroCall);

        macroFormRenderer.renderNextPrev(appendable, new HashMap<>(), modelForm);

        genericSingleMacroRenderedVerification();
    }

    @Test
    public void fileFieldMacroRendered() throws IOException {
        final ModelFormField.FileField fileField = mock(ModelFormField.FileField.class);
        when(renderableFtlFormElementsBuilder.fileField(notNull(), eq(fileField))).thenReturn(genericMacroCall);
        genericTooltipRenderedExpectation(fileField);

        macroFormRenderer.renderFileField(appendable, ImmutableMap.of(), fileField);

        genericSingleMacroRenderedVerification();
        genericTooltipRenderedVerification();
    }

    @Test
    public void passwordFieldMacroRendered() throws IOException {
        final ModelFormField.PasswordField passwordField = mock(ModelFormField.PasswordField.class);
        when(renderableFtlFormElementsBuilder.passwordField(notNull(), eq(passwordField))).thenReturn(genericMacroCall);
        genericTooltipRenderedExpectation(passwordField);

        macroFormRenderer.renderPasswordField(appendable, ImmutableMap.of(), passwordField);

        genericSingleMacroRenderedVerification();
        genericTooltipRenderedVerification();
    }

    @Test
    public void imageFieldMacroRendered() throws IOException {
        final ModelFormField.ImageField imageField = mock(ModelFormField.ImageField.class);
        when(renderableFtlFormElementsBuilder.imageField(notNull(), eq(imageField))).thenReturn(genericMacroCall);
        genericTooltipRenderedExpectation(imageField);

        macroFormRenderer.renderImageField(appendable, ImmutableMap.of(), imageField);

        genericSingleMacroRenderedVerification();
        genericTooltipRenderedVerification();
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
        when(renderableFtlFormElementsBuilder.sortField(nullable(String.class), eq("TITLE"), nullable(String.class), eq(false),
                eq("HELPTEXT"))).thenReturn(genericMacroCall);

        final Map<String, Object> context = new HashMap<>();
        macroFormRenderer.renderSortField(appendable, context, modelFormField, "TITLE");

        genericSingleMacroRenderedVerification();
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
    public void bannerMacroRendered() throws IOException {
        final ModelForm.Banner banner = mock(ModelForm.Banner.class);
        when(renderableFtlFormElementsBuilder.banner(notNull(), eq(banner))).thenReturn(genericMacroCall);

        macroFormRenderer.renderBanner(appendable, ImmutableMap.of(), banner);

        genericSingleMacroRenderedVerification();
    }

    @Test
    public void renderSortFieldUsesQueryString() throws IOException {
        final ModelForm modelForm = mock(ModelForm.class);
        final String paginateTarget = "TARGET";
        final String qbeString = "field2=value2 with spaces";
        final String encodedQbeString = "field2=value2+with+spaces";
        final String linkFromQbeString = "LinkFromQBEString";

        when(modelFormField.getModelForm()).thenReturn(modelForm);
        when(modelForm.getPaginateTarget(notNull())).thenReturn(paginateTarget);
        when(requestHandler.makeLink(notNull(), notNull(), contains(encodedQbeString))).thenReturn(linkFromQbeString);
        when(modelFormField.getSortFieldHelpText(notNull())).thenReturn("HELPTEXT");
        when(renderableFtlFormElementsBuilder.sortField(nullable(String.class), eq(""), eq(linkFromQbeString), eq(false),
                eq("HELPTEXT"))).thenReturn(genericMacroCall);

        final Map<String, Object> context = new HashMap<>();
        context.put("_QBESTRING_", qbeString);
        context.put("listSize", 100);
        macroFormRenderer.renderSortField(appendable, context, modelFormField, "");

        genericSingleMacroRenderedVerification();
    }

    @Test
    public void hyperlinkFieldRendersHyperlinkWithTooltip() throws IOException {
        final ModelFormField.HyperlinkField hyperlinkField = mock(ModelFormField.HyperlinkField.class);
        when(renderableFtlFormElementsBuilder.makeHyperlinkByType(nullable(String.class), nullable(String.class),
                nullable(String.class), nullable(String.class), anyMap(), nullable(String.class), nullable(String.class),
                nullable(String.class), eq(modelFormField), eq(request), eq(response), notNull()))
                .thenReturn(genericHyperlinkMacroCall);
        genericTooltipRenderedExpectation(hyperlinkField);

        macroFormRenderer.renderHyperlinkField(appendable, new HashMap<>(), hyperlinkField);

        genericSubHyperlinkRenderedVerification();
        genericTooltipRenderedVerification();
    }

    @Test
    public void hyperlinkFieldPassesTheAlternateAndTheImageTitleWithoutEscapedQuotes() throws IOException {
        final ModelFormField.HyperlinkField hyperlinkField = mock(ModelFormField.HyperlinkField.class);
        when(hyperlinkField.getModelFormField()).thenReturn(modelFormField);
        when(hyperlinkField.getAlternate(notNull())).thenReturn("say \"hi\"");
        when(hyperlinkField.getImageTitle(notNull())).thenReturn("a \"title\"");

        macroFormRenderer.renderHyperlinkField(appendable, new HashMap<>(), hyperlinkField);

        verify(request).setAttribute("alternate", "say \"hi\"");
        verify(request).setAttribute("imageTitle", "a \"title\"");
    }

    @Test
    public void fieldTitleWithHeaderLinkRendersHyperlinkTitle() throws IOException {
        when(modelFormField.getTitle(notNull())).thenReturn("FIELDTITLE");
        when(modelFormField.getHeaderLink()).thenReturn("/target");
        when(modelFormField.getModelForm().getName()).thenReturn("FORMNAME");
        when(renderableFtlFormElementsBuilder.hyperlinkTitle(eq("FORMNAME"), anyString(), eq(false))).thenReturn(genericMacroCall);

        macroFormRenderer.renderFieldTitle(appendable, ImmutableMap.of(), modelFormField);

        verify(renderableFtlFormElementsBuilder).hyperlinkMacroCall(nullable(String.class), anyString(), eq("/target"), isNull(),
                eq("FIELDTITLE"), eq(""), eq(modelFormField), eq(request), eq(response), notNull(), eq(""));
        genericSingleMacroRenderedVerification();
    }

    @Test
    public void dropDownSubHyperlinkIsPrecededByASpace() throws IOException {
        final ModelFormField.DropDownField dropDownField = mock(ModelFormField.DropDownField.class);
        final ModelFormField.SubHyperlink subHyperlink = mock(ModelFormField.SubHyperlink.class);
        when(dropDownField.getSubHyperlink()).thenReturn(subHyperlink);
        when(subHyperlink.shouldUse(notNull())).thenReturn(true);
        when(renderableFtlFormElementsBuilder.dropDownField(notNull(), eq(dropDownField), anyBoolean())).thenReturn(genericMacroCall);
        when(renderableFtlFormElementsBuilder.makeHyperlinkString(eq(subHyperlink), notNull())).thenReturn(genericHyperlinkMacroCall);
        genericTooltipRenderedExpectation(dropDownField);

        macroFormRenderer.renderDropDownField(appendable, ImmutableMap.of(), dropDownField);

        assertThat(appendable.toString(), equalTo(" "));
        genericSingleMacroRenderedVerification();
        genericSubHyperlinkRenderedVerification();
        genericTooltipRenderedVerification();
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
}
