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

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
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

import org.ofbiz.base.location.FlexibleLocation;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.FileUtil;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.base.util.template.FreeMarkerWorker;
import org.ofbiz.webapp.control.RequestHandler;
import org.ofbiz.webapp.taglib.ContentUrlTag;
import org.ofbiz.widget.ModelWidget;
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
import org.ofbiz.widget.screen.ModelScreenWidget;

import freemarker.core.Environment;
import freemarker.template.Template;
import freemarker.template.TemplateException;


/**
 * Widget Library - Csv Form Renderer implementation
 *
 */
public class MacroFormRenderer implements FormStringRenderer {

    public static final String module = MacroFormRenderer.class.getName();
    private Template macroLibrary;
    private Environment environment;
    protected RequestHandler rh;
    protected HttpServletRequest request;
    protected HttpServletResponse response;
    protected boolean javaScriptEnabled = false;
    protected boolean renderPagination = true;

    public MacroFormRenderer(String macroLibraryPath, Appendable writer, HttpServletRequest request, HttpServletResponse response) throws TemplateException, IOException {
        macroLibrary = FreeMarkerWorker.getTemplate(macroLibraryPath);
        Map<String, Object> input = UtilMisc.toMap("key", null);
        environment = FreeMarkerWorker.renderTemplate(macroLibrary, input, writer);
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


    private void executeMacro(Appendable writer, String macro) throws IOException {
        try {
            Reader templateReader = new StringReader(macro);
            // FIXME: I am using a Date as an hack to provide a unique name for the template...
            Template template = new Template((new java.util.Date()).toString(), templateReader, FreeMarkerWorker.getDefaultOfbizConfig());
            templateReader.close();
            environment.include(template);
        } catch (TemplateException e) {
            Debug.logError(e, "Error rendering screen thru ftl", module);
        } catch (IOException e) {
            Debug.logError(e, "Error rendering screen thru ftl", module);
        }
    }

    private void appendWhitespace(Appendable writer) throws IOException {
        // appending line ends for now, but this could be replaced with a simple space or something
        writer.append("\r\n");
        //writer.append(' ');
    }

    private void makeTextString(Appendable writer, String widgetStyle, String text) throws IOException {
        // TODO: escape characters here
        //writer.append(text);
        if(UtilValidate.isNotEmpty(text)){
            text = StringUtil.htmlEncoder.encode(text);
        }
        StringWriter sr = new StringWriter();
        sr.append("<@renderField ");
        sr.append("text=\"");
        sr.append(text);
        sr.append("\"");
        sr.append(" />");
        executeMacro(writer, sr.toString());
    }

    public void renderLabel(Appendable writer, Map<String, Object> context, ModelScreenWidget.Label label) throws IOException {
        String labelText = label.getText(context);
        if (UtilValidate.isEmpty(labelText)) {
            // nothing to render
            return;
        }
        StringWriter sr = new StringWriter();
        sr.append("<@renderLabel ");
        sr.append("text=\"");
        sr.append(labelText);
        sr.append("\"");
        sr.append(" />");
        executeMacro(writer, sr.toString());
    }
    
    public void renderDisplayField(Appendable writer, Map<String, Object> context, DisplayField displayField) throws IOException {
        ModelFormField modelFormField = displayField.getModelFormField();
        ModelForm modelForm = modelFormField.getModelForm();
        String idName = modelFormField.getIdName();
        if (UtilValidate.isNotEmpty(idName) && ("list".equals(modelForm.getType()) || "multi".equals(modelForm.getType()))) {
        	idName += "_" + modelForm.getRowCount();
        }
        String description = displayField.getDescription(context);
        // Replace new lines with <br/>
        description = description.replaceAll("\n", "<br/>");
        StringWriter sr = new StringWriter();
        sr.append("<@renderDisplayField ");
        sr.append("idName=\"");
        sr.append(idName);
        sr.append("\" description=\"");
        sr.append(description);
        sr.append("\" class=\"");
        sr.append(modelFormField.getWidgetStyle());
        sr.append("\" alert=\"");
        sr.append(modelFormField.shouldBeRed(context)?"true":"false");
        sr.append("\" />");
        executeMacro(writer, sr.toString());
        if (displayField instanceof DisplayEntityField) {
            makeHyperlinkString(writer,((DisplayEntityField) displayField).getSubHyperlink(),context);
        }
        this.appendTooltip(writer, context, modelFormField);
    }

    public void renderHyperlinkField(Appendable writer, Map<String, Object> context, HyperlinkField hyperlinkField) throws IOException {
    	// FIXME copy from HtmlFormRenderer.java
        this.request.setAttribute("image", hyperlinkField.getImage());
        ModelFormField modelFormField = hyperlinkField.getModelFormField();

        WidgetWorker.makeHyperlinkByType(writer, hyperlinkField.getLinkType(), modelFormField.getWidgetStyle(), hyperlinkField.getTargetType(), hyperlinkField.getTarget(context), 
                hyperlinkField.getParameterList(), hyperlinkField.getDescription(context), hyperlinkField.getTargetWindow(context), modelFormField, 
                this.request, this.response, context);

        this.appendTooltip(writer, context, modelFormField);
    }

    public void renderTextField(Appendable writer, Map<String, Object> context, TextField textField) throws IOException {
        ModelFormField modelFormField = textField.getModelFormField();
        String name = modelFormField.getParameterName(context);
        String className = "";   
        String alert = "false";
        if (UtilValidate.isNotEmpty(modelFormField.getWidgetStyle())) {
        	className = modelFormField.getWidgetStyle();
            if(modelFormField.shouldBeRed(context)){
            	alert = "true";
            }
        }

        String value = modelFormField.getEntry(context, textField.getDefaultValue(context));
        String textSize = Integer.toString(textField.getSize());
        String maxlength = "";
        if (textField.getMaxlength() != null) {
        	maxlength = Integer.toString(textField.getMaxlength());
        }
        String event = modelFormField.getEvent();
        String action = modelFormField.getAction(context);
        String id = modelFormField.getIdName();
        String clientAutocomplete = "false";

        List<ModelForm.UpdateArea> updateAreas = modelFormField.getOnChangeUpdateAreas();
        boolean ajaxEnabled = updateAreas != null && this.javaScriptEnabled;
        if (!textField.getClientAutocompleteField() || ajaxEnabled) {
            clientAutocomplete = "true";
        }

        String ajaxUrl = createAjaxParamsFromUpdateAreas(updateAreas, null, context);

        StringWriter sr = new StringWriter();
        sr.append("<@renderTextField ");
        sr.append("name=\"");
        sr.append(name);
        sr.append("\" className=\"");
        sr.append(className);
        sr.append("\" alert=\"");
        sr.append(alert);
        sr.append("\" value=\"");
        sr.append(value);
        sr.append("\" textSize=\"");
        sr.append(textSize);
        sr.append("\" maxlength=\"");
        sr.append(maxlength);
        sr.append("\" id=\"");
        sr.append(id);
        sr.append("\" event=\"");
        sr.append(event==null?"":event);
        sr.append("\" action=\"");
        sr.append(action==null?"":action);
        sr.append("\" clientAutocomplete=\"");
        sr.append(clientAutocomplete);
        sr.append("\" ajaxUrl=\"");
        sr.append(ajaxUrl);
        sr.append("\" ajaxEnabled=");
        sr.append(Boolean.toString(ajaxEnabled));
        sr.append(" />");
        executeMacro(writer, sr.toString());

        ModelFormField.SubHyperlink subHyperlink = textField.getSubHyperlink();
        if (subHyperlink != null && subHyperlink.shouldUse(context)) {
        	makeHyperlinkString(writer,subHyperlink,context);
        }
        this.addAsterisks(writer, context, modelFormField);
        this.appendTooltip(writer, context, modelFormField);
    }

    public void renderTextareaField(Appendable writer, Map<String, Object> context, TextareaField textareaField) throws IOException {
        ModelFormField modelFormField = textareaField.getModelFormField();
        String name = modelFormField.getParameterName(context);
        String cols = Integer.toString(textareaField.getCols());
        String rows = Integer.toString(textareaField.getRows());
        String id = modelFormField.getIdName();
        String className = "";   
        String alert = "false";
        if (UtilValidate.isNotEmpty(modelFormField.getWidgetStyle())) {
            className = modelFormField.getWidgetStyle();
            if(modelFormField.shouldBeRed(context)){
                alert = "true";
            }
        }
        String visualEdtiorEnalble = "";
        String buttons = "";
        if(textareaField.getVisualEditorEnable()){
            visualEdtiorEnalble = "true";
            buttons = textareaField.getVisualEditorButtons(context);
            if (UtilValidate.isEmpty(buttons)) {
                buttons = "all";
            }
        }
        String readonly = "";
        if (textareaField.isReadOnly()) {
            readonly = "readonly";
        }
        String value = modelFormField.getEntry(context, textareaField
                .getDefaultValue(context));
        if (UtilValidate.isNotEmpty(value)) {
            value = StringUtil.htmlEncoder.encode(value);
        }
        StringWriter sr = new StringWriter();
        sr.append("<@renderTextareaField ");
        sr.append("name=\"");
        sr.append(name);
        sr.append("\" className=\"");
        sr.append(className);
        sr.append("\" alert=\"");
        sr.append(alert);
        sr.append("\" value=\"");
        sr.append(value);
        sr.append("\" cols=\"");
        sr.append(cols);
        sr.append("\" rows=\"");
        sr.append(rows);
        sr.append("\" id=\"");
        sr.append(id);
        sr.append("\" readonly=\"");
        sr.append(readonly);
        sr.append("\" value=\"");
        sr.append(value);
        sr.append("\" visualEdtiorEnalble=\"");
        sr.append(visualEdtiorEnalble);
        sr.append("\" buttons=\"");
        sr.append(buttons);
        sr.append("\" />");
        executeMacro(writer, sr.toString());
        this.addAsterisks(writer, context, modelFormField);
        this.appendTooltip(writer, context, modelFormField);
    }

    public void renderDateTimeField(Appendable writer, Map<String, Object> context, DateTimeField dateTimeField) throws IOException {
        ModelFormField modelFormField = dateTimeField.getModelFormField();
        String paramName = modelFormField.getParameterName(context);
        String defaultDateTimeString = dateTimeField.getDefaultDateTimeString(context);
        String className = "";   
        String alert = "false";
        String name = "";
        if (UtilValidate.isNotEmpty(modelFormField.getWidgetStyle())) {
            className = modelFormField.getWidgetStyle();
            if(modelFormField.shouldBeRed(context)){
                alert = "true";
            }
        }
        Map<String, String> uiLabelMap = UtilGenerics.checkMap(context.get("uiLabelMap"));
        if (uiLabelMap == null) {
            Debug.logWarning("Could not find uiLabelMap in context", module);
        }
        String localizedInputTitle = "", localizedIconTitle = "";

        // whether the date field is short form, yyyy-mm-dd
        boolean shortDateInput = ("date".equals(dateTimeField.getType()) || "time-dropdown".equals(dateTimeField.getInputMethod()) ? true : false);

        if ("time-dropdown".equals(dateTimeField.getInputMethod())) {
            name = UtilHttp.makeCompositeParam(paramName, "date");
        } else {
            name = paramName;
        }

        // the default values for a timestamp
        int size = 25;
        int maxlength = 30;

        if (shortDateInput) {
            //size = maxlength = 15;
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

        String value = modelFormField.getEntry(context, dateTimeField.getDefaultValue(context));
        if (UtilValidate.isNotEmpty(value)) {
            if (value.length() > maxlength) {
                value = value.substring(0, maxlength);
            }
        }
        String id = modelFormField.getIdName();
        String formName = modelFormField.getModelForm().getCurrentFormName(context); 
        String timeDropdown = dateTimeField.getInputMethod();
        String timeDropdownParamName = "";
        String classString = "";
        StringBuilder calGif = new StringBuilder();
        boolean isTwelveHour = false ;
        String timeHourName = "";
        int hour2 = 0, hour1 = 0, minutes = 0;
        String timeMinutesName = "";
        String amSelected="", pmSelected="", ampmName="";
        String compositeType = "";
        // search for a localized label for the icon
        if (uiLabelMap != null) {
            localizedIconTitle = (String) uiLabelMap.get("CommonViewCalendar");
        }

        if (!"time".equals(dateTimeField.getType())) {
            String tempParamName;
            if ("time-dropdown".equals(dateTimeField.getInputMethod())) {
                tempParamName = UtilHttp.makeCompositeParam(paramName, "date");
            } else {
                tempParamName = paramName;
            }
            timeDropdownParamName = tempParamName;
            defaultDateTimeString = UtilHttp.encodeBlanks(modelFormField.getEntry(context, defaultDateTimeString));
            this.appendContentUrl(calGif, "/images/cal.gif");            
        }

        // if we have an input method of time-dropdown, then render two
        // dropdowns
        if ("time-dropdown".equals(dateTimeField.getInputMethod())) {
            className = modelFormField.getWidgetStyle();
            classString = (className != null ? className : "");
            isTwelveHour = "12".equals(dateTimeField.getClock());

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

            timeHourName = UtilHttp.makeCompositeParam(paramName, "hour");
            if( cal != null){
                int hour = cal.get(Calendar.HOUR_OF_DAY);
                hour2 = hour;
                if (hour == 0)
                    hour = 12;
                if (hour > 12)
                    hour -= 12;
                hour1 = hour;
                minutes = cal.get(Calendar.MINUTE);
            }
            timeMinutesName = UtilHttp.makeCompositeParam(paramName, "minutes");
            compositeType = UtilHttp.makeCompositeParam(paramName,"compositeType");
            // if 12 hour clock, write the AM/PM selector
            if (isTwelveHour) {
                amSelected = ((cal != null && cal.get(Calendar.AM_PM) == Calendar.AM) ? "selected" : "");
                pmSelected = ((cal != null && cal.get(Calendar.AM_PM) == Calendar.PM) ? "selected" : "");
                ampmName = UtilHttp.makeCompositeParam(paramName, "ampm");
            }

        }
        StringWriter sr = new StringWriter();
        sr.append("<@renderDateTimeField ");
        sr.append("name=\"");
        sr.append(name);
        sr.append("\" className=\"");
        sr.append(className);
        sr.append("\" alert=\"");
        sr.append(alert);
        sr.append("\" value=\"");
        sr.append(value);
        sr.append("\" title=\"");
        sr.append(localizedInputTitle);
        sr.append("\" value=\"");
        sr.append(value);
        sr.append("\" size=\"");
        sr.append(Integer.toString(size));
        sr.append("\" maxlength=\"");
        sr.append(Integer.toString(maxlength));
        sr.append("\" value=\"");
        sr.append(value);
        sr.append("\" id=\"");
        sr.append(id);
        sr.append("\" dateType=\"");
        sr.append(dateTimeField.getType());
        sr.append("\" shortDateInput=");
        sr.append(Boolean.toString(shortDateInput));
        sr.append(" timeDropdownParamName=\"");
        sr.append(timeDropdownParamName);
        sr.append("\" defaultDateTimeString=\"");
        sr.append(defaultDateTimeString);
        sr.append("\" calGif=\"");
        sr.append(calGif.toString());
        sr.append("\" localizedIconTitle=\"");
        sr.append(localizedIconTitle);
        sr.append("\" timeDropdown=\"");
        sr.append(timeDropdown);
        sr.append("\" timeHourName=\"");
        sr.append(timeHourName);
        sr.append("\" classString=\"");
        sr.append(classString);
        sr.append("\" hour1=");
        sr.append(Integer.toString(hour1));
        sr.append(" hour2=");
        sr.append(Integer.toString(hour2));
        sr.append(" timeMinutesName=\"");
        sr.append(timeMinutesName);
        sr.append("\" minutes=");
        sr.append(Integer.toString(minutes));
        sr.append(" isTwelveHour=");
        sr.append(Boolean.toString(isTwelveHour));
        sr.append(" ampmName=\"");
        sr.append(ampmName);
        sr.append("\" amSelected=\"");
        sr.append(amSelected);
        sr.append("\" pmSelected=\"");
        sr.append(pmSelected);
        sr.append("\" compositeType=\"");
        sr.append(compositeType);
        sr.append("\" formName=\"");
        sr.append(formName);        
        sr.append("\" />");
        executeMacro(writer, sr.toString());
        this.addAsterisks(writer, context, modelFormField);
        this.appendTooltip(writer, context, modelFormField);
    }

    public void renderDropDownField(Appendable writer, Map<String, Object> context, DropDownField dropDownField) throws IOException {
        ModelFormField modelFormField = dropDownField.getModelFormField();
        ModelForm modelForm = modelFormField.getModelForm();
        String currentValue = modelFormField.getEntry(context);
        List<ModelFormField.OptionValue> allOptionValues = dropDownField.getAllOptionValues(context, modelForm.getDelegator(context));
        ModelFormField.AutoComplete autoComplete = dropDownField.getAutoComplete();
        String event = modelFormField.getEvent();
        String action = modelFormField.getAction(context);
        boolean ajaxEnabled = autoComplete != null && this.javaScriptEnabled;
        String className = "";   
        String alert = "false";
        String name = modelFormField.getParameterName(context);
        String id = modelFormField.getIdName();
        String multiple = dropDownField.isAllowMultiple()?"multiple":"";
        String otherFieldName = "";
        String formName = modelForm.getName();
        String size =  dropDownField.getSize();
        String dDFCurrent = dropDownField.getCurrent();
        String firstInList = "";
        String explicitDescription = "";
        String allowEmpty = "";
        StringBuilder options = new StringBuilder();
        StringBuilder ajaxOptions = new StringBuilder();
        if (UtilValidate.isNotEmpty(modelFormField.getWidgetStyle())) {
            className = modelFormField.getWidgetStyle();
            if(modelFormField.shouldBeRed(context)){
                alert = "true";
            }
        }

        String currentDescription = null;
        if (UtilValidate.isNotEmpty(currentValue)) {
            for (ModelFormField.OptionValue optionValue : allOptionValues) {
                if (optionValue.getKey().equals(currentValue)) {
                    currentDescription = optionValue.getDescription();
                    break;
                }
            }
        }
        
        int otherFieldSize = dropDownField.getOtherFieldSize();
        if (otherFieldSize > 0) {            
            otherFieldName = dropDownField.getParameterNameOther(context);
        }
        // if the current value should go first, stick it in
        if (UtilValidate.isNotEmpty(currentValue) && "first-in-list".equals(dropDownField.getCurrent())) {
            firstInList = "first-in-list";            
        }
        explicitDescription = (currentDescription != null ? currentDescription : dropDownField.getCurrentDescription(context));;
        if (UtilValidate.isEmpty(explicitDescription)) {
            explicitDescription = (ModelFormField.FieldInfoWithOptions.getDescriptionForOptionKey(currentValue, allOptionValues));
        } 

       // if allow empty is true, add an empty option
       if (dropDownField.isAllowEmpty()) {
           allowEmpty = "&nbsp;";
       }

       options.append("[");
       Iterator<ModelFormField.OptionValue> optionValueIter = allOptionValues.iterator();
       int count = 0;
       while (optionValueIter.hasNext()) {
           ModelFormField.OptionValue optionValue = (ModelFormField.OptionValue) optionValueIter.next();
           if(options.length() >1){
               options.append(",");
           }
           options.append("{'key':'");
           options.append(optionValue.getKey());
           options.append("'");
           options.append(",'description':'");
           options.append(optionValue.getDescription());
           options.append("'}");
           if(ajaxEnabled){
        	   count++;
        	   ajaxOptions.append(optionValue.getKey()).append(": ");
        	   ajaxOptions.append(" '").append(optionValue.getDescription()).append("'");
               if (count != allOptionValues.size()) {
            	   ajaxOptions.append(", ");
               }
           }
       }
       options.append("]");
       String noCurrentSelectedKey = dropDownField.getNoCurrentSelectedKey(context);
       String otherValue = "", fieldName = "";
       // Adapted from work by Yucca Korpela
       // http://www.cs.tut.fi/~jkorpela/forms/combo.html
       if (otherFieldSize > 0) {

          fieldName = modelFormField.getParameterName(context);
          Map dataMap = modelFormField.getMap(context);
          if (dataMap == null) {
              dataMap = context;
          }
          Object otherValueObj = dataMap.get(otherFieldName);
          otherValue = (otherValueObj == null) ? "": otherValueObj.toString();
      }
      String frequency = "";
      String minChars = "";
      String choices = "";
      String autoSelect = "";
      String partialSearch = "";
      String partialChars = "";
      String ignoreCase = "";
      String fullSearch = "";
      if(ajaxEnabled){
    	  frequency = autoComplete.getFrequency();
    	  minChars = autoComplete.getMinChars();
    	  choices = autoComplete.getChoices();
    	  autoSelect = autoComplete.getAutoSelect();
    	  partialSearch = autoComplete.getPartialSearch() ;
    	  partialChars = autoComplete.getPartialChars();
    	  ignoreCase = autoComplete.getIgnoreCase();
    	  fullSearch = autoComplete.getFullSearch();
      }

      StringWriter sr = new StringWriter();
      sr.append("<@renderDropDownField ");
      sr.append("name=\"");
      sr.append(name);
      sr.append("\" className=\"");
      sr.append(className);
      sr.append("\" alert=\"");
      sr.append(alert);
      sr.append("\" id=\"");
      sr.append(id);
      sr.append("\" multiple=\"");
      sr.append(multiple);
      sr.append("\" formName=\"");
      sr.append(formName);
      sr.append("\" otherFieldName=\"");
      sr.append(otherFieldName);
      sr.append("\" event=\"");
      sr.append(event==null?"":event);
      sr.append("\" action=\"");
      sr.append(action==null?"":action);
      sr.append("\" size=\"");
      sr.append(size);
      sr.append("\" firstInList=\"");
      sr.append(firstInList);
      sr.append("\" currentValue=\"");
      sr.append(currentValue);
      sr.append("\" explicitDescription=\"");
      sr.append(explicitDescription);
      sr.append("\" allowEmpty=\"");
      sr.append(allowEmpty);
      sr.append("\" options=");
      sr.append(options.toString());
      sr.append(" fieldName=\"");
      sr.append(fieldName);
      sr.append("\" otherFieldName=\"");
      sr.append(otherFieldName);
      sr.append("\" otherValue=\"");
      sr.append(otherValue);
      sr.append("\" otherFieldSize=");
      sr.append(Integer.toString(otherFieldSize));
      sr.append(" dDFCurrent=\"");
      sr.append(dDFCurrent);
      sr.append("\" ajaxEnabled=");
      sr.append(Boolean.toString(ajaxEnabled));
      sr.append(" noCurrentSelectedKey=\"");
      sr.append(noCurrentSelectedKey);
      sr.append("\" ajaxOptions=\"");
      sr.append(ajaxOptions.toString());
      sr.append("\" frequency=\"");
      sr.append(frequency);
      sr.append("\" minChars=\"");
      sr.append(minChars);
      sr.append("\" choices=\"");
      sr.append(choices);
      sr.append("\" autoSelect=\"");
      sr.append(autoSelect);
      sr.append("\" partialSearch=\"");
      sr.append(partialSearch);
      sr.append("\" partialChars=\"");
      sr.append(partialChars);
      sr.append("\" ignoreCase=\"");
      sr.append(ignoreCase);
      sr.append("\" fullSearch=\"");
      sr.append(fullSearch);
      sr.append("\" />");
      executeMacro(writer, sr.toString());
      ModelFormField.SubHyperlink subHyperlink = dropDownField.getSubHyperlink();
      if (subHyperlink != null && subHyperlink.shouldUse(context)) {
          makeHyperlinkString(writer,subHyperlink,context);
      }
      this.appendTooltip(writer, context, modelFormField);

    }

    public void renderCheckField(Appendable writer, Map<String, Object> context, CheckField checkField) throws IOException {
        ModelFormField modelFormField = checkField.getModelFormField();
        ModelForm modelForm = modelFormField.getModelForm();
        String currentValue = modelFormField.getEntry(context);
        Boolean allChecked = checkField.isAllChecked(context);
        String className = "";   
        String alert = "false";
        String name = modelFormField.getParameterName(context);
        String event = modelFormField.getEvent();
        String action = modelFormField.getAction(context);
        StringBuilder items = new StringBuilder();
        if (UtilValidate.isNotEmpty(modelFormField.getWidgetStyle())) {
            className = modelFormField.getWidgetStyle();
            if(modelFormField.shouldBeRed(context)){
                alert = "true";
            }
        }

        List allOptionValues = checkField.getAllOptionValues(context, modelForm.getDelegator(context));
        Iterator<ModelFormField.OptionValue> optionValueIter = allOptionValues.iterator();
        items.append("[");
        while (optionValueIter.hasNext()) {
            ModelFormField.OptionValue optionValue = (ModelFormField.OptionValue) optionValueIter.next();
            if(items.length() >1){
                items.append(",");
            }
            items.append("{'value':'");
            items.append(optionValue.getKey());
            items.append("'}");
        }
        items.append("]");

        StringWriter sr = new StringWriter();
        sr.append("<@renderCheckField ");
        sr.append("items=");
        sr.append(items.toString());
        sr.append(" className=\"");
        sr.append(className);
        sr.append("\" alert=\"");
        sr.append(alert);
        sr.append("\" allChecked=");
        sr.append(Boolean.toString(allChecked));
        sr.append(" currentValue=\"");
        sr.append(currentValue);
        sr.append("\" name=\"");
        sr.append(name);
        sr.append("\" event=\"");
        sr.append(event==null?"":event);
        sr.append("\" action=\"");
        sr.append(action==null?"":action);
        sr.append("\" />");
        executeMacro(writer, sr.toString());

        this.appendTooltip(writer, context, modelFormField);
    }

    public void renderRadioField(Appendable writer, Map<String, Object> context, RadioField radioField) throws IOException {
        ModelFormField modelFormField = radioField.getModelFormField();
        ModelForm modelForm = modelFormField.getModelForm();
        List allOptionValues = radioField.getAllOptionValues(context, modelForm
                .getDelegator(context));
        String currentValue = modelFormField.getEntry(context);
        String className = "";   
        String alert = "false";
        String name = modelFormField.getParameterName(context);
        String event = modelFormField.getEvent();
        String action = modelFormField.getAction(context);
        StringBuilder items = new StringBuilder();
        if (UtilValidate.isNotEmpty(modelFormField.getWidgetStyle())) {
            className = modelFormField.getWidgetStyle();
            if(modelFormField.shouldBeRed(context)){
                alert = "true";
            }
        }

        String noCurrentSelectedKey = radioField.getNoCurrentSelectedKey(context);
        Iterator<ModelFormField.OptionValue> optionValueIter = allOptionValues.iterator();
        items.append("[");
        while (optionValueIter.hasNext()) {
            ModelFormField.OptionValue optionValue = (ModelFormField.OptionValue) optionValueIter.next();
            if(items.length() >1){
                items.append(",");
            }
            items.append("{'value':'");
            items.append(optionValue.getKey());
            items.append("'}");
        }
        items.append("]");

        StringWriter sr = new StringWriter();
        sr.append("<@renderRadioField ");
        sr.append("items=");
        sr.append(items.toString());
        sr.append(" className=\"");
        sr.append(className);
        sr.append("\" alert=\"");
        sr.append(alert);
        sr.append(" currentValue=\"");
        sr.append(currentValue);
        sr.append("\" noCurrentSelectedKey=");
        sr.append(noCurrentSelectedKey);        
        sr.append("\" name=\"");
        sr.append(name);
        sr.append("\" event=\"");
        sr.append(event==null?"":event);
        sr.append("\" action=\"");
        sr.append(action==null?"":action);
        sr.append("\" />");
        executeMacro(writer, sr.toString());

        this.appendTooltip(writer, context, modelFormField);
    }

    public void renderSubmitField(Appendable writer, Map<String, Object> context, SubmitField submitField) throws IOException {
        ModelFormField modelFormField = submitField.getModelFormField();
        ModelForm modelForm = modelFormField.getModelForm();
        String event = modelFormField.getEvent();
        String action = modelFormField.getAction(context);
        String title = modelFormField.getTitle(context);
        String name = modelFormField.getParameterName(context);
        String buttonType =  submitField.getButtonType();
        String formName = modelForm.getCurrentFormName(context);
        String imgSrc = submitField.getImageLocation();
        String className = "";   
        String alert = "false";
        if (UtilValidate.isNotEmpty(modelFormField.getWidgetStyle())) {
            className = modelFormField.getWidgetStyle();
            if(modelFormField.shouldBeRed(context)){
                alert = "true";
            }
        }
        StringWriter sr = new StringWriter();
        sr.append("<@renderSubmitField ");
        sr.append("buttonType=\"");
        sr.append(buttonType);
        sr.append("\" className=\"");
        sr.append(className);
        sr.append("\" alert=\"");
        sr.append(alert);
        sr.append("\" formName=\"");
        sr.append(formName);
        sr.append("\" title=\"");
        sr.append(title);
        sr.append("\" name=\"");
        sr.append(name);
        sr.append("\" event=\"");
        sr.append(event==null?"":event);
        sr.append("\" action=\"");
        sr.append(action==null?"":action);
        sr.append("\" imgSrc=\"");
        sr.append(imgSrc);
        sr.append("\" />");
        executeMacro(writer, sr.toString());
        this.appendTooltip(writer, context, modelFormField);
    }

    public void renderResetField(Appendable writer, Map<String, Object> context, ResetField resetField) throws IOException {
        ModelFormField modelFormField = resetField.getModelFormField();
        String name = modelFormField.getParameterName(context);
        String className = "";   
        String alert = "false";
        if (UtilValidate.isNotEmpty(modelFormField.getWidgetStyle())) {
            className = modelFormField.getWidgetStyle();
            if(modelFormField.shouldBeRed(context)){
                alert = "true";
            }
        }
        String title = modelFormField.getTitle(context);

        StringWriter sr = new StringWriter();
        sr.append("<@renderResetField ");
        sr.append(" className=\"");
        sr.append(className);
        sr.append("\" alert=\"");
        sr.append(alert);
        sr.append("\" name=\"");
        sr.append(name);
        sr.append(" title=\"");
        sr.append(title);
        sr.append("\" />");
        executeMacro(writer, sr.toString());

        this.appendTooltip(writer, context, modelFormField);
    }

    public void renderHiddenField(Appendable writer, Map<String, Object> context, HiddenField hiddenField) throws IOException {
        ModelFormField modelFormField = hiddenField.getModelFormField();
        String value = hiddenField.getValue(context);
        this.renderHiddenField(writer, context, modelFormField, value);
    }

    public void renderHiddenField(Appendable writer, Map<String, Object> context, ModelFormField modelFormField, String value) throws IOException {
        String name = modelFormField.getParameterName(context);
        StringWriter sr = new StringWriter();
        sr.append("<@renderHiddenField ");
        sr.append(" name=\"");
        sr.append(name);
        sr.append("\" value=\"");
        sr.append(value);
        sr.append("\" />");
        executeMacro(writer, sr.toString());
    }

    public void renderIgnoredField(Appendable writer, Map<String, Object> context, IgnoredField ignoredField) {
     // do nothing, it's an ignored field; could add a comment or something if we wanted to
    }

    public void renderFieldTitle(Appendable writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
        String tempTitleText = modelFormField.getTitle(context);
        String titleText = UtilHttp.encodeAmpersands(tempTitleText);
        String style = modelFormField.getTitleStyle();
        StringBuilder sb = new StringBuilder();
        if (UtilValidate.isNotEmpty(titleText)) {
            if (" ".equals(titleText)) {
                // If the title content is just a blank then render it colling renderFormatEmptySpace:
                // the method will set its content to work fine in most browser
                sb.append("&nbsp;");
            } else {
                renderHyperlinkTitle(sb, context, modelFormField, StringUtil.htmlEncoder.encode(titleText));
            }

        }
        StringWriter sr = new StringWriter();
        sr.append("<@renderFieldTitle ");
        sr.append(" style=\"");
        sr.append(style);
        sr.append("\" title=\"");
        sr.append(sb.toString());
        sr.append("\" />");
        executeMacro(writer, sr.toString());
    }

    public void renderSingleFormFieldTitle(Appendable writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
        renderFieldTitle(writer, context, modelFormField);
    }

    public void renderFormOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        renderBeginningBoundaryComment(writer, "Form Widget - Form Element", modelForm);
        String targetType = modelForm.getTargetType();
        String targ = modelForm.getTarget(context, targetType);
        StringBuilder linkUrl = new StringBuilder();
        if (targ != null && targ.length() > 0) {
            //this.appendOfbizUrl(writer, "/" + targ);
            WidgetWorker.buildHyperlinkUrl(linkUrl, targ, targetType, null, null, false, false, true, request, response, context);
        }       
        String formType = modelForm.getType();
        String targetWindow = modelForm.getTargetWindow(context);
        String containerId =  modelForm.getContainerId();
        String containerStyle =  modelForm.getContainerStyle();
        String autocomplete = "";
        String name = modelForm.getCurrentFormName(context);
        boolean useRowSubmit = modelForm.getUseRowSubmit();
        if (!modelForm.getClientAutocompleteFields()) {
            autocomplete = "off";
        }
        StringWriter sr = new StringWriter();
        sr.append("<@renderFormOpen ");
        sr.append(" linkUrl=\"");
        sr.append(linkUrl);
        sr.append("\" formType=\"");
        sr.append(formType);
        sr.append("\" targetWindow=\"");
        sr.append(targetWindow);
        sr.append("\" containerId=\"");
        sr.append(containerId);
        sr.append("\" containerStyle=\"");
        sr.append(containerStyle);
        sr.append("\" autocomplete=\"");
        sr.append(autocomplete);
        sr.append("\" name=\"");
        sr.append(name);
        sr.append("\" useRowSubmit=");
        sr.append(Boolean.toString(useRowSubmit));
        sr.append(" />");
        executeMacro(writer, sr.toString());
    }

    public void renderFormClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        String focusFieldName = modelForm.getfocusFieldName();
        String formName = modelForm.getCurrentFormName(context);
        StringWriter sr = new StringWriter();
        sr.append("<@renderFormClose ");
        sr.append(" focusFieldName=\"");
        sr.append(focusFieldName);
        sr.append("\" formName=\"");
        sr.append(formName);
        sr.append("\" />");
        executeMacro(writer, sr.toString());
        renderEndingBoundaryComment(writer, "Form Widget - Form Element", modelForm);
    }

