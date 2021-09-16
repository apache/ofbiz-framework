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
import freemarker.core.Environment;
import freemarker.template.Template;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.Tested;
import mockit.Verifications;
import org.apache.ofbiz.base.util.UtilCodec.SimpleEncoder;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.template.FreeMarkerWorker;
import org.apache.ofbiz.entity.Delegator;
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
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

public class MacroFormRendererTest {

    @Injectable
    private HttpServletRequest request;

    @Injectable
    private HttpServletResponse response;

    @Injectable
    private FtlWriter ftlWriter;

    @Injectable
    private RenderableFtlFormElementsBuilder renderableFtlFormElementsBuilder;

    @Mocked
    private HttpSession httpSession;

    @Mocked
    private Template template;

    @Mocked
    private Environment environment;

    @Mocked
    private VisualTheme visualTheme;

    @Mocked
    private RequestHandler requestHandler;

    @Mocked
    private ModelFormField.ContainerField containerField;

    @Mocked
    private ModelFormField modelFormField;

    @Injectable
    private String macroLibraryPath = null;

    @Tested
    private MacroFormRenderer macroFormRenderer;

    private final StringWriter appendable = new StringWriter();
    private RenderableFtlMacroCall genericMacroCall = RenderableFtlMacroCall.builder()
            .name("genericTest")
            .build();
    private RenderableFtlMacroCall genericHyperlinkMacroCall = RenderableFtlMacroCall.builder()
            .name("genericHyperlink")
            .build();
    private RenderableFtlMacroCall genericTooltipMacroCall = RenderableFtlMacroCall.builder()
            .name("genericTooltip")
            .build();

    @Before
    public void setupMockups() {
        new FreeMarkerWorkerMockUp();
        new ThemeFactoryMockUp();
        new RequestHandlerMockUp();
        new UtilHttpMockUp();
        new UtilPropertiesMockUp();
    }

    @SuppressWarnings("checkstyle:InnerAssignment")
    @Test
    public void labelRenderedAsSingleMacro(@Mocked ModelScreenWidget.Label label) {
        new Expectations() {
            {
                renderableFtlFormElementsBuilder.label(withNotNull(), withNotNull());
                result = genericMacroCall;
            }
        };

        macroFormRenderer.renderLabel(appendable, ImmutableMap.of(), label);
        genericSingleMacroRenderedVerification();
    }

    @Test
    public void displayFieldRendersFieldWithTooltip(@Mocked ModelFormField.DisplayField displayField) {
        new Expectations() {
            {
                renderableFtlFormElementsBuilder.displayField(withNotNull(), withNotNull(), anyBoolean);
                result = genericMacroCall;
            }
        };
        genericTooltipRenderedExpectation(displayField);

        macroFormRenderer.renderDisplayField(appendable, ImmutableMap.of(), displayField);

        genericSingleMacroRenderedVerification();
        genericTooltipRenderedVerification();
    }

    @Test
    public void displayEntityFieldRendersFieldWithLinkAndTooltip(
            @Mocked ModelFormField.DisplayEntityField displayEntityField,
            @Mocked ModelFormField.SubHyperlink subHyperlink) {
        new Expectations() {
            {
                renderableFtlFormElementsBuilder.displayField(withNotNull(), withNotNull(), anyBoolean);
                result = genericMacroCall;

                displayEntityField.getSubHyperlink();
                result = subHyperlink;

                renderableFtlFormElementsBuilder.makeHyperlinkString(subHyperlink, withNotNull());
                result = genericHyperlinkMacroCall;
            }
        };
        genericTooltipRenderedExpectation(displayEntityField);

        macroFormRenderer.renderDisplayField(appendable, ImmutableMap.of(), displayEntityField);

        genericSingleMacroRenderedVerification();
        genericSubHyperlinkRenderedVerification();
        genericTooltipRenderedVerification();
    }

