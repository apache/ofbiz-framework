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
package org.apache.ofbiz.widget.renderer.fo;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.widget.WidgetWorker;
import org.apache.ofbiz.widget.model.FieldInfo;
import org.apache.ofbiz.widget.model.ModelForm;
import org.apache.ofbiz.widget.model.ModelFormField;
import org.apache.ofbiz.widget.model.ModelFormField.CheckField;
import org.apache.ofbiz.widget.model.ModelFormField.ContainerField;
import org.apache.ofbiz.widget.model.ModelFormField.DateFindField;
import org.apache.ofbiz.widget.model.ModelFormField.DateTimeField;
import org.apache.ofbiz.widget.model.ModelFormField.DisplayField;
import org.apache.ofbiz.widget.model.ModelFormField.DropDownField;
import org.apache.ofbiz.widget.model.ModelFormField.FieldInfoWithOptions;
import org.apache.ofbiz.widget.model.ModelFormField.FileField;
import org.apache.ofbiz.widget.model.ModelFormField.HiddenField;
import org.apache.ofbiz.widget.model.ModelFormField.HyperlinkField;
import org.apache.ofbiz.widget.model.ModelFormField.IgnoredField;
import org.apache.ofbiz.widget.model.ModelFormField.ImageField;
import org.apache.ofbiz.widget.model.ModelFormField.LookupField;
import org.apache.ofbiz.widget.model.ModelFormField.MenuField;
import org.apache.ofbiz.widget.model.ModelFormField.PasswordField;
import org.apache.ofbiz.widget.model.ModelFormField.RadioField;
import org.apache.ofbiz.widget.model.ModelFormField.RangeFindField;
import org.apache.ofbiz.widget.model.ModelFormField.ResetField;
import org.apache.ofbiz.widget.model.ModelFormField.SubmitField;
import org.apache.ofbiz.widget.model.ModelFormField.TextField;
import org.apache.ofbiz.widget.model.ModelFormField.TextFindField;
import org.apache.ofbiz.widget.model.ModelFormField.TextareaField;
import org.apache.ofbiz.widget.model.ModelWidget;
import org.apache.ofbiz.widget.renderer.FormStringRenderer;
import org.apache.ofbiz.widget.renderer.html.HtmlWidgetRenderer;
import org.apache.ofbiz.widget.renderer.macro.MacroScreenRenderer;


/**
 * Widget Library - FO Form Renderer implementation
 *
 */
public class FoFormRenderer extends HtmlWidgetRenderer implements FormStringRenderer {

    private static final String MODULE = FoFormRenderer.class.getName();

    HttpServletRequest request;
    HttpServletResponse response;

    public FoFormRenderer() { }

    public FoFormRenderer(HttpServletRequest request, HttpServletResponse response) throws IOException {
        this.request = request;
        this.response = response;
    }

    private static void makeBlockString(Appendable writer, String widgetStyle, String text) throws IOException {
        writer.append("<fo:block");
        if (UtilValidate.isNotEmpty(widgetStyle)) {
            writer.append(" ");
            writer.append(MacroScreenRenderer.getFoStyle(widgetStyle));
        }
        writer.append(">");
        writer.append(UtilFormatOut.encodeXmlValue(text));
        writer.append("</fo:block>");
    }

    @Override
    public void renderDisplayField(Appendable writer, Map<String, Object> context, DisplayField displayField) throws IOException {
        ModelFormField modelFormField = displayField.getModelFormField();
        makeBlockString(writer, modelFormField.getWidgetStyle(), displayField.getDescription(context));
        appendWhitespace(writer);
    }

    @Override
    public void renderHyperlinkField(Appendable writer, Map<String, Object> context, HyperlinkField hyperlinkField) throws IOException {
        ModelFormField modelFormField = hyperlinkField.getModelFormField();
        makeBlockString(writer, modelFormField.getWidgetStyle(), hyperlinkField.getDescription(context));
        appendWhitespace(writer);
    }

    @Override
    public void renderMenuField(Appendable writer, Map<String, Object> context, MenuField menuField) throws IOException {
        menuField.renderFieldString(writer, context, null);
    }

