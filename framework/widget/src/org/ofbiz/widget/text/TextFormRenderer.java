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

    public void appendWhitespace(StringBuffer buffer) {
        // appending line ends for now, but this could be replaced with a simple space or something
        buffer.append("\r\n");
        //buffer.append(' ');
    }

    private void makeTextString(StringBuffer buffer, String widgetStyle, String text) {
        // TODO: escape characters here
        buffer.append(text);
    }

    public void renderDisplayField(StringBuffer buffer, Map context, DisplayField displayField) {
        ModelFormField modelFormField = displayField.getModelFormField();
        this.makeTextString(buffer, modelFormField.getWidgetStyle(), displayField.getDescription(context));
    }

    public void renderHyperlinkField(StringBuffer buffer, Map context, HyperlinkField hyperlinkField) {
        ModelFormField modelFormField = hyperlinkField.getModelFormField();
        this.makeTextString(buffer, modelFormField.getWidgetStyle(), hyperlinkField.getDescription(context));
    }

    public void renderTextField(StringBuffer buffer, Map context, TextField textField) {
        ModelFormField modelFormField = textField.getModelFormField();
        this.makeTextString(buffer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, textField.getDefaultValue(context)));
    }

    public void renderTextareaField(StringBuffer buffer, Map context, TextareaField textareaField) {
        ModelFormField modelFormField = textareaField.getModelFormField();
        this.makeTextString(buffer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, textareaField.getDefaultValue(context)));
    }

    public void renderDateTimeField(StringBuffer buffer, Map context, DateTimeField dateTimeField) {
        ModelFormField modelFormField = dateTimeField.getModelFormField();
        this.makeTextString(buffer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, dateTimeField.getDefaultValue(context)));
    }

    public void renderDropDownField(StringBuffer buffer, Map context, DropDownField dropDownField) {
        ModelFormField modelFormField = dropDownField.getModelFormField();
        ModelForm modelForm = modelFormField.getModelForm();
        String currentValue = modelFormField.getEntry(context);
        List allOptionValues = dropDownField.getAllOptionValues(context, modelForm.getDelegator(context));
        // if the current value should go first, display it
        if (UtilValidate.isNotEmpty(currentValue) && "first-in-list".equals(dropDownField.getCurrent())) {
            String explicitDescription = dropDownField.getCurrentDescription(context);
            if (UtilValidate.isNotEmpty(explicitDescription)) {
                this.makeTextString(buffer, modelFormField.getWidgetStyle(), explicitDescription);
            } else {
                this.makeTextString(buffer, modelFormField.getWidgetStyle(), ModelFormField.FieldInfoWithOptions.getDescriptionForOptionKey(currentValue, allOptionValues));
            }
        } else {
            Iterator optionValueIter = allOptionValues.iterator();
            while (optionValueIter.hasNext()) {
                ModelFormField.OptionValue optionValue = (ModelFormField.OptionValue) optionValueIter.next();
                String noCurrentSelectedKey = dropDownField.getNoCurrentSelectedKey(context);
                if ((UtilValidate.isNotEmpty(currentValue) && currentValue.equals(optionValue.getKey()) && "selected".equals(dropDownField.getCurrent())) ||
                        (UtilValidate.isEmpty(currentValue) && noCurrentSelectedKey != null && noCurrentSelectedKey.equals(optionValue.getKey()))) {
                    this.makeTextString(buffer, modelFormField.getWidgetStyle(), optionValue.getDescription());
                    break;
                }
            }
        }
    }

    public void renderCheckField(StringBuffer buffer, Map context, CheckField checkField) {
    }

    public void renderRadioField(StringBuffer buffer, Map context, RadioField radioField) {
    }

    public void renderSubmitField(StringBuffer buffer, Map context, SubmitField submitField) {
    }

    public void renderResetField(StringBuffer buffer, Map context, ResetField resetField) {
    }

    public void renderHiddenField(StringBuffer buffer, Map context, HiddenField hiddenField) {
    }

    public void renderHiddenField(StringBuffer buffer, Map context, ModelFormField modelFormField, String value) {
    }

    public void renderIgnoredField(StringBuffer buffer, Map context, IgnoredField ignoredField) {
    }

    public void renderFieldTitle(StringBuffer buffer, Map context, ModelFormField modelFormField) {
        this.makeTextString(buffer, modelFormField.getWidgetStyle(), modelFormField.getTitle(context));
    }

    public void renderSingleFormFieldTitle(StringBuffer buffer, Map context, ModelFormField modelFormField) {
        renderFieldTitle(buffer, context, modelFormField);
    }

    public void renderFormOpen(StringBuffer buffer, Map context, ModelForm modelForm) {
    }

    public void renderFormClose(StringBuffer buffer, Map context, ModelForm modelForm) {
    }

    public void renderMultiFormClose(StringBuffer buffer, Map context, ModelForm modelForm) {
    }

    public void renderFormatListWrapperOpen(StringBuffer buffer, Map context, ModelForm modelForm) {
    }

    public void renderFormatListWrapperClose(StringBuffer buffer, Map context, ModelForm modelForm) {
    }

    public void renderFormatHeaderRowOpen(StringBuffer buffer, Map context, ModelForm modelForm) {
    }

    public void renderFormatHeaderRowClose(StringBuffer buffer, Map context, ModelForm modelForm) {
        this.appendWhitespace(buffer);
    }

    public void renderFormatHeaderRowCellOpen(StringBuffer buffer, Map context, ModelForm modelForm, ModelFormField modelFormField, int positionSpan) {
    }

    public void renderFormatHeaderRowCellClose(StringBuffer buffer, Map context, ModelForm modelForm, ModelFormField modelFormField) {
    }

    public void renderFormatHeaderRowFormCellOpen(StringBuffer buffer, Map context, ModelForm modelForm) {
    }

    public void renderFormatHeaderRowFormCellClose(StringBuffer buffer, Map context, ModelForm modelForm) {
    }

    public void renderFormatHeaderRowFormCellTitleSeparator(StringBuffer buffer, Map context, ModelForm modelForm, ModelFormField modelFormField, boolean isLast) {
    }

    public void renderFormatItemRowOpen(StringBuffer buffer, Map context, ModelForm modelForm) {
    }

    public void renderFormatItemRowClose(StringBuffer buffer, Map context, ModelForm modelForm) {
        this.appendWhitespace(buffer);
    }

    public void renderFormatItemRowCellOpen(StringBuffer buffer, Map context, ModelForm modelForm, ModelFormField modelFormField, int positionSpan) {
    }

    public void renderFormatItemRowCellClose(StringBuffer buffer, Map context, ModelForm modelForm, ModelFormField modelFormField) {
    }

    public void renderFormatItemRowFormCellOpen(StringBuffer buffer, Map context, ModelForm modelForm) {
    }

    public void renderFormatItemRowFormCellClose(StringBuffer buffer, Map context, ModelForm modelForm) {
    }

    public void renderFormatSingleWrapperOpen(StringBuffer buffer, Map context, ModelForm modelForm) {
    }

    public void renderFormatSingleWrapperClose(StringBuffer buffer, Map context, ModelForm modelForm) {
    }

    public void renderFormatFieldRowOpen(StringBuffer buffer, Map context, ModelForm modelForm) {
    }

    public void renderFormatFieldRowClose(StringBuffer buffer, Map context, ModelForm modelForm) {
    }

    public void renderFormatFieldRowTitleCellOpen(StringBuffer buffer, Map context, ModelFormField modelFormField) {
    }

    public void renderFormatFieldRowTitleCellClose(StringBuffer buffer, Map context, ModelFormField modelFormField) {
    }

    public void renderFormatFieldRowSpacerCell(StringBuffer buffer, Map context, ModelFormField modelFormField) {
    }

    public void renderFormatFieldRowWidgetCellOpen(StringBuffer buffer, Map context, ModelFormField modelFormField, int positions, int positionSpan, Integer nextPositionInRow) {
    }

    public void renderFormatFieldRowWidgetCellClose(StringBuffer buffer, Map context, ModelFormField modelFormField, int positions, int positionSpan, Integer nextPositionInRow) {
    }

    public void renderFormatEmptySpace(StringBuffer buffer, Map context, ModelForm modelForm) {
        // TODO
    }

    public void renderTextFindField(StringBuffer buffer, Map context, TextFindField textFindField) {
        ModelFormField modelFormField = textFindField.getModelFormField();
        this.makeTextString(buffer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, textFindField.getDefaultValue(context)));
    }

    public void renderRangeFindField(StringBuffer buffer, Map context, RangeFindField rangeFindField) {
        ModelFormField modelFormField = rangeFindField.getModelFormField();
        this.makeTextString(buffer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, rangeFindField.getDefaultValue(context)));
    }

    public void renderDateFindField(StringBuffer buffer, Map context, DateFindField dateFindField) {
        ModelFormField modelFormField = dateFindField.getModelFormField();
        this.makeTextString(buffer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, dateFindField.getDefaultValue(context)));
    }

    public void renderLookupField(StringBuffer buffer, Map context, LookupField lookupField) {
        ModelFormField modelFormField = lookupField.getModelFormField();
        this.makeTextString(buffer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, lookupField.getDefaultValue(context)));
    }

    public void renderNextPrev(StringBuffer buffer, Map context, ModelForm modelForm) {
    }

    public void renderFileField(StringBuffer buffer, Map context, FileField textField) {
        ModelFormField modelFormField = textField.getModelFormField();
        this.makeTextString(buffer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, textField.getDefaultValue(context)));
    }

    public void renderPasswordField(StringBuffer buffer, Map context, PasswordField passwordField) {
    }

    public void renderImageField(StringBuffer buffer, Map context, ImageField imageField) {
        // TODO
    }

    public void renderFieldGroupOpen(StringBuffer buffer, Map context, ModelForm.FieldGroup fieldGroup) {
        // TODO
    }

    public void renderFieldGroupClose(StringBuffer buffer, Map context, ModelForm.FieldGroup fieldGroup) {
        // TODO
    }
    
    public void renderBanner(StringBuffer buffer, Map context, ModelForm.Banner banner) {
        // TODO
    }
    
    public void renderHyperlinkTitle(StringBuffer buffer, Map context, ModelFormField modelFormField, String titleText) {
    }
}
