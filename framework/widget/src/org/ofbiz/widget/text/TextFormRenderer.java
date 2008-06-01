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
package org.ofbiz.widget.text;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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


/**
 * Widget Library - Csv Form Renderer implementation
 *
 */
public class TextFormRenderer implements FormStringRenderer {

    public static final String module = TextFormRenderer.class.getName();
    
    HttpServletRequest request;
    HttpServletResponse response;

    protected TextFormRenderer() {}

    public TextFormRenderer(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    public void appendWhitespace(Writer writer) throws IOException {
        // appending line ends for now, but this could be replaced with a simple space or something
        writer.write("\r\n");
        //writer.write(' ');
    }

    private void makeTextString(Writer writer, String widgetStyle, String text) throws IOException {
        // TODO: escape characters here
        writer.write(text);
    }

    public void renderDisplayField(Writer writer, Map<String, Object> context, DisplayField displayField) throws IOException {
        ModelFormField modelFormField = displayField.getModelFormField();
        this.makeTextString(writer, modelFormField.getWidgetStyle(), displayField.getDescription(context));
    }

    public void renderHyperlinkField(Writer writer, Map<String, Object> context, HyperlinkField hyperlinkField) throws IOException {
        ModelFormField modelFormField = hyperlinkField.getModelFormField();
        this.makeTextString(writer, modelFormField.getWidgetStyle(), hyperlinkField.getDescription(context));
    }

    public void renderTextField(Writer writer, Map<String, Object> context, TextField textField) throws IOException {
        ModelFormField modelFormField = textField.getModelFormField();
        this.makeTextString(writer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, textField.getDefaultValue(context)));
    }

