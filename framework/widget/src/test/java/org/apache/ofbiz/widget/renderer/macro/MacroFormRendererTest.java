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
import org.apache.ofbiz.webapp.control.ConfigXMLReader;
import org.apache.ofbiz.webapp.control.RequestHandler;
import org.apache.ofbiz.widget.model.ModelForm;
import org.apache.ofbiz.widget.model.ModelFormField;
import org.apache.ofbiz.widget.model.ModelScreenWidget;
import org.apache.ofbiz.widget.model.ModelSingleForm;
import org.apache.ofbiz.widget.model.ThemeFactory;
import org.apache.ofbiz.widget.renderer.VisualTheme;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;

public class MacroFormRendererTest {

    @Injectable
    private HttpServletRequest request;

    @Injectable
    private HttpServletResponse response;

    @Injectable
    private FtlWriter ftlWriter;

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

    private final StringWriter appendable = new StringWriter();

    @Injectable
    private String macroLibraryPath = null;

    @Tested
    private MacroFormRenderer macroFormRenderer;

    @Before
    public void setupMockups() {
        new FreeMarkerWorkerMockUp();
        new ThemeFactoryMockUp();
        new RequestHandlerMockUp();
        new UtilHttpMockUp();
        new UtilPropertiesMockUp();
    }

    @Test
    public void emptyLabelNotRendered(@Mocked ModelScreenWidget.Label label) {
        new Expectations() {
            {
                label.getText(withNotNull());
                result = "";

                ftlWriter.executeMacro(withNotNull(), withNotNull());
                times = 0;
            }
        };

        macroFormRenderer.renderLabel(appendable, ImmutableMap.of(), label);
    }

    @SuppressWarnings("checkstyle:InnerAssignment")
    @Test
    public void labelMacroRenderedWithText(@Mocked ModelScreenWidget.Label label) throws IOException {
        new Expectations() {
            {
                label.getText(withNotNull());
                result = "TEXT";
            }
        };

        macroFormRenderer.renderLabel(appendable, ImmutableMap.of(), label);

        assertAndGetMacroString("renderLabel", ImmutableMap.of("text", "TEXT"));
    }

    @Test
    public void displayFieldMacroRendered(@Mocked ModelFormField.DisplayField displayField) throws IOException {
        new Expectations() {
            {
                displayField.getType();
                result = "TYPE";

                displayField.getDescription(withNotNull());
                result = "DESCRIPTION";

                modelFormField.getTooltip(withNotNull());
                result = "TOOLTIP";
            }
        };

        macroFormRenderer.renderDisplayField(appendable, ImmutableMap.of(), displayField);

        assertAndGetMacroString("renderDisplayField", ImmutableMap.of("type", "TYPE"));
    }

    @Test
    public void displayEntityFieldMacroRenderedWithLink(@Mocked ModelFormField.DisplayEntityField displayEntityField,
                                                        @Mocked ModelFormField.SubHyperlink subHyperlink)
            throws IOException {

        final Map<String, ConfigXMLReader.RequestMap> requestMapMap = new HashMap<>();

        new Expectations() {
            {
                displayEntityField.getType();
                result = "TYPE";

                displayEntityField.getDescription(withNotNull());
                result = "DESCRIPTION";

                modelFormField.getTooltip(withNotNull());
                result = "TOOLTIP";

                displayEntityField.getSubHyperlink();
                result = subHyperlink;

                subHyperlink.getStyle(withNotNull());
                result = "TestLinkStyle";

                subHyperlink.getUrlMode();
                result = "url-mode";

                subHyperlink.shouldUse(withNotNull());
                result = true;

                subHyperlink.getDescription(withNotNull());
                result = "LinkDescription";

                subHyperlink.getTarget(withNotNull());
                result = "/link/target/path";

                request.getAttribute("requestMapMap");
                result = requestMapMap;
            }
        };

        Map<String, Object> context = new HashMap<>();
        macroFormRenderer.renderDisplayField(appendable, context, displayEntityField);

        System.out.println(appendable.toString());
        assertAndGetMacroString("renderDisplayField", ImmutableMap.of("type", "TYPE"));
    }

    @Test
    public void textFieldMacroRendered(@Mocked ModelFormField.TextField textField) throws IOException {
        new Expectations() {
            {
                httpSession.getAttribute("delegatorName");
                result = "delegator";

                modelFormField.getEntry(withNotNull(), anyString);
                result = "TEXTVALUE";

                modelFormField.getTooltip(withNotNull());
                result = "";
            }
        };

        macroFormRenderer.renderTextField(appendable, ImmutableMap.of("session", httpSession), textField);

        assertAndGetMacroString("renderTextField", ImmutableMap.of("value", "TEXTVALUE"));
    }

