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

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashSet;
import java.util.Calendar;
import java.sql.Timestamp;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilFormatOut;
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
public class HtmlFormRenderer implements FormStringRenderer {

    public static final String module = HtmlFormRenderer.class.getName();
    
    HttpServletRequest request;
    HttpServletResponse response;
    protected String lastFieldGroupId = "";

    protected HtmlFormRenderer() {}

    public HtmlFormRenderer(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    public void appendWhitespace(StringBuffer buffer) {
        // appending line ends for now, but this could be replaced with a simple space or something
        buffer.append("\r\n");
        //buffer.append(' ');
    }

    public void appendOfbizUrl(StringBuffer buffer, String location) {
        ServletContext ctx = (ServletContext) this.request.getAttribute("servletContext");
        RequestHandler rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");
        // make and append the link
        buffer.append(rh.makeLink(this.request, this.response, location));
    }

    public void appendContentUrl(StringBuffer buffer, String location) {
        ContentUrlTag.appendContentPrefix(this.request, buffer);
        buffer.append(location);
    }

    public void appendTooltip(StringBuffer buffer, Map context, ModelFormField modelFormField) {
        // render the tooltip, in other methods too
        String tooltip = modelFormField.getTooltip(context);
        if (UtilValidate.isNotEmpty(tooltip)) {
            buffer.append("<span class=\"");
            String tooltipStyle = modelFormField.getTooltipStyle();
            if (UtilValidate.isNotEmpty(tooltipStyle)) {
                buffer.append(tooltipStyle);
            } else {
                buffer.append("tooltip");
            }
            buffer.append("\">");
            buffer.append(tooltip);
            buffer.append("</span>");
        }
    }

    public void addAstericks(StringBuffer buffer, Map context, ModelFormField modelFormField) {
           
        boolean requiredField = modelFormField.getRequiredField();
        if (requiredField) {
            String requiredStyle = modelFormField.getRequiredFieldStyle();
            
            if (UtilValidate.isEmpty(requiredStyle)) {
               buffer.append("*");
            }
        }
    }
    
    public void appendClassNames(StringBuffer buffer, Map context, ModelFormField modelFormField) {
        String className = modelFormField.getWidgetStyle();
        if (UtilValidate.isNotEmpty(className) || modelFormField.shouldBeRed(context)) {
            buffer.append(" class=\"");
            buffer.append(className);
            // add a style of red if redWhen is true
            if (modelFormField.shouldBeRed(context)) {
                buffer.append(" alert");
            }
            buffer.append('"');
        }
    }
    
    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderDisplayField(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelFormField.DisplayField)
     */
    public void renderDisplayField(StringBuffer buffer, Map context, DisplayField displayField) {
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
            buffer.append(str);
        }
        String description = displayField.getDescription(context);
        //Replace new lines with <br>
        description = description.replaceAll("\n", "<br>");
        buffer.append(description);
        if (str.length() > 0) {
            buffer.append("</span>");
        }

        if (displayField instanceof DisplayEntityField) {
            this.makeHyperlinkString(buffer, ((DisplayEntityField) displayField).getSubHyperlink(), context);
        }
        
        this.appendTooltip(buffer, context, modelFormField);

        //this.appendWhitespace(buffer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderHyperlinkField(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelFormField.HyperlinkField)
     */
    public void renderHyperlinkField(StringBuffer buffer, Map context, HyperlinkField hyperlinkField) {
        ModelFormField modelFormField = hyperlinkField.getModelFormField();
        this.makeHyperlinkString(
            buffer,
            modelFormField.getWidgetStyle(),
            hyperlinkField.getTargetType(),
            hyperlinkField.getTarget(context),
            hyperlinkField.getDescription(context),
            hyperlinkField.getTargetWindow(context));
        this.appendTooltip(buffer, context, modelFormField);
        //this.appendWhitespace(buffer);
    }

    public void makeHyperlinkString(StringBuffer buffer, ModelFormField.SubHyperlink subHyperlink, Map context) {
        if (subHyperlink == null) {
            return;
        }
        if (subHyperlink.shouldUse(context)) {
            buffer.append(' ');
            this.makeHyperlinkString(
                buffer,
                subHyperlink.getLinkStyle(),
                subHyperlink.getTargetType(),
                subHyperlink.getTarget(context),
                subHyperlink.getDescription(context),
                subHyperlink.getTargetWindow(context));
        }
    }

    public void makeHyperlinkString(StringBuffer buffer, String linkStyle, String targetType, String target, String description, String targetWindow) {
        Map context = null;
        WidgetWorker.makeHyperlinkString(buffer, linkStyle, targetType, target, description, this.request, this.response, context, targetWindow);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderTextField(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelFormField.TextField)
     */
    public void renderTextField(StringBuffer buffer, Map context, TextField textField) {
        ModelFormField modelFormField = textField.getModelFormField();

        buffer.append("<input type=\"text\"");

        appendClassNames(buffer, context, modelFormField);

        buffer.append(" name=\"");
        buffer.append(modelFormField.getParameterName(context));
        buffer.append('"');

        String value = modelFormField.getEntry(context, textField.getDefaultValue(context));
        if (UtilValidate.isNotEmpty(value)) {
            buffer.append(" value=\"");
            buffer.append(UtilFormatOut.encodeXmlValue(value));
            buffer.append('"');
        }

        buffer.append(" size=\"");
        buffer.append(textField.getSize());
        buffer.append('"');

        Integer maxlength = textField.getMaxlength();
        if (maxlength != null) {
            buffer.append(" maxlength=\"");
            buffer.append(maxlength.intValue());
            buffer.append('"');
        }

        String idName = modelFormField.getIdName();
        if (UtilValidate.isNotEmpty(idName)) {
            buffer.append(" id=\"");
            buffer.append(idName);
            buffer.append('"');
        }

        String event = modelFormField.getEvent();
        String action = modelFormField.getAction(context);
        if (UtilValidate.isNotEmpty(event) && UtilValidate.isNotEmpty(action)) {
            buffer.append(" ");
            buffer.append(event);
            buffer.append("=\"");
            buffer.append(action);
            buffer.append('"');
        }

        buffer.append("/>");
        
        this.addAstericks(buffer, context, modelFormField);

        this.makeHyperlinkString(buffer, textField.getSubHyperlink(), context);

        this.appendTooltip(buffer, context, modelFormField);

        //this.appendWhitespace(buffer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderTextareaField(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelFormField.TextareaField)
     */
    public void renderTextareaField(StringBuffer buffer, Map context, TextareaField textareaField) {
        ModelFormField modelFormField = textareaField.getModelFormField();

        buffer.append("<textarea");

        appendClassNames(buffer, context, modelFormField);

        buffer.append(" name=\"");
        buffer.append(modelFormField.getParameterName(context));
        buffer.append('"');

        buffer.append(" cols=\"");
        buffer.append(textareaField.getCols());
        buffer.append('"');

        buffer.append(" rows=\"");
        buffer.append(textareaField.getRows());
        buffer.append('"');

        String idName = modelFormField.getIdName();
        if (UtilValidate.isNotEmpty(idName)) {
            buffer.append(" id=\"");
            buffer.append(idName);
            buffer.append('"');
        } else if (textareaField.getVisualEditorEnable()) {
            buffer.append(" id=\"");
            buffer.append("htmlEditArea");
            buffer.append('"');
        }
 
        buffer.append('>');

        String value = modelFormField.getEntry(context, textareaField.getDefaultValue(context));
        if (UtilValidate.isNotEmpty(value)) {
            buffer.append(UtilFormatOut.encodeXmlValue(value));
        }

        buffer.append("</textarea>");
        
        if (textareaField.getVisualEditorEnable()) {
            buffer.append("<script language=\"javascript\" src=\"/images/htmledit/whizzywig.js\" type=\"text/javascript\"></script>");
            buffer.append("<script language=\"javascript\" type=\"text/javascript\"> buttonPath = \"/images/htmledit/\"; cssFile=\"/images/htmledit/simple.css\";makeWhizzyWig(\"");
            if (UtilValidate.isNotEmpty(idName)) { 
                buffer.append(idName);
            } else {
                buffer.append("htmlEditArea");
            }
            buffer.append("\",\"");
            String buttons = textareaField.getVisualEditorButtons(context);
            if (UtilValidate.isNotEmpty(buttons)) {
                buffer.append(buttons);
            } else {
                buffer.append("all");
            }
            buffer.append("\") </script>");
        }

        this.addAstericks(buffer, context, modelFormField);

        this.appendTooltip(buffer, context, modelFormField);

        //this.appendWhitespace(buffer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderDateTimeField(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelFormField.DateTimeField)
     */
    public void renderDateTimeField(StringBuffer buffer, Map context, DateTimeField dateTimeField) {
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

        buffer.append("<input type=\"text\"");

        appendClassNames(buffer, context, modelFormField);

        buffer.append(" name=\"");
        if ("time-dropdown".equals(dateTimeField.getInputMethod())) {
            buffer.append(UtilHttp.makeCompositeParam(paramName, "date"));
        } else {
            buffer.append(paramName);
        }
        buffer.append('"');

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
        buffer.append(" title=\"");
        buffer.append(localizedInputTitle);
        buffer.append('"');
        
        String value = modelFormField.getEntry(context, dateTimeField.getDefaultValue(context));
        if (UtilValidate.isNotEmpty(value)) {
            if(value.length() > maxlength) {
                value = value.substring(0, maxlength);
            }
            buffer.append(" value=\"");
            buffer.append(value);
            buffer.append('"');
        }
        
        buffer.append(" size=\"");
        buffer.append(size);
        buffer.append('"');

        buffer.append(" maxlength=\"");
        buffer.append(maxlength);
        buffer.append('"');

        String idName = modelFormField.getIdName();
        if (UtilValidate.isNotEmpty(idName)) {
            buffer.append(" id=\"");
            buffer.append(idName);
            buffer.append('"');
        }

        buffer.append("/>");

        // search for a localized label for the icon
        if (uiLabelMap != null) {
            localizedIconTitle = (String) uiLabelMap.get("CommonViewCalendar");
        }

        // add calendar pop-up button and seed data IF this is not a "time" type date-time
        if (!"time".equals(dateTimeField.getType())) {
            if (shortDateInput) {
                buffer.append("<a href=\"javascript:call_cal_notime(document.");
            } else {
                buffer.append("<a href=\"javascript:call_cal(document.");
            }
            buffer.append(modelFormField.getModelForm().getCurrentFormName(context));
            buffer.append('.');
            if ("time-dropdown".equals(dateTimeField.getInputMethod())) {
                buffer.append(UtilHttp.makeCompositeParam(paramName, "date"));
            } else {
                buffer.append(paramName);
            }
            buffer.append(",'");
            buffer.append(UtilHttp.encodeBlanks(modelFormField.getEntry(context, defaultDateTimeString)));
            buffer.append("');\">");
           
            buffer.append("<img src=\"");
            this.appendContentUrl(buffer, "/images/cal.gif");
            buffer.append("\" width=\"16\" height=\"16\" border=\"0\" alt=\"");
            buffer.append(localizedIconTitle);
            buffer.append("\" title=\"");
            buffer.append(localizedIconTitle);
            buffer.append("\"/></a>");
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
            buffer.append("&nbsp;<select name=\"").append(UtilHttp.makeCompositeParam(paramName, "hour")).append("\"");
            buffer.append(classString).append(">");

            // keep the two cases separate because it's hard to understand a combined loop
            if (isTwelveHour) {
                for (int i = 1; i <= 12; i++) {
                    buffer.append("<option value=\"").append(i).append("\"");
                    if (cal != null) { 
                        int hour = cal.get(Calendar.HOUR_OF_DAY);
                        if (hour == 0) hour = 12;
                        if (hour > 12) hour -= 12;
                        if (i == hour) buffer.append(" selected");
                    }
                    buffer.append(">").append(i).append("</option>");
                }
            } else {
                for (int i = 0; i < 24; i++) {
                    buffer.append("<option value=\"").append(i).append("\"");
                    if (cal != null && i == cal.get(Calendar.HOUR_OF_DAY)) {
                        buffer.append(" selected");
                    }
                    buffer.append(">").append(i).append("</option>");
                }
            }
            
            // write the select for minutes
            buffer.append("</select>:<select name=\"");
            buffer.append(UtilHttp.makeCompositeParam(paramName, "minutes")).append("\"");
            buffer.append(classString).append(">");
            for (int i = 0; i < 60; i++) {
                buffer.append("<option value=\"").append(i).append("\"");
                if (cal != null && i == cal.get(Calendar.MINUTE)) {
                    buffer.append(" selected");
                }
                buffer.append(">").append(i).append("</option>");
            }
            buffer.append("</select>");

            // if 12 hour clock, write the AM/PM selector
            if (isTwelveHour) {
                String amSelected = ((cal != null && cal.get(Calendar.AM_PM) == Calendar.AM) ? "selected" : "");
                String pmSelected = ((cal != null && cal.get(Calendar.AM_PM) == Calendar.PM) ? "selected" : "");
                buffer.append("<select name=\"").append(UtilHttp.makeCompositeParam(paramName, "ampm")).append("\"");
                buffer.append(classString).append(">");
                buffer.append("<option value=\"").append("AM").append("\" ").append(amSelected).append(">AM</option>");
                buffer.append("<option value=\"").append("PM").append("\" ").append(pmSelected).append(">PM</option>");
                buffer.append("</select>");
            }

            // create a hidden field for the composite type, which is a Timestamp
            buffer.append("<input type=\"hidden\" name=\"");
            buffer.append(UtilHttp.makeCompositeParam(paramName, "compositeType"));
            buffer.append("\" value=\"Timestamp\">");
        }

        this.appendTooltip(buffer, context, modelFormField);

        //this.appendWhitespace(buffer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderDropDownField(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelFormField.DropDownField)
     */
    public void renderDropDownField(StringBuffer buffer, Map context, DropDownField dropDownField) {
        ModelFormField modelFormField = dropDownField.getModelFormField();
        ModelForm modelForm = modelFormField.getModelForm();

        String event = modelFormField.getEvent();
        String action = modelFormField.getAction(context);

        buffer.append("<select");

        appendClassNames(buffer, context, modelFormField);

        buffer.append(" name=\"");
        buffer.append(modelFormField.getParameterName(context));
        buffer.append('"');

        String idName = modelFormField.getIdName();
        if (UtilValidate.isNotEmpty(idName)) {
            buffer.append(" id=\"");
            buffer.append(idName);
            buffer.append('"');
        }

        if (dropDownField.isAllowMultiple()) {
            buffer.append(" multiple=\"multiple\"");
        }
        
        int otherFieldSize = dropDownField.getOtherFieldSize();
        String otherFieldName = dropDownField.getParameterNameOther(context);
        if (otherFieldSize > 0) {
            //buffer.append(" onchange=\"alert('ONCHANGE, process_choice:' + process_choice)\"");
            //buffer.append(" onchange='test_js()' ");
            buffer.append(" onchange=\"process_choice(this,document.");
            buffer.append(modelForm.getName());
            buffer.append(".");
            buffer.append(otherFieldName);
            buffer.append(")\" "); 
        }


        if (UtilValidate.isNotEmpty(event) && UtilValidate.isNotEmpty(action)) {
            buffer.append(" ");
            buffer.append(event);
            buffer.append("=\"");
            buffer.append(action);
            buffer.append('"');
        }

        buffer.append(" size=\"").append(dropDownField.getSize()).append("\">");

        String currentValue = modelFormField.getEntry(context);
        List allOptionValues = dropDownField.getAllOptionValues(context, modelForm.getDelegator());

        // if the current value should go first, stick it in
        if (UtilValidate.isNotEmpty(currentValue) && "first-in-list".equals(dropDownField.getCurrent())) {
            buffer.append("<option");
            buffer.append(" selected=\"selected\"");
            buffer.append(" value=\"");
            buffer.append(currentValue);
            buffer.append("\">");
            String explicitDescription = dropDownField.getCurrentDescription(context);
            if (UtilValidate.isNotEmpty(explicitDescription)) {
                buffer.append(explicitDescription);
            } else {
                buffer.append(ModelFormField.FieldInfoWithOptions.getDescriptionForOptionKey(currentValue, allOptionValues));
            }
            buffer.append("</option>");

            // add a "separator" option
            buffer.append("<option value=\"");
            buffer.append(currentValue);
            buffer.append("\">---</option>");
        }

        // if allow empty is true, add an empty option
        if (dropDownField.isAllowEmpty()) {
            buffer.append("<option value=\"\">&nbsp;</option>");
        }

        // list out all options according to the option list
        Iterator optionValueIter = allOptionValues.iterator();
        while (optionValueIter.hasNext()) {
            ModelFormField.OptionValue optionValue = (ModelFormField.OptionValue) optionValueIter.next();
            String noCurrentSelectedKey = dropDownField.getNoCurrentSelectedKey(context);
            buffer.append("<option");
            // if current value should be selected in the list, select it
            if (UtilValidate.isNotEmpty(currentValue) && currentValue.equals(optionValue.getKey()) && "selected".equals(dropDownField.getCurrent())) {
                buffer.append(" selected=\"selected\"");
            } else if (UtilValidate.isEmpty(currentValue) && noCurrentSelectedKey != null && noCurrentSelectedKey.equals(optionValue.getKey())) {
                buffer.append(" selected=\"selected\"");
            }
            buffer.append(" value=\"");
            buffer.append(optionValue.getKey());
            buffer.append("\">");
            buffer.append(optionValue.getDescription());
            buffer.append("</option>");
        }

        buffer.append("</select>");

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
            
            buffer.append("<noscript>");
            buffer.append("<input type='text' name='");
            buffer.append(otherFieldName);
            buffer.append("'/> ");
            buffer.append("</noscript>");
            buffer.append("\n<script type='text/javascript' language='JavaScript'><!--");
            buffer.append("\ndisa = ' disabled';");
            buffer.append("\nif(other_choice(document.");
            buffer.append(modelForm.getName());
            buffer.append(".");
            buffer.append(fieldName);
            buffer.append(")) disa = '';");
            buffer.append("\ndocument.write(\"<input type=");
            buffer.append("'text' name='");
            buffer.append(otherFieldName);
            buffer.append("' value='");
            buffer.append(otherValue);
            buffer.append("' size='");
            buffer.append(otherFieldSize);
            buffer.append("' ");
            buffer.append("\" +disa+ \" onfocus='check_choice(document.");
            buffer.append(modelForm.getName());
            buffer.append(".");
            buffer.append(fieldName);
            buffer.append(")'/>\");");
            buffer.append("\nif(disa && document.styleSheets)");
            buffer.append(" document.");
            buffer.append(modelForm.getName());
            buffer.append(".");
            buffer.append(otherFieldName);
            buffer.append(".style.visibility  = 'hidden';");
            buffer.append("\n//--></script>");
        }
        this.makeHyperlinkString(buffer, dropDownField.getSubHyperlink(), context);

        this.appendTooltip(buffer, context, modelFormField);

        //this.appendWhitespace(buffer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderCheckField(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelFormField.CheckField)
     */
    public void renderCheckField(StringBuffer buffer, Map context, CheckField checkField) {
        ModelFormField modelFormField = checkField.getModelFormField();
        ModelForm modelForm = modelFormField.getModelForm();
        String currentValue = modelFormField.getEntry(context);
        Boolean allChecked = checkField.isAllChecked(context);
        
        List allOptionValues = checkField.getAllOptionValues(context, modelForm.getDelegator());
        String event = modelFormField.getEvent();
        String action = modelFormField.getAction(context);

        // list out all options according to the option list
        Iterator optionValueIter = allOptionValues.iterator();
        while (optionValueIter.hasNext()) {
            ModelFormField.OptionValue optionValue = (ModelFormField.OptionValue) optionValueIter.next();

            buffer.append("<div");

            appendClassNames(buffer, context, modelFormField);

            buffer.append("><input type=\"checkbox\"");
            
            // if current value should be selected in the list, select it
            if (Boolean.TRUE.equals(allChecked)) {
                buffer.append(" checked=\"checked\"");
            } else if (Boolean.FALSE.equals(allChecked)) {
                // do nothing
            } else if (UtilValidate.isNotEmpty(currentValue) && currentValue.equals(optionValue.getKey())) {
                buffer.append(" checked=\"checked\"");
            }
            buffer.append(" name=\"");
            buffer.append(modelFormField.getParameterName(context));
            buffer.append('"');
            buffer.append(" value=\"");
            buffer.append(optionValue.getKey());
            buffer.append("\"");

            if (UtilValidate.isNotEmpty(event) && UtilValidate.isNotEmpty(action)) {
                buffer.append(" ");
                buffer.append(event);
                buffer.append("=\"");
                buffer.append(action);
                buffer.append('"');
            }
            
            buffer.append("/>");

            buffer.append(optionValue.getDescription());
            buffer.append("</div>");
        }

        this.appendTooltip(buffer, context, modelFormField);

        //this.appendWhitespace(buffer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderRadioField(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelFormField.RadioField)
     */
    public void renderRadioField(StringBuffer buffer, Map context, RadioField radioField) {
        ModelFormField modelFormField = radioField.getModelFormField();
        ModelForm modelForm = modelFormField.getModelForm();
        List allOptionValues = radioField.getAllOptionValues(context, modelForm.getDelegator());
        String currentValue = modelFormField.getEntry(context);
        String event = modelFormField.getEvent();
        String action = modelFormField.getAction(context);

        // list out all options according to the option list
        Iterator optionValueIter = allOptionValues.iterator();
        while (optionValueIter.hasNext()) {
            ModelFormField.OptionValue optionValue = (ModelFormField.OptionValue) optionValueIter.next();

            buffer.append("<div");

            appendClassNames(buffer, context, modelFormField);

            buffer.append("><input type=\"radio\"");
            
            // if current value should be selected in the list, select it
            String noCurrentSelectedKey = radioField.getNoCurrentSelectedKey(context);
            if (UtilValidate.isNotEmpty(currentValue) && currentValue.equals(optionValue.getKey())) {
                buffer.append(" checked=\"checked\"");
            } else if (UtilValidate.isEmpty(currentValue) && noCurrentSelectedKey != null && noCurrentSelectedKey.equals(optionValue.getKey())) {
                buffer.append(" checked=\"checked\"");
            }
            buffer.append(" name=\"");
            buffer.append(modelFormField.getParameterName(context));
            buffer.append('"');
            buffer.append(" value=\"");
            buffer.append(optionValue.getKey());
            buffer.append("\"");

            if (UtilValidate.isNotEmpty(event) && UtilValidate.isNotEmpty(action)) {
                buffer.append(" ");
                buffer.append(event);
                buffer.append("=\"");
                buffer.append(action);
                buffer.append('"');
            }
            
            buffer.append("/>");

            buffer.append(optionValue.getDescription());
            buffer.append("</div>");
        }

        this.appendTooltip(buffer, context, modelFormField);

        //this.appendWhitespace(buffer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderSubmitField(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelFormField.SubmitField)
     */
    public void renderSubmitField(StringBuffer buffer, Map context, SubmitField submitField) {
        ModelFormField modelFormField = submitField.getModelFormField();
        ModelForm modelForm = modelFormField.getModelForm();
        String singleClickAction = " onClick=\"javascript:submitFormDisableButton(this)\" ";
        String event = null;
        String action = null;        

        if ("text-link".equals(submitField.getButtonType())) {
            buffer.append("<a");

            appendClassNames(buffer, context, modelFormField);

            buffer.append(" href=\"javascript:document.");
            buffer.append(modelForm.getCurrentFormName(context));
            buffer.append(".submit()\">");

            buffer.append(modelFormField.getTitle(context));

            buffer.append("</a>");
        } else if ("image".equals(submitField.getButtonType())) {
            buffer.append("<input type=\"image\"");

            appendClassNames(buffer, context, modelFormField);

            buffer.append(" name=\"");
            buffer.append(modelFormField.getParameterName(context));
            buffer.append('"');

            String title = modelFormField.getTitle(context);
            if (UtilValidate.isNotEmpty(title)) {
                buffer.append(" alt=\"");
                buffer.append(title);
                buffer.append('"');
            }

            buffer.append(" src=\"");
            this.appendContentUrl(buffer, submitField.getImageLocation());
            buffer.append('"');
            
            event = modelFormField.getEvent();
            action = modelFormField.getAction(context);
            if (UtilValidate.isNotEmpty(event) && UtilValidate.isNotEmpty(action)) {
                buffer.append(" ");
                buffer.append(event);
                buffer.append("=\"");
                buffer.append(action);
                buffer.append('"');
            } else {
            	// disabling for now, using form onSubmit action instead: buffer.append(singleClickAction);
            }
            
            buffer.append("/>");
        } else {
            // default to "button"

            buffer.append("<input type=\"submit\"");

            appendClassNames(buffer, context, modelFormField);

            buffer.append(" name=\"");
            buffer.append(modelFormField.getParameterName(context));
            buffer.append('"');

            String title = modelFormField.getTitle(context);
            if (UtilValidate.isNotEmpty(title)) {
                buffer.append(" value=\"");
                buffer.append(title);
                buffer.append('"');
            }

            
            event = modelFormField.getEvent();
            action = modelFormField.getAction(context);
            if (UtilValidate.isNotEmpty(event) && UtilValidate.isNotEmpty(action)) {
                buffer.append(" ");
                buffer.append(event);
                buffer.append("=\"");
                buffer.append(action);
                buffer.append('"');
            } else {
            	//add single click JS onclick
                // disabling for now, using form onSubmit action instead: buffer.append(singleClickAction);
            }
            
            buffer.append("/>");
        }

        this.appendTooltip(buffer, context, modelFormField);

        //this.appendWhitespace(buffer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderResetField(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelFormField.ResetField)
     */
    public void renderResetField(StringBuffer buffer, Map context, ResetField resetField) {
        ModelFormField modelFormField = resetField.getModelFormField();

        buffer.append("<input type=\"reset\"");

        appendClassNames(buffer, context, modelFormField);

        buffer.append(" name=\"");
        buffer.append(modelFormField.getParameterName(context));
        buffer.append('"');

        String title = modelFormField.getTitle(context);
        if (UtilValidate.isNotEmpty(title)) {
            buffer.append(" value=\"");
            buffer.append(title);
            buffer.append('"');
        }

        buffer.append("/>");

        this.appendTooltip(buffer, context, modelFormField);

        //this.appendWhitespace(buffer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderHiddenField(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelFormField.HiddenField)
     */
    public void renderHiddenField(StringBuffer buffer, Map context, HiddenField hiddenField) {
        ModelFormField modelFormField = hiddenField.getModelFormField();
        String value = hiddenField.getValue(context);
        this.renderHiddenField(buffer, context, modelFormField, value);
    }

    public void renderHiddenField(StringBuffer buffer, Map context, ModelFormField modelFormField, String value) {
        buffer.append("<input type=\"hidden\"");

        buffer.append(" name=\"");
        buffer.append(modelFormField.getParameterName(context));
        buffer.append('"');

        if (UtilValidate.isNotEmpty(value)) {
            buffer.append(" value=\"");
            buffer.append(value);
            buffer.append('"');
        }

        buffer.append("/>");

        //this.appendWhitespace(buffer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderIgnoredField(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelFormField.IgnoredField)
     */
    public void renderIgnoredField(StringBuffer buffer, Map context, IgnoredField ignoredField) {
        // do nothing, it's an ignored field; could add a comment or something if we wanted to
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFieldTitle(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelFormField)
     */
    public void renderFieldTitle(StringBuffer buffer, Map context, ModelFormField modelFormField) {
        String tempTitleText = modelFormField.getTitle(context);
        String titleText = UtilHttp.encodeAmpersands(tempTitleText);
        
        if (UtilValidate.isNotEmpty(titleText)) {
            if (UtilValidate.isNotEmpty(modelFormField.getTitleStyle())) {
                buffer.append("<span class=\"");
                buffer.append(modelFormField.getTitleStyle());
                buffer.append("\">");
            }
            renderHyperlinkTitle(buffer, context, modelFormField, titleText);         
            if (UtilValidate.isNotEmpty(modelFormField.getTitleStyle())) {
                buffer.append("</span>");
            }

            //this.appendWhitespace(buffer);
        }
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFieldTitle(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelFormField)
     */
    public void renderSingleFormFieldTitle(StringBuffer buffer, Map context, ModelFormField modelFormField) {
        boolean requiredField = modelFormField.getRequiredField();
        if (requiredField) {
            
            String requiredStyle = modelFormField.getRequiredFieldStyle();
            if (UtilValidate.isEmpty(requiredStyle)) {
                requiredStyle = modelFormField.getTitleStyle();
            }
            
            if (UtilValidate.isNotEmpty(requiredStyle)) {
                buffer.append("<span class=\"");
                buffer.append(requiredStyle);
                buffer.append("\">");
            }
            renderHyperlinkTitle(buffer, context, modelFormField, modelFormField.getTitle(context)); 
            if (UtilValidate.isNotEmpty(requiredStyle)) {
                buffer.append("</span>");
            }

            //this.appendWhitespace(buffer);
        } else {
            renderFieldTitle(buffer, context, modelFormField);
        }
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormOpen(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelForm)
     */
    public void renderFormOpen(StringBuffer buffer, Map context, ModelForm modelForm) {
        buffer.append("<!-- begin form widget -->");
        this.appendWhitespace(buffer);
        buffer.append("<form method=\"post\" ");
        String targ = modelForm.getTarget(context);
        String targetType = modelForm.getTargetType();
        // The 'action' attribute is mandatory in a form definition,
        // even if it is empty.
        buffer.append(" action=\"");
        if (targ != null && targ.length() > 0) {
            //this.appendOfbizUrl(buffer, "/" + targ);
            WidgetWorker.buildHyperlinkUrl(buffer, targ, targetType, request, response, context);
        }
        buffer.append("\" ");

        String formType = modelForm.getType();
        if (formType.equals("upload") ) {
            buffer.append(" enctype=\"multipart/form-data\"");
        }

        String targetWindow = modelForm.getTargetWindow(context);
        if (UtilValidate.isNotEmpty(targetWindow)) {
            buffer.append(" target=\"");
            buffer.append(targetWindow);
            buffer.append("\"");
        }

        String containerId =  modelForm.getContainerId();
        if (UtilValidate.isNotEmpty(containerId)) {
            buffer.append(" id=\"");
            buffer.append(containerId);
            buffer.append("\"");
        }

        buffer.append(" class=\"");
        String containerStyle =  modelForm.getContainerStyle();
        if (UtilValidate.isNotEmpty(containerStyle)) {
            buffer.append(containerStyle);
        } else {
            buffer.append("basic-form");
        }
        buffer.append("\"");
        
        buffer.append(" onSubmit=\"javascript:submitFormDisableSubmits(this)\"");

        buffer.append(" name=\"");
        buffer.append(modelForm.getCurrentFormName(context));
        buffer.append("\">");

        this.appendWhitespace(buffer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormClose(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelForm)
     */
    public void renderFormClose(StringBuffer buffer, Map context, ModelForm modelForm) {
        buffer.append("</form>");
        String focusFieldName = modelForm.getfocusFieldName();
        if (UtilValidate.isNotEmpty(focusFieldName)) {
            this.appendWhitespace(buffer);
            buffer.append("<script language=\"JavaScript\" type=\"text/javascript\">");
            this.appendWhitespace(buffer);
            buffer.append("document." + modelForm.getCurrentFormName(context) + ".");
            buffer.append(focusFieldName + ".focus();");
            this.appendWhitespace(buffer);
            buffer.append("</script>");
        }
        this.appendWhitespace(buffer);
        buffer.append("<!-- end form widget -->");

        this.appendWhitespace(buffer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormClose(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelForm)
     */
    public void renderMultiFormClose(StringBuffer buffer, Map context, ModelForm modelForm) {
        String rowCount = modelForm.getPassedRowCount(context);
        if (UtilValidate.isEmpty(rowCount)) {
            int rCount = modelForm.getRowCount();
            rowCount = Integer.toString(rCount);
        }
        if (UtilValidate.isNotEmpty(rowCount)) {
            buffer.append("<input type=\"hidden\" name=\"_rowCount\" value=\"" + rowCount + "\"/>");
        }
        boolean useRowSubmit = modelForm.getUseRowSubmit();
        if (useRowSubmit) {
            buffer.append("<input type=\"hidden\" name=\"_useRowSubmit\" value=\"Y\"/>");
        }
        
        ModelFormField submitField = modelForm.getMultiSubmitField();
        if (submitField != null) {

            // Threw this in that as a hack to keep the submit button from expanding the first field
            // Needs a more rugged solution
            // WARNING: this method (renderMultiFormClose) must be called after the 
            // table that contains the list has been closed (to avoid validation errors) so
            // we cannot call here the methods renderFormatItemRowCell*: for this reason
            // they are now commented.
            
            // this.renderFormatItemRowCellOpen(buffer, context, modelForm, submitField);
            // this.renderFormatItemRowCellClose(buffer, context, modelForm, submitField);
            
            // this.renderFormatItemRowCellOpen(buffer, context, modelForm, submitField);

            submitField.renderFieldString(buffer, context, this);

            // this.renderFormatItemRowCellClose(buffer, context, modelForm, submitField);
            
        }
        buffer.append("</form>");
        this.appendWhitespace(buffer);
        buffer.append("<!-- end form widget -->");

        this.appendWhitespace(buffer);
    }

    public void renderFormatListWrapperOpen(StringBuffer buffer, Map context, ModelForm modelForm) {

        buffer.append("<!-- begin form widget -->");
        this.appendWhitespace(buffer);
        if(UtilValidate.isNotEmpty(modelForm.getDefaultTableStyle())) {
            buffer.append(" <table");
            buffer.append(" class=\"");
            buffer.append(modelForm.getDefaultTableStyle());
            buffer.append("\" cellspacing=\"0\">");
        } else {
            buffer.append(" <table cellspacing=\"0\" class=\"basic-table form-widget-table dark-grid\">");
            // DEJ 20050101 removed the width=\"100%\", doesn't look very good with CSS float: left based side "columns"
        }

        this.appendWhitespace(buffer);
    }

    public void renderFormatListWrapperClose(StringBuffer buffer, Map context, ModelForm modelForm) {
        buffer.append(" </table>");

        this.appendWhitespace(buffer);
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
        this.renderNextPrev(buffer, context, modelForm);
        buffer.append("<!-- end form widget -->");
        this.appendWhitespace(buffer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatHeaderRowOpen(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelForm)
     */
    public void renderFormatHeaderRowOpen(StringBuffer buffer, Map context, ModelForm modelForm) {
        buffer.append("  <tr");
        String headerStyle = modelForm.getHeaderRowStyle();
        buffer.append(" class=\"");
        if (UtilValidate.isNotEmpty(headerStyle)) {
            buffer.append(headerStyle);
        } else {
            buffer.append("header-row");
        }
        buffer.append("\">");
        this.appendWhitespace(buffer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatHeaderRowClose(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelForm)
     */
    public void renderFormatHeaderRowClose(StringBuffer buffer, Map context, ModelForm modelForm) {
        buffer.append("  </tr>");

        this.appendWhitespace(buffer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatHeaderRowCellOpen(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelForm, org.ofbiz.widget.form.ModelFormField)
     */
    public void renderFormatHeaderRowCellOpen(StringBuffer buffer, Map context, ModelForm modelForm, ModelFormField modelFormField) {
        buffer.append("   <td");
        String areaStyle = modelFormField.getTitleAreaStyle();
        if (UtilValidate.isNotEmpty(areaStyle)) {
            buffer.append(" class=\"");
            buffer.append(areaStyle);
            buffer.append("\"");
        }
        buffer.append(">");
        //this.appendWhitespace(buffer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatHeaderRowCellClose(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelForm, org.ofbiz.widget.form.ModelFormField)
     */
    public void renderFormatHeaderRowCellClose(StringBuffer buffer, Map context, ModelForm modelForm, ModelFormField modelFormField) {
        buffer.append("</td>");
        this.appendWhitespace(buffer);
    }

    public void renderFormatHeaderRowFormCellOpen(StringBuffer buffer, Map context, ModelForm modelForm) {
        buffer.append("   <td");
        String areaStyle = modelForm.getFormTitleAreaStyle();
        if (UtilValidate.isNotEmpty(areaStyle)) {
            buffer.append(" class=\"");
            buffer.append(areaStyle);
            buffer.append("\"");
        }
        buffer.append(">");
        //this.appendWhitespace(buffer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatHeaderRowFormCellClose(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelForm)
     */
    public void renderFormatHeaderRowFormCellClose(StringBuffer buffer, Map context, ModelForm modelForm) {
        buffer.append("</td>");
        this.appendWhitespace(buffer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatHeaderRowFormCellTitleSeparator(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelForm, boolean)
     */
    public void renderFormatHeaderRowFormCellTitleSeparator(StringBuffer buffer, Map context, ModelForm modelForm, ModelFormField modelFormField, boolean isLast) {
        
        String titleStyle = modelFormField.getTitleStyle();
        if (UtilValidate.isNotEmpty(titleStyle)) {
            buffer.append("<span class=\"");
            buffer.append(titleStyle);
            buffer.append("\">");
        }
        if (isLast) {
            buffer.append(" - ");
        } else {
            buffer.append(" - ");
        }
        if (UtilValidate.isNotEmpty(titleStyle)) {
            buffer.append("</span>");
        }
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatItemRowOpen(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelForm)
     */
    public void renderFormatItemRowOpen(StringBuffer buffer, Map context, ModelForm modelForm) {
        Integer itemIndex = (Integer)context.get("itemIndex"); 
        
        buffer.append("  <tr");
        if (itemIndex!=null) {
            
            if (itemIndex.intValue()%2==0) {
               String evenRowStyle = modelForm.getEvenRowStyle();
               if (UtilValidate.isNotEmpty(evenRowStyle)) {
                   buffer.append(" class=\"");
                   buffer.append(evenRowStyle);
                   buffer.append("\"");
                }
            } else {
                  String oddRowStyle = modelForm.getOddRowStyle();
                  if (UtilValidate.isNotEmpty(oddRowStyle)) {
                      buffer.append(" class=\"");
                      buffer.append(oddRowStyle);
                      buffer.append("\"");
                  }
            }
        }
        buffer.append(">");
        this.appendWhitespace(buffer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatItemRowClose(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelForm)
     */
    public void renderFormatItemRowClose(StringBuffer buffer, Map context, ModelForm modelForm) {
        buffer.append("  </tr>");

        this.appendWhitespace(buffer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatItemRowCellOpen(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelForm, org.ofbiz.widget.form.ModelFormField)
     */
    public void renderFormatItemRowCellOpen(StringBuffer buffer, Map context, ModelForm modelForm, ModelFormField modelFormField) {
        buffer.append("   <td");
        String areaStyle = modelFormField.getWidgetAreaStyle();
        if (UtilValidate.isNotEmpty(areaStyle)) {
            buffer.append(" class=\"");
            buffer.append(areaStyle);
            buffer.append("\"");
        }
        buffer.append(">");
        //this.appendWhitespace(buffer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatItemRowCellClose(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelForm, org.ofbiz.widget.form.ModelFormField)
     */
    public void renderFormatItemRowCellClose(StringBuffer buffer, Map context, ModelForm modelForm, ModelFormField modelFormField) {
        buffer.append("</td>");
        this.appendWhitespace(buffer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatItemRowFormCellOpen(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelForm)
     */
    public void renderFormatItemRowFormCellOpen(StringBuffer buffer, Map context, ModelForm modelForm) {
        buffer.append("   <td");
        String areaStyle = modelForm.getFormWidgetAreaStyle();
        if (UtilValidate.isNotEmpty(areaStyle)) {
            buffer.append(" class=\"");
            buffer.append(areaStyle);
            buffer.append("\"");
        }
        buffer.append(">");
        //this.appendWhitespace(buffer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatItemRowFormCellClose(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelForm)
     */
    public void renderFormatItemRowFormCellClose(StringBuffer buffer, Map context, ModelForm modelForm) {
        buffer.append("</td>");
        this.appendWhitespace(buffer);
    }

    public void renderFormatSingleWrapperOpen(StringBuffer buffer, Map context, ModelForm modelForm) {
        buffer.append(" <table cellspacing=\"0\">");

        this.appendWhitespace(buffer);
    }

    public void renderFormatSingleWrapperClose(StringBuffer buffer, Map context, ModelForm modelForm) {
        buffer.append(" </table>");

        this.appendWhitespace(buffer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatFieldRowOpen(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelForm)
     */
    public void renderFormatFieldRowOpen(StringBuffer buffer, Map context, ModelForm modelForm) {
        buffer.append("  <tr>");

        this.appendWhitespace(buffer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatFieldRowClose(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelForm)
     */
    public void renderFormatFieldRowClose(StringBuffer buffer, Map context, ModelForm modelForm) {
        buffer.append("  </tr>");

        this.appendWhitespace(buffer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatFieldRowTitleCellOpen(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelFormField)
     */
    public void renderFormatFieldRowTitleCellOpen(StringBuffer buffer, Map context, ModelFormField modelFormField) {
        buffer.append("   <td");
        String areaStyle = modelFormField.getTitleAreaStyle();
        if (UtilValidate.isNotEmpty(areaStyle)) {
            buffer.append(" class=\"");
            buffer.append(areaStyle);
            buffer.append("\"");
        } else {
            buffer.append(" class=\"label\"");
        }
        buffer.append(">");
        //this.appendWhitespace(buffer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatFieldRowTitleCellClose(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelFormField)
     */
    public void renderFormatFieldRowTitleCellClose(StringBuffer buffer, Map context, ModelFormField modelFormField) {
        buffer.append("</td>");
        this.appendWhitespace(buffer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatFieldRowSpacerCell(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelFormField)
     */
    public void renderFormatFieldRowSpacerCell(StringBuffer buffer, Map context, ModelFormField modelFormField) {
        // Embedded styling - bad idea
        //buffer.append("<td>&nbsp;</td>");

        //this.appendWhitespace(buffer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatFieldRowWidgetCellOpen(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelFormField, int)
     */
    public void renderFormatFieldRowWidgetCellOpen(StringBuffer buffer, Map context, ModelFormField modelFormField, int positions, int positionSpan, Integer nextPositionInRow) {
//        buffer.append("<td width=\"");
//        if (nextPositionInRow != null || modelFormField.getPosition() > 1) {
//            buffer.append("30");
//        } else {
//            buffer.append("80");
//        }
//        buffer.append("%\"");
        buffer.append("   <td");
        if (positionSpan > 0) {
            buffer.append(" colspan=\"");
            // do a span of 1 for this column, plus 3 columns for each spanned 
            //position or each blank position that this will be filling in 
            buffer.append(1 + (positionSpan * 3));
            buffer.append("\"");
        }
        String areaStyle = modelFormField.getWidgetAreaStyle();
        if (UtilValidate.isNotEmpty(areaStyle)) {
            buffer.append(" class=\"");
            buffer.append(areaStyle);
            buffer.append("\"");
        }
        buffer.append(">");

        //this.appendWhitespace(buffer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatFieldRowWidgetCellClose(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelFormField, int)
     */
    public void renderFormatFieldRowWidgetCellClose(StringBuffer buffer, Map context, ModelFormField modelFormField, int positions, int positionSpan, Integer nextPositionInRow) {
        buffer.append("</td>");
        this.appendWhitespace(buffer);
    }

    public void renderFormatEmptySpace(StringBuffer buffer, Map context, ModelForm modelForm) {
        buffer.append("&nbsp;");
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderTextFindField(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelFormField.TextFindField)
     */
    public void renderTextFindField(StringBuffer buffer, Map context, TextFindField textFindField) {

        ModelFormField modelFormField = textFindField.getModelFormField();
        Locale locale = (Locale)context.get("locale");
        String opEquals = UtilProperties.getMessage("conditional", "equals", locale);
        String opBeginsWith = UtilProperties.getMessage("conditional", "begins_with", locale);
        String opContains = UtilProperties.getMessage("conditional", "contains", locale);
        String opIsEmpty = UtilProperties.getMessage("conditional", "is_empty", locale);
        String ignoreCase = UtilProperties.getMessage("conditional", "ignore_case", locale);

        String defaultOption = textFindField.getDefaultOption();
        boolean ignCase = textFindField.getIgnoreCase();

        buffer.append(" <select name=\"");
        buffer.append(modelFormField.getParameterName(context));
        buffer.append("_op\" class=\"selectBox\">");
        buffer.append("<option value=\"equals\"" + ("equals".equals(defaultOption)? " selected": "") + ">" + opEquals + "</option>");
        buffer.append("<option value=\"like\"" + ("like".equals(defaultOption)? " selected": "") + ">" + opBeginsWith + "</option>");
        buffer.append("<option value=\"contains\"" + ("contains".equals(defaultOption)? " selected": "") + ">" + opContains + "</option>");
        buffer.append("<option value=\"empty\"" + ("empty".equals(defaultOption)? " selected": "") + ">" + opIsEmpty + "</option>");
        buffer.append("</select>");
        
        buffer.append("<input type=\"text\"");

        appendClassNames(buffer, context, modelFormField);

        buffer.append(" name=\"");
        buffer.append(modelFormField.getParameterName(context));
        buffer.append('"');

        String value = modelFormField.getEntry(context, textFindField.getDefaultValue(context));
        if (UtilValidate.isNotEmpty(value)) {
            buffer.append(" value=\"");
            buffer.append(value);
            buffer.append('"');
        }

        buffer.append(" size=\"");
        buffer.append(textFindField.getSize());
        buffer.append('"');

        Integer maxlength = textFindField.getMaxlength();
        if (maxlength != null) {
            buffer.append(" maxlength=\"");
            buffer.append(maxlength.intValue());
            buffer.append('"');
        }

        buffer.append("/>");

        if (UtilValidate.isNotEmpty(modelFormField.getTitleStyle())) {
            buffer.append(" <span class=\"");
            buffer.append(modelFormField.getTitleStyle());
            buffer.append("\">");
        }

        buffer.append(" <input type=\"checkbox\" name=\"");
        buffer.append(modelFormField.getParameterName(context));
        buffer.append("_ic\" value=\"Y\"" + (ignCase ? " checked=\"checked\"" : "") + "/>");
        buffer.append(ignoreCase);
        
        if (UtilValidate.isNotEmpty(modelFormField.getTitleStyle())) {
            buffer.append("</span>");
        }

        this.appendTooltip(buffer, context, modelFormField);

        //this.appendWhitespace(buffer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderRangeFindField(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelFormField.RangeFindField)
     */
    public void renderRangeFindField(StringBuffer buffer, Map context, RangeFindField rangeFindField) {

        ModelFormField modelFormField = rangeFindField.getModelFormField();
        Locale locale = (Locale)context.get("locale");
        String opEquals = UtilProperties.getMessage("conditional", "equals", locale);
        String opGreaterThan = UtilProperties.getMessage("conditional", "greater_than", locale);
        String opGreaterThanEquals = UtilProperties.getMessage("conditional", "greater_than_equals", locale);
        String opLessThan = UtilProperties.getMessage("conditional", "less_than", locale);
        String opLessThanEquals = UtilProperties.getMessage("conditional", "less_than_equals", locale);
        //String opIsEmpty = UtilProperties.getMessage("conditional", "is_empty", locale);

        buffer.append("<input type=\"text\"");

        appendClassNames(buffer, context, modelFormField);

        buffer.append(" name=\"");
        buffer.append(modelFormField.getParameterName(context));
        buffer.append("_fld0_value\"");

        String value = modelFormField.getEntry(context, rangeFindField.getDefaultValue(context));
        if (UtilValidate.isNotEmpty(value)) {
            buffer.append(" value=\"");
            buffer.append(value);
            buffer.append('"');
        }

        buffer.append(" size=\"");
        buffer.append(rangeFindField.getSize());
        buffer.append('"');

        Integer maxlength = rangeFindField.getMaxlength();
        if (maxlength != null) {
            buffer.append(" maxlength=\"");
            buffer.append(maxlength.intValue());
            buffer.append('"');
        }

        buffer.append("/>");

        if (UtilValidate.isNotEmpty(modelFormField.getTitleStyle())) {
            buffer.append(" <span class=\"");
            buffer.append(modelFormField.getTitleStyle());
            buffer.append('"');
        }
        buffer.append('>');

        buffer.append(" <select name=\"");
        buffer.append(modelFormField.getParameterName(context));
        buffer.append("_fld0_op\" class=\"selectBox\">");
        buffer.append("<option value=\"equals\" selected>" + opEquals + "</option>");
        buffer.append("<option value=\"greaterThan\">" + opGreaterThan + "</option>");
        buffer.append("<option value=\"greaterThanEqualTo\">" + opGreaterThanEquals + "</option>");
        buffer.append("</select>");

        buffer.append("</span>");

        buffer.append(" <br/> ");

        buffer.append("<input type=\"text\"");

        appendClassNames(buffer, context, modelFormField);

        buffer.append(" name=\"");
        buffer.append(modelFormField.getParameterName(context));
        buffer.append("_fld1_value\"");

        value = modelFormField.getEntry(context);
        if (UtilValidate.isNotEmpty(value)) {
            buffer.append(" value=\"");
            buffer.append(value);
            buffer.append('"');
        }

        buffer.append(" size=\"");
        buffer.append(rangeFindField.getSize());
        buffer.append('"');

        if (maxlength != null) {
            buffer.append(" maxlength=\"");
            buffer.append(maxlength.intValue());
            buffer.append('"');
        }

        buffer.append("/>");

        if (UtilValidate.isNotEmpty(modelFormField.getTitleStyle())) {
            buffer.append(" <span class=\"");
            buffer.append(modelFormField.getTitleStyle());
            buffer.append("\">");
        }

        buffer.append(" <select name=\"");
        buffer.append(modelFormField.getParameterName(context));
        buffer.append("_fld1_op\" class=\"selectBox\">");
        buffer.append("<option value=\"lessThan\">" + opLessThan + "</option>");
        buffer.append("<option value=\"lessThanEqualTo\">" + opLessThanEquals + "</option>");
        buffer.append("</select>");

        if (UtilValidate.isNotEmpty(modelFormField.getTitleStyle())) {
            buffer.append("</span>");
        }

        this.appendTooltip(buffer, context, modelFormField);

        //this.appendWhitespace(buffer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderDateFindField(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelFormField.DateFindField)
     */
    public void renderDateFindField(StringBuffer buffer, Map context, DateFindField dateFindField) {
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

        buffer.append("<input type=\"text\"");

        appendClassNames(buffer, context, modelFormField);

        buffer.append(" name=\"");
        buffer.append(modelFormField.getParameterName(context));
        buffer.append("_fld0_value\"");

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
        buffer.append(" title=\"");
        buffer.append(localizedInputTitle);
        buffer.append('"');

        String value = modelFormField.getEntry(context, dateFindField.getDefaultValue(context));
        if (UtilValidate.isNotEmpty(value)) {
            if (value.length() > maxlength) {
                value = value.substring(0, maxlength);
            }
            buffer.append(" value=\"");
            buffer.append(value);
            buffer.append('"');
        }

        buffer.append(" size=\"");
        buffer.append(size);
        buffer.append('"');

        buffer.append(" maxlength=\"");
        buffer.append(maxlength);
        buffer.append('"');

        buffer.append("/>");

        // search for a localized label for the icon
        if (uiLabelMap != null) {
            localizedIconTitle = (String) uiLabelMap.get("CommonViewCalendar");
        } 

        // add calendar pop-up button and seed data IF this is not a "time" type date-find
        if (!"time".equals(dateFindField.getType())) {
            if ("date".equals(dateFindField.getType())) {
                buffer.append("<a href=\"javascript:call_cal_notime(document.");
            } else {
                buffer.append("<a href=\"javascript:call_cal(document.");            
            }
            buffer.append(modelFormField.getModelForm().getCurrentFormName(context));
            buffer.append('.');
            buffer.append(modelFormField.getParameterName(context));
            buffer.append("_fld0_value,'");
            buffer.append(UtilHttp.encodeBlanks(modelFormField.getEntry(context, dateFindField.getDefaultDateTimeString(context))));
            buffer.append("');\">");

            buffer.append("<img src=\"");
            this.appendContentUrl(buffer, "/images/cal.gif");
            buffer.append("\" width=\"16\" height=\"16\" border=\"0\" alt=\"");
            buffer.append(localizedIconTitle);
            buffer.append("\" title=\"");
            buffer.append(localizedIconTitle);
            buffer.append("\"/></a>");
        }

        if (UtilValidate.isNotEmpty(modelFormField.getTitleStyle())) {
            buffer.append(" <span class=\"");
            buffer.append(modelFormField.getTitleStyle());
            buffer.append("\">");
        }

        buffer.append(" <select name=\"");
        buffer.append(modelFormField.getParameterName(context));
        buffer.append("_fld0_op\" class=\"selectBox\">");
        buffer.append("<option value=\"equals\" selected>" + opEquals + "</option>");
        buffer.append("<option value=\"sameDay\">" + opSameDay + "</option>");
        buffer.append("<option value=\"greaterThanFromDayStart\">" + opGreaterThanFromDayStart + "</option>");
        buffer.append("<option value=\"greaterThan\">" + opGreaterThan + "</option>");
        buffer.append("</select>");

        if (UtilValidate.isNotEmpty(modelFormField.getTitleStyle())) {
            buffer.append(" </span>");
        }

        buffer.append(" <br/> ");

        buffer.append("<input type=\"text\"");

        appendClassNames(buffer, context, modelFormField);

        buffer.append(" name=\"");
        buffer.append(modelFormField.getParameterName(context));
        buffer.append("_fld1_value\"");

        buffer.append(" title=\"");
        buffer.append(localizedInputTitle);
        buffer.append('"');

        value = modelFormField.getEntry(context);
        if (UtilValidate.isNotEmpty(value)) {
            if (value.length() > maxlength) {
                value = value.substring(0, maxlength);
            }
            buffer.append(" value=\"");
            buffer.append(value);
            buffer.append('"');
        }

        buffer.append(" size=\"");
        buffer.append(size);
        buffer.append('"');

        buffer.append(" maxlength=\"");
        buffer.append(maxlength);
        buffer.append('"');

        buffer.append("/>");

        // add calendar pop-up button and seed data IF this is not a "time" type date-find
        if (!"time".equals(dateFindField.getType())) {
            if ("date".equals(dateFindField.getType())) {
                buffer.append("<a href=\"javascript:call_cal_notime(document.");
            } else {
                buffer.append("<a href=\"javascript:call_cal(document.");            
            }
            buffer.append(modelFormField.getModelForm().getCurrentFormName(context));
            buffer.append('.');
            buffer.append(modelFormField.getParameterName(context));
            buffer.append("_fld1_value,'");
            buffer.append(UtilHttp.encodeBlanks(modelFormField.getEntry(context, dateFindField.getDefaultDateTimeString(context))));
            buffer.append("');\">");

            buffer.append("<img src=\"");
            this.appendContentUrl(buffer, "/images/cal.gif");
            buffer.append("\" width=\"16\" height=\"16\" border=\"0\" alt=\"");
            buffer.append(localizedIconTitle);
            buffer.append("\" title=\"");
            buffer.append(localizedIconTitle);
            buffer.append("\"/></a>");
        }

        if (UtilValidate.isNotEmpty(modelFormField.getTitleStyle())) {
            buffer.append(" <span class=\"");
            buffer.append(modelFormField.getTitleStyle());
            buffer.append("\">");
        }

        buffer.append(" <select name=\"");
        buffer.append(modelFormField.getParameterName(context));
        buffer.append("_fld1_op\" class=\"selectBox\">");
        buffer.append("<option value=\"lessThan\">" + opLessThan + "</option>");
        buffer.append("<option value=\"upToDay\">" + opUpToDay + "</option>");
        buffer.append("<option value=\"upThruDay\">" + opUpThruDay + "</option>");
        buffer.append("<option value=\"empty\">" + opIsEmpty + "</option>");
        buffer.append("</select>");

        if (UtilValidate.isNotEmpty(modelFormField.getTitleStyle())) {
            buffer.append("</span>");
        }

        this.appendTooltip(buffer, context, modelFormField);

        //this.appendWhitespace(buffer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderLookupField(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelFormField.LookupField)
     */
    public void renderLookupField(StringBuffer buffer, Map context, LookupField lookupField) {
        ModelFormField modelFormField = lookupField.getModelFormField();

        buffer.append("<input type=\"text\"");

        appendClassNames(buffer, context, modelFormField);

        buffer.append(" name=\"");
        buffer.append(modelFormField.getParameterName(context));
        buffer.append('"');

        String value = modelFormField.getEntry(context, lookupField.getDefaultValue(context));
        if (UtilValidate.isNotEmpty(value)) {
            buffer.append(" value=\"");
            buffer.append(value);
            buffer.append('"');
        }

        buffer.append(" size=\"");
        buffer.append(lookupField.getSize());
        buffer.append('"');

        Integer maxlength = lookupField.getMaxlength();
        if (maxlength != null) {
            buffer.append(" maxlength=\"");
            buffer.append(maxlength.intValue());
            buffer.append('"');
        }

        buffer.append("/>");

        String descriptionFieldName = lookupField.getDescriptionFieldName();
        // add lookup pop-up button 
        if (UtilValidate.isNotEmpty(descriptionFieldName)) {
            buffer.append("<a href=\"javascript:call_fieldlookup3(document.");
            buffer.append(modelFormField.getModelForm().getCurrentFormName(context));
            buffer.append('.');
            buffer.append(modelFormField.getParameterName(context));
            buffer.append(",'");
            buffer.append(descriptionFieldName);
            buffer.append(",'");
        } else {
            buffer.append("<a href=\"javascript:call_fieldlookup2(document.");
            buffer.append(modelFormField.getModelForm().getCurrentFormName(context));
            buffer.append('.');
            buffer.append(modelFormField.getParameterName(context));
            buffer.append(",'");
        }
        buffer.append(lookupField.getFormName(context));
        buffer.append("'");
        List targetParameterList = lookupField.getTargetParameterList();
        if (targetParameterList.size() > 0) {
            Iterator targetParameterIter = targetParameterList.iterator();
            while (targetParameterIter.hasNext()) {
                String targetParameter = (String) targetParameterIter.next();
                // named like: document.${formName}.${targetParameter}.value
                buffer.append(", document.");
                buffer.append(modelFormField.getModelForm().getCurrentFormName(context));
                buffer.append(".");
                buffer.append(targetParameter);
                buffer.append(".value");
            }
        }
        buffer.append(");\">");
        buffer.append("<img src=\"");
        this.appendContentUrl(buffer, "/images/fieldlookup.gif");
        buffer.append("\" width=\"16\" height=\"16\" border=\"0\" alt=\"Lookup\"/></a>");

        this.makeHyperlinkString(buffer, lookupField.getSubHyperlink(), context);
        this.appendTooltip(buffer, context, modelFormField);

        //this.appendWhitespace(buffer);
    }

    public void renderNextPrev(StringBuffer buffer, Map context, ModelForm modelForm) {
        String targetService = modelForm.getPaginateTarget(context);
        if (targetService == null) {
            targetService = "${targetService}";
        }
        if (UtilValidate.isEmpty(targetService)) {
            Debug.logWarning("TargetService is empty.", module);   
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
        if (actualPageSize >= listSize && listSize >= 0) {
            return;
        }

        // for legacy support, the viewSizeParam is VIEW_SIZE and viewIndexParam is VIEW_INDEX when the fields are "viewSize" and "viewIndex"
        if (viewIndexParam.equals("viewIndex")) viewIndexParam = "VIEW_INDEX";
        if (viewSizeParam.equals("viewSize")) viewSizeParam = "VIEW_SIZE";

        String str = (String) context.get("_QBESTRING_");
        ServletContext ctx = (ServletContext) request.getAttribute("servletContext");
        RequestHandler rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");

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

        buffer.append(" <table border=\"0\" cellpadding=\"2\">\n");
        buffer.append("  <tr>\n");
        buffer.append("    <td align=\"right\">\n");
        buffer.append("      <b>\n");
        if (viewIndex > 0) {
            buffer.append(" <a href=\"");
            String linkText = targetService;
            if (linkText.indexOf("?") < 0)  linkText += "?";
            else linkText += "&amp;";
            if (queryString != null && !queryString.equals("null"))
                linkText += queryString + "&amp;";
            linkText += viewSizeParam + "=" + viewSize + "&amp;" + viewIndexParam + "=" + (viewIndex - 1) + anchor + "\"";

            // make the link
            String tmp = rh.makeLink(request, response, linkText);
            buffer.append(tmp);
            buffer.append(" class=\"").append(modelForm.getPaginatePreviousStyle()).append("\">").append(modelForm.getPaginatePreviousLabel(context)).append("</a>\n");

        }
        if (listSize > 0) {
            buffer.append("          <span class=\"tabletext\">" + (lowIndex + 1) + " - " + (lowIndex + actualPageSize ) + " of " + listSize + "</span> \n");
        }
        if (highIndex < listSize) {
            buffer.append(" <a href=\"");
            String linkText = "" + targetService;
            if (linkText.indexOf("?") < 0)  linkText += "?";
            else linkText += "&amp;";
            linkText += queryString + "&amp;" + viewSizeParam + "=" + viewSize + "&amp;" + viewIndexParam + "=" + (viewIndex + 1) + anchor + "\"";

            // make the link
            buffer.append(rh.makeLink(request, response, linkText));
            buffer.append(" class=\"").append(modelForm.getPaginatePreviousStyle()).append("\">").append(modelForm.getPaginateNextLabel(context)).append("</a>\n");

        }
        buffer.append("      </b>\n");
        buffer.append("   </td>\n");
        buffer.append("  </tr>\n");
        buffer.append(" </table>\n");

        this.appendWhitespace(buffer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFileField(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelFormField.FileField)
     */
    public void renderFileField(StringBuffer buffer, Map context, FileField textField) {
        ModelFormField modelFormField = textField.getModelFormField();

        buffer.append("<input type=\"file\"");

        appendClassNames(buffer, context, modelFormField);

        buffer.append(" name=\"");
        buffer.append(modelFormField.getParameterName(context));
        buffer.append('"');

        String value = modelFormField.getEntry(context, textField.getDefaultValue(context));
        if (UtilValidate.isNotEmpty(value)) {
            buffer.append(" value=\"");
            buffer.append(value);
            buffer.append('"');
        }

        buffer.append(" size=\"");
        buffer.append(textField.getSize());
        buffer.append('"');

        Integer maxlength = textField.getMaxlength();
        if (maxlength != null) {
            buffer.append(" maxlength=\"");
            buffer.append(maxlength.intValue());
            buffer.append('"');
        }

        buffer.append("/>");

        this.makeHyperlinkString(buffer, textField.getSubHyperlink(), context);

        this.appendTooltip(buffer, context, modelFormField);

        //this.appendWhitespace(buffer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderPasswordField(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelFormField.PasswordField)
     */
    public void renderPasswordField(StringBuffer buffer, Map context, PasswordField passwordField) {
        ModelFormField modelFormField = passwordField.getModelFormField();

        buffer.append("<input type=\"password\"");

        appendClassNames(buffer, context, modelFormField);

        buffer.append(" name=\"");
        buffer.append(modelFormField.getParameterName(context));
        buffer.append('"');

        String value = modelFormField.getEntry(context, passwordField.getDefaultValue(context));
        if (UtilValidate.isNotEmpty(value)) {
            buffer.append(" value=\"");
            buffer.append(value);
            buffer.append('"');
        }

        buffer.append(" size=\"");
        buffer.append(passwordField.getSize());
        buffer.append('"');

        Integer maxlength = passwordField.getMaxlength();
        if (maxlength != null) {
            buffer.append(" maxlength=\"");
            buffer.append(maxlength.intValue());
            buffer.append('"');
        }

        String idName = modelFormField.getIdName();
        if (UtilValidate.isNotEmpty(idName)) {
            buffer.append(" id=\"");
            buffer.append(idName);
            buffer.append('"');
        }

        buffer.append("/>");

        this.addAstericks(buffer, context, modelFormField);

        this.makeHyperlinkString(buffer, passwordField.getSubHyperlink(), context);

        this.appendTooltip(buffer, context, modelFormField);

        //this.appendWhitespace(buffer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderImageField(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.form.ModelFormField.ImageField)
     */
    public void renderImageField(StringBuffer buffer, Map context, ImageField imageField) {
        ModelFormField modelFormField = imageField.getModelFormField();

        buffer.append("<img ");


        String value = modelFormField.getEntry(context, imageField.getValue(context));
        if (UtilValidate.isNotEmpty(value)) {
            buffer.append(" src=\"");
            ContentUrlTag.appendContentPrefix(request, buffer);
            buffer.append(value);
            buffer.append('"');
        }

        buffer.append(" border=\"");
        buffer.append(imageField.getBorder());
        buffer.append('"');

        Integer width = imageField.getWidth();
        if (width != null) {
            buffer.append(" width=\"");
            buffer.append(width.intValue());
            buffer.append('"');
        }

        Integer height = imageField.getHeight();
        if (height != null) {
            buffer.append(" height=\"");
            buffer.append(height.intValue());
            buffer.append('"');
        }

        String event = modelFormField.getEvent();
        String action = modelFormField.getAction(context);
        if (UtilValidate.isNotEmpty(event) && UtilValidate.isNotEmpty(action)) {
            buffer.append(" ");
            buffer.append(event);
            buffer.append("=\"");
            buffer.append(action);
            buffer.append('"');
        }

        buffer.append("/>");

        this.makeHyperlinkString(buffer, imageField.getSubHyperlink(), context);

        this.appendTooltip(buffer, context, modelFormField);

        //this.appendWhitespace(buffer);
    }
    
    public void renderFieldGroupOpen(StringBuffer buffer, Map context, ModelForm.FieldGroup fieldGroup) {
        String style = fieldGroup.getStyle(); 
        if (UtilValidate.isNotEmpty(style)) {
            buffer.append("<div");
            buffer.append(" class=\"");
            buffer.append(style);
            buffer.append("\">");
        }
    }
     
    public void renderFieldGroupClose(StringBuffer buffer, Map context, ModelForm.FieldGroup fieldGroup) {
        String style = fieldGroup.getStyle(); 
        if (UtilValidate.isNotEmpty(style)) {
            buffer.append("</div>");
        }
    }
     
    // TODO: Remove embedded styling
    public void renderBanner(StringBuffer buffer, Map context, ModelForm.Banner banner) {
        buffer.append(" <table width=\"100%\">  <tr>");
        String style = banner.getStyle(context);
        String leftStyle = banner.getLeftTextStyle(context);
        if (UtilValidate.isEmpty(leftStyle)) leftStyle = style;
        String rightStyle = banner.getRightTextStyle(context);
        if (UtilValidate.isEmpty(rightStyle)) rightStyle = style;
        
        String leftText = banner.getLeftText(context);
        if (UtilValidate.isNotEmpty(leftText)) {
            buffer.append("   <td align=\"left\">");
            if (UtilValidate.isNotEmpty(leftStyle)) {
               buffer.append("<div");
               buffer.append(" class=\"");
               buffer.append(leftStyle);
               buffer.append("\"");
               buffer.append(">" );
            }
            buffer.append(leftText);
            if (UtilValidate.isNotEmpty(leftStyle)) {
                buffer.append("</div>");
            }
            buffer.append("</td>");
        }
        
        String text = banner.getText(context);
        if (UtilValidate.isNotEmpty(text)) {
            buffer.append("   <td align=\"center\">");
            if (UtilValidate.isNotEmpty(style)) {
               buffer.append("<div");
               buffer.append(" class=\"");
               buffer.append(style);
               buffer.append("\"");
               buffer.append(">" );
            }
            buffer.append(text);
            if (UtilValidate.isNotEmpty(style)) {
                buffer.append("</div>");
            }
            buffer.append("</td>");
        }
        
        String rightText = banner.getRightText(context);
        if (UtilValidate.isNotEmpty(rightText)) {
            buffer.append("   <td align=\"right\">");
            if (UtilValidate.isNotEmpty(rightStyle)) {
               buffer.append("<div");
               buffer.append(" class=\"");
               buffer.append(rightStyle);
               buffer.append("\"");
               buffer.append(">" );
            }
            buffer.append(rightText);
            if (UtilValidate.isNotEmpty(rightStyle)) {
                buffer.append("</div>");
            }
            buffer.append("</td>");
        }
        buffer.append("</tr> </table>");
    }
    
    /**
     * Renders a link for the column header fields when there is a header-link="" specified in the <field > tag, using
     * style from header-link-style="".  Also renders a selectAll checkbox in multi forms.
     * @param buffer
     * @param context
     * @param modelFormField
     * @param titleText
     */
    public void renderHyperlinkTitle(StringBuffer buffer, Map context, ModelFormField modelFormField, String titleText) {
        if (UtilValidate.isNotEmpty(modelFormField.getHeaderLink())) {
            StringBuffer targetBuffer = new StringBuffer();
            FlexibleStringExpander target = new FlexibleStringExpander(modelFormField.getHeaderLink());         
            String fullTarget = target.expandString(context);
            targetBuffer.append(fullTarget);
            String targetType = HyperlinkField.DEFAULT_TARGET_TYPE;
            if (UtilValidate.isNotEmpty(targetBuffer.toString()) && targetBuffer.toString().toLowerCase().startsWith("javascript:")) {
            	targetType="plain";
            }
            makeHyperlinkString(buffer, modelFormField.getHeaderLinkStyle(), targetType, targetBuffer.toString(), titleText, null);
        } else if (modelFormField.isRowSubmit()) {
            if (UtilValidate.isNotEmpty(titleText)) buffer.append(titleText).append("<br>");
            buffer.append("<input type=\"checkbox\" name=\"selectAll\" value=\"Y\" onclick=\"javascript:toggleAll(this, '");
            buffer.append(modelFormField.getModelForm().getName());
            buffer.append("');\"/>");
        } else {
             buffer.append(titleText);
        }
    }
}
