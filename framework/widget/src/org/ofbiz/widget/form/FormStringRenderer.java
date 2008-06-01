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

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

/**
 * Widget Library - Form String Renderer interface.
 */
public interface FormStringRenderer {
    public void renderDisplayField(Writer writer, Map<String, Object> context, ModelFormField.DisplayField displayField) throws IOException;
    public void renderHyperlinkField(Writer writer, Map<String, Object> context, ModelFormField.HyperlinkField hyperlinkField) throws IOException;

    public void renderTextField(Writer writer, Map<String, Object> context, ModelFormField.TextField textField) throws IOException;
    public void renderTextareaField(Writer writer, Map<String, Object> context, ModelFormField.TextareaField textareaField) throws IOException;
    public void renderDateTimeField(Writer writer, Map<String, Object> context, ModelFormField.DateTimeField dateTimeField) throws IOException;

    public void renderDropDownField(Writer writer, Map<String, Object> context, ModelFormField.DropDownField dropDownField) throws IOException;
    public void renderCheckField(Writer writer, Map<String, Object> context, ModelFormField.CheckField checkField) throws IOException;
    public void renderRadioField(Writer writer, Map<String, Object> context, ModelFormField.RadioField radioField) throws IOException;

    public void renderSubmitField(Writer writer, Map<String, Object> context, ModelFormField.SubmitField submitField) throws IOException;
    public void renderResetField(Writer writer, Map<String, Object> context, ModelFormField.ResetField resetField) throws IOException;

    public void renderHiddenField(Writer writer, Map<String, Object> context, ModelFormField modelFormField, String value) throws IOException;
    public void renderHiddenField(Writer writer, Map<String, Object> context, ModelFormField.HiddenField hiddenField) throws IOException;
    public void renderIgnoredField(Writer writer, Map<String, Object> context, ModelFormField.IgnoredField ignoredField) throws IOException;

    public void renderFieldTitle(Writer writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException;
    public void renderSingleFormFieldTitle(Writer writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException;
    
    public void renderFormOpen(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException;
    public void renderFormClose(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException;
    public void renderMultiFormClose(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException;
    
    public void renderFormatListWrapperOpen(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException;
    public void renderFormatListWrapperClose(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException;

    public void renderFormatHeaderRowOpen(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException;
    public void renderFormatHeaderRowClose(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException;
    public void renderFormatHeaderRowCellOpen(Writer writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField, int positionSpan) throws IOException;
    public void renderFormatHeaderRowCellClose(Writer writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField) throws IOException;

    public void renderFormatHeaderRowFormCellOpen(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException;
    public void renderFormatHeaderRowFormCellClose(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException;
    public void renderFormatHeaderRowFormCellTitleSeparator(Writer writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField, boolean isLast) throws IOException;
    
    public void renderFormatItemRowOpen(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException;
    public void renderFormatItemRowClose(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException;
    public void renderFormatItemRowCellOpen(Writer writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField, int positionSpan) throws IOException;
    public void renderFormatItemRowCellClose(Writer writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField) throws IOException;
    public void renderFormatItemRowFormCellOpen(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException;
    public void renderFormatItemRowFormCellClose(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException;

    public void renderFormatSingleWrapperOpen(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException;
    public void renderFormatSingleWrapperClose(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException;

    public void renderFormatFieldRowOpen(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException;
    public void renderFormatFieldRowClose(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException;
    public void renderFormatFieldRowTitleCellOpen(Writer writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException;
    public void renderFormatFieldRowTitleCellClose(Writer writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException;
    public void renderFormatFieldRowSpacerCell(Writer writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException;
    public void renderFormatFieldRowWidgetCellOpen(Writer writer, Map<String, Object> context, ModelFormField modelFormField, int positions, int positionSpan, Integer nextPositionInRow) throws IOException;
    public void renderFormatFieldRowWidgetCellClose(Writer writer, Map<String, Object> context, ModelFormField modelFormField, int positions, int positionSpan, Integer nextPositionInRow) throws IOException;

    public void renderFormatEmptySpace(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException;

    public void renderTextFindField(Writer writer, Map<String, Object> context, ModelFormField.TextFindField textField) throws IOException;
    public void renderDateFindField(Writer writer, Map<String, Object> context, ModelFormField.DateFindField textField) throws IOException;
    public void renderRangeFindField(Writer writer, Map<String, Object> context, ModelFormField.RangeFindField textField) throws IOException;
    public void renderLookupField(Writer writer, Map<String, Object> context, ModelFormField.LookupField textField) throws IOException;
    public void renderFileField(Writer writer, Map<String, Object> context, ModelFormField.FileField textField) throws IOException;
    public void renderPasswordField(Writer writer, Map<String, Object> context, ModelFormField.PasswordField textField) throws IOException;
    public void renderImageField(Writer writer, Map<String, Object> context, ModelFormField.ImageField textField) throws IOException;
    public void renderBanner(Writer writer, Map<String, Object> context, ModelForm.Banner banner) throws IOException;
    public void renderFieldGroupOpen(Writer writer, Map<String, Object> context, ModelForm.FieldGroup fieldGroup) throws IOException;
    public void renderFieldGroupClose(Writer writer, Map<String, Object> context, ModelForm.FieldGroup fieldGroup) throws IOException;
}