    @Test
    public void textRendererUsesContainerId(@Mocked ModelFormField.TextField textField)
            throws IOException {

        new Expectations() {
            {
                httpSession.getAttribute("delegatorName");
                result = "delegator";

                modelFormField.getTooltip(withNotNull());
                result = "";

                modelFormField.getCurrentContainerId(withNotNull());
                result = "CurrentTextId";

                new StringReader(withSubstring("id=\"CurrentTextId\""));
            }
        };

        macroFormRenderer.renderTextField(appendable, ImmutableMap.of("session", httpSession), textField);
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

                modelFormField.getTooltip(withNotNull());
                result = "";
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

                modelFormField.getTooltip(withNotNull());
                result = "";
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

                modelFormField.getTooltip(withNotNull());
                result = "";
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

                modelFormField.getTooltip(withNotNull());
                result = "";
            }
        };

        macroFormRenderer.renderCheckField(appendable, ImmutableMap.of(), checkField);
        assertAndGetMacroString("renderCheckField", ImmutableMap.of(
                "currentValue", "KEY2",
                "items", ImmutableList.of("{'value':'KEY1', 'description':'DESC1'}",
                        "{'value':'KEY2', 'description':'DESC2'}")));
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

                modelFormField.getTooltip(withNotNull());
                result = "";
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

                modelFormField.getTooltip(withNotNull());
                result = "";
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

                modelFormField.getTooltip(withNotNull());
                result = "";
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

                modelFormField.getTooltip(withNotNull());
                result = "";
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

                modelFormField.getTooltip(withNotNull());
                result = "";
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

                modelFormField.getTooltip(withNotNull());
                result = "";
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

                modelFormField.getTooltip(withNotNull());
                result = "";
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

                modelFormField.getTooltip(withNotNull());
                result = "TOOLTIP";
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

                modelFormField.getTooltip(withNotNull());
                result = "TOOLTIP";
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

                modelFormField.getTooltip(withNotNull());
                result = "TOOLTIP";
            }
        };

        macroFormRenderer.renderImageField(appendable, ImmutableMap.of(), imageField);

        assertAndGetMacroString("renderImageField", ImmutableMap.of("value", "VALUE"));
    }

    @Test
    public void fieldGroupOpenMacroRendered(@Mocked ModelForm.FieldGroup fieldGroup) throws IOException {

        new Expectations() {
            {
                fieldGroup.getStyle();
                result = "GROUPSTYLE";

                fieldGroup.getTitle();
                result = "TITLE${title}";

                fieldGroup.initiallyCollapsed();
                result = true;
            }
        };

        final Map<String, Object> context = new HashMap<>();
        context.put("title", "ABC");

        macroFormRenderer.renderFieldGroupOpen(appendable, context, fieldGroup);

        assertAndGetMacroString("renderFieldGroupOpen", ImmutableMap.of(
                "title", "TITLEABC",
                "collapsed", true));
    }

    @Test
    public void fieldGroupCloseMacroRendered(@Mocked ModelForm.FieldGroup fieldGroup) throws IOException {

        new Expectations() {
            {
                fieldGroup.getStyle();
                result = "GROUPSTYLE";

                fieldGroup.getTitle();
                result = "TITLE${title}";
            }
        };

        final Map<String, Object> context = new HashMap<>();
        context.put("title", "ABC");

        macroFormRenderer.renderFieldGroupClose(appendable, context, fieldGroup);

        assertAndGetMacroString("renderFieldGroupClose", ImmutableMap.of("title", "TITLEABC"));
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
    public void tooltipMacroRendered() throws IOException {
        new Expectations() {
            {
                modelFormField.getTooltip(withNotNull());
                result = "TOOLTIP\"With\"Quotes";

                modelFormField.getTooltipStyle();
                result = "TOOLTIPSTYLE";
            }
        };

        final Map<String, Object> context = new HashMap<>();
        macroFormRenderer.appendTooltip(appendable, context, modelFormField);

        assertAndGetMacroString("renderTooltip", ImmutableMap.of(
                "tooltip", "TOOLTIP\\\"With\\\"Quotes",
                "tooltipStyle", "TOOLTIPSTYLE"));
    }

    @Test
    public void asterisksMacroRendered() throws IOException {
        new Expectations() {
            {
                modelFormField.getRequiredField();
                result = true;

                modelFormField.getRequiredFieldStyle();
                result = "REQUIREDSTYLE";
            }
        };

        final Map<String, Object> context = new HashMap<>();
        macroFormRenderer.addAsterisks(appendable, context, modelFormField);

        assertAndGetMacroString("renderAsterisks", ImmutableMap.of(
                "requiredField", "true",
                "requiredStyle", "REQUIREDSTYLE"));
    }

    @Test
    public void containerRendererUsesContainerId() throws IOException {
        new Expectations() {
            {
                modelFormField.getCurrentContainerId(withNotNull());
                result = "CurrentContainerId";

                new StringReader(withSubstring("id=\"CurrentContainerId\""));
            }
        };

        macroFormRenderer.renderContainerFindField(appendable, ImmutableMap.of(), containerField);
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
                ftlWriter.executeMacro(withNotNull(), withCapture(macros));

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
            assertThat(macro, containsString(attributeName + "=r\"" + valueString + "\""));
        } else {
            assertThat(macro, containsString(attributeName + "=\"" + attributeValue + "\""));
        }
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
