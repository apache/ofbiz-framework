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
package org.ofbiz.widget.html;

import java.io.IOException;
import java.io.Writer;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javolution.util.FastList;

import org.apache.commons.lang.StringEscapeUtils;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.webapp.control.RequestHandler;
import org.ofbiz.webapp.taglib.ContentUrlTag;
import org.ofbiz.widget.WidgetWorker;
import org.ofbiz.widget.form.FormStringRenderer;
import org.ofbiz.widget.form.ModelForm;
import org.ofbiz.widget.form.ModelFormField;
import org.ofbiz.widget.form.ModelFormField.CheckField;
import org.ofbiz.widget.form.ModelFormField.DateFindField;
import org.ofbiz.widget.form.ModelFormField.DateTimeField;
import org.ofbiz.widget.form.ModelFormField.DisplayEntityField;
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
 * Widget Library - HTML Form Renderer implementation
 */
public class HtmlFormRenderer extends HtmlWidgetRenderer implements FormStringRenderer {

    public static final String module = HtmlFormRenderer.class.getName();
    
    protected HttpServletRequest request;
    protected HttpServletResponse response;
    protected RequestHandler rh;
    protected String lastFieldGroupId = "";
    protected boolean renderPagination = true;
    protected boolean javaScriptEnabled = false;

    protected HtmlFormRenderer() {}

    public HtmlFormRenderer(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
        ServletContext ctx = (ServletContext) request.getAttribute("servletContext");
        this.rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");
        this.javaScriptEnabled = UtilHttp.isJavaScriptEnabled(request);
    }

    public boolean getRenderPagination() {
        return this.renderPagination;
    }

    public void setRenderPagination(boolean renderPagination) {
        this.renderPagination = renderPagination;
    }

    public void appendOfbizUrl(Writer writer, String location) throws IOException {
        writer.write(this.rh.makeLink(this.request, this.response, location));
    }

    public void appendContentUrl(Writer writer, String location) throws IOException {
        StringBuffer buffer = new StringBuffer();
        ContentUrlTag.appendContentPrefix(this.request, buffer);
        writer.write(buffer.toString());
        writer.write(location);
    }

    public void appendTooltip(Writer writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
        // render the tooltip, in other methods too
        String tooltip = modelFormField.getTooltip(context);
        if (UtilValidate.isNotEmpty(tooltip)) {
            writer.write("<span class=\"");
            String tooltipStyle = modelFormField.getTooltipStyle();
            if (UtilValidate.isNotEmpty(tooltipStyle)) {
                writer.write(tooltipStyle);
            } else {
                writer.write("tooltip");
            }
            writer.write("\">");
            writer.write(tooltip);
            writer.write("</span>");
        }
    }

    public void addAsterisks(Writer writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
           
        boolean requiredField = modelFormField.getRequiredField();
        if (requiredField) {
            String requiredStyle = modelFormField.getRequiredFieldStyle();
            
            if (UtilValidate.isEmpty(requiredStyle)) {
               writer.write("*");
            }
        }
    }
    
