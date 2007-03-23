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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilFormatOut;
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
 * Widget Library - FO Form Renderer implementation
 *
 */
public class FoFormRenderer implements FormStringRenderer {

    public static final String module = FoFormRenderer.class.getName();
    
    HttpServletRequest request;
    HttpServletResponse response;

    public FoFormRenderer() {}

    public FoFormRenderer(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    public void appendWhitespace(StringBuffer buffer) {
        // appending line ends for now, but this could be replaced with a simple space or something
        buffer.append("\r\n");
        //buffer.append(' ');
    }

    private String getFoStyle(String styleName) {
        String value = UtilProperties.getPropertyValue("fo-styles.properties", styleName);
        if (value.equals(styleName)) {
            return "";
        }
        return value;
    }

    private void makeBlockString(StringBuffer buffer, String widgetStyle, String text) {
        buffer.append("<fo:block");
        if (UtilValidate.isNotEmpty(widgetStyle)) {
            buffer.append(" ");
            buffer.append(getFoStyle(widgetStyle));
        }
        buffer.append(">");
        buffer.append(UtilFormatOut.encodeXmlValue(text));
        buffer.append("</fo:block>");
    }

    public void renderDisplayField(StringBuffer buffer, Map context, DisplayField displayField) {
        ModelFormField modelFormField = displayField.getModelFormField();
        this.makeBlockString(buffer, modelFormField.getWidgetStyle(), displayField.getDescription(context));
        this.appendWhitespace(buffer);
    }

    public void renderHyperlinkField(StringBuffer buffer, Map context, HyperlinkField hyperlinkField) {
        ModelFormField modelFormField = hyperlinkField.getModelFormField();
        this.makeBlockString(buffer, modelFormField.getWidgetStyle(), hyperlinkField.getDescription(context));
        this.appendWhitespace(buffer);
    }

    public void renderTextField(StringBuffer buffer, Map context, TextField textField) {
        ModelFormField modelFormField = textField.getModelFormField();
        this.makeBlockString(buffer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, textField.getDefaultValue(context)));
        this.appendWhitespace(buffer);
    }