    public void renderMultiFormClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        //FIXME copy from HtmlFormRenderer.java
        Iterator<ModelFormField> submitFields = modelForm.getMultiSubmitFields().iterator();
        while (submitFields.hasNext()) {
            ModelFormField submitField = (ModelFormField) submitFields.next();
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
        writer.append("</form>");
        appendWhitespace(writer);

        // see if there is anything that needs to be added outside of the multi-form
        Map<String, Object> wholeFormContext = UtilGenerics.checkMap(context.get("wholeFormContext"));
        Appendable postMultiFormWriter = wholeFormContext != null ? (Appendable) wholeFormContext.get("postMultiFormWriter") : null;
        if (postMultiFormWriter != null) {
            writer.append(postMultiFormWriter.toString());
            appendWhitespace(writer);
        }

        renderEndingBoundaryComment(writer, "Form Widget - Form Element (Multi)", modelForm);
    }

    public void renderFormatListWrapperOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        Map<String, Object> inputFields = UtilGenerics.checkMap(context.get("requestParameters"));
        Map<String, Object> queryStringMap = UtilGenerics.toMap(context.get("queryStringMap"));
        if (UtilValidate.isNotEmpty(queryStringMap)) {
            inputFields.putAll(queryStringMap);
        }
        if (modelForm.getType().equals("multi")) {
            inputFields = UtilHttp.removeMultiFormParameters(inputFields);
        }
        String queryString = UtilHttp.urlEncodeArgs(inputFields);
        context.put("_QBESTRING_", queryString);

