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
package org.ofbiz.widget.form;

import java.util.Map;

/**
 * Widget Library - Form String Renderer interface
 */
public interface FormStringRenderer {
    public void renderDisplayField(StringBuffer buffer, Map context, ModelFormField.DisplayField displayField);
    public void renderHyperlinkField(StringBuffer buffer, Map context, ModelFormField.HyperlinkField hyperlinkField);

    public void renderTextField(StringBuffer buffer, Map context, ModelFormField.TextField textField);
    public void renderTextareaField(StringBuffer buffer, Map context, ModelFormField.TextareaField textareaField);
    public void renderDateTimeField(StringBuffer buffer, Map context, ModelFormField.DateTimeField dateTimeField);

    public void renderDropDownField(StringBuffer buffer, Map context, ModelFormField.DropDownField dropDownField);
    public void renderCheckField(StringBuffer buffer, Map context, ModelFormField.CheckField checkField);
    public void renderRadioField(StringBuffer buffer, Map context, ModelFormField.RadioField radioField);

    public void renderSubmitField(StringBuffer buffer, Map context, ModelFormField.SubmitField submitField);
    public void renderResetField(StringBuffer buffer, Map context, ModelFormField.ResetField resetField);

    public void renderHiddenField(StringBuffer buffer, Map context, ModelFormField modelFormField, String value);
    public void renderHiddenField(StringBuffer buffer, Map context, ModelFormField.HiddenField hiddenField);
    public void renderIgnoredField(StringBuffer buffer, Map context, ModelFormField.IgnoredField ignoredField);

    public void renderFieldTitle(StringBuffer buffer, Map context, ModelFormField modelFormField);
    public void renderSingleFormFieldTitle(StringBuffer buffer, Map context, ModelFormField modelFormField);
    
    public void renderFormOpen(StringBuffer buffer, Map context, ModelForm modelForm);
    public void renderFormClose(StringBuffer buffer, Map context, ModelForm modelForm);
    public void renderMultiFormClose(StringBuffer buffer, Map context, ModelForm modelForm);
    
    public void renderFormatListWrapperOpen(StringBuffer buffer, Map context, ModelForm modelForm);
    public void renderFormatListWrapperClose(StringBuffer buffer, Map context, ModelForm modelForm);

    public void renderFormatHeaderRowOpen(StringBuffer buffer, Map context, ModelForm modelForm);
    public void renderFormatHeaderRowClose(StringBuffer buffer, Map context, ModelForm modelForm);
    public void renderFormatHeaderRowCellOpen(StringBuffer buffer, Map context, ModelForm modelForm, ModelFormField modelFormField);
    public void renderFormatHeaderRowCellClose(StringBuffer buffer, Map context, ModelForm modelForm, ModelFormField modelFormField);

    public void renderFormatHeaderRowFormCellOpen(StringBuffer buffer, Map context, ModelForm modelForm);
    public void renderFormatHeaderRowFormCellClose(StringBuffer buffer, Map context, ModelForm modelForm);
    public void renderFormatHeaderRowFormCellTitleSeparator(StringBuffer buffer, Map context, ModelForm modelForm, ModelFormField modelFormField, boolean isLast);
    
    public void renderFormatItemRowOpen(StringBuffer buffer, Map context, ModelForm modelForm);
    public void renderFormatItemRowClose(StringBuffer buffer, Map context, ModelForm modelForm);
    public void renderFormatItemRowCellOpen(StringBuffer buffer, Map context, ModelForm modelForm, ModelFormField modelFormField);
    public void renderFormatItemRowCellClose(StringBuffer buffer, Map context, ModelForm modelForm, ModelFormField modelFormField);
    public void renderFormatItemRowFormCellOpen(StringBuffer buffer, Map context, ModelForm modelForm);
    public void renderFormatItemRowFormCellClose(StringBuffer buffer, Map context, ModelForm modelForm);

    public void renderFormatSingleWrapperOpen(StringBuffer buffer, Map context, ModelForm modelForm);
    public void renderFormatSingleWrapperClose(StringBuffer buffer, Map context, ModelForm modelForm);

    public void renderFormatFieldRowOpen(StringBuffer buffer, Map context, ModelForm modelForm);
    public void renderFormatFieldRowClose(StringBuffer buffer, Map context, ModelForm modelForm);
    public void renderFormatFieldRowTitleCellOpen(StringBuffer buffer, Map context, ModelFormField modelFormField);
    public void renderFormatFieldRowTitleCellClose(StringBuffer buffer, Map context, ModelFormField modelFormField);
    public void renderFormatFieldRowSpacerCell(StringBuffer buffer, Map context, ModelFormField modelFormField);
    public void renderFormatFieldRowWidgetCellOpen(StringBuffer buffer, Map context, ModelFormField modelFormField, int positions, int positionSpan, Integer nextPositionInRow);
    public void renderFormatFieldRowWidgetCellClose(StringBuffer buffer, Map context, ModelFormField modelFormField, int positions, int positionSpan, Integer nextPositionInRow);

    public void renderFormatEmptySpace(StringBuffer buffer, Map context, ModelForm modelForm);

    public void renderTextFindField(StringBuffer buffer, Map context, ModelFormField.TextFindField textField);
    public void renderDateFindField(StringBuffer buffer, Map context, ModelFormField.DateFindField textField);
    public void renderRangeFindField(StringBuffer buffer, Map context, ModelFormField.RangeFindField textField);
    public void renderLookupField(StringBuffer buffer, Map context, ModelFormField.LookupField textField);
    public void renderFileField(StringBuffer buffer, Map context, ModelFormField.FileField textField);
    public void renderPasswordField(StringBuffer buffer, Map context, ModelFormField.PasswordField textField);
    public void renderImageField(StringBuffer buffer, Map context, ModelFormField.ImageField textField);
    public void renderBanner(StringBuffer buffer, Map context, ModelForm.Banner banner);
    public void renderFieldGroupOpen(StringBuffer buffer, Map context, ModelForm.FieldGroup fieldGroup);
    public void renderFieldGroupClose(StringBuffer buffer, Map context, ModelForm.FieldGroup fieldGroup);
}