    public void renderTextareaField(StringBuffer buffer, Map context, TextareaField textareaField) {
        ModelFormField modelFormField = textareaField.getModelFormField();
        this.makeBlockString(buffer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, textareaField.getDefaultValue(context)));
        this.appendWhitespace(buffer);
    }

    public void renderDateTimeField(StringBuffer buffer, Map context, DateTimeField dateTimeField) {
        ModelFormField modelFormField = dateTimeField.getModelFormField();
        this.makeBlockString(buffer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, dateTimeField.getDefaultValue(context)));
        this.appendWhitespace(buffer);
    }

    public void renderDropDownField(StringBuffer buffer, Map context, DropDownField dropDownField) {
        ModelFormField modelFormField = dropDownField.getModelFormField();
        ModelForm modelForm = modelFormField.getModelForm();
        String currentValue = modelFormField.getEntry(context);
        List allOptionValues = dropDownField.getAllOptionValues(context, modelForm.getDelegator());
        // if the current value should go first, display it
        if (UtilValidate.isNotEmpty(currentValue) && "first-in-list".equals(dropDownField.getCurrent())) {
            String explicitDescription = dropDownField.getCurrentDescription(context);
            if (UtilValidate.isNotEmpty(explicitDescription)) {
                this.makeBlockString(buffer, modelFormField.getWidgetStyle(), explicitDescription);
            } else {
                this.makeBlockString(buffer, modelFormField.getWidgetStyle(), ModelFormField.FieldInfoWithOptions.getDescriptionForOptionKey(currentValue, allOptionValues));
            }
        } else {
            Iterator optionValueIter = allOptionValues.iterator();
            while (optionValueIter.hasNext()) {
                ModelFormField.OptionValue optionValue = (ModelFormField.OptionValue) optionValueIter.next();
                String noCurrentSelectedKey = dropDownField.getNoCurrentSelectedKey(context);
                if ((UtilValidate.isNotEmpty(currentValue) && currentValue.equals(optionValue.getKey()) && "selected".equals(dropDownField.getCurrent())) ||
                        (UtilValidate.isEmpty(currentValue) && noCurrentSelectedKey != null && noCurrentSelectedKey.equals(optionValue.getKey()))) {
                    this.makeBlockString(buffer, modelFormField.getWidgetStyle(), optionValue.getDescription());
                    break;
                }
            }
        }
        this.appendWhitespace(buffer);
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
        String tempTitleText = modelFormField.getTitle(context);
        buffer.append(tempTitleText);
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
        buffer.append("<fo:table border=\"solid black\">");
        Iterator fieldListIter = modelForm.getFieldList().iterator();
        while (fieldListIter.hasNext()) {
            ModelFormField childField = (ModelFormField)fieldListIter.next();
            int childFieldType = childField.getFieldInfo().getFieldType();
            if (childFieldType == ModelFormField.FieldInfo.HIDDEN || childFieldType == ModelFormField.FieldInfo.IGNORED) {
                continue;
            }
            buffer.append("<fo:table-column");
            String areaStyle = childField.getTitleAreaStyle();
            if (UtilValidate.isNotEmpty(areaStyle)) {
                buffer.append(" ");
                buffer.append(getFoStyle(areaStyle));
            }
            buffer.append("/>");
            this.appendWhitespace(buffer);
        }
        this.appendWhitespace(buffer);
    }

    public void renderFormatListWrapperClose(StringBuffer buffer, Map context, ModelForm modelForm) {
        buffer.append("</fo:table-body>");
        buffer.append("</fo:table>");
        this.appendWhitespace(buffer);
    }

    public void renderFormatHeaderRowOpen(StringBuffer buffer, Map context, ModelForm modelForm) {
        buffer.append("<fo:table-header>");
        buffer.append("<fo:table-row>");
        this.appendWhitespace(buffer);
    }

    public void renderFormatHeaderRowClose(StringBuffer buffer, Map context, ModelForm modelForm) {
        buffer.append("</fo:table-row>");
        buffer.append("</fo:table-header>");
        buffer.append("<fo:table-body>");
        this.appendWhitespace(buffer);
    }

    public void renderFormatHeaderRowCellOpen(StringBuffer buffer, Map context, ModelForm modelForm, ModelFormField modelFormField) {
        buffer.append("<fo:table-cell font-weight=\"bold\" text-align=\"center\" border=\"solid black\" padding=\"2pt\">");
        buffer.append("<fo:block>");
        this.appendWhitespace(buffer);
    }

    public void renderFormatHeaderRowCellClose(StringBuffer buffer, Map context, ModelForm modelForm, ModelFormField modelFormField) {
        buffer.append("</fo:block>");
        buffer.append("</fo:table-cell>");
        this.appendWhitespace(buffer);
    }

    public void renderFormatHeaderRowFormCellOpen(StringBuffer buffer, Map context, ModelForm modelForm) {
        buffer.append("<fo:table-cell>");
        this.appendWhitespace(buffer);
    }

    public void renderFormatHeaderRowFormCellClose(StringBuffer buffer, Map context, ModelForm modelForm) {
        buffer.append("</fo:table-cell>");
        this.appendWhitespace(buffer);
    }

    public void renderFormatHeaderRowFormCellTitleSeparator(StringBuffer buffer, Map context, ModelForm modelForm, ModelFormField modelFormField, boolean isLast) {
    }

    public void renderFormatItemRowOpen(StringBuffer buffer, Map context, ModelForm modelForm) {
        buffer.append("<fo:table-row>");
        this.appendWhitespace(buffer);
    }

    public void renderFormatItemRowClose(StringBuffer buffer, Map context, ModelForm modelForm) {
        buffer.append("</fo:table-row>");
        this.appendWhitespace(buffer);
    }

    public void renderFormatItemRowCellOpen(StringBuffer buffer, Map context, ModelForm modelForm, ModelFormField modelFormField) {
        buffer.append("<fo:table-cell ");
        String areaStyle = modelFormField.getWidgetAreaStyle();
        if (UtilValidate.isEmpty(areaStyle)) {
            areaStyle = "tabletext";
        }
        buffer.append(getFoStyle(areaStyle));
        buffer.append(">");
        this.appendWhitespace(buffer);
    }

    public void renderFormatItemRowCellClose(StringBuffer buffer, Map context, ModelForm modelForm, ModelFormField modelFormField) {
        buffer.append("</fo:table-cell>");
        this.appendWhitespace(buffer);
    }

    public void renderFormatItemRowFormCellOpen(StringBuffer buffer, Map context, ModelForm modelForm) {
        buffer.append("<fo:table-cell>");
        this.appendWhitespace(buffer);
    }

    public void renderFormatItemRowFormCellClose(StringBuffer buffer, Map context, ModelForm modelForm) {
        buffer.append("</fo:table-cell>");
        this.appendWhitespace(buffer);
    }

    // TODO: multi columns (position attribute) in single forms are still not implemented
    public void renderFormatSingleWrapperOpen(StringBuffer buffer, Map context, ModelForm modelForm) {
        buffer.append("<fo:table>");
        this.appendWhitespace(buffer);
        buffer.append("<fo:table-column column-width=\"2in\"/>");
        this.appendWhitespace(buffer);
        buffer.append("<fo:table-column/>");
        this.appendWhitespace(buffer);
        buffer.append("<fo:table-body>");
        this.appendWhitespace(buffer);
    }
    public void renderFormatSingleWrapperClose(StringBuffer buffer, Map context, ModelForm modelForm) {
        buffer.append("</fo:table-body>");
        buffer.append("</fo:table>");
        this.appendWhitespace(buffer);
    }

    public void renderFormatFieldRowOpen(StringBuffer buffer, Map context, ModelForm modelForm) {
        buffer.append("<fo:table-row>");
        this.appendWhitespace(buffer);
    }

    public void renderFormatFieldRowClose(StringBuffer buffer, Map context, ModelForm modelForm) {
        buffer.append("</fo:table-row>");
        this.appendWhitespace(buffer);
    }


    public void renderFormatFieldRowTitleCellOpen(StringBuffer buffer, Map context, ModelFormField modelFormField) {
        buffer.append("<fo:table-cell font-weight=\"bold\" text-align=\"right\" padding=\"3pt\">");
        buffer.append("<fo:block>");
        this.appendWhitespace(buffer);
    }

    public void renderFormatFieldRowTitleCellClose(StringBuffer buffer, Map context, ModelFormField modelFormField) {
        buffer.append("</fo:block>");
        buffer.append("</fo:table-cell>");
        this.appendWhitespace(buffer);
    }

    public void renderFormatFieldRowSpacerCell(StringBuffer buffer, Map context, ModelFormField modelFormField) {
    }

    public void renderFormatFieldRowWidgetCellOpen(StringBuffer buffer, Map context, ModelFormField modelFormField, int positions, int positionSpan, Integer nextPositionInRow) {
        buffer.append("<fo:table-cell text-align=\"left\" padding=\"2pt\" padding-left=\"5pt\">");
        this.appendWhitespace(buffer);
    }

    public void renderFormatFieldRowWidgetCellClose(StringBuffer buffer, Map context, ModelFormField modelFormField, int positions, int positionSpan, Integer nextPositionInRow) {
        buffer.append("</fo:table-cell>");
        this.appendWhitespace(buffer);
    }

    public void renderFormatEmptySpace(StringBuffer buffer, Map context, ModelForm modelForm) {
        // TODO
    }

    public void renderTextFindField(StringBuffer buffer, Map context, TextFindField textFindField) {
        ModelFormField modelFormField = textFindField.getModelFormField();
        this.makeBlockString(buffer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, textFindField.getDefaultValue(context)));
        this.appendWhitespace(buffer);
    }

    public void renderRangeFindField(StringBuffer buffer, Map context, RangeFindField rangeFindField) {
        ModelFormField modelFormField = rangeFindField.getModelFormField();
        this.makeBlockString(buffer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, rangeFindField.getDefaultValue(context)));
        this.appendWhitespace(buffer);
    }

    public void renderDateFindField(StringBuffer buffer, Map context, DateFindField dateFindField) {
        ModelFormField modelFormField = dateFindField.getModelFormField();
        this.makeBlockString(buffer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, dateFindField.getDefaultValue(context)));
        this.appendWhitespace(buffer);
    }

    public void renderLookupField(StringBuffer buffer, Map context, LookupField lookupField) {
        ModelFormField modelFormField = lookupField.getModelFormField();
        this.makeBlockString(buffer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, lookupField.getDefaultValue(context)));
        this.appendWhitespace(buffer);
    }

    public void renderNextPrev(StringBuffer buffer, Map context, ModelForm modelForm) {
    }

    public void renderFileField(StringBuffer buffer, Map context, FileField textField) {
        ModelFormField modelFormField = textField.getModelFormField();
        this.makeBlockString(buffer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, textField.getDefaultValue(context)));
        this.appendWhitespace(buffer);
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