        renderBeginningBoundaryComment(writer, "Form Widget", modelForm);

        if (this.renderPagination) {
            this.renderNextPrev(writer, context, modelForm);
        }
        StringWriter sr = new StringWriter();
        sr.append("<@renderFormatListWrapperOpen ");
        sr.append(" style=\"");
        sr.append(modelForm.getDefaultTableStyle());
        sr.append("\" />");
        executeMacro(writer, sr.toString());

    }

    public void renderFormatListWrapperClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        StringWriter sr = new StringWriter();
        sr.append("<@renderFormatListWrapperClose />");
        executeMacro(writer, sr.toString());
        if (this.renderPagination) {
            this.renderNextPrev(writer, context, modelForm);
        }
        renderEndingBoundaryComment(writer, "Form Widget - Formal List Wrapper", modelForm);
    }

    public void renderFormatHeaderRowOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        String headerStyle = modelForm.getHeaderRowStyle();
        StringWriter sr = new StringWriter();
        sr.append("<@renderFormatHeaderRowOpen ");
        sr.append(" style=\"");
        sr.append(headerStyle);
        sr.append("\" />");
        executeMacro(writer, sr.toString());
    }

    public void renderFormatHeaderRowClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        StringWriter sr = new StringWriter();
        sr.append("<@renderFormatHeaderRowClose />");
        executeMacro(writer, sr.toString());
    }

    public void renderFormatHeaderRowCellOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField, int positionSpan) throws IOException {
        String areaStyle = modelFormField.getTitleAreaStyle();
        StringWriter sr = new StringWriter();
        sr.append("<@renderFormatHeaderRowCellOpen ");
        sr.append(" style=\"");
        sr.append(areaStyle);
        sr.append("\" positionSpan=");
        sr.append(Integer.toString(positionSpan));
        sr.append(" />");
        executeMacro(writer, sr.toString());
    }

    public void renderFormatHeaderRowCellClose(Appendable writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField) throws IOException {
        StringWriter sr = new StringWriter();
        sr.append("<@renderFormatHeaderRowCellClose />");
        executeMacro(writer, sr.toString());
    }

    public void renderFormatHeaderRowFormCellOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        String areaStyle = modelForm.getFormTitleAreaStyle();
        StringWriter sr = new StringWriter();
        sr.append("<@renderFormatHeaderRowFormCellOpen ");
        sr.append(" style=\"");
        sr.append(areaStyle);
        sr.append("\" />");
        executeMacro(writer, sr.toString());
    }

    public void renderFormatHeaderRowFormCellClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        StringWriter sr = new StringWriter();
        sr.append("<@renderFormatHeaderRowFormCellClose />");
        executeMacro(writer, sr.toString());
    }


    public void renderFormatHeaderRowFormCellTitleSeparator(Appendable writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField, boolean isLast) throws IOException {
        String titleStyle = modelFormField.getTitleStyle();
        StringWriter sr = new StringWriter();
        sr.append("<@renderFormatHeaderRowFormCellTitleSeparator ");
        sr.append(" style=\"");
        sr.append(titleStyle);
        sr.append("\" isLast=");
        sr.append(Boolean.toString(isLast));
        sr.append(" />");
        executeMacro(writer, sr.toString());
    }

    public void renderFormatItemRowOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        Integer itemIndex = (Integer)context.get("itemIndex");
        String altRowStyles = "";
        String evenRowStyle = "";
        String oddRowStyle = "";
        if (itemIndex!=null) { 
            altRowStyles = modelForm.getStyleAltRowStyle(context);
            if (itemIndex.intValue() % 2 == 0) {
                evenRowStyle = modelForm.getEvenRowStyle();               
            } else {
                oddRowStyle = modelForm.getOddRowStyle();
            }
        }
        StringWriter sr = new StringWriter();
        sr.append("<@renderFormatItemRowOpen ");
        sr.append(" itemIndex=");
        sr.append(Integer.toString(itemIndex));
        sr.append(" altRowStyles=\"");
        sr.append(altRowStyles);
        sr.append("\" evenRowStyle=\"");
        sr.append(evenRowStyle);
        sr.append("\" oddRowStyle=\"");
        sr.append(oddRowStyle);
        sr.append("\" />");
        executeMacro(writer, sr.toString());
    }

    public void renderFormatItemRowClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        StringWriter sr = new StringWriter();
        sr.append("<@renderFormatItemRowClose />");
        executeMacro(writer, sr.toString());
    }

    public void renderFormatItemRowCellOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField, int positionSpan) throws IOException {
        String areaStyle = modelFormField.getTitleAreaStyle();
        StringWriter sr = new StringWriter();
        sr.append("<@renderFormatItemRowCellOpen ");
        sr.append(" style=\"");
        sr.append(areaStyle);
        sr.append("\" positionSpan=");
        sr.append(Integer.toString(positionSpan));
        sr.append(" />");
        executeMacro(writer, sr.toString());
    }

    public void renderFormatItemRowCellClose(Appendable writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField) throws IOException {
        StringWriter sr = new StringWriter();
        sr.append("<@renderFormatItemRowCellClose />");
        executeMacro(writer, sr.toString());
    }

    public void renderFormatItemRowFormCellOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        String areaStyle = modelForm.getFormTitleAreaStyle();
        StringWriter sr = new StringWriter();
        sr.append("<@renderFormatItemRowFormCellOpen ");
        sr.append(" style=\"");
        sr.append(areaStyle);
        sr.append("\" />");
        executeMacro(writer, sr.toString());
    }

    public void renderFormatItemRowFormCellClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        StringWriter sr = new StringWriter();
        sr.append("<@renderFormatItemRowFormCellClose />");
        executeMacro(writer, sr.toString());
    }

    public void renderFormatSingleWrapperOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        String style = modelForm.getDefaultTableStyle();
        StringWriter sr = new StringWriter();
        sr.append("<@renderFormatSingleWrapperOpen ");
        sr.append(" style=\"");
        sr.append(style);
        sr.append("\" />");
        executeMacro(writer, sr.toString());
    }

    public void renderFormatSingleWrapperClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        StringWriter sr = new StringWriter();
        sr.append("<@renderFormatSingleWrapperClose />");
        executeMacro(writer, sr.toString());
    }

    public void renderFormatFieldRowOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        StringWriter sr = new StringWriter();
        sr.append("<@renderFormatFieldRowOpen />");
        executeMacro(writer, sr.toString());
    }

    public void renderFormatFieldRowClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        StringWriter sr = new StringWriter();
        sr.append("<@renderFormatFieldRowClose />");
        executeMacro(writer, sr.toString());
    }

    public void renderFormatFieldRowTitleCellOpen(Appendable writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
        String style = modelFormField.getTitleAreaStyle();
        StringWriter sr = new StringWriter();
        sr.append("<@renderFormatFieldRowTitleCellOpen ");
        sr.append(" style=\"");
        sr.append(style);
        sr.append("\" />");
        executeMacro(writer, sr.toString());
    }

    public void renderFormatFieldRowTitleCellClose(Appendable writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
        StringWriter sr = new StringWriter();
        sr.append("<@renderFormatFieldRowTitleCellClose />");
        executeMacro(writer, sr.toString());
    }

    public void renderFormatFieldRowSpacerCell(Appendable writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
    }

    public void renderFormatFieldRowWidgetCellOpen(Appendable writer, Map<String, Object> context, ModelFormField modelFormField, int positions, int positionSpan, Integer nextPositionInRow) throws IOException {
        String areaStyle = modelFormField.getWidgetAreaStyle();
        StringWriter sr = new StringWriter();
        sr.append("<@renderFormatFieldRowWidgetCellOpen ");
        sr.append(" positionSpan=");
        sr.append(Integer.toString(positionSpan));
        sr.append(" style=\"");
        sr.append(areaStyle);
        sr.append("\" />");
        executeMacro(writer, sr.toString());

    }

    public void renderFormatFieldRowWidgetCellClose(Appendable writer, Map<String, Object> context, ModelFormField modelFormField, int positions, int positionSpan, Integer nextPositionInRow) throws IOException {
        StringWriter sr = new StringWriter();
        sr.append("<@renderFormatFieldRowWidgetCellClose />");
        executeMacro(writer, sr.toString());
    }

    public void renderFormatEmptySpace(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        StringWriter sr = new StringWriter();
        sr.append("<@renderFormatEmptySpace />");
        executeMacro(writer, sr.toString());
    }

    public void renderTextFindField(Appendable writer, Map<String, Object> context, TextFindField textFindField) throws IOException {
        ModelFormField modelFormField = textFindField.getModelFormField();
        this.makeTextString(writer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, textFindField.getDefaultValue(context)));
    }

    public void renderRangeFindField(Appendable writer, Map<String, Object> context, RangeFindField rangeFindField) throws IOException {
        ModelFormField modelFormField = rangeFindField.getModelFormField();
        this.makeTextString(writer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, rangeFindField.getDefaultValue(context)));
    }

    public void renderDateFindField(Appendable writer, Map<String, Object> context, DateFindField dateFindField) throws IOException {
        ModelFormField modelFormField = dateFindField.getModelFormField();
        this.makeTextString(writer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, dateFindField.getDefaultValue(context)));
    }

    public void renderLookupField(Appendable writer, Map<String, Object> context, LookupField lookupField) throws IOException {
        ModelFormField modelFormField = lookupField.getModelFormField();
        this.makeTextString(writer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, lookupField.getDefaultValue(context)));
    }

    public void renderNextPrev(Appendable writer, Map<String, Object> context, ModelForm modelForm) {
    }

    public void renderFileField(Appendable writer, Map<String, Object> context, FileField textField) throws IOException {
        ModelFormField modelFormField = textField.getModelFormField();
        this.makeTextString(writer, modelFormField.getWidgetStyle(), modelFormField.getEntry(context, textField.getDefaultValue(context)));
    }

    public void renderPasswordField(Appendable writer, Map<String, Object> context, PasswordField passwordField) {
    }

    public void renderImageField(Appendable writer, Map<String, Object> context, ImageField imageField) {
        // TODO
    }

    public void renderFieldGroupOpen(Appendable writer, Map<String, Object> context, ModelForm.FieldGroup fieldGroup) {
        // TODO
    }

    public void renderFieldGroupClose(Appendable writer, Map<String, Object> context, ModelForm.FieldGroup fieldGroup) {
        // TODO
    }

    public void renderBanner(Appendable writer, Map<String, Object> context, ModelForm.Banner banner) {
        // TODO
    }


    /**
     * Renders the beginning boundary comment string.
     * @param writer The writer to write to
     * @param widgetType The widget type: "Screen Widget", "Form Widget", etc.
     * @param modelWidget The widget
     */
    public void renderBeginningBoundaryComment(Appendable writer, String widgetType, ModelWidget modelWidget) throws IOException {
        if (modelWidget.boundaryCommentsEnabled()) {
            StringWriter sr = new StringWriter();
            sr.append("<@formatBoundaryComment ");
            sr.append(" boundaryType=\"");
            sr.append("Begin");
            sr.append("\" widgetType=\"");
            sr.append(widgetType);
            sr.append("\" widgetName=\"");
            sr.append(modelWidget.getBoundaryCommentName());
            sr.append("\" />");
            executeMacro(writer, sr.toString());
        }
    }

    /**
     * Renders the ending boundary comment string.
     * @param writer The writer to write to
     * @param widgetType The widget type: "Screen Widget", "Form Widget", etc.
     * @param modelWidget The widget
     */
    public void renderEndingBoundaryComment(Appendable writer, String widgetType, ModelWidget modelWidget) throws IOException {
        if (modelWidget.boundaryCommentsEnabled()) {
            StringWriter sr = new StringWriter();
            sr.append("<@formatBoundaryComment ");
            sr.append(" boundaryType=\"");
            sr.append("End");
            sr.append("\" widgetType=\"");
            sr.append(widgetType);
            sr.append("\" widgetName=\"");
            sr.append(modelWidget.getBoundaryCommentName());
            sr.append("\" />");
            executeMacro(writer, sr.toString());
        }
    }
    public void renderHyperlinkTitle(Appendable writer, Map<String, Object> context, ModelFormField modelFormField, String titleText) throws IOException {
        if (UtilValidate.isNotEmpty(modelFormField.getHeaderLink())) {
            StringBuffer targetBuffer = new StringBuffer();
            FlexibleStringExpander target = FlexibleStringExpander.getInstance(modelFormField.getHeaderLink());
            String fullTarget = target.expandString(context);
            targetBuffer.append(fullTarget);
            String targetType = HyperlinkField.DEFAULT_TARGET_TYPE;
            if (UtilValidate.isNotEmpty(targetBuffer.toString()) && targetBuffer.toString().toLowerCase().startsWith("javascript:")) {
                targetType="plain";
            }
            //FIXME need to change to macro
            WidgetWorker.makeHyperlinkString(writer, modelFormField.getHeaderLinkStyle(), targetType, targetBuffer.toString(), null, titleText, modelFormField, this.request, this.response, null, null);
        } else if (modelFormField.isSortField()) {
            renderSortField (writer, context, modelFormField, titleText);
        } else if (modelFormField.isRowSubmit()) {
            StringWriter sr = new StringWriter();
            sr.append("<@renderFieldTitle ");
            sr.append(" name=\"");
            sr.append(modelFormField.getModelForm().getName());
            sr.append("\" title=\"");
            sr.append(titleText);
            sr.append("\" />");
            executeMacro(writer, sr.toString());
        } else {
             writer.append(titleText);
        }
    }

    public void renderSortField(Appendable writer, Map<String, Object> context, ModelFormField modelFormField, String titleText) throws IOException {
        boolean ajaxEnabled = false;
        ModelForm modelForm = modelFormField.getModelForm();
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
            Debug.logWarning("Cannot sort because TargetService is empty for the form: " + modelForm.getName(), module);
            return;
        }

        String str = (String) context.get("_QBESTRING_");
        String oldSortField = modelForm.getSortField(context);
        String sortFieldStyle = modelFormField.getSortFieldStyle();

        // if the entry-name is defined use this instead of field name
        String columnField = modelFormField.getEntryName();
        if (UtilValidate.isEmpty(columnField)) {
            columnField = modelFormField.getFieldName();
        }

        // switch beetween asc/desc order
        String newSortField = columnField;
        if (UtilValidate.isNotEmpty(oldSortField)) {
            if (oldSortField.equals(columnField)) {
                newSortField = "-" + columnField;
                sortFieldStyle = modelFormField.getSortFieldStyleDesc();
            } else if (oldSortField.equals("-" + columnField)) {
                newSortField = columnField;
                sortFieldStyle = modelFormField.getSortFieldStyleAsc();
            }
        }

        //  strip sortField param from the query string
        HashSet<String> paramName = new HashSet<String>();
        paramName.add("sortField");
        String queryString = UtilHttp.stripNamedParamsFromQueryString(str, paramName);
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
        prepLinkText += "sortField" + "=" + newSortField;
        if (ajaxEnabled) {
            prepLinkText = prepLinkText.replace("?", "");
            prepLinkText = prepLinkText.replace("&amp;", "&");
        }
        String linkUrl = "";
        if (ajaxEnabled) {
            linkUrl = createAjaxParamsFromUpdateAreas(updateAreas, prepLinkText, context);
        } else {
            linkUrl = rh.makeLink(this.request, this.response, urlPath + prepLinkText);
        }

        StringWriter sr = new StringWriter();
        sr.append("<@renderSortField ");
        sr.append(" style=\"");
        sr.append(sortFieldStyle);
        sr.append("\" title=\"");
        sr.append(titleText);
        sr.append("\" linkUrl=\"");
        sr.append(linkUrl);
        sr.append("\" ajaxEnabled=");
        sr.append(Boolean.toString(ajaxEnabled));
        sr.append(" />");
        executeMacro(writer, sr.toString());

    }
    /** Create an ajaxXxxx JavaScript CSV string from a list of UpdateArea objects. See
     * <code>selectall.js</code>.
     * @param updateAreas
     * @param extraParams Renderer-supplied additional target parameters
     * @param context
     * @return Parameter string or empty string if no UpdateArea objects were found
     */
    public String createAjaxParamsFromUpdateAreas(List<ModelForm.UpdateArea> updateAreas, String extraParams, Map<String, ? extends Object> context) {
        //FIXME copy from HtmlFormRenderer.java
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
    /** Extracts parameters from a target URL string, prepares them for an Ajax
     * JavaScript call. This method is currently set to return a parameter string
     * suitable for the Prototype.js library.
     * @param target Target URL string
     * @return Parameter string
     */
    public static String getAjaxParamsFromTarget(String target) {
        String targetParams = UtilHttp.getQueryStringFromTarget(target);
        targetParams = targetParams.replace("?", "");
        targetParams = targetParams.replace("&amp;", "&");
        return targetParams;
    }
    public void appendTooltip(Appendable writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
        // render the tooltip, in other methods too
        String tooltip = modelFormField.getTooltip(context);
        StringWriter sr = new StringWriter();
        sr.append("<@renderTooltip ");
        sr.append("tooltip=\"");
        sr.append(tooltip);
        sr.append("\" tooltipStyle=\"");
        sr.append(modelFormField.getTooltipStyle());
        sr.append("\" />");
        executeMacro(writer, sr.toString());
    }
    public void makeHyperlinkString(Appendable writer, ModelFormField.SubHyperlink subHyperlink, Map<String, Object> context) throws IOException {
        if (subHyperlink == null) {
            return;
        }
        if (subHyperlink.shouldUse(context)) {
            writer.append(' ');
            WidgetWorker.makeHyperlinkByType(writer, subHyperlink.getLinkType(), subHyperlink.getLinkStyle(), subHyperlink.getTargetType(), subHyperlink.getTarget(context), 
                    subHyperlink.getParameterList(), subHyperlink.getDescription(context), subHyperlink.getTargetWindow(context), subHyperlink.getModelFormField(), 
                    this.request, this.response, context);
        }
    }
    public void addAsterisks(Appendable writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
        String requiredField = "false";
        String requiredStyle = "";
    	if( modelFormField.getRequiredField() ){
    	    requiredField = "true";
    	    requiredStyle = modelFormField.getRequiredFieldStyle();
        }
    	 StringWriter sr = new StringWriter();
         sr.append("<@renderAsterisks ");
         sr.append("requiredField=\"");
         sr.append(requiredField);
         sr.append("\" requiredStyle=\"");
         sr.append(requiredStyle);
         sr.append("\" />");
         executeMacro(writer, sr.toString());
    }
    public void appendContentUrl(Appendable writer, String location) throws IOException {
        StringBuffer buffer = new StringBuffer();
        ContentUrlTag.appendContentPrefix(this.request, buffer);
        writer.append(buffer.toString());
        writer.append(location);
    }
    
    public void makeHyperlinkByType(Appendable writer, String linkType, String linkStyle, String targetType, String target,
            List<WidgetWorker.Parameter> parameterList, String description, String targetWindow, ModelFormField modelFormField,
            HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) throws IOException {
        String realLinkType = WidgetWorker.determineAutoLinkType(linkType, target, targetType, request);
        if ("hidden-form".equals(realLinkType)) {
            if (modelFormField != null && "multi".equals(modelFormField.getModelForm().getType())) {
                WidgetWorker.makeHiddenFormLinkAnchor(writer, linkStyle, description, modelFormField, request, response, context);

                // this is a bit trickier, since we can't do a nested form we'll have to put the link to submit the form in place, but put the actual form def elsewhere, ie after the big form is closed
                Map<String, Object> wholeFormContext = UtilGenerics.checkMap(context.get("wholeFormContext"));
                Appendable postMultiFormWriter = wholeFormContext != null ? (Appendable) wholeFormContext.get("postMultiFormWriter") : null;
                if (postMultiFormWriter == null) {
                    postMultiFormWriter = new StringWriter();
                    wholeFormContext.put("postMultiFormWriter", postMultiFormWriter);
                }
                WidgetWorker.makeHiddenFormLinkForm(postMultiFormWriter, target, targetType, targetWindow, parameterList, modelFormField, request, response, context);
            } else {
                WidgetWorker.makeHiddenFormLinkForm(writer, target, targetType, targetWindow, parameterList, modelFormField, request, response, context);
                WidgetWorker.makeHiddenFormLinkAnchor(writer, linkStyle, description, modelFormField, request, response, context);
            }
        } else {
            WidgetWorker.makeHyperlinkString(writer, linkStyle, targetType, target, parameterList, description, modelFormField, request, response, context, targetWindow);
        }
 
    }
 
    public void makeHyperlinkString(Appendable writer, String linkStyle, String targetType, String target, List<WidgetWorker.Parameter> parameterList,
            String description, ModelFormField modelFormField, HttpServletRequest request, HttpServletResponse response, Map<String, Object> context, String targetWindow)
            throws IOException {
        if (UtilValidate.isNotEmpty(description) || UtilValidate.isNotEmpty(request.getAttribute("image"))) {
            StringBuilder linkUrl = new StringBuilder();

            WidgetWorker.buildHyperlinkUrl(linkUrl, target, targetType, parameterList, null, false, false, true, request, response, context);

            String event = "";
            String action = "";
            String imgSrc = "";
            String hiddenFormName = WidgetWorker.makeLinkHiddenFormName(context, modelFormField);
   
            if (UtilValidate.isNotEmpty(modelFormField.getEvent()) && UtilValidate.isNotEmpty(modelFormField.getAction(context))) {
                event = modelFormField.getEvent();
                action = modelFormField.getAction(context);
            }

            if (UtilValidate.isNotEmpty(request.getAttribute("image"))){
            	imgSrc = request.getAttribute("image").toString();
            }
            
            StringWriter sr = new StringWriter();
            sr.append("<@makeHyperlinkString ");
            sr.append("linkStyle=\"");
            sr.append(linkStyle==null?"":linkStyle);
            sr.append("\" hiddenFormName=\"");
            sr.append(hiddenFormName==null?"":hiddenFormName);
            sr.append("\" event=\"");
            sr.append(event);
            sr.append("\" action=\"");
            sr.append(action);
            sr.append("\" imgSrc=\"");
            sr.append(imgSrc);
            sr.append("\" linkUrl=\"");
            sr.append(linkUrl.toString());
            sr.append("\" targetWindow=\"");
            sr.append(targetWindow);
            sr.append("\" description=\"");
            sr.append(description);
            sr.append("\" />");
            executeMacro(writer, sr.toString());
        }
    }
 
    public void makeHiddenFormLinkAnchor(Appendable writer, String linkStyle, String description, ModelFormField modelFormField, HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) throws IOException {
        if (UtilValidate.isNotEmpty(description) || UtilValidate.isNotEmpty(request.getAttribute("image"))) {
            String hiddenFormName = WidgetWorker.makeLinkHiddenFormName(context, modelFormField);
        	String event = "";
        	String action = "";
        	String imgSrc = "";
 
            if (UtilValidate.isNotEmpty(modelFormField.getEvent()) && UtilValidate.isNotEmpty(modelFormField.getAction(context))) {
                event = modelFormField.getEvent();
                action = modelFormField.getAction(context);
            }

            if (UtilValidate.isNotEmpty(request.getAttribute("image"))){
            	imgSrc = request.getAttribute("image").toString();
            }
            
            StringWriter sr = new StringWriter();
            sr.append("<@makeHiddenFormLinkAnchor ");
            sr.append("linkStyle=\"");
            sr.append(linkStyle==null?"":linkStyle);
            sr.append("\" hiddenFormName=\"");
            sr.append(hiddenFormName==null?"":hiddenFormName);
            sr.append("\" event=\"");
            sr.append(event);
            sr.append("\" action=\"");
            sr.append(action);
            sr.append("\" imgSrc=\"");
            sr.append(imgSrc);
            sr.append("\" description=\"");
            sr.append(description);
            sr.append("\" />");
            executeMacro(writer, sr.toString());
        }
    }
 
    public void makeHiddenFormLinkForm(Appendable writer, String target, String targetType, String targetWindow, List<WidgetWorker.Parameter> parameterList, ModelFormField modelFormField, HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) throws IOException {
    	StringBuilder actionUrl = new StringBuilder();
    	WidgetWorker.buildHyperlinkUrl(actionUrl, target, targetType, null, null, false, false, true, request, response, context);
        String name = WidgetWorker.makeLinkHiddenFormName(context, modelFormField);
    	StringBuilder parameters = new StringBuilder();
    	parameters.append("[");
        for (WidgetWorker.Parameter parameter: parameterList) {
        	 if(parameters.length() >1){
        		 parameters.append(",");
             }
        	 parameters.append("{'name':'");
        	 parameters.append(parameter.getName());
        	 parameters.append("'");
        	 parameters.append(",'value':'");
        	 parameters.append(parameter.getValue(context));
        	 parameters.append("'}");
        }
        parameters.append("]");
        
        StringWriter sr = new StringWriter();
        sr.append("<@makeHiddenFormLinkForm ");
        sr.append("actionUrl=\"");
        sr.append(actionUrl.toString());
        sr.append("\" name=\"");
        sr.append(name);
        sr.append("\" parameters=");
        sr.append(parameters.toString());
        sr.append(" targetWindow=\"");
        sr.append(targetWindow);
        sr.append("\" />");
        executeMacro(writer, sr.toString());
    }
}