    public void renderTextareaField(Writer writer, Map<String, Object> context, TextareaField textareaField) throws IOException {
        ModelFormField modelFormField = textareaField.getModelFormField();
        this.makeTextString(writer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, textareaField.getDefaultValue(context)));
    }

    public void renderDateTimeField(Writer writer, Map<String, Object> context, DateTimeField dateTimeField) throws IOException {
        ModelFormField modelFormField = dateTimeField.getModelFormField();
        this.makeTextString(writer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, dateTimeField.getDefaultValue(context)));
    }

    public void renderDropDownField(Writer writer, Map<String, Object> context, DropDownField dropDownField) throws IOException {
        ModelFormField modelFormField = dropDownField.getModelFormField();
        ModelForm modelForm = modelFormField.getModelForm();
        String currentValue = modelFormField.getEntry(context);
        List allOptionValues = dropDownField.getAllOptionValues(context, modelForm.getDelegator(context));
        // if the current value should go first, display it
        if (UtilValidate.isNotEmpty(currentValue) && "first-in-list".equals(dropDownField.getCurrent())) {
            String explicitDescription = dropDownField.getCurrentDescription(context);
            if (UtilValidate.isNotEmpty(explicitDescription)) {
                this.makeTextString(writer, modelFormField.getWidgetStyle(), explicitDescription);
            } else {
                this.makeTextString(writer, modelFormField.getWidgetStyle(), ModelFormField.FieldInfoWithOptions.getDescriptionForOptionKey(currentValue, allOptionValues));
            }
        } else {
            Iterator optionValueIter = allOptionValues.iterator();
            while (optionValueIter.hasNext()) {
                ModelFormField.OptionValue optionValue = (ModelFormField.OptionValue) optionValueIter.next();
                String noCurrentSelectedKey = dropDownField.getNoCurrentSelectedKey(context);
                if ((UtilValidate.isNotEmpty(currentValue) && currentValue.equals(optionValue.getKey()) && "selected".equals(dropDownField.getCurrent())) ||
                        (UtilValidate.isEmpty(currentValue) && noCurrentSelectedKey != null && noCurrentSelectedKey.equals(optionValue.getKey()))) {
                    this.makeTextString(writer, modelFormField.getWidgetStyle(), optionValue.getDescription());
                    break;
                }
            }
        }
    }

    public void renderCheckField(Writer writer, Map<String, Object> context, CheckField checkField) {
    }

    public void renderRadioField(Writer writer, Map<String, Object> context, RadioField radioField) {
    }

    public void renderSubmitField(Writer writer, Map<String, Object> context, SubmitField submitField) {
    }

    public void renderResetField(Writer writer, Map<String, Object> context, ResetField resetField) {
    }

    public void renderHiddenField(Writer writer, Map<String, Object> context, HiddenField hiddenField) {
    }

    public void renderHiddenField(Writer writer, Map<String, Object> context, ModelFormField modelFormField, String value) {
    }

    public void renderIgnoredField(Writer writer, Map<String, Object> context, IgnoredField ignoredField) {
    }

    public void renderFieldTitle(Writer writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
        this.makeTextString(writer, modelFormField.getWidgetStyle(), modelFormField.getTitle(context));
    }

    public void renderSingleFormFieldTitle(Writer writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
        renderFieldTitle(writer, context, modelFormField);
    }

    public void renderFormOpen(Writer writer, Map<String, Object> context, ModelForm modelForm) {
    }

    public void renderFormClose(Writer writer, Map<String, Object> context, ModelForm modelForm) {
    }

    public void renderMultiFormClose(Writer writer, Map<String, Object> context, ModelForm modelForm) {
    }

    public void renderFormatListWrapperOpen(Writer writer, Map<String, Object> context, ModelForm modelForm) {
    }

    public void renderFormatListWrapperClose(Writer writer, Map<String, Object> context, ModelForm modelForm) {
    }

    public void renderFormatHeaderRowOpen(Writer writer, Map<String, Object> context, ModelForm modelForm) {
    }

    public void renderFormatHeaderRowClose(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        this.appendWhitespace(writer);
    }

    public void renderFormatHeaderRowCellOpen(Writer writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField, int positionSpan) {
    }

    public void renderFormatHeaderRowCellClose(Writer writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField) {
    }

    public void renderFormatHeaderRowFormCellOpen(Writer writer, Map<String, Object> context, ModelForm modelForm) {
    }

    public void renderFormatHeaderRowFormCellClose(Writer writer, Map<String, Object> context, ModelForm modelForm) {
    }

    public void renderFormatHeaderRowFormCellTitleSeparator(Writer writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField, boolean isLast) {
    }

    public void renderFormatItemRowOpen(Writer writer, Map<String, Object> context, ModelForm modelForm) {
    }

    public void renderFormatItemRowClose(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        this.appendWhitespace(writer);
    }

    public void renderFormatItemRowCellOpen(Writer writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField, int positionSpan) {
    }

    public void renderFormatItemRowCellClose(Writer writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField) {
    }

    public void renderFormatItemRowFormCellOpen(Writer writer, Map<String, Object> context, ModelForm modelForm) {
    }

    public void renderFormatItemRowFormCellClose(Writer writer, Map<String, Object> context, ModelForm modelForm) {
    }

    public void renderFormatSingleWrapperOpen(Writer writer, Map<String, Object> context, ModelForm modelForm) {
    }

    public void renderFormatSingleWrapperClose(Writer writer, Map<String, Object> context, ModelForm modelForm) {
    }

    public void renderFormatFieldRowOpen(Writer writer, Map<String, Object> context, ModelForm modelForm) {
    }

    public void renderFormatFieldRowClose(Writer writer, Map<String, Object> context, ModelForm modelForm) {
    }

    public void renderFormatFieldRowTitleCellOpen(Writer writer, Map<String, Object> context, ModelFormField modelFormField) {
    }

    public void renderFormatFieldRowTitleCellClose(Writer writer, Map<String, Object> context, ModelFormField modelFormField) {
    }

    public void renderFormatFieldRowSpacerCell(Writer writer, Map<String, Object> context, ModelFormField modelFormField) {
    }

    public void renderFormatFieldRowWidgetCellOpen(Writer writer, Map<String, Object> context, ModelFormField modelFormField, int positions, int positionSpan, Integer nextPositionInRow) {
    }

    public void renderFormatFieldRowWidgetCellClose(Writer writer, Map<String, Object> context, ModelFormField modelFormField, int positions, int positionSpan, Integer nextPositionInRow) {
    }

    public void renderFormatEmptySpace(Writer writer, Map<String, Object> context, ModelForm modelForm) {
        // TODO
    }

    public void renderTextFindField(Writer writer, Map<String, Object> context, TextFindField textFindField) throws IOException {
        ModelFormField modelFormField = textFindField.getModelFormField();
        this.makeTextString(writer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, textFindField.getDefaultValue(context)));
    }

    public void renderRangeFindField(Writer writer, Map<String, Object> context, RangeFindField rangeFindField) throws IOException {
        ModelFormField modelFormField = rangeFindField.getModelFormField();
        this.makeTextString(writer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, rangeFindField.getDefaultValue(context)));
    }

    public void renderDateFindField(Writer writer, Map<String, Object> context, DateFindField dateFindField) throws IOException {
        ModelFormField modelFormField = dateFindField.getModelFormField();
        this.makeTextString(writer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, dateFindField.getDefaultValue(context)));
    }

    public void renderLookupField(Writer writer, Map<String, Object> context, LookupField lookupField) throws IOException {
        ModelFormField modelFormField = lookupField.getModelFormField();
        this.makeTextString(writer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, lookupField.getDefaultValue(context)));
    }

    public void renderNextPrev(Writer writer, Map<String, Object> context, ModelForm modelForm) {
    }

    public void renderFileField(Writer writer, Map<String, Object> context, FileField textField) throws IOException {
        ModelFormField modelFormField = textField.getModelFormField();
        this.makeTextString(writer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, textField.getDefaultValue(context)));
    }

    public void renderPasswordField(Writer writer, Map<String, Object> context, PasswordField passwordField) {
    }

    public void renderImageField(Writer writer, Map<String, Object> context, ImageField imageField) {
        // TODO
    }

    public void renderFieldGroupOpen(Writer writer, Map<String, Object> context, ModelForm.FieldGroup fieldGroup) {
        // TODO
    }

    public void renderFieldGroupClose(Writer writer, Map<String, Object> context, ModelForm.FieldGroup fieldGroup) {
        // TODO
    }
    
    public void renderBanner(Writer writer, Map<String, Object> context, ModelForm.Banner banner) {
        // TODO
    }
    
    public void renderHyperlinkTitle(Writer writer, Map<String, Object> context, ModelFormField modelFormField, String titleText) {
    }
}