    @Test
    public void textFieldRendersFieldWithLinkAndTooltip(@Mocked final ModelFormField.TextField textField,
                                                        @Mocked final ModelFormField.SubHyperlink subHyperlink) {
        final RenderableFtl renderableFtlAsterisk = RenderableFtlMacroCall.builder()
                .name("asterisks")
                .build();
        new Expectations() {
            {
                renderableFtlFormElementsBuilder.textField(withNotNull(), textField, anyBoolean);
                result = genericMacroCall;

                textField.getSubHyperlink();
                result = subHyperlink;

                renderableFtlFormElementsBuilder.makeHyperlinkString(subHyperlink, withNotNull());
                result = genericHyperlinkMacroCall;

                renderableFtlFormElementsBuilder.asterisks(withNotNull(), withNotNull());
                result = renderableFtlAsterisk;
            }
        };

        genericTooltipRenderedExpectation(textField);

        macroFormRenderer.renderTextField(appendable, ImmutableMap.of("session", httpSession), textField);
        genericSingleMacroRenderedVerification();
        genericSubHyperlinkRenderedVerification();
        genericTooltipRenderedVerification();

        new Verifications() {
            {
                ftlWriter.processFtl(appendable, renderableFtlAsterisk);
            }
        };
    }

    @Test
    public void textAreaMacroRendered(@Mocked ModelFormField.TextareaField textareaField) throws IOException {
        new Expectations() {
            {
                modelFormField.getEntry(withNotNull(), anyString);
                result = "TEXTAREAVALUE";

                textareaField.getCols();
                result = 11;

                textareaField.getRows();
                result = 22;
            }
        };

        macroFormRenderer.renderTextareaField(appendable, ImmutableMap.of(), textareaField);

        assertAndGetMacroString("renderTextareaField", ImmutableMap.of(
                "value", "TEXTAREAVALUE",
                "cols", "11",
                "rows", "22"));
    }

    @Test
    public void dateTimeMacroRendered(@Mocked ModelFormField.DateTimeField dateTimeField) throws IOException {
        new Expectations() {
            {
                modelFormField.getEntry(withNotNull(), anyString);
                result = "2020-01-02";

                dateTimeField.getInputMethod();
                result = "date";
            }
        };

        macroFormRenderer.renderDateTimeField(appendable, ImmutableMap.of(), dateTimeField);
        assertAndGetMacroString("renderDateTimeField", ImmutableMap.of("value", "2020-01-02"));
    }

    @Test
    public void dropDownMacroRendered(@Mocked ModelFormField.DropDownField dropDownField) throws IOException {
        final List<ModelFormField.OptionValue> optionValues = ImmutableList.of(
                new ModelFormField.OptionValue("KEY1", "DESC1"),
                new ModelFormField.OptionValue("KEY2", "DESC2"));

        new Expectations() {
            {
                modelFormField.getEntry(withNotNull());
                result = "KEY2";

                dropDownField.getAllOptionValues(withNotNull(), (Delegator) any);
                result = optionValues;
            }
        };

        macroFormRenderer.renderDropDownField(appendable, ImmutableMap.of(), dropDownField);
        assertAndGetMacroString("renderDropDownField", ImmutableMap.of(
                "currentValue", "KEY2",
                "options", ImmutableList.of("{'key':'KEY1','description':'DESC1'}",
                        "{'key':'KEY2','description':'DESC2'}")));
    }

    @Test
    public void checkFieldMacroRendered(@Mocked ModelFormField.CheckField checkField) throws IOException {
        final List<ModelFormField.OptionValue> optionValues = ImmutableList.of(
                new ModelFormField.OptionValue("KEY1", "DESC1"),
                new ModelFormField.OptionValue("KEY2", "DESC2"));

        new Expectations() {
            {
                modelFormField.getEntry(withNotNull());
                result = "KEY2";

                checkField.getAllOptionValues(withNotNull(), (Delegator) any);
                result = optionValues;
            }
        };

        macroFormRenderer.renderCheckField(appendable, ImmutableMap.of(), checkField);
        assertAndGetMacroString("renderCheckField", ImmutableMap.of(
                "currentValue", "KEY2",
                "items", ImmutableList.of("{'value':'KEY1', 'description':'DESC1'}",
                        "{'value':'KEY2', 'description':'DESC2', 'checked':'true'}")));
    }

