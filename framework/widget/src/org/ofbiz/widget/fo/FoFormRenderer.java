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
package org.ofbiz.widget.fo;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.widget.form.FormStringRenderer;
import org.ofbiz.widget.form.ModelForm;
import org.ofbiz.widget.form.ModelFormField;
import org.ofbiz.widget.form.ModelFormField.CheckField;
import org.ofbiz.widget.form.ModelFormField.DateFindField;
import org.ofbiz.widget.form.ModelFormField.DateTimeField;
import org.ofbiz.widget.form.ModelFormField.DisplayField;
import org.ofbiz.widget.form.ModelFormField.DropDownField;
import org.ofbiz.widget.form.ModelFormField.FileField;
import org.ofbiz.widget.form.ModelFormField.HiddenField;
import org.ofbiz.widget.form.ModelFormField.HyperlinkField;
import org.ofbiz.widget.form.ModelFormField.IgnoredField;
import org.ofbiz.widget.form.ModelFormField.ImageField;
import org.ofbiz.widget.form.ModelFormField.LookupField;
import org.ofbiz.widget.form.ModelFormField.PasswordField;
import org.ofbiz.widget.form.ModelFormField.RadioField;
import org.ofbiz.widget.form.ModelFormField.RangeFindField;
import org.ofbiz.widget.form.ModelFormField.ResetField;
import org.ofbiz.widget.form.ModelFormField.SubmitField;
import org.ofbiz.widget.form.ModelFormField.TextField;
import org.ofbiz.widget.form.ModelFormField.TextFindField;
import org.ofbiz.widget.form.ModelFormField.TextareaField;
import org.ofbiz.widget.html.HtmlWidgetRenderer;


/**
 * Widget Library - FO Form Renderer implementation
 *
 */
public class FoFormRenderer extends HtmlWidgetRenderer implements FormStringRenderer {

    public static final String module = FoFormRenderer.class.getName();
    
    HttpServletRequest request;
    HttpServletResponse response;

    public FoFormRenderer() {}

    public FoFormRenderer(HttpServletRequest request, HttpServletResponse response) throws IOException {
        this.request = request;
        this.response = response;
    }

    private void makeBlockString(Writer writer, String widgetStyle, String text) throws IOException {
        writer.write("<fo:block");
        if (UtilValidate.isNotEmpty(widgetStyle)) {
            writer.write(" ");
            writer.write(FoScreenRenderer.getFoStyle(widgetStyle));
        }
        writer.write(">");
        writer.write(UtilFormatOut.encodeXmlValue(text));
        writer.write("</fo:block>");
    }

    public void renderDisplayField(Writer writer, Map<String, Object> context, DisplayField displayField) throws IOException {
        ModelFormField modelFormField = displayField.getModelFormField();
        this.makeBlockString(writer, modelFormField.getWidgetStyle(), displayField.getDescription(context));
        appendWhitespace(writer);
    }

    public void renderHyperlinkField(Writer writer, Map<String, Object> context, HyperlinkField hyperlinkField) throws IOException {
        ModelFormField modelFormField = hyperlinkField.getModelFormField();
        this.makeBlockString(writer, modelFormField.getWidgetStyle(), hyperlinkField.getDescription(context));
        appendWhitespace(writer);
    }