    @Override
    public void renderTextField(Appendable writer, Map<String, Object> context, TextField textField) throws IOException {
        ModelFormField modelFormField = textField.getModelFormField();
        makeBlockString(writer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, textField.getDefaultValue(context)));
        appendWhitespace(writer);
    }

    @Override
    public void renderTextareaField(Appendable writer, Map<String, Object> context, TextareaField textareaField) throws IOException {
        ModelFormField modelFormField = textareaField.getModelFormField();
        makeBlockString(writer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, textareaField.getDefaultValue(context)));
        appendWhitespace(writer);
    }

    @Override
    public void renderDateTimeField(Appendable writer, Map<String, Object> context, DateTimeField dateTimeField) throws IOException {
        ModelFormField modelFormField = dateTimeField.getModelFormField();
        makeBlockString(writer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, dateTimeField.getDefaultValue(context)));
        appendWhitespace(writer);
    }

    @Override
    public void renderDropDownField(Appendable writer, Map<String, Object> context, DropDownField dropDownField) throws IOException {
        ModelFormField modelFormField = dropDownField.getModelFormField();
        String currentValue = modelFormField.getEntry(context);
        List<ModelFormField.OptionValue> allOptionValues = dropDownField.getAllOptionValues(context, WidgetWorker.getDelegator(context));
        // if the current value should go first, display it
        if (UtilValidate.isNotEmpty(currentValue) && "first-in-list".equals(dropDownField.getCurrent())) {
            String explicitDescription = dropDownField.getCurrentDescription(context);
            if (UtilValidate.isNotEmpty(explicitDescription)) {
                makeBlockString(writer, modelFormField.getWidgetStyle(), explicitDescription);
            } else {
                makeBlockString(writer, modelFormField.getWidgetStyle(), FieldInfoWithOptions.getDescriptionForOptionKey(currentValue, allOptionValues));
            }
        } else {
            boolean optionSelected = false;
            for (ModelFormField.OptionValue optionValue : allOptionValues) {
                String noCurrentSelectedKey = dropDownField.getNoCurrentSelectedKey(context);
                if ((UtilValidate.isNotEmpty(currentValue) && currentValue.equals(optionValue.getKey()) && "selected".equals(dropDownField.getCurrent()))
                        || (UtilValidate.isEmpty(currentValue) && noCurrentSelectedKey != null && noCurrentSelectedKey.equals(optionValue.getKey()))) {
                    makeBlockString(writer, modelFormField.getWidgetStyle(), optionValue.getDescription());
                    optionSelected = true;
                    break;
                }
            }
            if (!optionSelected) {
                makeBlockString(writer, null, "");
            }
        }
        appendWhitespace(writer);
    }

    @Override
    public void renderCheckField(Appendable writer, Map<String, Object> context, CheckField checkField) throws IOException {
        makeBlockString(writer, null, "");
    }

    @Override
    public void renderRadioField(Appendable writer, Map<String, Object> context, RadioField radioField) throws IOException {
        makeBlockString(writer, null, "");
    }

    @Override
    public void renderSubmitField(Appendable writer, Map<String, Object> context, SubmitField submitField) throws IOException {
        makeBlockString(writer, null, "");
    }

    @Override
    public void renderResetField(Appendable writer, Map<String, Object> context, ResetField resetField) throws IOException {
        makeBlockString(writer, null, "");
    }

    @Override
    public void renderHiddenField(Appendable writer, Map<String, Object> context, HiddenField hiddenField) throws IOException {
    }

    @Override
    public void renderHiddenField(Appendable writer, Map<String, Object> context, ModelFormField modelFormField, String value) throws IOException {
    }

    @Override
    public void renderIgnoredField(Appendable writer, Map<String, Object> context, IgnoredField ignoredField) throws IOException {
    }

    @Override
    public void renderFieldTitle(Appendable writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
        String tempTitleText = modelFormField.getTitle(context);
        writer.append(tempTitleText);
    }

    @Override
    public void renderSingleFormFieldTitle(Appendable writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
        renderFieldTitle(writer, context, modelFormField);
    }

    @Override
    public void renderFormOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        this.widgetCommentsEnabled = ModelWidget.widgetBoundaryCommentsEnabled(context);
        renderBeginningBoundaryComment(writer, "Form Widget", modelForm);
    }

    @Override
    public void renderFormClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        renderEndingBoundaryComment(writer, "Form Widget", modelForm);
    }

    @Override
    public void renderMultiFormClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        renderEndingBoundaryComment(writer, "Form Widget", modelForm);
    }

    @Override
    public void renderFormatListWrapperOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.append("<fo:table border=\"solid black\">");
        List<ModelFormField> childFieldList = modelForm.getFieldList();
        for (ModelFormField childField : childFieldList) {
            int childFieldType = childField.getFieldInfo().getFieldType();
            if (childFieldType == FieldInfo.HIDDEN || childFieldType == FieldInfo.IGNORED) {
                continue;
            }
            writer.append("<fo:table-column");
            String areaStyle = childField.getTitleAreaStyle();
            if (UtilValidate.isNotEmpty(areaStyle)) {
                writer.append(" ");
                writer.append(MacroScreenRenderer.getFoStyle(areaStyle));
            }
            writer.append("/>");
            appendWhitespace(writer);
        }
        appendWhitespace(writer);
    }

    @Override
    public void renderFormatListWrapperClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.append("</fo:table-body>");
        writer.append("</fo:table>");
        appendWhitespace(writer);
    }
    @Override
    public void renderFormatHeaderOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.append("<fo:table-header>");
        appendWhitespace(writer);
    }
    @Override
    public void renderFormatHeaderClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.append("  </fo:table-header>");
        writer.append("  <fo:table-body>");
        // FIXME: this is an hack to avoid FOP rendering errors for empty lists (fo:table-body cannot be null)
        writer.append("<fo:table-row><fo:table-cell><fo:block/></fo:table-cell></fo:table-row>");
        appendWhitespace(writer);
    }

    @Override
    public void renderFormatHeaderRowOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.append("<fo:table-row>");
        appendWhitespace(writer);
    }

    @Override
    public void renderFormatHeaderRowClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.append("</fo:table-row>");
        appendWhitespace(writer);
    }

    @Override
    public void renderFormatHeaderRowCellOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField, int positionSpan) throws IOException {
        writer.append("<fo:table-cell ");
        if (positionSpan > 1) {
            writer.append("number-columns-spanned=\"");
            writer.append(Integer.toString(positionSpan));
            writer.append("\" ");
        }
        writer.append("font-weight=\"bold\" text-align=\"center\" border=\"solid black\" padding=\"2pt\"");
        writer.append(">");
        writer.append("<fo:block>");
        appendWhitespace(writer);
    }

    @Override
    public void renderFormatHeaderRowCellClose(Appendable writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField) throws IOException {
        writer.append("</fo:block>");
        writer.append("</fo:table-cell>");
        appendWhitespace(writer);
    }

    @Override
    public void renderFormatHeaderRowFormCellOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.append("<fo:table-cell>");
        appendWhitespace(writer);
    }

    @Override
    public void renderFormatHeaderRowFormCellClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.append("</fo:table-cell>");
        appendWhitespace(writer);
    }

    @Override
    public void renderFormatHeaderRowFormCellTitleSeparator(Appendable writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField, boolean isLast) throws IOException {
    }

    @Override
    public void renderFormatItemRowOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.append("<fo:table-row>");
        appendWhitespace(writer);
    }

    @Override
    public void renderFormatItemRowClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.append("</fo:table-row>");
        appendWhitespace(writer);
    }

    @Override
    public void renderFormatItemRowCellOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField, int positionSpan) throws IOException {
        writer.append("<fo:table-cell ");
        if (positionSpan > 1) {
            writer.append("number-columns-spanned=\"");
            writer.append(Integer.toString(positionSpan));
            writer.append("\" ");
        }
        String areaStyle = modelFormField.getWidgetAreaStyle();
        if (UtilValidate.isEmpty(areaStyle)) {
            areaStyle = "tabletext";
        }
        writer.append(MacroScreenRenderer.getFoStyle(areaStyle));
        writer.append(">");
        appendWhitespace(writer);
    }

    @Override
    public void renderFormatItemRowCellClose(Appendable writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField) throws IOException {
        writer.append("</fo:table-cell>");
        appendWhitespace(writer);
    }

    @Override
    public void renderFormatItemRowFormCellOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.append("<fo:table-cell>");
        appendWhitespace(writer);
    }

    @Override
    public void renderFormatItemRowFormCellClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.append("</fo:table-cell>");
        appendWhitespace(writer);
    }

    // TODO: multi columns (position attribute) in single forms are still not implemented
    @Override
    public void renderFormatSingleWrapperOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.append("<fo:table>");
        appendWhitespace(writer);
        writer.append("<fo:table-column column-width=\"2in\"/>");
        appendWhitespace(writer);
        writer.append("<fo:table-column/>");
        appendWhitespace(writer);
        writer.append("<fo:table-body>");
        appendWhitespace(writer);
    }
    @Override
    public void renderFormatSingleWrapperClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.append("</fo:table-body>");
        writer.append("</fo:table>");
        appendWhitespace(writer);
    }

    @Override
    public void renderFormatFieldRowOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.append("<fo:table-row>");
        appendWhitespace(writer);
    }

    @Override
    public void renderFormatFieldRowClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.append("</fo:table-row>");
        appendWhitespace(writer);
    }


    @Override
    public void renderFormatFieldRowTitleCellOpen(Appendable writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
        writer.append("<fo:table-cell font-weight=\"bold\" text-align=\"right\" padding=\"3pt\">");
        writer.append("<fo:block>");
        appendWhitespace(writer);
    }

    @Override
    public void renderFormatFieldRowTitleCellClose(Appendable writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
        writer.append("</fo:block>");
        writer.append("</fo:table-cell>");
        appendWhitespace(writer);
    }

    @Override
    public void renderFormatFieldRowSpacerCell(Appendable writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
    }

    @Override
    public void renderFormatFieldRowWidgetCellOpen(Appendable writer, Map<String, Object> context, ModelFormField modelFormField, int positions, int positionSpan, Integer nextPositionInRow) throws IOException {
        writer.append("<fo:table-cell text-align=\"left\" padding=\"2pt\" padding-left=\"5pt\">");
        appendWhitespace(writer);
    }

    @Override
    public void renderFormatFieldRowWidgetCellClose(Appendable writer, Map<String, Object> context, ModelFormField modelFormField, int positions, int positionSpan, Integer nextPositionInRow) throws IOException {
        writer.append("</fo:table-cell>");
        appendWhitespace(writer);
    }

    @Override
    public void renderFormatEmptySpace(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        // TODO
    }

    @Override
    public void renderTextFindField(Appendable writer, Map<String, Object> context, TextFindField textFindField) throws IOException {
        ModelFormField modelFormField = textFindField.getModelFormField();
        makeBlockString(writer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, textFindField.getDefaultValue(context)));
        appendWhitespace(writer);
    }

    @Override
    public void renderRangeFindField(Appendable writer, Map<String, Object> context, RangeFindField rangeFindField) throws IOException {
        ModelFormField modelFormField = rangeFindField.getModelFormField();
        makeBlockString(writer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, rangeFindField.getDefaultValue(context)));
        appendWhitespace(writer);
    }

    @Override
    public void renderDateFindField(Appendable writer, Map<String, Object> context, DateFindField dateFindField) throws IOException {
        ModelFormField modelFormField = dateFindField.getModelFormField();
        makeBlockString(writer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, dateFindField.getDefaultValue(context)));
        appendWhitespace(writer);
    }

    @Override
    public void renderLookupField(Appendable writer, Map<String, Object> context, LookupField lookupField) throws IOException {
        ModelFormField modelFormField = lookupField.getModelFormField();
        makeBlockString(writer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, lookupField.getDefaultValue(context)));
        appendWhitespace(writer);
    }

    public void renderNextPrev(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
    }

    @Override
    public void renderFileField(Appendable writer, Map<String, Object> context, FileField textField) throws IOException {
        ModelFormField modelFormField = textField.getModelFormField();
        makeBlockString(writer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, textField.getDefaultValue(context)));
        appendWhitespace(writer);
    }

    @Override
    public void renderPasswordField(Appendable writer, Map<String, Object> context, PasswordField passwordField) throws IOException {
        makeBlockString(writer, null, "");
    }

    @Override
    public void renderImageField(Appendable writer, Map<String, Object> context, ImageField imageField) throws IOException {
        // TODO
        makeBlockString(writer, null, "");
    }

    @Override
    public void renderFieldGroupOpen(Appendable writer, Map<String, Object> context, ModelForm.FieldGroup fieldGroup) throws IOException {
        // TODO
    }

    @Override
    public void renderFieldGroupClose(Appendable writer, Map<String, Object> context, ModelForm.FieldGroup fieldGroup) throws IOException {
        // TODO
    }

    @Override
    public void renderBanner(Appendable writer, Map<String, Object> context, ModelForm.Banner banner) throws IOException {
        // TODO
        makeBlockString(writer, null, "");
    }

    public void renderHyperlinkTitle(Appendable writer, Map<String, Object> context, ModelFormField modelFormField, String titleText) throws IOException {
    }

    @Override
    public void renderContainerFindField(Appendable writer, Map<String, Object> context, ContainerField containerField) throws IOException {
    }
    @Override
    public void renderEmptyFormDataMessage(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        // TODO
    }
}