    @Test
    public void radioFieldMacroRendered(@Mocked ModelFormField.RadioField radioField) throws IOException {
        final List<ModelFormField.OptionValue> optionValues = ImmutableList.of(
                new ModelFormField.OptionValue("KEY1", "DESC1"),
                new ModelFormField.OptionValue("KEY2", "DESC2"));

        new Expectations() {
            {
                modelFormField.getEntry(withNotNull());
                result = "KEY2";

                radioField.getAllOptionValues(withNotNull(), (Delegator) any);
                result = optionValues;
            }
        };

        macroFormRenderer.renderRadioField(appendable, ImmutableMap.of(), radioField);
        assertAndGetMacroString("renderRadioField", ImmutableMap.of(
                "currentValue", "KEY2",
                "items", ImmutableList.of("{'key':'KEY1', 'description':'DESC1'}",
                        "{'key':'KEY2', 'description':'DESC2'}")));
    }

    @Test
    public void submitFieldMacroRendered(@Mocked ModelFormField.SubmitField submitField) throws IOException {
        new Expectations() {
            {
                modelFormField.getTitle(withNotNull());
                result = "BUTTONTITLE";
            }
        };

        macroFormRenderer.renderSubmitField(appendable, ImmutableMap.of(), submitField);
        assertAndGetMacroString("renderSubmitField", ImmutableMap.of("title", "BUTTONTITLE"));
    }

    @Test
    public void resetFieldMacroRendered(@Mocked ModelFormField.ResetField resetField) throws IOException {
        new Expectations() {
            {
                modelFormField.getTitle(withNotNull());
                result = "BUTTONTITLE";
            }
        };

        macroFormRenderer.renderResetField(appendable, ImmutableMap.of(), resetField);
        assertAndGetMacroString("renderResetField", ImmutableMap.of("title", "BUTTONTITLE"));
    }

    @Test
    public void hiddenFieldMacroRendered(@Mocked ModelFormField.HiddenField hiddenField) throws IOException {
        new Expectations() {
            {
                hiddenField.getValue(withNotNull());
                result = "HIDDENVALUE";
            }
        };

        macroFormRenderer.renderHiddenField(appendable, ImmutableMap.of(), hiddenField);
        assertAndGetMacroString("renderHiddenField", ImmutableMap.of("value", "HIDDENVALUE"));
    }

    @Test
    public void emptyFieldTitleMacroRendered() throws IOException {
        new Expectations() {
            {
                modelFormField.getTitle(withNotNull());
                result = " ";
            }
        };

        macroFormRenderer.renderFieldTitle(appendable, ImmutableMap.of(), modelFormField);
        assertAndGetMacroString("renderFormatEmptySpace");
    }

    @Test
    public void fieldTitleMacroRendered() throws IOException {
        new Expectations() {
            {
                modelFormField.getTitle(withNotNull());
                result = "FIELDTITLE";
            }
        };

        macroFormRenderer.renderFieldTitle(appendable, ImmutableMap.of(), modelFormField);
        assertAndGetMacroString("renderFieldTitle", ImmutableMap.of("title", "FIELDTITLE"));
    }

    @Test
    public void formOpenedMacroRendered(@Mocked ModelSingleForm modelSingleForm) throws IOException {
        new Expectations() {
            {
                modelSingleForm.getType();
                result = "single";
            }
        };

        macroFormRenderer.renderFormOpen(appendable, ImmutableMap.of(), modelSingleForm);
        assertAndGetMacroString("renderFormOpen", ImmutableMap.of("formType", "single"));
    }

    @Test
    public void formClosedMacroRendered(@Mocked ModelSingleForm modelSingleForm) throws IOException {
        macroFormRenderer.renderFormClose(appendable, ImmutableMap.of(), modelSingleForm);
        assertAndGetMacroString("renderFormClose");
    }

    @Test
    public void multiFormClosedMacroRendered(@Mocked ModelForm modelForm) throws IOException {
        macroFormRenderer.renderMultiFormClose(appendable, ImmutableMap.of(), modelForm);
        assertAndGetMacroString("renderMultiFormClose");
    }

    @Test
    public void listWrapperOpenMacroRendered(@Mocked ModelSingleForm modelSingleForm) throws IOException {
        macroFormRenderer.setRenderPagination(false);
        macroFormRenderer.renderFormatListWrapperOpen(appendable, new HashMap<>(), modelSingleForm);
        assertAndGetMacroString("renderFormatListWrapperOpen");
    }