    public void renderTextField(Writer writer, Map<String, Object> context, TextField textField) throws IOException {
        ModelFormField modelFormField = textField.getModelFormField();
        this.makeBlockString(writer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, textField.getDefaultValue(context)));
        appendWhitespace(writer);
    }

    public void renderTextareaField(Writer writer, Map<String, Object> context, TextareaField textareaField) throws IOException {
        ModelFormField modelFormField = textareaField.getModelFormField();
        this.makeBlockString(writer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, textareaField.getDefaultValue(context)));
        appendWhitespace(writer);
    }

    public void renderDateTimeField(Writer writer, Map<String, Object> context, DateTimeField dateTimeField) throws IOException {
        ModelFormField modelFormField = dateTimeField.getModelFormField();
        this.makeBlockString(writer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, dateTimeField.getDefaultValue(context)));
        appendWhitespace(writer);
    }

    public void renderDropDownField(Writer writer, Map<String, Object> context, DropDownField dropDownField) throws IOException {
        ModelFormField modelFormField = dropDownField.getModelFormField();
        ModelForm modelForm = modelFormField.getModelForm();
        String currentValue = modelFormField.getEntry(context);
        List<ModelFormField.OptionValue> allOptionValues = dropDownField.getAllOptionValues(context, modelForm.getDelegator(context));
        // if the current value should go first, display it
        if (UtilValidate.isNotEmpty(currentValue) && "first-in-list".equals(dropDownField.getCurrent())) {
            String explicitDescription = dropDownField.getCurrentDescription(context);
            if (UtilValidate.isNotEmpty(explicitDescription)) {
                this.makeBlockString(writer, modelFormField.getWidgetStyle(), explicitDescription);
            } else {
                this.makeBlockString(writer, modelFormField.getWidgetStyle(), ModelFormField.FieldInfoWithOptions.getDescriptionForOptionKey(currentValue, allOptionValues));
            }
        } else {
            boolean optionSelected = false;
            for (ModelFormField.OptionValue optionValue : allOptionValues) {
                String noCurrentSelectedKey = dropDownField.getNoCurrentSelectedKey(context);
                if ((UtilValidate.isNotEmpty(currentValue) && currentValue.equals(optionValue.getKey()) && "selected".equals(dropDownField.getCurrent())) ||
                        (UtilValidate.isEmpty(currentValue) && noCurrentSelectedKey != null && noCurrentSelectedKey.equals(optionValue.getKey()))) {
                    this.makeBlockString(writer, modelFormField.getWidgetStyle(), optionValue.getDescription());
                    optionSelected = true;
                    break;
                }
            }
            if (!optionSelected) {
                this.makeBlockString(writer, null, "");
            }
        }
        appendWhitespace(writer);
    }

    public void renderCheckField(Writer writer, Map<String, Object> context, CheckField checkField) throws IOException {
        this.makeBlockString(writer, null, "");
    }

    public void renderRadioField(Writer writer, Map<String, Object> context, RadioField radioField) throws IOException {
        this.makeBlockString(writer, null, "");
    }

    public void renderSubmitField(Writer writer, Map<String, Object> context, SubmitField submitField) throws IOException {
        this.makeBlockString(writer, null, "");
    }

    public void renderResetField(Writer writer, Map<String, Object> context, ResetField resetField) throws IOException {
        this.makeBlockString(writer, null, "");
    }

    public void renderHiddenField(Writer writer, Map<String, Object> context, HiddenField hiddenField) throws IOException {
    }

    public void renderHiddenField(Writer writer, Map<String, Object> context, ModelFormField modelFormField, String value) throws IOException {
    }

    public void renderIgnoredField(Writer writer, Map<String, Object> context, IgnoredField ignoredField) throws IOException {
    }

    public void renderFieldTitle(Writer writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
        String tempTitleText = modelFormField.getTitle(context);
        writer.write(tempTitleText);
    }

    public void renderSingleFormFieldTitle(Writer writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
        renderFieldTitle(writer, context, modelFormField);
    }

    public void renderFormOpen(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        renderBeginningBoundaryComment(writer, "Form Widget", modelForm);
    }

    public void renderFormClose(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        renderEndingBoundaryComment(writer, "Form Widget", modelForm);
    }

    public void renderMultiFormClose(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        renderEndingBoundaryComment(writer, "Form Widget", modelForm);
    }

    public void renderFormatListWrapperOpen(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.write("<fo:table border=\"solid black\">");
        List<ModelFormField> childFieldList = modelForm.getFieldList();
        for (ModelFormField childField : childFieldList) {
            int childFieldType = childField.getFieldInfo().getFieldType();
            if (childFieldType == ModelFormField.FieldInfo.HIDDEN || childFieldType == ModelFormField.FieldInfo.IGNORED) {
                continue;
            }
            writer.write("<fo:table-column");
            String areaStyle = childField.getTitleAreaStyle();
            if (UtilValidate.isNotEmpty(areaStyle)) {
                writer.write(" ");
                writer.write(FoScreenRenderer.getFoStyle(areaStyle));
            }
            writer.write("/>");
            appendWhitespace(writer);
        }
        appendWhitespace(writer);
    }

    public void renderFormatListWrapperClose(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.write("</fo:table-body>");
        writer.write("</fo:table>");
        appendWhitespace(writer);
    }

    public void renderFormatHeaderRowOpen(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.write("<fo:table-header>");
        writer.write("<fo:table-row>");
        appendWhitespace(writer);
    }

    public void renderFormatHeaderRowClose(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.write("</fo:table-row>");
        writer.write("</fo:table-header>");
        writer.write("<fo:table-body>");
        // FIXME: this is an hack to avoid FOP rendering errors for empty lists (fo:table-body cannot be null)
        writer.write("<fo:table-row><fo:table-cell><fo:block/></fo:table-cell></fo:table-row>");
        appendWhitespace(writer);
    }

    public void renderFormatHeaderRowCellOpen(Writer writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField, int positionSpan) throws IOException {
        writer.write("<fo:table-cell ");
        if (positionSpan > 1) {
            writer.write("number-columns-spanned=\"");
            writer.write(Integer.toString(positionSpan));
            writer.write("\" ");
        }
        writer.write("font-weight=\"bold\" text-align=\"center\" border=\"solid black\" padding=\"2pt\"");
        writer.write(">");
        writer.write("<fo:block>");
        appendWhitespace(writer);
    }

    public void renderFormatHeaderRowCellClose(Writer writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField) throws IOException {
        writer.write("</fo:block>");
        writer.write("</fo:table-cell>");
        appendWhitespace(writer);
    }

    public void renderFormatHeaderRowFormCellOpen(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.write("<fo:table-cell>");
        appendWhitespace(writer);
    }

    public void renderFormatHeaderRowFormCellClose(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.write("</fo:table-cell>");
        appendWhitespace(writer);
    }

    public void renderFormatHeaderRowFormCellTitleSeparator(Writer writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField, boolean isLast) throws IOException {
    }

    public void renderFormatItemRowOpen(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.write("<fo:table-row>");
        appendWhitespace(writer);
    }

    public void renderFormatItemRowClose(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.write("</fo:table-row>");
        appendWhitespace(writer);
    }

    public void renderFormatItemRowCellOpen(Writer writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField, int positionSpan) throws IOException {
        writer.write("<fo:table-cell ");
        if (positionSpan > 1) {
            writer.write("number-columns-spanned=\"");
            writer.write(Integer.toString(positionSpan));
            writer.write("\" ");
        }
        String areaStyle = modelFormField.getWidgetAreaStyle();
        if (UtilValidate.isEmpty(areaStyle)) {
            areaStyle = "tabletext";
        }
        writer.write(FoScreenRenderer.getFoStyle(areaStyle));
        writer.write(">");
        appendWhitespace(writer);
    }

    public void renderFormatItemRowCellClose(Writer writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField) throws IOException {
        writer.write("</fo:table-cell>");
        appendWhitespace(writer);
    }

    public void renderFormatItemRowFormCellOpen(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.write("<fo:table-cell>");
        appendWhitespace(writer);
    }

    public void renderFormatItemRowFormCellClose(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.write("</fo:table-cell>");
        appendWhitespace(writer);
    }

    // TODO: multi columns (position attribute) in single forms are still not implemented
    public void renderFormatSingleWrapperOpen(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.write("<fo:table>");
        appendWhitespace(writer);
        writer.write("<fo:table-column column-width=\"2in\"/>");
        appendWhitespace(writer);
        writer.write("<fo:table-column/>");
        appendWhitespace(writer);
        writer.write("<fo:table-body>");
        appendWhitespace(writer);
    }
    public void renderFormatSingleWrapperClose(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.write("</fo:table-body>");
        writer.write("</fo:table>");
        appendWhitespace(writer);
    }

    public void renderFormatFieldRowOpen(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.write("<fo:table-row>");
        appendWhitespace(writer);
    }

    public void renderFormatFieldRowClose(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.write("</fo:table-row>");
        appendWhitespace(writer);
    }


    public void renderFormatFieldRowTitleCellOpen(Writer writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
        writer.write("<fo:table-cell font-weight=\"bold\" text-align=\"right\" padding=\"3pt\">");
        writer.write("<fo:block>");
        appendWhitespace(writer);
    }

    public void renderFormatFieldRowTitleCellClose(Writer writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
        writer.write("</fo:block>");
        writer.write("</fo:table-cell>");
        appendWhitespace(writer);
    }

    public void renderFormatFieldRowSpacerCell(Writer writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
    }

    public void renderFormatFieldRowWidgetCellOpen(Writer writer, Map<String, Object> context, ModelFormField modelFormField, int positions, int positionSpan, Integer nextPositionInRow) throws IOException {
        writer.write("<fo:table-cell text-align=\"left\" padding=\"2pt\" padding-left=\"5pt\">");
        appendWhitespace(writer);
    }

    public void renderFormatFieldRowWidgetCellClose(Writer writer, Map<String, Object> context, ModelFormField modelFormField, int positions, int positionSpan, Integer nextPositionInRow) throws IOException {
        writer.write("</fo:table-cell>");
        appendWhitespace(writer);
    }

    public void renderFormatEmptySpace(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        // TODO
    }

    public void renderTextFindField(Writer writer, Map<String, Object> context, TextFindField textFindField) throws IOException {
        ModelFormField modelFormField = textFindField.getModelFormField();
        this.makeBlockString(writer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, textFindField.getDefaultValue(context)));
        appendWhitespace(writer);
    }

    public void renderRangeFindField(Writer writer, Map<String, Object> context, RangeFindField rangeFindField) throws IOException {
        ModelFormField modelFormField = rangeFindField.getModelFormField();
        this.makeBlockString(writer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, rangeFindField.getDefaultValue(context)));
        appendWhitespace(writer);
    }

    public void renderDateFindField(Writer writer, Map<String, Object> context, DateFindField dateFindField) throws IOException {
        ModelFormField modelFormField = dateFindField.getModelFormField();
        this.makeBlockString(writer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, dateFindField.getDefaultValue(context)));
        appendWhitespace(writer);
    }

    public void renderLookupField(Writer writer, Map<String, Object> context, LookupField lookupField) throws IOException {
        ModelFormField modelFormField = lookupField.getModelFormField();
        this.makeBlockString(writer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, lookupField.getDefaultValue(context)));
        appendWhitespace(writer);
    }

    public void renderNextPrev(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
    }

    public void renderFileField(Writer writer, Map<String, Object> context, FileField textField) throws IOException {
        ModelFormField modelFormField = textField.getModelFormField();
        this.makeBlockString(writer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, textField.getDefaultValue(context)));
        appendWhitespace(writer);
    }

    public void renderPasswordField(Writer writer, Map<String, Object> context, PasswordField passwordField) throws IOException {
        this.makeBlockString(writer, null, "");
    }

    public void renderImageField(Writer writer, Map<String, Object> context, ImageField imageField) throws IOException {
        // TODO
        this.makeBlockString(writer, null, "");
    }

    public void renderFieldGroupOpen(Writer writer, Map<String, Object> context, ModelForm.FieldGroup fieldGroup) throws IOException {
        // TODO
    }

    public void renderFieldGroupClose(Writer writer, Map<String, Object> context, ModelForm.FieldGroup fieldGroup) throws IOException {
        // TODO
    }
    
    public void renderBanner(Writer writer, Map<String, Object> context, ModelForm.Banner banner) throws IOException {
        // TODO
        this.makeBlockString(writer, null, "");
    }
    
    public void renderHyperlinkTitle(Writer writer, Map<String, Object> context, ModelFormField modelFormField, String titleText) throws IOException {
    }
}
