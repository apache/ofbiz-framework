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
package org.apache.ofbiz.widget.renderer;

import java.io.IOException;
import java.util.Map;

import org.apache.ofbiz.widget.model.ModelForm;
import org.apache.ofbiz.widget.model.ModelFormField;

/**
 * Widget Library - Form/Grid renderer.
 */
public interface FormStringRenderer {
    void renderDisplayField(Appendable writer, Map<String, Object> context, ModelFormField.DisplayField displayField) throws IOException;
    void renderHyperlinkField(Appendable writer, Map<String, Object> context, ModelFormField.HyperlinkField hyperlinkField) throws IOException;
    void renderMenuField(Appendable writer, Map<String, Object> context, ModelFormField.MenuField menuField) throws IOException;

    void renderTextField(Appendable writer, Map<String, Object> context, ModelFormField.TextField textField) throws IOException;
    void renderTextareaField(Appendable writer, Map<String, Object> context, ModelFormField.TextareaField textareaField) throws IOException;
    void renderDateTimeField(Appendable writer, Map<String, Object> context, ModelFormField.DateTimeField dateTimeField) throws IOException;

    void renderDropDownField(Appendable writer, Map<String, Object> context, ModelFormField.DropDownField dropDownField) throws IOException;
    void renderCheckField(Appendable writer, Map<String, Object> context, ModelFormField.CheckField checkField) throws IOException;
    void renderRadioField(Appendable writer, Map<String, Object> context, ModelFormField.RadioField radioField) throws IOException;

    void renderSubmitField(Appendable writer, Map<String, Object> context, ModelFormField.SubmitField submitField) throws IOException;
    void renderResetField(Appendable writer, Map<String, Object> context, ModelFormField.ResetField resetField) throws IOException;

    void renderHiddenField(Appendable writer, Map<String, Object> context, ModelFormField modelFormField, String value) throws IOException;
    void renderHiddenField(Appendable writer, Map<String, Object> context, ModelFormField.HiddenField hiddenField) throws IOException;
    void renderIgnoredField(Appendable writer, Map<String, Object> context, ModelFormField.IgnoredField ignoredField) throws IOException;

    void renderFieldTitle(Appendable writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException;
    void renderSingleFormFieldTitle(Appendable writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException;

    void renderFormOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException;
    void renderFormClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException;
    void renderMultiFormClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException;

    void renderFormatListWrapperOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException;
    void renderFormatListWrapperClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException;

    void renderFormatHeaderRowOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException;
    void renderFormatHeaderRowClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException;
    void renderFormatHeaderOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException;
    void renderFormatHeaderClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException;
    void renderFormatHeaderRowCellOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField,
                                       int positionSpan) throws IOException;
    void renderFormatHeaderRowCellClose(Appendable writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField)
            throws IOException;

    void renderFormatHeaderRowFormCellOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException;
    void renderFormatHeaderRowFormCellClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException;
    void renderFormatHeaderRowFormCellTitleSeparator(Appendable writer, Map<String, Object> context, ModelForm modelForm,
                                                     ModelFormField modelFormField, boolean isLast) throws IOException;

    void renderFormatItemRowOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException;
    void renderFormatItemRowClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException;
    void renderFormatItemRowCellOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField,
                                     int positionSpan) throws IOException;
    void renderFormatItemRowCellClose(Appendable writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField)
            throws IOException;
    void renderFormatItemRowFormCellOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException;
    void renderFormatItemRowFormCellClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException;

    void renderFormatSingleWrapperOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException;
    void renderFormatSingleWrapperClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException;

    void renderFormatFieldRowOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException;
    void renderFormatFieldRowClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException;
    void renderFormatFieldRowTitleCellOpen(Appendable writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException;
    void renderFormatFieldRowTitleCellClose(Appendable writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException;
    void renderFormatFieldRowSpacerCell(Appendable writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException;
    void renderFormatFieldRowWidgetCellOpen(Appendable writer, Map<String, Object> context, ModelFormField modelFormField, int positions,
                                            int positionSpan, Integer nextPositionInRow) throws IOException;
    void renderFormatFieldRowWidgetCellClose(Appendable writer, Map<String, Object> context, ModelFormField modelFormField, int positions,
                                             int positionSpan, Integer nextPositionInRow) throws IOException;

    void renderFormatEmptySpace(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException;

    void renderTextFindField(Appendable writer, Map<String, Object> context, ModelFormField.TextFindField textField) throws IOException;
    void renderDateFindField(Appendable writer, Map<String, Object> context, ModelFormField.DateFindField textField) throws IOException;
    void renderRangeFindField(Appendable writer, Map<String, Object> context, ModelFormField.RangeFindField textField) throws IOException;
    void renderLookupField(Appendable writer, Map<String, Object> context, ModelFormField.LookupField textField) throws IOException;
    void renderFileField(Appendable writer, Map<String, Object> context, ModelFormField.FileField textField) throws IOException;
    void renderPasswordField(Appendable writer, Map<String, Object> context, ModelFormField.PasswordField textField) throws IOException;
    void renderImageField(Appendable writer, Map<String, Object> context, ModelFormField.ImageField textField) throws IOException;
    void renderBanner(Appendable writer, Map<String, Object> context, ModelForm.Banner banner) throws IOException;
    void renderContainerFindField(Appendable writer, Map<String, Object> context, ModelFormField.ContainerField containerField) throws IOException;
    void renderFieldGroupOpen(Appendable writer, Map<String, Object> context, ModelForm.FieldGroup fieldGroup) throws IOException;
    void renderFieldGroupClose(Appendable writer, Map<String, Object> context, ModelForm.FieldGroup fieldGroup) throws IOException;
    void renderEmptyFormDataMessage(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException;
}