    public void appendClassNames(Writer writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
        String className = modelFormField.getWidgetStyle();
        if (UtilValidate.isNotEmpty(className) || modelFormField.shouldBeRed(context)) {
            writer.write(" class=\"");
            writer.write(className);
            // add a style of red if redWhen is true
            if (modelFormField.shouldBeRed(context)) {
                writer.write(" alert");
            }
            writer.write('"');
        }
    }
    
    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderDisplayField(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelFormField.DisplayField)
     */
    public void renderDisplayField(Writer writer, Map<String, Object> context, DisplayField displayField) throws IOException {
        ModelFormField modelFormField = displayField.getModelFormField();

        StringBuffer str = new StringBuffer();

        if (UtilValidate.isNotEmpty(modelFormField.getWidgetStyle()) || modelFormField.shouldBeRed(context)) {
            str.append("<span class=\"");
            str.append(modelFormField.getWidgetStyle());
            // add a style of red if this is a date/time field and redWhen is true
            if (modelFormField.shouldBeRed(context)) {
                str.append(" alert");
            }
            str.append("\">");
        }

        if (str.length() > 0) {
            writer.write(str.toString());
        }
        String description = displayField.getDescription(context);
        //Replace new lines with <br/>
        description = description.replaceAll("\n", "<br/>");

        if (UtilValidate.isEmpty(description)) {
            this.renderFormatEmptySpace(writer, context, modelFormField.getModelForm());
        } else {
            writer.write(description);
        }

        if (str.length() > 0) {
            writer.write("</span>");
        }

        if (displayField instanceof DisplayEntityField) {
            this.makeHyperlinkString(writer, ((DisplayEntityField) displayField).getSubHyperlink(), context);
        }
        
        this.appendTooltip(writer, context, modelFormField);

        //appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderHyperlinkField(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelFormField.HyperlinkField)
     */
    public void renderHyperlinkField(Writer writer, Map<String, Object> context, HyperlinkField hyperlinkField) throws IOException {
        ModelFormField modelFormField = hyperlinkField.getModelFormField();
        this.makeHyperlinkString(
            writer,
            modelFormField.getWidgetStyle(),
            hyperlinkField.getTargetType(),
            hyperlinkField.getTarget(context),
            hyperlinkField.getDescription(context),
            hyperlinkField.getTargetWindow(context),
            modelFormField.getEvent(),
            modelFormField.getAction(context));
        this.appendTooltip(writer, context, modelFormField);
        //appendWhitespace(writer);
    }

    public void makeHyperlinkString(Writer writer, ModelFormField.SubHyperlink subHyperlink, Map<String, Object> context) throws IOException {
        if (subHyperlink == null) {
            return;
        }
        if (subHyperlink.shouldUse(context)) {
            writer.write(' ');
            this.makeHyperlinkString(
                writer,
                subHyperlink.getLinkStyle(),
                subHyperlink.getTargetType(),
                subHyperlink.getTarget(context),
                subHyperlink.getDescription(context),
                subHyperlink.getTargetWindow(context),
                null, null);
        }
    }

    public void makeHyperlinkString(Writer writer, String linkStyle, String targetType, String target, String description, String targetWindow, String event, String action) throws IOException {
        WidgetWorker.makeHyperlinkString(writer, linkStyle, targetType, target, description, this.request, this.response, null, targetWindow, event, action);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderTextField(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelFormField.TextField)
     */
    public void renderTextField(Writer writer, Map<String, Object> context, TextField textField) throws IOException {
        ModelFormField modelFormField = textField.getModelFormField();

        writer.write("<input type=\"text\"");

        appendClassNames(writer, context, modelFormField);

        writer.write(" name=\"");
        writer.write(modelFormField.getParameterName(context));
        writer.write('"');

        String value = modelFormField.getEntry(context, textField.getDefaultValue(context));
        if (UtilValidate.isNotEmpty(value)) {
            writer.write(" value=\"");
            writer.write(StringEscapeUtils.escapeHtml(value));
            writer.write('"');
        }

        writer.write(" size=\"");
        writer.write(Integer.toString(textField.getSize()));
        writer.write('"');

        Integer maxlength = textField.getMaxlength();
        if (maxlength != null) {
            writer.write(" maxlength=\"");
            writer.write(maxlength.toString());
            writer.write('"');
        }

        String idName = modelFormField.getIdName();
        if (UtilValidate.isNotEmpty(idName)) {
            writer.write(" id=\"");
            writer.write(idName);
            writer.write('"');
        }

        String event = modelFormField.getEvent();
        String action = modelFormField.getAction(context);
        if (UtilValidate.isNotEmpty(event) && UtilValidate.isNotEmpty(action)) {
            writer.write(" ");
            writer.write(event);
            writer.write("=\"");
            writer.write(action);
            writer.write('"');
        }

        List<ModelForm.UpdateArea> updateAreas = modelFormField.getOnChangeUpdateAreas();
        boolean ajaxEnabled = updateAreas != null && this.javaScriptEnabled;
        if (!textField.getClientAutocompleteField() || ajaxEnabled) {
            writer.write(" autocomplete=\"off\"");
        }

        writer.write("/>");
        
        this.addAsterisks(writer, context, modelFormField);

        this.makeHyperlinkString(writer, textField.getSubHyperlink(), context);

        this.appendTooltip(writer, context, modelFormField);

        if (ajaxEnabled) {
            appendWhitespace(writer);
            writer.write("<script language=\"JavaScript\" type=\"text/javascript\">");
            appendWhitespace(writer);
            writer.write("ajaxAutoCompleter('" + createAjaxParamsFromUpdateAreas(updateAreas, null, context) + "');");
            appendWhitespace(writer);
            writer.write("</script>");
        }
        appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderTextareaField(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelFormField.TextareaField)
     */
    public void renderTextareaField(Writer writer, Map<String, Object> context, TextareaField textareaField) throws IOException {
        ModelFormField modelFormField = textareaField.getModelFormField();

        writer.write("<textarea");

        appendClassNames(writer, context, modelFormField);

        writer.write(" name=\"");
        writer.write(modelFormField.getParameterName(context));
        writer.write('"');

        writer.write(" cols=\"");
        writer.write(Integer.toString(textareaField.getCols()));
        writer.write('"');

        writer.write(" rows=\"");
        writer.write(Integer.toString(textareaField.getRows()));
        writer.write('"');

        String idName = modelFormField.getIdName();
        if (UtilValidate.isNotEmpty(idName)) {
            writer.write(" id=\"");
            writer.write(idName);
            writer.write('"');
        } else if (textareaField.getVisualEditorEnable()) {
            writer.write(" id=\"");
            writer.write("htmlEditArea");
            writer.write('"');
        }

        if (textareaField.isReadOnly()) {
            writer.write(" readonly");
        }
 
        writer.write('>');

        String value = modelFormField.getEntry(context, textareaField.getDefaultValue(context));
        if (UtilValidate.isNotEmpty(value)) {
            writer.write(StringEscapeUtils.escapeHtml(value));
        }

        writer.write("</textarea>");
        
        if (textareaField.getVisualEditorEnable()) {
            writer.write("<script language=\"javascript\" src=\"/images/htmledit/whizzywig.js\" type=\"text/javascript\"></script>");
            writer.write("<script language=\"javascript\" type=\"text/javascript\"> buttonPath = \"/images/htmledit/\"; cssFile=\"/images/htmledit/simple.css\";makeWhizzyWig(\"");
            if (UtilValidate.isNotEmpty(idName)) { 
                writer.write(idName);
            } else {
                writer.write("htmlEditArea");
            }
            writer.write("\",\"");
            String buttons = textareaField.getVisualEditorButtons(context);
            if (UtilValidate.isNotEmpty(buttons)) {
                writer.write(buttons);
            } else {
                writer.write("all");
            }
            writer.write("\") </script>");
        }

        this.addAsterisks(writer, context, modelFormField);

        this.appendTooltip(writer, context, modelFormField);

        //appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderDateTimeField(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelFormField.DateTimeField)
     */
    public void renderDateTimeField(Writer writer, Map<String, Object> context, DateTimeField dateTimeField) throws IOException {
        ModelFormField modelFormField = dateTimeField.getModelFormField();
        String paramName = modelFormField.getParameterName(context);
        String defaultDateTimeString = dateTimeField.getDefaultDateTimeString(context);
        
        Map uiLabelMap = (Map) context.get("uiLabelMap");
        if (uiLabelMap == null) {
            Debug.logWarning("Could not find uiLabelMap in context", module);
        }
        String localizedInputTitle = "" , localizedIconTitle = "";

        // whether the date field is short form, yyyy-mm-dd
        boolean shortDateInput = ("date".equals(dateTimeField.getType()) || "time-dropdown".equals(dateTimeField.getInputMethod()) ? true : false);

        writer.write("<input type=\"text\"");

        appendClassNames(writer, context, modelFormField);

        writer.write(" name=\"");
        if ("time-dropdown".equals(dateTimeField.getInputMethod())) {
            writer.write(UtilHttp.makeCompositeParam(paramName, "date"));
        } else {
            writer.write(paramName);
        }
        writer.write('"');

        // the default values for a timestamp
        int size = 25;
        int maxlength = 30;

        if (shortDateInput) {
            size = maxlength = 10;
            if (uiLabelMap != null) {
                localizedInputTitle = (String) uiLabelMap.get("CommonFormatDate");
            }
        } else if ("time".equals(dateTimeField.getType())) {
            size = maxlength = 8;
            if (uiLabelMap != null) {
                localizedInputTitle = (String) uiLabelMap.get("CommonFormatTime");
            }
        } else {
            if (uiLabelMap != null) {
                localizedInputTitle = (String) uiLabelMap.get("CommonFormatDateTime");
            }
        }
        writer.write(" title=\"");
        writer.write(localizedInputTitle);
        writer.write('"');
        
        String value = modelFormField.getEntry(context, dateTimeField.getDefaultValue(context));
        if (UtilValidate.isNotEmpty(value)) {
            if(value.length() > maxlength) {
                value = value.substring(0, maxlength);
            }
            writer.write(" value=\"");
            writer.write(value);
            writer.write('"');
        }
        
        writer.write(" size=\"");
        writer.write(Integer.toString(size));
        writer.write('"');

        writer.write(" maxlength=\"");
        writer.write(Integer.toString(maxlength));
        writer.write('"');

        String idName = modelFormField.getIdName();
        if (UtilValidate.isNotEmpty(idName)) {
            writer.write(" id=\"");
            writer.write(idName);
            writer.write('"');
        }

        writer.write("/>");

        // search for a localized label for the icon
        if (uiLabelMap != null) {
            localizedIconTitle = (String) uiLabelMap.get("CommonViewCalendar");
        }

        // add calendar pop-up button and seed data IF this is not a "time" type date-time
        if (!"time".equals(dateTimeField.getType())) {
            if (shortDateInput) {
                writer.write("<a href=\"javascript:call_cal_notime(document.");
            } else {
                writer.write("<a href=\"javascript:call_cal(document.");
            }
            writer.write(modelFormField.getModelForm().getCurrentFormName(context));
            writer.write('.');
            if ("time-dropdown".equals(dateTimeField.getInputMethod())) {
                writer.write(UtilHttp.makeCompositeParam(paramName, "date"));
            } else {
                writer.write(paramName);
            }
            writer.write(",'");
            writer.write(UtilHttp.encodeBlanks(modelFormField.getEntry(context, defaultDateTimeString)));
            writer.write("');\">");
           
            writer.write("<img src=\"");
            this.appendContentUrl(writer, "/images/cal.gif");
            writer.write("\" width=\"16\" height=\"16\" border=\"0\" alt=\"");
            writer.write(localizedIconTitle);
            writer.write("\" title=\"");
            writer.write(localizedIconTitle);
            writer.write("\"/></a>");
        }
        
        // if we have an input method of time-dropdown, then render two dropdowns
        if ("time-dropdown".equals(dateTimeField.getInputMethod())) {       		
            String className = modelFormField.getWidgetStyle();
            String classString = (className != null ? " class=\"" + className + "\" " : "");
            boolean isTwelveHour = "12".equals(dateTimeField.getClock());

            // set the Calendar to the default time of the form or now()
            Calendar cal = null;
            try {
                Timestamp defaultTimestamp = Timestamp.valueOf(modelFormField.getEntry(context, defaultDateTimeString));
                cal = Calendar.getInstance();
                cal.setTime(defaultTimestamp);
            } catch (IllegalArgumentException e) {
                Debug.logWarning("Form widget field [" + paramName + "] with input-method=\"time-dropdown\" was not able to understand the default time [" 
                        + defaultDateTimeString + "]. The parsing error was: " + e.getMessage(), module);
            }

            // write the select for hours
            writer.write("&nbsp;<select name=\"" + UtilHttp.makeCompositeParam(paramName, "hour") + "\"");
            writer.write(classString + ">");

            // keep the two cases separate because it's hard to understand a combined loop
            if (isTwelveHour) {
                for (int i = 1; i <= 12; i++) {
                    writer.write("<option value=\"" + Integer.toString(i) + "\"");
                    if (cal != null) { 
                        int hour = cal.get(Calendar.HOUR_OF_DAY);
                        if (hour == 0) hour = 12;
                        if (hour > 12) hour -= 12;
                        if (i == hour) writer.write(" selected");
                    }
                    writer.write(">" + Integer.toString(i) + "</option>");
                }
            } else {
                for (int i = 0; i < 24; i++) {
                    writer.write("<option value=\"" + Integer.toString(i) + "\"");
                    if (cal != null && i == cal.get(Calendar.HOUR_OF_DAY)) {
                        writer.write(" selected");
                    }
                    writer.write(">" + Integer.toString(i) + "</option>");
                }
            }
            
            // write the select for minutes
            writer.write("</select>:<select name=\"");
            writer.write(UtilHttp.makeCompositeParam(paramName, "minutes") + "\"");
            writer.write(classString + ">");
            for (int i = 0; i < 60; i++) {
                writer.write("<option value=\"" + Integer.toString(i) + "\"");
                if (cal != null && i == cal.get(Calendar.MINUTE)) {
                    writer.write(" selected");
                }
                writer.write(">" + Integer.toString(i) + "</option>");
            }
            writer.write("</select>");

            // if 12 hour clock, write the AM/PM selector
            if (isTwelveHour) {
                String amSelected = ((cal != null && cal.get(Calendar.AM_PM) == Calendar.AM) ? "selected" : "");
                String pmSelected = ((cal != null && cal.get(Calendar.AM_PM) == Calendar.PM) ? "selected" : "");
                writer.write("<select name=\"" + UtilHttp.makeCompositeParam(paramName, "ampm") + "\"");
                writer.write(classString + ">");
                writer.write("<option value=\"" + "AM" + "\" " + amSelected + ">AM</option>");
                writer.write("<option value=\"" + "PM" + "\" " + pmSelected + ">PM</option>");
                writer.write("</select>");
            }

            // create a hidden field for the composite type, which is a Timestamp
            writer.write("<input type=\"hidden\" name=\"");
            writer.write(UtilHttp.makeCompositeParam(paramName, "compositeType"));
            writer.write("\" value=\"Timestamp\"/>");
        }

        this.appendTooltip(writer, context, modelFormField);

        //appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderDropDownField(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelFormField.DropDownField)
     */
    public void renderDropDownField(Writer writer, Map<String, Object> context, DropDownField dropDownField) throws IOException {
        ModelFormField modelFormField = dropDownField.getModelFormField();
        ModelForm modelForm = modelFormField.getModelForm();

        String event = modelFormField.getEvent();
        String action = modelFormField.getAction(context);

        writer.write("<select");

        appendClassNames(writer, context, modelFormField);

        writer.write(" name=\"");
        writer.write(modelFormField.getParameterName(context));
        writer.write('"');

        String idName = modelFormField.getIdName();
        if (UtilValidate.isNotEmpty(idName)) {
            writer.write(" id=\"");
            writer.write(idName);
            writer.write('"');
        }

        if (dropDownField.isAllowMultiple()) {
            writer.write(" multiple=\"multiple\"");
        }
        
        int otherFieldSize = dropDownField.getOtherFieldSize();
        String otherFieldName = dropDownField.getParameterNameOther(context);
        if (otherFieldSize > 0) {
            //writer.write(" onchange=\"alert('ONCHANGE, process_choice:' + process_choice)\"");
            //writer.write(" onchange='test_js()' ");
            writer.write(" onchange=\"process_choice(this,document.");
            writer.write(modelForm.getName());
            writer.write(".");
            writer.write(otherFieldName);
            writer.write(")\" "); 
        }


        if (UtilValidate.isNotEmpty(event) && UtilValidate.isNotEmpty(action)) {
            writer.write(" ");
            writer.write(event);
            writer.write("=\"");
            writer.write(action);
            writer.write('"');
        }

        writer.write(" size=\"" + dropDownField.getSize() + "\">");

        String currentValue = modelFormField.getEntry(context);
        List allOptionValues = dropDownField.getAllOptionValues(context, modelForm.getDelegator(context));

        // if the current value should go first, stick it in
        if (UtilValidate.isNotEmpty(currentValue) && "first-in-list".equals(dropDownField.getCurrent())) {
            writer.write("<option");
            writer.write(" selected=\"selected\"");
            writer.write(" value=\"");
            writer.write(currentValue);
            writer.write("\">");
            String explicitDescription = dropDownField.getCurrentDescription(context);
            if (UtilValidate.isNotEmpty(explicitDescription)) {
                writer.write(explicitDescription);
            } else {
                writer.write(ModelFormField.FieldInfoWithOptions.getDescriptionForOptionKey(currentValue, allOptionValues));
            }
            writer.write("</option>");

            // add a "separator" option
            writer.write("<option value=\"");
            writer.write(currentValue);
            writer.write("\">---</option>");
        }

        // if allow empty is true, add an empty option
        if (dropDownField.isAllowEmpty()) {
            writer.write("<option value=\"\">&nbsp;</option>");
        }

        // list out all options according to the option list
        Iterator optionValueIter = allOptionValues.iterator();
        while (optionValueIter.hasNext()) {
            ModelFormField.OptionValue optionValue = (ModelFormField.OptionValue) optionValueIter.next();
            String noCurrentSelectedKey = dropDownField.getNoCurrentSelectedKey(context);
            writer.write("<option");
            // if current value should be selected in the list, select it
            if (UtilValidate.isNotEmpty(currentValue) && currentValue.equals(optionValue.getKey()) && "selected".equals(dropDownField.getCurrent())) {
                writer.write(" selected=\"selected\"");
            } else if (UtilValidate.isEmpty(currentValue) && noCurrentSelectedKey != null && noCurrentSelectedKey.equals(optionValue.getKey())) {
                writer.write(" selected=\"selected\"");
            }
            writer.write(" value=\"");
            writer.write(optionValue.getKey());
            writer.write("\">");
            writer.write(optionValue.getDescription());
            writer.write("</option>");
        }

        writer.write("</select>");

        // Adapted from work by Yucca Korpela
        // http://www.cs.tut.fi/~jkorpela/forms/combo.html
        if (otherFieldSize > 0) {
        
            String fieldName = modelFormField.getParameterName(context);
            Map dataMap = modelFormField.getMap(context);
            if (dataMap == null) {
                dataMap = context;
            }
            Object otherValueObj = dataMap.get(otherFieldName);
            String otherValue = (otherValueObj == null) ? "" : otherValueObj.toString();
            
            writer.write("<noscript>");
            writer.write("<input type='text' name='");
            writer.write(otherFieldName);
            writer.write("'/> ");
            writer.write("</noscript>");
            writer.write("\n<script type='text/javascript' language='JavaScript'><!--");
            writer.write("\ndisa = ' disabled';");
            writer.write("\nif(other_choice(document.");
            writer.write(modelForm.getName());
            writer.write(".");
            writer.write(fieldName);
            writer.write(")) disa = '';");
            writer.write("\ndocument.write(\"<input type=");
            writer.write("'text' name='");
            writer.write(otherFieldName);
            writer.write("' value='");
            writer.write(otherValue);
            writer.write("' size='");
            writer.write(Integer.toString(otherFieldSize));
            writer.write("' ");
            writer.write("\" +disa+ \" onfocus='check_choice(document.");
            writer.write(modelForm.getName());
            writer.write(".");
            writer.write(fieldName);
            writer.write(")'/>\");");
            writer.write("\nif(disa && document.styleSheets)");
            writer.write(" document.");
            writer.write(modelForm.getName());
            writer.write(".");
            writer.write(otherFieldName);
            writer.write(".style.visibility  = 'hidden';");
            writer.write("\n//--></script>");
        }
        this.makeHyperlinkString(writer, dropDownField.getSubHyperlink(), context);

        this.appendTooltip(writer, context, modelFormField);

        //appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderCheckField(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelFormField.CheckField)
     */
    public void renderCheckField(Writer writer, Map<String, Object> context, CheckField checkField) throws IOException {
        ModelFormField modelFormField = checkField.getModelFormField();
        ModelForm modelForm = modelFormField.getModelForm();
        String currentValue = modelFormField.getEntry(context);
        Boolean allChecked = checkField.isAllChecked(context);
        
        List allOptionValues = checkField.getAllOptionValues(context, modelForm.getDelegator(context));
        String event = modelFormField.getEvent();
        String action = modelFormField.getAction(context);

        // list out all options according to the option list
        Iterator optionValueIter = allOptionValues.iterator();
        while (optionValueIter.hasNext()) {
            ModelFormField.OptionValue optionValue = (ModelFormField.OptionValue) optionValueIter.next();

            writer.write("<input type=\"checkbox\"");

            appendClassNames(writer, context, modelFormField);
            
            // if current value should be selected in the list, select it
            if (Boolean.TRUE.equals(allChecked)) {
                writer.write(" checked=\"checked\"");
            } else if (Boolean.FALSE.equals(allChecked)) {
                // do nothing
            } else if (UtilValidate.isNotEmpty(currentValue) && currentValue.equals(optionValue.getKey())) {
                writer.write(" checked=\"checked\"");
            }
            writer.write(" name=\"");
            writer.write(modelFormField.getParameterName(context));
            writer.write('"');
            writer.write(" value=\"");
            writer.write(optionValue.getKey());
            writer.write("\"");

            if (UtilValidate.isNotEmpty(event) && UtilValidate.isNotEmpty(action)) {
                writer.write(" ");
                writer.write(event);
                writer.write("=\"");
                writer.write(action);
                writer.write('"');
            }
            
            writer.write("/>");

            writer.write(optionValue.getDescription());
        }

        this.appendTooltip(writer, context, modelFormField);

        //appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderRadioField(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelFormField.RadioField)
     */
    public void renderRadioField(Writer writer, Map<String, Object> context, RadioField radioField) throws IOException {
        ModelFormField modelFormField = radioField.getModelFormField();
        ModelForm modelForm = modelFormField.getModelForm();
        List allOptionValues = radioField.getAllOptionValues(context, modelForm.getDelegator(context));
        String currentValue = modelFormField.getEntry(context);
        String event = modelFormField.getEvent();
        String action = modelFormField.getAction(context);

        // list out all options according to the option list
        Iterator optionValueIter = allOptionValues.iterator();
        while (optionValueIter.hasNext()) {
            ModelFormField.OptionValue optionValue = (ModelFormField.OptionValue) optionValueIter.next();

            writer.write("<div");

            appendClassNames(writer, context, modelFormField);

            writer.write("><input type=\"radio\"");
            
            // if current value should be selected in the list, select it
            String noCurrentSelectedKey = radioField.getNoCurrentSelectedKey(context);
            if (UtilValidate.isNotEmpty(currentValue) && currentValue.equals(optionValue.getKey())) {
                writer.write(" checked=\"checked\"");
            } else if (UtilValidate.isEmpty(currentValue) && noCurrentSelectedKey != null && noCurrentSelectedKey.equals(optionValue.getKey())) {
                writer.write(" checked=\"checked\"");
            }
            writer.write(" name=\"");
            writer.write(modelFormField.getParameterName(context));
            writer.write('"');
            writer.write(" value=\"");
            writer.write(optionValue.getKey());
            writer.write("\"");

            if (UtilValidate.isNotEmpty(event) && UtilValidate.isNotEmpty(action)) {
                writer.write(" ");
                writer.write(event);
                writer.write("=\"");
                writer.write(action);
                writer.write('"');
            }
            
            writer.write("/>");

            writer.write(optionValue.getDescription());
            writer.write("</div>");
        }

        this.appendTooltip(writer, context, modelFormField);

        //appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderSubmitField(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelFormField.SubmitField)
     */
    public void renderSubmitField(Writer writer, Map<String, Object> context, SubmitField submitField) throws IOException {
        ModelFormField modelFormField = submitField.getModelFormField();
        ModelForm modelForm = modelFormField.getModelForm();
        String event = null;
        String action = null;        

        if ("text-link".equals(submitField.getButtonType())) {
            writer.write("<a");

            appendClassNames(writer, context, modelFormField);

            writer.write(" href=\"javascript:document.");
            writer.write(modelForm.getCurrentFormName(context));
            writer.write(".submit()\">");

            writer.write(modelFormField.getTitle(context));

            writer.write("</a>");
        } else if ("image".equals(submitField.getButtonType())) {
            writer.write("<input type=\"image\"");

            appendClassNames(writer, context, modelFormField);

            writer.write(" name=\"");
            writer.write(modelFormField.getParameterName(context));
            writer.write('"');

            String title = modelFormField.getTitle(context);
            if (UtilValidate.isNotEmpty(title)) {
                writer.write(" alt=\"");
                writer.write(title);
                writer.write('"');
            }

            writer.write(" src=\"");
            this.appendContentUrl(writer, submitField.getImageLocation());
            writer.write('"');
            
            event = modelFormField.getEvent();
            action = modelFormField.getAction(context);
            if (UtilValidate.isNotEmpty(event) && UtilValidate.isNotEmpty(action)) {
                writer.write(" ");
                writer.write(event);
                writer.write("=\"");
                writer.write(action);
                writer.write('"');
            }
            
            writer.write("/>");
        } else {
            // default to "button"

            String formId = modelForm.getContainerId();
            List<ModelForm.UpdateArea> updateAreas = modelForm.getOnSubmitUpdateAreas();
            // This is here for backwards compatibility. Use on-event-update-area
            // elements instead.
            String backgroundSubmitRefreshTarget = submitField.getBackgroundSubmitRefreshTarget(context);
            if (UtilValidate.isNotEmpty(backgroundSubmitRefreshTarget)) {
                if (updateAreas == null) {
                    updateAreas = FastList.newInstance();
                }
                updateAreas.add(new ModelForm.UpdateArea("submit", formId, backgroundSubmitRefreshTarget));
            }

            boolean ajaxEnabled = (updateAreas != null || UtilValidate.isNotEmpty(backgroundSubmitRefreshTarget)) && this.javaScriptEnabled;
            if (ajaxEnabled) {
                writer.write("<input type=\"button\"");
            } else {
                writer.write("<input type=\"submit\"");
            }

            appendClassNames(writer, context, modelFormField);

            writer.write(" name=\"");
            writer.write(modelFormField.getParameterName(context));
            writer.write('"');

            String title = modelFormField.getTitle(context);
            if (UtilValidate.isNotEmpty(title)) {
                writer.write(" value=\"");
                writer.write(title);
                writer.write('"');
            }

            
            event = modelFormField.getEvent();
            action = modelFormField.getAction(context);
            if (UtilValidate.isNotEmpty(event) && UtilValidate.isNotEmpty(action)) {
                writer.write(" ");
                writer.write(event);
                writer.write("=\"");
                writer.write(action);
                writer.write('"');
            } else {
            	//add single click JS onclick
                // disabling for now, using form onSubmit action instead: writer.write(singleClickAction);
            }
            
            if (ajaxEnabled) {
                writer.write(" onclick=\"");
                writer.write("ajaxSubmitFormUpdateAreas($('");
                writer.write(formId);
                writer.write("'), '" + createAjaxParamsFromUpdateAreas(updateAreas, null, context));
                writer.write("')\"");
            }
            
            writer.write("/>");
        }

        this.appendTooltip(writer, context, modelFormField);

        //appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderResetField(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelFormField.ResetField)
     */
    public void renderResetField(Writer writer, Map<String, Object> context, ResetField resetField) throws IOException {
        ModelFormField modelFormField = resetField.getModelFormField();

        writer.write("<input type=\"reset\"");

        appendClassNames(writer, context, modelFormField);

        writer.write(" name=\"");
        writer.write(modelFormField.getParameterName(context));
        writer.write('"');

        String title = modelFormField.getTitle(context);
        if (UtilValidate.isNotEmpty(title)) {
            writer.write(" value=\"");
            writer.write(title);
            writer.write('"');
        }

        writer.write("/>");

        this.appendTooltip(writer, context, modelFormField);

        //appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderHiddenField(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelFormField.HiddenField)
     */
    public void renderHiddenField(Writer writer, Map<String, Object> context, HiddenField hiddenField) throws IOException {
        ModelFormField modelFormField = hiddenField.getModelFormField();
        String value = hiddenField.getValue(context);
        this.renderHiddenField(writer, context, modelFormField, value);
    }

    public void renderHiddenField(Writer writer, Map<String, Object> context, ModelFormField modelFormField, String value) throws IOException {
        writer.write("<input type=\"hidden\"");

        writer.write(" name=\"");
        writer.write(modelFormField.getParameterName(context));
        writer.write('"');

        if (UtilValidate.isNotEmpty(value)) {
            writer.write(" value=\"");
            writer.write(StringEscapeUtils.escapeHtml(value));
            writer.write('"');
        }

        writer.write("/>");
        appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderIgnoredField(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelFormField.IgnoredField)
     */
    public void renderIgnoredField(Writer writer, Map<String, Object> context, IgnoredField ignoredField) throws IOException {
        // do nothing, it's an ignored field; could add a comment or something if we wanted to
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFieldTitle(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelFormField)
     */
    public void renderFieldTitle(Writer writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
        String tempTitleText = modelFormField.getTitle(context);
        String titleText = UtilHttp.encodeAmpersands(tempTitleText);
        
        if (UtilValidate.isNotEmpty(titleText)) {
            if (UtilValidate.isNotEmpty(modelFormField.getTitleStyle())) {
                writer.write("<span class=\"");
                writer.write(modelFormField.getTitleStyle());
                writer.write("\">");
            }
            if (" ".equals(titleText)) {
                // If the title content is just a blank then render it colling renderFormatEmptySpace:
                // the method will set its content to work fine in most browser
                this.renderFormatEmptySpace(writer, context, modelFormField.getModelForm());
            } else {
                renderHyperlinkTitle(writer, context, modelFormField, titleText);
            }

            if (UtilValidate.isNotEmpty(modelFormField.getTitleStyle())) {
                writer.write("</span>");
            }

            //appendWhitespace(writer);
        }
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFieldTitle(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelFormField)
     */
    public void renderSingleFormFieldTitle(Writer writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
        boolean requiredField = modelFormField.getRequiredField();
        if (requiredField) {
            
            String requiredStyle = modelFormField.getRequiredFieldStyle();
            if (UtilValidate.isEmpty(requiredStyle)) {
                requiredStyle = modelFormField.getTitleStyle();
            }
            
            if (UtilValidate.isNotEmpty(requiredStyle)) {
                writer.write("<span class=\"");
                writer.write(requiredStyle);
                writer.write("\">");
            }
            renderHyperlinkTitle(writer, context, modelFormField, modelFormField.getTitle(context)); 
            if (UtilValidate.isNotEmpty(requiredStyle)) {
                writer.write("</span>");
            }

            //appendWhitespace(writer);
        } else {
            renderFieldTitle(writer, context, modelFormField);
        }
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormOpen(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelForm)
     */
    public void renderFormOpen(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        renderBeginningBoundaryComment(writer, "Form Widget", modelForm);
        writer.write("<form method=\"post\" ");
        String targetType = modelForm.getTargetType();
        String targ = modelForm.getTarget(context, targetType);
        // The 'action' attribute is mandatory in a form definition,
        // even if it is empty.
        writer.write(" action=\"");
        if (targ != null && targ.length() > 0) {
            //this.appendOfbizUrl(writer, "/" + targ);
            WidgetWorker.buildHyperlinkUrl(writer, targ, targetType, request, response, context);
        }
        writer.write("\" ");

        String formType = modelForm.getType();
        if (formType.equals("upload") ) {
            writer.write(" enctype=\"multipart/form-data\"");
        }

        String targetWindow = modelForm.getTargetWindow(context);
        if (UtilValidate.isNotEmpty(targetWindow)) {
            writer.write(" target=\"");
            writer.write(targetWindow);
            writer.write("\"");
        }

        String containerId =  modelForm.getContainerId();
        if (UtilValidate.isNotEmpty(containerId)) {
            writer.write(" id=\"");
            writer.write(containerId);
            writer.write("\"");
        }

        writer.write(" class=\"");
        String containerStyle =  modelForm.getContainerStyle();
        if (UtilValidate.isNotEmpty(containerStyle)) {
            writer.write(containerStyle);
        } else {
            writer.write("basic-form");
        }
        writer.write("\"");
        
        writer.write(" onSubmit=\"javascript:submitFormDisableSubmits(this)\"");

        if (!modelForm.getClientAutocompleteFields()) {
            writer.write(" autocomplete=\"off\"");
        }

        writer.write(" name=\"");
        writer.write(modelForm.getCurrentFormName(context));
        writer.write("\">");

        appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormClose(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelForm)
     */
    public void renderFormClose(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.write("</form>");
        String focusFieldName = modelForm.getfocusFieldName();
        if (UtilValidate.isNotEmpty(focusFieldName)) {
            appendWhitespace(writer);
            writer.write("<script language=\"JavaScript\" type=\"text/javascript\">");
            appendWhitespace(writer);
            writer.write("document." + modelForm.getCurrentFormName(context) + ".");
            writer.write(focusFieldName + ".focus();");
            appendWhitespace(writer);
            writer.write("</script>");
        }
        appendWhitespace(writer);
        renderEndingBoundaryComment(writer, "Form Widget", modelForm);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormClose(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelForm)
     */
    public void renderMultiFormClose(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        String rowCount = modelForm.getPassedRowCount(context);
        if (UtilValidate.isEmpty(rowCount)) {
            int rCount = modelForm.getRowCount();
            rowCount = Integer.toString(rCount);
        }
        if (UtilValidate.isNotEmpty(rowCount)) {
            writer.write("<input type=\"hidden\" name=\"_rowCount\" value=\"" + rowCount + "\"/>");
        }
        boolean useRowSubmit = modelForm.getUseRowSubmit();
        if (useRowSubmit) {
            writer.write("<input type=\"hidden\" name=\"_useRowSubmit\" value=\"Y\"/>");
        }
        
        Iterator submitFields = modelForm.getMultiSubmitFields().iterator();
        while (submitFields.hasNext()) {
            ModelFormField submitField = (ModelFormField)submitFields.next();
            if (submitField != null) {

                // Threw this in that as a hack to keep the submit button from expanding the first field
                // Needs a more rugged solution
                // WARNING: this method (renderMultiFormClose) must be called after the 
                // table that contains the list has been closed (to avoid validation errors) so
                // we cannot call here the methods renderFormatItemRowCell*: for this reason
                // they are now commented.

                // this.renderFormatItemRowCellOpen(writer, context, modelForm, submitField);
                // this.renderFormatItemRowCellClose(writer, context, modelForm, submitField);

                // this.renderFormatItemRowCellOpen(writer, context, modelForm, submitField);

                submitField.renderFieldString(writer, context, this);

                // this.renderFormatItemRowCellClose(writer, context, modelForm, submitField);

            }
        }
        writer.write("</form>");
        appendWhitespace(writer);
        renderEndingBoundaryComment(writer, "Form Widget", modelForm);
    }

    public void renderFormatListWrapperOpen(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException {

        String queryString = null;
        if (UtilValidate.isNotEmpty((String)context.get("queryString"))) {
            queryString = (String)context.get("queryString");
        } else {
            Map inputFields = (Map)context.get("requestParameters");
            // strip out any multi form fields if the form is of type multi
            if (modelForm.getType().equals("multi")) {
                inputFields = UtilHttp.removeMultiFormParameters(inputFields);
            }
            queryString = UtilHttp.urlEncodeArgs(inputFields);
        }
        context.put("_QBESTRING_", queryString);

        renderBeginningBoundaryComment(writer, "Form Widget", modelForm);

        if (this.renderPagination) {
            this.renderNextPrev(writer, context, modelForm);
        }
        writer.write(" <table cellspacing=\"0\" class=\"");
        if(UtilValidate.isNotEmpty(modelForm.getDefaultTableStyle())) {
            writer.write(modelForm.getDefaultTableStyle());
        } else {
            writer.write("basic-table form-widget-table dark-grid");
        }
        writer.write("\">");
        appendWhitespace(writer);
    }

    public void renderFormatListWrapperClose(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.write(" </table>");

        appendWhitespace(writer);
        if (this.renderPagination) {
            this.renderNextPrev(writer, context, modelForm);
        }
        renderEndingBoundaryComment(writer, "Form Widget", modelForm);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatHeaderRowOpen(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelForm)
     */
    public void renderFormatHeaderRowOpen(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.write("  <tr");
        String headerStyle = modelForm.getHeaderRowStyle();
        writer.write(" class=\"");
        if (UtilValidate.isNotEmpty(headerStyle)) {
            writer.write(headerStyle);
        } else {
            writer.write("header-row");
        }
        writer.write("\">");
        appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatHeaderRowClose(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelForm)
     */
    public void renderFormatHeaderRowClose(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.write("  </tr>");

        appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatHeaderRowCellOpen(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelForm, org.ofbiz.widget.form.ModelFormField, int positionSpan)
     */
    public void renderFormatHeaderRowCellOpen(Writer writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField, int positionSpan) throws IOException {
        writer.write("   <td");
        String areaStyle = modelFormField.getTitleAreaStyle();
        if (positionSpan > 1) {
            writer.write(" colspan=\"");
            writer.write(Integer.toString(positionSpan));
            writer.write("\"");
        }
        if (UtilValidate.isNotEmpty(areaStyle)) {
            writer.write(" class=\"");
            writer.write(areaStyle);
            writer.write("\"");
        }
        writer.write(">");
        //appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatHeaderRowCellClose(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelForm, org.ofbiz.widget.form.ModelFormField)
     */
    public void renderFormatHeaderRowCellClose(Writer writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField) throws IOException {
        writer.write("</td>");
        appendWhitespace(writer);
    }

    public void renderFormatHeaderRowFormCellOpen(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.write("   <td");
        String areaStyle = modelForm.getFormTitleAreaStyle();
        if (UtilValidate.isNotEmpty(areaStyle)) {
            writer.write(" class=\"");
            writer.write(areaStyle);
            writer.write("\"");
        }
        writer.write(">");
        //appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatHeaderRowFormCellClose(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelForm)
     */
    public void renderFormatHeaderRowFormCellClose(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.write("</td>");
        appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatHeaderRowFormCellTitleSeparator(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelForm, boolean)
     */
    public void renderFormatHeaderRowFormCellTitleSeparator(Writer writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField, boolean isLast) throws IOException {
        
        String titleStyle = modelFormField.getTitleStyle();
        if (UtilValidate.isNotEmpty(titleStyle)) {
            writer.write("<span class=\"");
            writer.write(titleStyle);
            writer.write("\">");
        }
        if (isLast) {
            writer.write(" - ");
        } else {
            writer.write(" - ");
        }
        if (UtilValidate.isNotEmpty(titleStyle)) {
            writer.write("</span>");
        }
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatItemRowOpen(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelForm)
     */
    public void renderFormatItemRowOpen(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        Integer itemIndex = (Integer)context.get("itemIndex"); 
        
        writer.write("  <tr");
        if (itemIndex!=null) {
            
            if (itemIndex.intValue()%2==0) {
               String evenRowStyle = modelForm.getEvenRowStyle();
               if (UtilValidate.isNotEmpty(evenRowStyle)) {
                   writer.write(" class=\"");
                   writer.write(evenRowStyle);
                   writer.write("\"");
                }
            } else {
                  String oddRowStyle = modelForm.getOddRowStyle();
                  if (UtilValidate.isNotEmpty(oddRowStyle)) {
                      writer.write(" class=\"");
                      writer.write(oddRowStyle);
                      writer.write("\"");
                  }
            }
        }
        writer.write(">");
        appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatItemRowClose(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelForm)
     */
    public void renderFormatItemRowClose(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.write("  </tr>");

        appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatItemRowCellOpen(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelForm, org.ofbiz.widget.form.ModelFormField)
     */
    public void renderFormatItemRowCellOpen(Writer writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField, int positionSpan) throws IOException {
        writer.write("   <td");
        String areaStyle = modelFormField.getWidgetAreaStyle();
        if (positionSpan > 1) {
            writer.write(" colspan=\"");
            writer.write(Integer.toString(positionSpan));
            writer.write("\"");
        }
        if (UtilValidate.isNotEmpty(areaStyle)) {
            writer.write(" class=\"");
            writer.write(areaStyle);
            writer.write("\"");
        }
        writer.write(">");
        //appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatItemRowCellClose(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelForm, org.ofbiz.widget.form.ModelFormField)
     */
    public void renderFormatItemRowCellClose(Writer writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField) throws IOException {
        writer.write("</td>");
        appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatItemRowFormCellOpen(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelForm)
     */
    public void renderFormatItemRowFormCellOpen(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.write("   <td");
        String areaStyle = modelForm.getFormWidgetAreaStyle();
        if (UtilValidate.isNotEmpty(areaStyle)) {
            writer.write(" class=\"");
            writer.write(areaStyle);
            writer.write("\"");
        }
        writer.write(">");
        //appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatItemRowFormCellClose(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelForm)
     */
    public void renderFormatItemRowFormCellClose(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.write("</td>");
        appendWhitespace(writer);
    }

    public void renderFormatSingleWrapperOpen(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.write(" <table cellspacing=\"0\"");
        if(UtilValidate.isNotEmpty(modelForm.getDefaultTableStyle())) {
            writer.write(" class=\"" + modelForm.getDefaultTableStyle() + "\"");
        }
        writer.write(">");
        appendWhitespace(writer);
    }

    public void renderFormatSingleWrapperClose(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.write(" </table>");
        appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatFieldRowOpen(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelForm)
     */
    public void renderFormatFieldRowOpen(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.write("  <tr>");
        appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatFieldRowClose(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelForm)
     */
    public void renderFormatFieldRowClose(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.write("  </tr>");
        appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatFieldRowTitleCellOpen(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelFormField)
     */
    public void renderFormatFieldRowTitleCellOpen(Writer writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
        writer.write("   <td");
        String areaStyle = modelFormField.getTitleAreaStyle();
        if (UtilValidate.isNotEmpty(areaStyle)) {
            writer.write(" class=\"");
            writer.write(areaStyle);
            writer.write("\"");
        } else {
            writer.write(" class=\"label\"");
        }
        writer.write(">");
        //appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatFieldRowTitleCellClose(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelFormField)
     */
    public void renderFormatFieldRowTitleCellClose(Writer writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
        writer.write("</td>");
        appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatFieldRowSpacerCell(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelFormField)
     */
    public void renderFormatFieldRowSpacerCell(Writer writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
        // Embedded styling - bad idea
        //writer.write("<td>&nbsp;</td>");

        //appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatFieldRowWidgetCellOpen(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelFormField, int)
     */
    public void renderFormatFieldRowWidgetCellOpen(Writer writer, Map<String, Object> context, ModelFormField modelFormField, int positions, int positionSpan, Integer nextPositionInRow) throws IOException {
//        writer.write("<td width=\"");
//        if (nextPositionInRow != null || modelFormField.getPosition() > 1) {
//            writer.write("30");
//        } else {
//            writer.write("80");
//        }
//        writer.write("%\"");
        writer.write("   <td");
        if (positionSpan > 0) {
            writer.write(" colspan=\"");
            // do a span of 1 for this column, plus 3 columns for each spanned 
            //position or each blank position that this will be filling in 
            writer.write(Integer.toString(1 + (positionSpan * 3)));
            writer.write("\"");
        }
        String areaStyle = modelFormField.getWidgetAreaStyle();
        if (UtilValidate.isNotEmpty(areaStyle)) {
            writer.write(" class=\"");
            writer.write(areaStyle);
            writer.write("\"");
        }
        writer.write(">");

        //appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatFieldRowWidgetCellClose(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelFormField, int)
     */
    public void renderFormatFieldRowWidgetCellClose(Writer writer, Map<String, Object> context, ModelFormField modelFormField, int positions, int positionSpan, Integer nextPositionInRow) throws IOException {
        writer.write("</td>");
        appendWhitespace(writer);
    }

    public void renderFormatEmptySpace(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.write("&nbsp;");
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderTextFindField(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelFormField.TextFindField)
     */
    public void renderTextFindField(Writer writer, Map<String, Object> context, TextFindField textFindField) throws IOException {

        ModelFormField modelFormField = textFindField.getModelFormField();
        
        String defaultOption = textFindField.getDefaultOption();
        Locale locale = (Locale)context.get("locale");
        if (!textFindField.getHideOptions()) {
            String opEquals = UtilProperties.getMessage("conditional", "equals", locale);
            String opBeginsWith = UtilProperties.getMessage("conditional", "begins_with", locale);
            String opContains = UtilProperties.getMessage("conditional", "contains", locale);
            String opIsEmpty = UtilProperties.getMessage("conditional", "is_empty", locale);
            String opNotEqual = UtilProperties.getMessage("conditional", "not_equal", locale);
            writer.write(" <select name=\"");
            writer.write(modelFormField.getParameterName(context));
            writer.write("_op\" class=\"selectBox\">");
            writer.write("<option value=\"equals\"" + ("equals".equals(defaultOption)? " selected": "") + ">" + opEquals + "</option>");
            writer.write("<option value=\"like\"" + ("like".equals(defaultOption)? " selected": "") + ">" + opBeginsWith + "</option>");
            writer.write("<option value=\"contains\"" + ("contains".equals(defaultOption)? " selected": "") + ">" + opContains + "</option>");
            writer.write("<option value=\"empty\"" + ("empty".equals(defaultOption)? " selected": "") + ">" + opIsEmpty + "</option>");
            writer.write("<option value=\"notEqual\"" + ("notEqual".equals(defaultOption)? " selected": "") + ">" + opNotEqual + "</option>");
            writer.write("</select>");
        } else {
            writer.write(" <input type=\"hidden\" name=\"");
            writer.write(modelFormField.getParameterName(context));
            writer.write("_op\" value=\"" + defaultOption + "\"/>");
        }
        
        writer.write("<input type=\"text\"");

        appendClassNames(writer, context, modelFormField);

        writer.write(" name=\"");
        writer.write(modelFormField.getParameterName(context));
        writer.write('"');

        String value = modelFormField.getEntry(context, textFindField.getDefaultValue(context));
        if (UtilValidate.isNotEmpty(value)) {
            writer.write(" value=\"");
            writer.write(value);
            writer.write('"');
        }

        writer.write(" size=\"");
        writer.write(Integer.toString(textFindField.getSize()));
        writer.write('"');

        Integer maxlength = textFindField.getMaxlength();
        if (maxlength != null) {
            writer.write(" maxlength=\"");
            writer.write(maxlength.toString());
            writer.write('"');
        }

        if (!textFindField.getClientAutocompleteField()) {
            writer.write(" autocomplete=\"off\"");
        }

        writer.write("/>");

        if (UtilValidate.isNotEmpty(modelFormField.getTitleStyle())) {
            writer.write(" <span class=\"");
            writer.write(modelFormField.getTitleStyle());
            writer.write("\">");
        }

        String ignoreCase = UtilProperties.getMessage("conditional", "ignore_case", locale);
        boolean ignCase = textFindField.getIgnoreCase();

        if (!textFindField.getHideIgnoreCase()) {
            writer.write(" <input type=\"checkbox\" name=\"");
            writer.write(modelFormField.getParameterName(context));
            writer.write("_ic\" value=\"Y\"" + (ignCase ? " checked=\"checked\"" : "") + "/>");
            writer.write(ignoreCase);
        } else {
            writer.write( "<input type=\"hidden\" name=\"");
            writer.write(modelFormField.getParameterName(context));
            writer.write("_ic\" value=\"" + (ignCase ? "Y" : "") + "\"/>");
        }
        
        if (UtilValidate.isNotEmpty(modelFormField.getTitleStyle())) {
            writer.write("</span>");
        }

        this.appendTooltip(writer, context, modelFormField);

        //appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderRangeFindField(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelFormField.RangeFindField)
     */
    public void renderRangeFindField(Writer writer, Map<String, Object> context, RangeFindField rangeFindField) throws IOException {

        ModelFormField modelFormField = rangeFindField.getModelFormField();
        Locale locale = (Locale)context.get("locale");
        String opEquals = UtilProperties.getMessage("conditional", "equals", locale);
        String opGreaterThan = UtilProperties.getMessage("conditional", "greater_than", locale);
        String opGreaterThanEquals = UtilProperties.getMessage("conditional", "greater_than_equals", locale);
        String opLessThan = UtilProperties.getMessage("conditional", "less_than", locale);
        String opLessThanEquals = UtilProperties.getMessage("conditional", "less_than_equals", locale);
        //String opIsEmpty = UtilProperties.getMessage("conditional", "is_empty", locale);

        writer.write("<input type=\"text\"");

        appendClassNames(writer, context, modelFormField);

        writer.write(" name=\"");
        writer.write(modelFormField.getParameterName(context));
        writer.write("_fld0_value\"");

        String value = modelFormField.getEntry(context, rangeFindField.getDefaultValue(context));
        if (UtilValidate.isNotEmpty(value)) {
            writer.write(" value=\"");
            writer.write(value);
            writer.write('"');
        }

        writer.write(" size=\"");
        writer.write(Integer.toString(rangeFindField.getSize()));
        writer.write('"');

        Integer maxlength = rangeFindField.getMaxlength();
        if (maxlength != null) {
            writer.write(" maxlength=\"");
            writer.write(maxlength.toString());
            writer.write('"');
        }

        if (!rangeFindField.getClientAutocompleteField()) {
            writer.write(" autocomplete=\"off\"");
        }

        writer.write("/>");

        if (UtilValidate.isNotEmpty(modelFormField.getTitleStyle())) {
            writer.write(" <span class=\"");
            writer.write(modelFormField.getTitleStyle());
            writer.write('"');
        }
        writer.write('>');

        writer.write(" <select name=\"");
        writer.write(modelFormField.getParameterName(context));
        writer.write("_fld0_op\" class=\"selectBox\">");
        writer.write("<option value=\"equals\" selected>" + opEquals + "</option>");
        writer.write("<option value=\"greaterThan\">" + opGreaterThan + "</option>");
        writer.write("<option value=\"greaterThanEqualTo\">" + opGreaterThanEquals + "</option>");
        writer.write("</select>");

        writer.write("</span>");

        writer.write(" <br/> ");

        writer.write("<input type=\"text\"");

        appendClassNames(writer, context, modelFormField);

        writer.write(" name=\"");
        writer.write(modelFormField.getParameterName(context));
        writer.write("_fld1_value\"");

        value = modelFormField.getEntry(context);
        if (UtilValidate.isNotEmpty(value)) {
            writer.write(" value=\"");
            writer.write(value);
            writer.write('"');
        }

        writer.write(" size=\"");
        writer.write(Integer.toString(rangeFindField.getSize()));
        writer.write('"');

        if (maxlength != null) {
            writer.write(" maxlength=\"");
            writer.write(maxlength.toString());
            writer.write('"');
        }

        if (!rangeFindField.getClientAutocompleteField()) {
            writer.write(" autocomplete=\"off\"");
        }

        writer.write("/>");

        if (UtilValidate.isNotEmpty(modelFormField.getTitleStyle())) {
            writer.write(" <span class=\"");
            writer.write(modelFormField.getTitleStyle());
            writer.write("\">");
        }

        writer.write(" <select name=\"");
        writer.write(modelFormField.getParameterName(context));
        writer.write("_fld1_op\" class=\"selectBox\">");
        writer.write("<option value=\"lessThan\">" + opLessThan + "</option>");
        writer.write("<option value=\"lessThanEqualTo\">" + opLessThanEquals + "</option>");
        writer.write("</select>");

        if (UtilValidate.isNotEmpty(modelFormField.getTitleStyle())) {
            writer.write("</span>");
        }

        this.appendTooltip(writer, context, modelFormField);

        //appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderDateFindField(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelFormField.DateFindField)
     */
    public void renderDateFindField(Writer writer, Map<String, Object> context, DateFindField dateFindField) throws IOException {
        ModelFormField modelFormField = dateFindField.getModelFormField();

        Locale locale = (Locale)context.get("locale");
        String opEquals = UtilProperties.getMessage("conditional", "equals", locale);
        String opGreaterThan = UtilProperties.getMessage("conditional", "greater_than", locale);
        String opSameDay = UtilProperties.getMessage("conditional", "same_day", locale);
        String opGreaterThanFromDayStart = UtilProperties.getMessage("conditional", 
                                                "greater_than_from_day_start", locale);
        String opLessThan = UtilProperties.getMessage("conditional", "less_than", locale);
        String opUpToDay = UtilProperties.getMessage("conditional", "up_to_day", locale);
        String opUpThruDay = UtilProperties.getMessage("conditional", "up_thru_day", locale);
        String opIsEmpty = UtilProperties.getMessage("conditional", "is_empty", locale);

        Map uiLabelMap = (Map) context.get("uiLabelMap");
        if (uiLabelMap == null) {
            Debug.logWarning("Could not find uiLabelMap in context", module);
        }
        String localizedInputTitle = "", localizedIconTitle = "";

        writer.write("<input type=\"text\"");

        appendClassNames(writer, context, modelFormField);

        writer.write(" name=\"");
        writer.write(modelFormField.getParameterName(context));
        writer.write("_fld0_value\"");

        // the default values for a timestamp
        int size = 25;
        int maxlength = 30;

        if ("date".equals(dateFindField.getType())) {
            size = maxlength = 10;
            if (uiLabelMap != null) {
                localizedInputTitle = (String) uiLabelMap.get("CommonFormatDate");
            }
        } else if ("time".equals(dateFindField.getType())) {
            size = maxlength = 8;
            if (uiLabelMap != null) {
                localizedInputTitle = (String) uiLabelMap.get("CommonFormatTime");
            }
        } else {
            if (uiLabelMap != null) {
                localizedInputTitle = (String) uiLabelMap.get("CommonFormatDateTime");
            }
        }
        writer.write(" title=\"");
        writer.write(localizedInputTitle);
        writer.write('"');

        String value = modelFormField.getEntry(context, dateFindField.getDefaultValue(context));
        if (UtilValidate.isNotEmpty(value)) {
            if (value.length() > maxlength) {
                value = value.substring(0, maxlength);
            }
            writer.write(" value=\"");
            writer.write(value);
            writer.write('"');
        }

        writer.write(" size=\"");
        writer.write(Integer.toString(size));
        writer.write('"');

        writer.write(" maxlength=\"");
        writer.write(Integer.toString(maxlength));
        writer.write('"');

        writer.write("/>");

        // search for a localized label for the icon
        if (uiLabelMap != null) {
            localizedIconTitle = (String) uiLabelMap.get("CommonViewCalendar");
        } 

        // add calendar pop-up button and seed data IF this is not a "time" type date-find
        if (!"time".equals(dateFindField.getType())) {
            if ("date".equals(dateFindField.getType())) {
                writer.write("<a href=\"javascript:call_cal_notime(document.");
            } else {
                writer.write("<a href=\"javascript:call_cal(document.");            
            }
            writer.write(modelFormField.getModelForm().getCurrentFormName(context));
            writer.write('.');
            writer.write(modelFormField.getParameterName(context));
            writer.write("_fld0_value,'");
            writer.write(UtilHttp.encodeBlanks(modelFormField.getEntry(context, dateFindField.getDefaultDateTimeString(context))));
            writer.write("');\">");

            writer.write("<img src=\"");
            this.appendContentUrl(writer, "/images/cal.gif");
            writer.write("\" width=\"16\" height=\"16\" border=\"0\" alt=\"");
            writer.write(localizedIconTitle);
            writer.write("\" title=\"");
            writer.write(localizedIconTitle);
            writer.write("\"/></a>");
        }

        if (UtilValidate.isNotEmpty(modelFormField.getTitleStyle())) {
            writer.write(" <span class=\"");
            writer.write(modelFormField.getTitleStyle());
            writer.write("\">");
        }

        writer.write(" <select name=\"");
        writer.write(modelFormField.getParameterName(context));
        writer.write("_fld0_op\" class=\"selectBox\">");
        writer.write("<option value=\"equals\" selected>" + opEquals + "</option>");
        writer.write("<option value=\"sameDay\">" + opSameDay + "</option>");
        writer.write("<option value=\"greaterThanFromDayStart\">" + opGreaterThanFromDayStart + "</option>");
        writer.write("<option value=\"greaterThan\">" + opGreaterThan + "</option>");
        writer.write("</select>");

        if (UtilValidate.isNotEmpty(modelFormField.getTitleStyle())) {
            writer.write(" </span>");
        }

        writer.write(" <br/> ");

        writer.write("<input type=\"text\"");

        appendClassNames(writer, context, modelFormField);

        writer.write(" name=\"");
        writer.write(modelFormField.getParameterName(context));
        writer.write("_fld1_value\"");

        writer.write(" title=\"");
        writer.write(localizedInputTitle);
        writer.write('"');

        value = modelFormField.getEntry(context);
        if (UtilValidate.isNotEmpty(value)) {
            if (value.length() > maxlength) {
                value = value.substring(0, maxlength);
            }
            writer.write(" value=\"");
            writer.write(value);
            writer.write('"');
        }

        writer.write(" size=\"");
        writer.write(Integer.toString(size));
        writer.write('"');

        writer.write(" maxlength=\"");
        writer.write(Integer.toString(maxlength));
        writer.write('"');

        writer.write("/>");

        // add calendar pop-up button and seed data IF this is not a "time" type date-find
        if (!"time".equals(dateFindField.getType())) {
            if ("date".equals(dateFindField.getType())) {
                writer.write("<a href=\"javascript:call_cal_notime(document.");
            } else {
                writer.write("<a href=\"javascript:call_cal(document.");            
            }
            writer.write(modelFormField.getModelForm().getCurrentFormName(context));
            writer.write('.');
            writer.write(modelFormField.getParameterName(context));
            writer.write("_fld1_value,'");
            writer.write(UtilHttp.encodeBlanks(modelFormField.getEntry(context, dateFindField.getDefaultDateTimeString(context))));
            writer.write("');\">");

            writer.write("<img src=\"");
            this.appendContentUrl(writer, "/images/cal.gif");
            writer.write("\" width=\"16\" height=\"16\" border=\"0\" alt=\"");
            writer.write(localizedIconTitle);
            writer.write("\" title=\"");
            writer.write(localizedIconTitle);
            writer.write("\"/></a>");
        }

        if (UtilValidate.isNotEmpty(modelFormField.getTitleStyle())) {
            writer.write(" <span class=\"");
            writer.write(modelFormField.getTitleStyle());
            writer.write("\">");
        }

        writer.write(" <select name=\"");
        writer.write(modelFormField.getParameterName(context));
        writer.write("_fld1_op\" class=\"selectBox\">");
        writer.write("<option value=\"lessThan\">" + opLessThan + "</option>");
        writer.write("<option value=\"upToDay\">" + opUpToDay + "</option>");
        writer.write("<option value=\"upThruDay\">" + opUpThruDay + "</option>");
        writer.write("<option value=\"empty\">" + opIsEmpty + "</option>");
        writer.write("</select>");

        if (UtilValidate.isNotEmpty(modelFormField.getTitleStyle())) {
            writer.write("</span>");
        }

        this.appendTooltip(writer, context, modelFormField);

        //appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderLookupField(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelFormField.LookupField)
     */
    public void renderLookupField(Writer writer, Map<String, Object> context, LookupField lookupField) throws IOException {
        ModelFormField modelFormField = lookupField.getModelFormField();

        writer.write("<input type=\"text\"");

        appendClassNames(writer, context, modelFormField);

        writer.write(" name=\"");
        writer.write(modelFormField.getParameterName(context));
        writer.write('"');

        String value = modelFormField.getEntry(context, lookupField.getDefaultValue(context));
        if (UtilValidate.isNotEmpty(value)) {
            writer.write(" value=\"");
            writer.write(value);
            writer.write('"');
        }

        writer.write(" size=\"");
        writer.write(Integer.toString(lookupField.getSize()));
        writer.write('"');

        Integer maxlength = lookupField.getMaxlength();
        if (maxlength != null) {
            writer.write(" maxlength=\"");
            writer.write(maxlength.toString());
            writer.write('"');
        }

        if (!lookupField.getClientAutocompleteField()) {
            writer.write(" autocomplete=\"off\"");
        }

        writer.write("/>");

        String descriptionFieldName = lookupField.getDescriptionFieldName();
        // add lookup pop-up button 
        if (UtilValidate.isNotEmpty(descriptionFieldName)) {
            writer.write("<a href=\"javascript:call_fieldlookup3(document.");
            writer.write(modelFormField.getModelForm().getCurrentFormName(context));
            writer.write('.');
            writer.write(modelFormField.getParameterName(context));
            writer.write(",'");
            writer.write(descriptionFieldName);
            writer.write(",'");
        } else {
            writer.write("<a href=\"javascript:call_fieldlookup2(document.");
            writer.write(modelFormField.getModelForm().getCurrentFormName(context));
            writer.write('.');
            writer.write(modelFormField.getParameterName(context));
            writer.write(",'");
        }
        writer.write(lookupField.getFormName(context));
        writer.write("'");
        List targetParameterList = lookupField.getTargetParameterList();
        if (targetParameterList.size() > 0) {
            Iterator targetParameterIter = targetParameterList.iterator();
            while (targetParameterIter.hasNext()) {
                String targetParameter = (String) targetParameterIter.next();
                // named like: document.${formName}.${targetParameter}.value
                writer.write(", document.");
                writer.write(modelFormField.getModelForm().getCurrentFormName(context));
                writer.write(".");
                writer.write(targetParameter);
                writer.write(".value");
            }
        }
        writer.write(");\">");
        writer.write("<img src=\"");
        this.appendContentUrl(writer, "/images/fieldlookup.gif");
        writer.write("\" width=\"16\" height=\"16\" border=\"0\" alt=\"Lookup\"/></a>");

        this.makeHyperlinkString(writer, lookupField.getSubHyperlink(), context);
        this.appendTooltip(writer, context, modelFormField);

        //appendWhitespace(writer);
    }

    public void renderNextPrev(Writer writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        boolean ajaxEnabled = false;
        List<ModelForm.UpdateArea> updateAreas = modelForm.getOnPaginateUpdateAreas();
        String targetService = modelForm.getPaginateTarget(context);
        if (this.javaScriptEnabled) {
            if (UtilValidate.isNotEmpty(updateAreas)) {
                ajaxEnabled = true;
            }
        }
        if (targetService == null) {
            targetService = "${targetService}";
        }
        if (UtilValidate.isEmpty(targetService) && updateAreas == null) {
            Debug.logWarning("Cannot paginate because TargetService is empty for the form: " + modelForm.getName(), module);
            return; 
        }

        // get the parametrized pagination index and size fields
        String viewIndexParam = modelForm.getPaginateIndexField(context);
        String viewSizeParam = modelForm.getPaginateSizeField(context);

        int viewIndex = modelForm.getViewIndex(context);
        int viewSize = modelForm.getViewSize(context);
        int listSize = modelForm.getListSize(context);

        int lowIndex = modelForm.getLowIndex(context);
        int highIndex = modelForm.getHighIndex(context);
        int actualPageSize = modelForm.getActualPageSize(context);

        // if this is all there seems to be (if listSize < 0, then size is unknown)
        if (actualPageSize >= listSize && listSize >= 0) return;

        // needed for the "Page" and "rows" labels
        Map uiLabelMap = (Map) context.get("uiLabelMap");
        String pageLabel = "";
        String rowsLabel = "";
        String ofLabel = "";
        if (uiLabelMap == null) {
            Debug.logWarning("Could not find uiLabelMap in context", module);
        } else {
            pageLabel = (String) uiLabelMap.get("CommonPage");
            rowsLabel = (String) uiLabelMap.get("CommonRows");
            ofLabel = (String) uiLabelMap.get("CommonOf");
            ofLabel = ofLabel.toLowerCase();
        }

        // for legacy support, the viewSizeParam is VIEW_SIZE and viewIndexParam is VIEW_INDEX when the fields are "viewSize" and "viewIndex"
        if (viewIndexParam.equals("viewIndex")) viewIndexParam = "VIEW_INDEX";
        if (viewSizeParam.equals("viewSize")) viewSizeParam = "VIEW_SIZE";

        String str = (String) context.get("_QBESTRING_");

        // strip legacy viewIndex/viewSize params from the query string
        String queryString = UtilHttp.stripViewParamsFromQueryString(str);

        // strip parametrized index/size params from the query string
        HashSet paramNames = new HashSet();
        paramNames.add(viewIndexParam);
        paramNames.add(viewSizeParam);
        queryString = UtilHttp.stripNamedParamsFromQueryString(queryString, paramNames);

        String anchor = "";
        String paginateAnchor = modelForm.getPaginateTargetAnchor();
        if (paginateAnchor != null) anchor = "#" + paginateAnchor;

        // Create separate url path String and request parameters String,
        // add viewIndex/viewSize parameters to request parameter String
        String urlPath = UtilHttp.removeQueryStringFromTarget(targetService);
        String prepLinkText = UtilHttp.getQueryStringFromTarget(targetService);
        if (prepLinkText == null) {
            prepLinkText = "";
        }
        if (prepLinkText.indexOf("?") < 0) {
            prepLinkText += "?";
        } else if (!prepLinkText.endsWith("?")) {
            prepLinkText += "&amp;";
        }
        if (!UtilValidate.isEmpty(queryString) && !queryString.equals("null")) {
            prepLinkText += queryString + "&amp;";
        }
        prepLinkText += viewSizeParam + "=" + viewSize + "&amp;" + viewIndexParam + "=";
        if (ajaxEnabled) {
            // Prepare params for prototype.js
            prepLinkText = prepLinkText.replace("?", "");
            prepLinkText = prepLinkText.replace("&amp;", "&");
        }

        writer.write("<div class=\"" + modelForm.getPaginateStyle() + "\">");
        appendWhitespace(writer);
        writer.write(" <ul>");
        appendWhitespace(writer);

        String linkText;

        // First button
        writer.write("  <li class=\"" + modelForm.getPaginateFirstStyle());
        if (viewIndex > 0) {
            writer.write("\"><a href=\"");
            if (ajaxEnabled) {
                writer.write("javascript:ajaxUpdateAreas('" + createAjaxParamsFromUpdateAreas(updateAreas, prepLinkText + 0 + anchor, context) + "')");
            } else {
                linkText = prepLinkText + 0 + anchor;
                writer.write(rh.makeLink(this.request, this.response, urlPath + linkText));
            }
            writer.write("\">" + modelForm.getPaginateFirstLabel(context) + "</a>");
        } else {
            // disabled button
            writer.write("-disabled\">" + modelForm.getPaginateFirstLabel(context));
        }
        writer.write("</li>");
        appendWhitespace(writer);

        // Previous button
        writer.write("  <li class=\"" + modelForm.getPaginatePreviousStyle());
        if (viewIndex > 0) {
            writer.write("\"><a href=\"");
            if (ajaxEnabled) {
                writer.write("javascript:ajaxUpdateAreas('" + createAjaxParamsFromUpdateAreas(updateAreas, prepLinkText + (viewIndex - 1) + anchor, context) + "')");
            } else {
                linkText = prepLinkText + (viewIndex - 1) + anchor;
                writer.write(rh.makeLink(this.request, this.response, urlPath + linkText));
            }
            writer.write("\">" + modelForm.getPaginatePreviousLabel(context) + "</a>");
        } else {
            // disabled button
            writer.write("-disabled\">" + modelForm.getPaginatePreviousLabel(context));
        }
        writer.write("</li>");
        appendWhitespace(writer);

        // Page select dropdown
        if (listSize > 0 && this.javaScriptEnabled) {
            writer.write("  <li>" + pageLabel + " <select name=\"page\" size=\"1\" onchange=\"");
            if (ajaxEnabled) {
                writer.write("javascript:ajaxUpdateAreas('" + createAjaxParamsFromUpdateAreas(updateAreas, prepLinkText + "' + this.value", context) + ")");
            } else {
                linkText = prepLinkText;
                if (linkText.startsWith("/")) {
                    linkText = linkText.substring(1);
                }
                writer.write("location.href = '" + urlPath + linkText + "' + this.value;");
            }
            writer.write("\">");
            // actual value
            int page = 0;
            for (int i = 0; i < listSize;) {
                if (page == viewIndex) {
                    writer.write("<option selected value=\"");
                } else {
                    writer.write("<option value=\"");
                }
                writer.write(Integer.toString(page));
                writer.write("\">");
                writer.write(Integer.toString(1 + page));
                writer.write("</option>");
                // increment page and calculate next index
                page++;
                i = page * viewSize;
            }
            writer.write("</select></li>");
            writer.write("<li>");
            writer.write(Integer.toString((lowIndex + 1)) + " - " + Integer.toString((lowIndex + actualPageSize)) + " " + ofLabel + " " + Integer.toString(listSize) + " " + rowsLabel);
            writer.write("</li>");
            appendWhitespace(writer);
        }

        // Next button
        writer.write("  <li class=\"" + modelForm.getPaginateNextStyle());
        if (highIndex < listSize) {
            writer.write("\"><a href=\"");
            if (ajaxEnabled) {
                writer.write("javascript:ajaxUpdateAreas('" + createAjaxParamsFromUpdateAreas(updateAreas, prepLinkText + (viewIndex + 1) + anchor, context) + "')");
            } else {
                linkText = prepLinkText + (viewIndex + 1) + anchor;
                writer.write(rh.makeLink(this.request, this.response, urlPath + linkText));
            }
            writer.write("\">" + modelForm.getPaginateNextLabel(context) + "</a>");
        } else {
            // disabled button
            writer.write("-disabled\">" + modelForm.getPaginateNextLabel(context));
        }
        writer.write("</li>");
        appendWhitespace(writer);

        // Last button
        writer.write("  <li class=\"" + modelForm.getPaginateLastStyle());
        if (highIndex < listSize) {
            writer.write("\"><a href=\"");
            if (ajaxEnabled) {
                writer.write("javascript:ajaxUpdateAreas('" + createAjaxParamsFromUpdateAreas(updateAreas, prepLinkText + (listSize / viewSize) + anchor, context) + "')");
            } else {
                linkText = prepLinkText + (listSize / viewSize) + anchor;
                writer.write(rh.makeLink(this.request, this.response, urlPath + linkText));
            }
            writer.write("\">" + modelForm.getPaginateLastLabel(context) + "</a>");
        } else {
            // disabled button
            writer.write("-disabled\">" + modelForm.getPaginateLastLabel(context));
        }
        writer.write("</li>");
        appendWhitespace(writer);

        writer.write(" </ul>");
        appendWhitespace(writer);
        writer.write("</div>");
        appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFileField(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelFormField.FileField)
     */
    public void renderFileField(Writer writer, Map<String, Object> context, FileField textField) throws IOException {
        ModelFormField modelFormField = textField.getModelFormField();

        writer.write("<input type=\"file\"");

        appendClassNames(writer, context, modelFormField);

        writer.write(" name=\"");
        writer.write(modelFormField.getParameterName(context));
        writer.write('"');

        String value = modelFormField.getEntry(context, textField.getDefaultValue(context));
        if (UtilValidate.isNotEmpty(value)) {
            writer.write(" value=\"");
            writer.write(StringEscapeUtils.escapeHtml(value));
            writer.write('"');
        }

        writer.write(" size=\"");
        writer.write(Integer.toString(textField.getSize()));
        writer.write('"');

        Integer maxlength = textField.getMaxlength();
        if (maxlength != null) {
            writer.write(" maxlength=\"");
            writer.write(maxlength.toString());
            writer.write('"');
        }

        if (!textField.getClientAutocompleteField()) {
            writer.write(" autocomplete=\"off\"");
        }

        writer.write("/>");

        this.makeHyperlinkString(writer, textField.getSubHyperlink(), context);

        this.appendTooltip(writer, context, modelFormField);

        //appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderPasswordField(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelFormField.PasswordField)
     */
    public void renderPasswordField(Writer writer, Map<String, Object> context, PasswordField passwordField) throws IOException {
        ModelFormField modelFormField = passwordField.getModelFormField();

        writer.write("<input type=\"password\"");

        appendClassNames(writer, context, modelFormField);

        writer.write(" name=\"");
        writer.write(modelFormField.getParameterName(context));
        writer.write('"');

        String value = modelFormField.getEntry(context, passwordField.getDefaultValue(context));
        if (UtilValidate.isNotEmpty(value)) {
            writer.write(" value=\"");
            writer.write(value);
            writer.write('"');
        }

        writer.write(" size=\"");
        writer.write(Integer.toString(passwordField.getSize()));
        writer.write('"');

        Integer maxlength = passwordField.getMaxlength();
        if (maxlength != null) {
            writer.write(" maxlength=\"");
            writer.write(maxlength.toString());
            writer.write('"');
        }

        String idName = modelFormField.getIdName();
        if (UtilValidate.isNotEmpty(idName)) {
            writer.write(" id=\"");
            writer.write(idName);
            writer.write('"');
        }

        if (!passwordField.getClientAutocompleteField()) {
            writer.write(" autocomplete=\"off\"");
        }

        writer.write("/>");

        this.addAsterisks(writer, context, modelFormField);

        this.makeHyperlinkString(writer, passwordField.getSubHyperlink(), context);

        this.appendTooltip(writer, context, modelFormField);

        //appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderImageField(java.io.Writer, java.util.Map, org.ofbiz.widget.form.ModelFormField.ImageField)
     */
    public void renderImageField(Writer writer, Map<String, Object> context, ImageField imageField) throws IOException {
        ModelFormField modelFormField = imageField.getModelFormField();

        writer.write("<img ");


        String value = modelFormField.getEntry(context, imageField.getValue(context));
        if (UtilValidate.isNotEmpty(value)) {
            writer.write(" src=\"");
            StringBuffer buffer = new StringBuffer();
            ContentUrlTag.appendContentPrefix(request, buffer);
            writer.write(buffer.toString());
            writer.write(value);
            writer.write('"');
        }

        writer.write(" border=\"");
        writer.write(imageField.getBorder());
        writer.write('"');

        Integer width = imageField.getWidth();
        if (width != null) {
            writer.write(" width=\"");
            writer.write(width.toString());
            writer.write('"');
        }

        Integer height = imageField.getHeight();
        if (height != null) {
            writer.write(" height=\"");
            writer.write(height.toString());
            writer.write('"');
        }

        String event = modelFormField.getEvent();
        String action = modelFormField.getAction(context);
        if (UtilValidate.isNotEmpty(event) && UtilValidate.isNotEmpty(action)) {
            writer.write(" ");
            writer.write(event);
            writer.write("=\"");
            writer.write(action);
            writer.write('"');
        }

        writer.write("/>");

        this.makeHyperlinkString(writer, imageField.getSubHyperlink(), context);

        this.appendTooltip(writer, context, modelFormField);

        //appendWhitespace(writer);
    }
    
    public void renderFieldGroupOpen(Writer writer, Map<String, Object> context, ModelForm.FieldGroup fieldGroup) throws IOException {
        String style = fieldGroup.getStyle(); 
        if (UtilValidate.isNotEmpty(style)) {
            writer.write("<div");
            writer.write(" class=\"");
            writer.write(style);
            writer.write("\">");
        }
    }
     
    public void renderFieldGroupClose(Writer writer, Map<String, Object> context, ModelForm.FieldGroup fieldGroup) throws IOException {
        String style = fieldGroup.getStyle(); 
        if (UtilValidate.isNotEmpty(style)) {
            writer.write("</div>");
        }
    }
     
    // TODO: Remove embedded styling
    public void renderBanner(Writer writer, Map<String, Object> context, ModelForm.Banner banner) throws IOException {
        writer.write(" <table width=\"100%\">  <tr>");
        String style = banner.getStyle(context);
        String leftStyle = banner.getLeftTextStyle(context);
        if (UtilValidate.isEmpty(leftStyle)) leftStyle = style;
        String rightStyle = banner.getRightTextStyle(context);
        if (UtilValidate.isEmpty(rightStyle)) rightStyle = style;
        
        String leftText = banner.getLeftText(context);
        if (UtilValidate.isNotEmpty(leftText)) {
            writer.write("   <td align=\"left\">");
            if (UtilValidate.isNotEmpty(leftStyle)) {
               writer.write("<div");
               writer.write(" class=\"");
               writer.write(leftStyle);
               writer.write("\"");
               writer.write(">" );
            }
            writer.write(leftText);
            if (UtilValidate.isNotEmpty(leftStyle)) {
                writer.write("</div>");
            }
            writer.write("</td>");
        }
        
        String text = banner.getText(context);
        if (UtilValidate.isNotEmpty(text)) {
            writer.write("   <td align=\"center\">");
            if (UtilValidate.isNotEmpty(style)) {
               writer.write("<div");
               writer.write(" class=\"");
               writer.write(style);
               writer.write("\"");
               writer.write(">" );
            }
            writer.write(text);
            if (UtilValidate.isNotEmpty(style)) {
                writer.write("</div>");
            }
            writer.write("</td>");
        }
        
        String rightText = banner.getRightText(context);
        if (UtilValidate.isNotEmpty(rightText)) {
            writer.write("   <td align=\"right\">");
            if (UtilValidate.isNotEmpty(rightStyle)) {
               writer.write("<div");
               writer.write(" class=\"");
               writer.write(rightStyle);
               writer.write("\"");
               writer.write(">" );
            }
            writer.write(rightText);
            if (UtilValidate.isNotEmpty(rightStyle)) {
                writer.write("</div>");
            }
            writer.write("</td>");
        }
        writer.write("</tr> </table>");
    }
    
    /**
     * Renders a link for the column header fields when there is a header-link="" specified in the <field > tag, using
     * style from header-link-style="".  Also renders a selectAll checkbox in multi forms.
     * @param writer
     * @param context
     * @param modelFormField
     * @param titleText
     */
    public void renderHyperlinkTitle(Writer writer, Map<String, Object> context, ModelFormField modelFormField, String titleText) throws IOException {
        if (UtilValidate.isNotEmpty(modelFormField.getHeaderLink())) {
            StringBuffer targetBuffer = new StringBuffer();
            FlexibleStringExpander target = new FlexibleStringExpander(modelFormField.getHeaderLink());         
            String fullTarget = target.expandString(context);
            targetBuffer.append(fullTarget);
            String targetType = HyperlinkField.DEFAULT_TARGET_TYPE;
            if (UtilValidate.isNotEmpty(targetBuffer.toString()) && targetBuffer.toString().toLowerCase().startsWith("javascript:")) {
            	targetType="plain";
            }
            makeHyperlinkString(writer, modelFormField.getHeaderLinkStyle(), targetType, targetBuffer.toString(), titleText, null, null, null);
        } else if (modelFormField.isRowSubmit()) {
            if (UtilValidate.isNotEmpty(titleText)) writer.write(titleText + "<br/>");
            writer.write("<input type=\"checkbox\" name=\"selectAll\" value=\"Y\" onclick=\"javascript:toggleAll(this, '");
            writer.write(modelFormField.getModelForm().getName());
            writer.write("');\"/>");
        } else {
             writer.write(titleText);
        }
    }

    /** Create an ajaxXxxx JavaScript CSV string from a list of UpdateArea objects. See
     * <code>selectall.js</code>.
     * @param updateAreas
     * @param extraParams Renderer-supplied additional target parameters
     * @param context
     * @return Parameter string or empty string if no UpdateArea objects were found
     */
    public String createAjaxParamsFromUpdateAreas(List<ModelForm.UpdateArea> updateAreas, String extraParams, Map<String, ? extends Object> context) {
        if (updateAreas == null) {
            return "";
        }
        String ajaxUrl = "";
        boolean firstLoop = true;
        for (ModelForm.UpdateArea updateArea : updateAreas) {
            if (firstLoop) {
                firstLoop = false;
            } else {
                ajaxUrl += ",";
            }
            String targetUrl = updateArea.getAreaTarget(context);
            String ajaxParams = getAjaxParamsFromTarget(targetUrl);
            if (UtilValidate.isNotEmpty(extraParams)) {
                if (ajaxParams.length() > 0 && !extraParams.startsWith("&")) {
                    ajaxParams += "&";
                }
                ajaxParams += extraParams;
            }
            ajaxUrl += updateArea.getAreaId() + ",";
            ajaxUrl += this.rh.makeLink(this.request, this.response, UtilHttp.removeQueryStringFromTarget(targetUrl));
            ajaxUrl += "," + ajaxParams;
        }
        return ajaxUrl;
    }
}