    @Test
    public void emptyFormDataMacroRendered(@Mocked ModelSingleForm modelSingleForm) throws IOException {
        new Expectations() {
            {
                modelSingleForm.getEmptyFormDataMessage(withNotNull());
                result = "EMPTY";
            }
        };

        macroFormRenderer.renderEmptyFormDataMessage(appendable, new HashMap<>(), modelSingleForm);
        assertAndGetMacroString("renderEmptyFormDataMessage", ImmutableMap.of("message", "EMPTY"));
    }

    @Test
    public void listWrapperCloseMacroRendered(@Mocked ModelSingleForm modelSingleForm) throws IOException {
        macroFormRenderer.setRenderPagination(false);
        macroFormRenderer.renderFormatListWrapperClose(appendable, new HashMap<>(), modelSingleForm);
        assertAndGetMacroString("renderFormatListWrapperClose");
    }

    @Test
    public void itemRowOpenMacroRendered(@Mocked ModelForm modelForm) throws IOException {
        new Expectations() {
            {
                modelForm.getName();
                result = "FORMNAME";
                modelForm.getEvenRowStyle();
                result = "EVENSTYLE";
            }
        };

        macroFormRenderer.renderFormatItemRowOpen(appendable, ImmutableMap.of("itemIndex", 2), modelForm);
        assertAndGetMacroString("renderFormatItemRowOpen", ImmutableMap.of(
                "formName", "FORMNAME",
                "itemIndex", 2,
                "evenRowStyle", "EVENSTYLE"));
    }

    @Test
    public void itemRowCellOpenMacroRendered(@Mocked ModelForm modelForm,
                                             @Mocked ModelFormField modelFormField) throws IOException {
        new Expectations() {
            {
                modelFormField.getWidgetAreaStyle();
                result = "AREASTYLE";
                modelFormField.getName();
                result = "FIELDNAME";
            }
        };

        macroFormRenderer.renderFormatItemRowCellOpen(appendable, ImmutableMap.of(), modelForm, modelFormField, 2);
        assertAndGetMacroString("renderFormatItemRowCellOpen", ImmutableMap.of(
                "fieldName", "FIELDNAME",
                "positionSpan", 2,
                "style", "AREASTYLE"));
    }

    @Test
    public void itemRowFormCellOpenMacroRendered(@Mocked ModelForm modelForm) throws IOException {
        new Expectations() {
            {
                modelForm.getFormTitleAreaStyle();
                result = "AREASTYLE";
            }
        };

        macroFormRenderer.renderFormatItemRowFormCellOpen(appendable, ImmutableMap.of(), modelForm);
        assertAndGetMacroString("renderFormatItemRowFormCellOpen", ImmutableMap.of("style", "AREASTYLE"));
    }

    @Test
    public void singleWrapperOpenMacroRendered(@Mocked ModelForm modelForm) throws IOException {
        new Expectations() {
            {
                modelForm.getDefaultTableStyle();
                result = "STYLE${styleParam}";
                modelForm.getName();
                result = "FORMNAME";
            }
        };

        macroFormRenderer.renderFormatSingleWrapperOpen(appendable, ImmutableMap.of("styleParam", "ABCD"), modelForm);
        assertAndGetMacroString("renderFormatSingleWrapperOpen", ImmutableMap.of(
                "formName", "FORMNAME",
                "style", "STYLEABCD"));
    }

    @Test
    public void fieldRowWidgetCellOpenMacroRendered(@Mocked ModelFormField modelFormField) throws IOException {
        new Expectations() {
            {
                modelFormField.getWidgetAreaStyle();
                result = "AREASTYLE";
            }
        };

        macroFormRenderer.renderFormatFieldRowWidgetCellOpen(appendable, ImmutableMap.of(), modelFormField, 1, 1, null);
        assertAndGetMacroString("renderFormatFieldRowWidgetCellOpen", ImmutableMap.of(
                "positionSpan", 1,
                "style", "AREASTYLE"));
    }

    @Test
    public void textFindFieldMacroRendered(@Mocked ModelFormField modelFormField,
                                           @Mocked ModelFormField.TextFindField textFindField) throws IOException {
        new Expectations() {
            {
                textFindField.getModelFormField();
                result = modelFormField;

                textFindField.getHideOptions();
                result = true;

                modelFormField.getWidgetStyle();
                result = "WIDGETSTYLE";

                modelFormField.shouldBeRed(withNotNull());
                result = true;

                modelFormField.getParameterName(withNotNull());
                result = "FIELDNAME";
            }
        };

        ImmutableMap<String, Object> context = ImmutableMap.of();
        macroFormRenderer.renderTextFindField(appendable, context, textFindField);
        assertAndGetMacroString("renderTextFindField", ImmutableMap.of(
                "name", "FIELDNAME",
                "className", "WIDGETSTYLE",
                "alert", "true"));
    }

    @Test
    public void rangeFindFieldMacroRendered(@Mocked ModelFormField modelFormField,
                                            @Mocked ModelFormField.RangeFindField rangeFindField) throws IOException {
        new Expectations() {
            {
                rangeFindField.getModelFormField();
                result = modelFormField;

                modelFormField.getWidgetStyle();
                result = "WIDGETSTYLE";

                rangeFindField.getDefaultValue(withNotNull());
                result = "AAA";

                modelFormField.getEntry(withNotNull(), "AAA");
                result = "AAA";

                modelFormField.getEntry(withNotNull());
                result = "BBB";

                modelFormField.shouldBeRed(withNotNull());
                result = true;

                modelFormField.getParameterName(withNotNull());
                result = "FIELDNAME";
            }
        };

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
    public void dateFindFieldMacroRendered(@Mocked ModelFormField modelFormField,
                                           @Mocked ModelFormField.DateFindField dateFindField) throws IOException {
        new Expectations() {
            {
                dateFindField.getModelFormField();
                result = modelFormField;

                modelFormField.getEntry(withNotNull(), withNull());
                result = "2020-01-01";

                modelFormField.getParameterName(withNotNull());
                result = "FIELDNAME";
            }
        };

        ImmutableMap<String, Object> context = ImmutableMap.of();
        macroFormRenderer.renderDateFindField(appendable, context, dateFindField);
        assertAndGetMacroString("renderDateFindField", ImmutableMap.of(
                "name", "FIELDNAME",
                "value", "2020-01-01"));
    }

    @Test
    public void lookupFieldMacroRendered(@Mocked ModelFormField modelFormField,
                                         @Mocked ModelFormField.LookupField lookupField) throws IOException {
        new Expectations() {
            {
                httpSession.getAttribute("delegatorName");
                result = "delegator";

                lookupField.getModelFormField();
                result = modelFormField;

                modelFormField.getEntry(withNotNull(), withNull());
                result = "VALUE";

                modelFormField.getParameterName(withNotNull());
                result = "FIELDNAME";

                modelFormField.getCurrentContainerId(withNotNull());
                result = "CONTAINERID";
            }
        };

        ImmutableMap<String, Object> context = ImmutableMap.of("session", httpSession);
        macroFormRenderer.renderLookupField(appendable, context, lookupField);
        assertAndGetMacroString("renderLookupField", ImmutableMap.of(
                "name", "FIELDNAME",
                "value", "VALUE",
                "id", "CONTAINERID"));
    }

    @Test
    public void renderNextPrevMacroRendered(@Mocked ModelForm modelForm) throws IOException {
        final String targetService = ""; // Leave empty to avoid CSRF token generation.
        final String paginateIndexField = "PAGINATE_INDEX";
        final String paginateSizeField = "PAGINATE_SIZE";

        new Expectations() {
            {
                modelForm.getPaginateTarget(withNotNull());
                result = targetService;

                modelForm.getMultiPaginateIndexField(withNotNull());
                result = paginateIndexField;
            }
        };

        Map<String, Object> context = new HashMap<>();
        context.put("session", httpSession);
        context.put(paginateIndexField, 0);
        context.put(paginateSizeField, 30);
        macroFormRenderer.renderNextPrev(appendable, context, modelForm);

        assertAndGetMacroString("renderNextPrev");
    }

    @Test
    public void fileFieldMacroRendered(@Mocked ModelFormField.FileField fileField) throws IOException {

        new Expectations() {
            {
                fileField.getModelFormField();
                result = modelFormField;

                modelFormField.getParameterName(withNotNull());
                result = "FIELDNAME";

                modelFormField.getEntry(withNotNull(), null);
                result = "VALUE";

                modelFormField.getWidgetStyle();
                result = "WIDGETSTYLE";
            }
        };

        macroFormRenderer.renderFileField(appendable, ImmutableMap.of(), fileField);

        assertAndGetMacroString("renderFileField", ImmutableMap.of(
                "name", "FIELDNAME",
                "value", "VALUE",
                "className", "WIDGETSTYLE"));
    }

    @Test
    public void passwordFieldMacroRendered(@Mocked ModelFormField.PasswordField passwordField) throws IOException {

        new Expectations() {
            {
                passwordField.getModelFormField();
                result = modelFormField;

                modelFormField.getParameterName(withNotNull());
                result = "FIELDNAME";

                modelFormField.getEntry(withNotNull(), null);
                result = "VALUE";

                modelFormField.getWidgetStyle();
                result = "WIDGETSTYLE";
            }
        };

        macroFormRenderer.renderPasswordField(appendable, ImmutableMap.of(), passwordField);

        assertAndGetMacroString("renderPasswordField", ImmutableMap.of(
                "name", "FIELDNAME",
                "value", "VALUE",
                "className", "WIDGETSTYLE"));
    }

    @Test
    public void imageFieldMacroRendered(@Mocked ModelFormField.ImageField imageField) throws IOException {

        new Expectations() {
            {
                imageField.getModelFormField();
                result = modelFormField;

                modelFormField.getEntry(withNotNull(), null);
                result = "VALUE";
            }
        };

        macroFormRenderer.renderImageField(appendable, ImmutableMap.of(), imageField);

        assertAndGetMacroString("renderImageField", ImmutableMap.of("value", "VALUE"));
    }

    @Test
    public void fieldGroupOpenMacroRendered(@Mocked ModelForm.FieldGroup fieldGroup) throws IOException {
        new Expectations() {
            {
                renderableFtlFormElementsBuilder.fieldGroupOpen(withNotNull(), withNotNull());
                result = genericMacroCall;
            }
        };

        macroFormRenderer.renderFieldGroupOpen(appendable, ImmutableMap.of(), fieldGroup);
        genericSingleMacroRenderedVerification();
    }

    @Test
    public void fieldGroupCloseMacroRendered(@Mocked ModelForm.FieldGroup fieldGroup) throws IOException {
        new Expectations() {
            {
                renderableFtlFormElementsBuilder.fieldGroupClose(withNotNull(), withNotNull());
                result = genericMacroCall;
            }
        };

        macroFormRenderer.renderFieldGroupClose(appendable, ImmutableMap.of(), fieldGroup);
        genericSingleMacroRenderedVerification();
    }

    @Test
    public void sortFieldMacroRendered(@Mocked ModelForm modelForm) throws IOException {

        final String paginateTarget = "TARGET";

        new Expectations() {
            {
                modelFormField.getModelForm();
                result = modelForm;

                modelForm.getPaginateTarget(withNotNull());
                result = paginateTarget;

                modelFormField.getSortFieldHelpText(withNotNull());
                result = "HELPTEXT";
            }
        };

        final Map<String, Object> context = new HashMap<>();
        macroFormRenderer.renderSortField(appendable, context, modelFormField, "TITLE");

        assertAndGetMacroString("renderSortField", ImmutableMap.of("title", "TITLE"));
    }

    @Test
    public void containerRendererAsSingleMacro() throws IOException {
        new Expectations() {
            {
                renderableFtlFormElementsBuilder.containerMacroCall(withNotNull(), withNotNull());
                result = genericMacroCall;
            }
        };

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
    public void renderFormatListWrapperOpenPopulatesQueryString(@Mocked ModelSingleForm modelSingleForm)
            throws IOException {
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
    public void renderNextPrevUsesQueryString(@Mocked ModelForm modelForm) throws IOException {
        final String targetService = ""; // Leave empty to avoid CSRF token generation.
        final String qbeString = "field1=value1&amp;field2=value2+with+spaces";
        final String linkFromQbeString = "LinkFromQBEString";

        new Expectations() {
            {
                modelForm.getPaginateTarget(withNotNull());
                result = targetService;

                requestHandler.makeLink(null, null, withSubstring(qbeString));
                result = linkFromQbeString;
            }
        };

        final Map<String, Object> context = new HashMap<>();
        context.put("_QBESTRING_", qbeString);
        context.put("listSize", 100);
        macroFormRenderer.renderNextPrev(appendable, context, modelForm);

        assertAndGetMacroString("renderNextPrev", ImmutableMap.of("nextUrl", linkFromQbeString));
    }

    @Test
    public void renderSortFieldUsesQueryString(@Mocked ModelForm modelForm) throws IOException {
        final String paginateTarget = "TARGET";
        final String qbeString = "field2=value2 with spaces";
        final String linkFromQbeString = "LinkFromQBEString";

        new Expectations() {
            {
                modelFormField.getModelForm();
                result = modelForm;

                modelForm.getPaginateTarget(withNotNull());
                result = paginateTarget;

                requestHandler.makeLink(null, null, withSubstring(qbeString));
                result = linkFromQbeString;

                modelFormField.getSortFieldHelpText(withNotNull());
                result = "HELPTEXT";
            }
        };

        final Map<String, Object> context = new HashMap<>();
        context.put("_QBESTRING_", qbeString);
        context.put("listSize", 100);
        macroFormRenderer.renderSortField(appendable, context, modelFormField, "");

        assertAndGetMacroString("renderSortField", ImmutableMap.of(
                "linkUrl", new FreemarkerRawString(linkFromQbeString)));
    }

    private String assertAndGetMacroString(final String expectedName) {
        return assertAndGetMacroString(expectedName, ImmutableMap.of());
    }

    private String assertAndGetMacroString(final String expectedName, final Map<String, Object> expectedAttributes) {
        final String[] str = new String[1];

        new Verifications() {
            {
                List<String> macros = new ArrayList<>();
                ftlWriter.processFtlString(withNotNull(), withNull(), withCapture(macros));

                assertThat(macros, not(empty()));
                final String macro = macros.get(0);
                assertThat(macro, startsWith("<@" + expectedName));

                expectedAttributes.forEach((name, value) -> assertMacroAttribute(macro, name, value));

                str[0] = macro;
            }
        };

        return str[0];
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
        new Verifications() {
            {
                ftlWriter.processFtl(appendable, genericMacroCall);
            }
        };
    }

    private void genericTooltipRenderedExpectation(final FieldInfo fieldInfo) {
        new Expectations() {
            {
                fieldInfo.getModelFormField();
                result = modelFormField;

                renderableFtlFormElementsBuilder.tooltip(withNotNull(), modelFormField);
                result = genericTooltipMacroCall;
            }
        };
    }

    private void genericTooltipRenderedVerification() {
        new Verifications() {
            {
                ftlWriter.processFtl(appendable, genericTooltipMacroCall);
            }
        };
    }

    private void genericSubHyperlinkRenderedExpectation(final ModelFormField.SubHyperlink subHyperlink) {
        new Expectations() {
            {
                subHyperlink.shouldUse(withNotNull());
                result = true;

                subHyperlink.getStyle(withNotNull());
                result = "buttontext";

                subHyperlink.getUrlMode();
                result = "inter-app";

                subHyperlink.getTarget(withNotNull());
                result = "/path/to/target";

                subHyperlink.getDescription(withNotNull());
                result = "LinkDescription";
            }
        };
    }

    private void genericSubHyperlinkRenderedVerification() {
        new Verifications() {
            {
                ftlWriter.processFtl(appendable, genericHyperlinkMacroCall);
            }
        };
    }

    class FreeMarkerWorkerMockUp extends MockUp<FreeMarkerWorker> {
        @Mock
        public Template getTemplate(String templateLocation) {
            return template;
        }

        @Mock
        public Environment renderTemplate(Template template, Map<String, Object> context, Appendable outWriter) {
            return environment;
        }
    }

    class ThemeFactoryMockUp extends MockUp<ThemeFactory> {
        @Mock
        public VisualTheme resolveVisualTheme(HttpServletRequest request) {
            return visualTheme;
        }
    }

    class RequestHandlerMockUp extends MockUp<RequestHandler> {
        @Mock
        public RequestHandler from(HttpServletRequest request) {
            return requestHandler;
        }
    }

    class UtilHttpMockUp extends MockUp<UtilHttp> {
        @Mock
        public boolean isJavaScriptEnabled(HttpServletRequest request) {
            return true;
        }
    }

    class SimpleEncoderMockUp extends MockUp<SimpleEncoder> {
        @Mock
        public String encode(String original) {
            return original;
        }
    }

    class UtilPropertiesMockUp extends MockUp<UtilProperties> {

        @Mock
        public String getMessage(String resource, String name, Locale locale) {
            return name + "_MESSAGE";
        }
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
