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
package org.ofbiz.widget.renderer.macro;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.rmi.server.UID;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilCodec;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.base.util.template.FreeMarkerWorker;
import org.ofbiz.entity.Delegator;
import org.ofbiz.webapp.control.RequestHandler;
import org.ofbiz.webapp.taglib.ContentUrlTag;
import org.ofbiz.widget.WidgetWorker;
import org.ofbiz.widget.model.CommonWidgetModels;
import org.ofbiz.widget.model.FieldInfo;
import org.ofbiz.widget.model.ModelForm;
import org.ofbiz.widget.model.ModelFormField;
import org.ofbiz.widget.model.ModelFormField.CheckField;
import org.ofbiz.widget.model.ModelFormField.ContainerField;
import org.ofbiz.widget.model.ModelFormField.DateFindField;
import org.ofbiz.widget.model.ModelFormField.DateTimeField;
import org.ofbiz.widget.model.ModelFormField.DisplayEntityField;
import org.ofbiz.widget.model.ModelFormField.DisplayField;
import org.ofbiz.widget.model.ModelFormField.DropDownField;
import org.ofbiz.widget.model.ModelFormField.FieldInfoWithOptions;
import org.ofbiz.widget.model.ModelFormField.FileField;
import org.ofbiz.widget.model.ModelFormField.HiddenField;
import org.ofbiz.widget.model.ModelFormField.HyperlinkField;
import org.ofbiz.widget.model.ModelFormField.IgnoredField;
import org.ofbiz.widget.model.ModelFormField.ImageField;
import org.ofbiz.widget.model.ModelFormField.LookupField;
import org.ofbiz.widget.model.ModelFormField.MenuField;
import org.ofbiz.widget.model.ModelFormField.PasswordField;
import org.ofbiz.widget.model.ModelFormField.RadioField;
import org.ofbiz.widget.model.ModelFormField.RangeFindField;
import org.ofbiz.widget.model.ModelFormField.ResetField;
import org.ofbiz.widget.model.ModelFormField.SubmitField;
import org.ofbiz.widget.model.ModelFormField.TextField;
import org.ofbiz.widget.model.ModelFormField.TextFindField;
import org.ofbiz.widget.model.ModelFormField.TextareaField;
import org.ofbiz.widget.model.ModelFormFieldBuilder;
import org.ofbiz.widget.model.ModelScreenWidget;
import org.ofbiz.widget.model.ModelSingleForm;
import org.ofbiz.widget.model.ModelWidget;
import org.ofbiz.widget.renderer.FormRenderer;
import org.ofbiz.widget.renderer.FormStringRenderer;
import org.ofbiz.widget.renderer.Paginator;
import org.ofbiz.widget.renderer.UtilHelpText;

import com.ibm.icu.util.Calendar;

import freemarker.core.Environment;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * Widget Library - Form Renderer implementation based on Freemarker macros
 *
 */
public final class MacroFormRenderer implements FormStringRenderer {

    public static final String module = MacroFormRenderer.class.getName();
    private final Template macroLibrary;
    private final WeakHashMap<Appendable, Environment> environments = new WeakHashMap<Appendable, Environment>();
    private final UtilCodec.SimpleEncoder internalEncoder;
    private final RequestHandler rh;
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final boolean javaScriptEnabled;
    private boolean renderPagination = true;
    private boolean widgetCommentsEnabled = false;

    public MacroFormRenderer(String macroLibraryPath, HttpServletRequest request, HttpServletResponse response) throws TemplateException, IOException {
        macroLibrary = FreeMarkerWorker.getTemplate(macroLibraryPath);
        this.request = request;
        this.response = response;
        ServletContext ctx = (ServletContext) request.getAttribute("servletContext");
        this.rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");
        this.javaScriptEnabled = UtilHttp.isJavaScriptEnabled(request);
        internalEncoder = UtilCodec.getEncoder("string");
    }

    @Deprecated
    public MacroFormRenderer(String macroLibraryPath, Appendable writer, HttpServletRequest request, HttpServletResponse response) throws TemplateException, IOException {
        this(macroLibraryPath, request, response);
    }

    public boolean getRenderPagination() {
        return this.renderPagination;
    }

    public void setRenderPagination(boolean renderPagination) {
        this.renderPagination = renderPagination;
    }

    private void executeMacro(Appendable writer, String macro) throws IOException {
        try {
            Environment environment = getEnvironment(writer);
            Reader templateReader = new StringReader(macro);
            Template template = new Template(new UID().toString(), templateReader, FreeMarkerWorker.getDefaultOfbizConfig());
            templateReader.close();
            environment.include(template);
        } catch (TemplateException e) {
            Debug.logError(e, "Error rendering screen thru ftl macro: " + macro, module);
        } catch (IOException e) {
            Debug.logError(e, "Error rendering screen thru ftl, macro: " + macro, module);
        }
    }

    private Environment getEnvironment(Appendable writer) throws TemplateException, IOException {
        Environment environment = environments.get(writer);
        if (environment == null) {
            Map<String, Object> input = UtilMisc.toMap("key", null);
            environment = FreeMarkerWorker.renderTemplate(macroLibrary, input, writer);
            environments.put(writer, environment);
        }
        return environment;
    }

    private String encode(String value, ModelFormField modelFormField, Map<String, Object> context) {
        if (UtilValidate.isEmpty(value)) {
            return value;
        }
        UtilCodec.SimpleEncoder encoder = (UtilCodec.SimpleEncoder) context.get("simpleEncoder");
        if (modelFormField.getEncodeOutput() && encoder != null) {
            value = encoder.encode(value);
        } else {
            value = internalEncoder.encode(value);
        }
        return value;
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
        String idName = modelFormField.getCurrentContainerId(context);
        String description = displayField.getDescription(context);
        String type = displayField.getType();
        String imageLocation = displayField.getImageLocation(context);
        Integer size = Integer.valueOf("0");
        String title = "";
        if (UtilValidate.isNotEmpty(displayField.getSize())) {
            try {
                size = Integer.parseInt(displayField.getSize());
            } catch (NumberFormatException nfe) {
                Debug.logError(nfe, "Error reading size of a field fieldName=" + displayField.getModelFormField().getFieldName() + " FormName= " + displayField.getModelFormField().getModelForm().getName(), module);
            }
        }
        ModelFormField.InPlaceEditor inPlaceEditor = displayField.getInPlaceEditor();
        boolean ajaxEnabled = inPlaceEditor != null && this.javaScriptEnabled;
        if (UtilValidate.isNotEmpty(description) && size > 0 && description.length() > size) {
            title = description;
            description = description.substring(0, size - 8) + "..." + description.substring(description.length() - 5);
        }
        StringWriter sr = new StringWriter();
        sr.append("<@renderDisplayField ");
        sr.append("type=\"");
        sr.append(type);
        sr.append("\" imageLocation=\"");
        sr.append(imageLocation);
        sr.append("\" idName=\"");
        sr.append(idName);
        sr.append("\" description=\"");
        sr.append(FreeMarkerWorker.encodeDoubleQuotes(description));
        sr.append("\" title=\"");
        sr.append(title);
        sr.append("\" class=\"");
        sr.append(modelFormField.getWidgetStyle());
        sr.append("\" alert=\"");
        sr.append(modelFormField.shouldBeRed(context) ? "true" : "false");
        if (ajaxEnabled) {
            String url = inPlaceEditor.getUrl(context);
            String extraParameter = "{";
            Map<String, Object> fieldMap = inPlaceEditor.getFieldMap(context);
            if (fieldMap != null) {
                Set<Entry<String, Object>> fieldSet = fieldMap.entrySet();
                Iterator<Entry<String, Object>> fieldIterator = fieldSet.iterator();
                int count = 0;
                while (fieldIterator.hasNext()) {
                    count++;
                    Entry<String, Object> field = fieldIterator.next();
                    extraParameter += field.getKey() + ":'" + (String) field.getValue() + "'";
                    if (count < fieldSet.size()) {
                        extraParameter += ',';
                    }
                }

            }
            extraParameter += "}";
            sr.append("\" inPlaceEditorUrl=\"");
            sr.append(url);
            sr.append("\" inPlaceEditorParams=\"");
            StringWriter inPlaceEditorParams = new StringWriter();
            inPlaceEditorParams.append("{name: '");
            if (UtilValidate.isNotEmpty(inPlaceEditor.getParamName())) {
                inPlaceEditorParams.append(inPlaceEditor.getParamName());
            } else {
                inPlaceEditorParams.append(modelFormField.getFieldName());
            }
            inPlaceEditorParams.append("'");
            inPlaceEditorParams.append(", method: 'POST'");
            inPlaceEditorParams.append(", submitdata: " + extraParameter);
            inPlaceEditorParams.append(", type: 'textarea'");
            inPlaceEditorParams.append(", select: 'true'");
            inPlaceEditorParams.append(", onreset: function(){jQuery('#cc_" + idName + "').css('background-color', 'transparent');}");
            if (UtilValidate.isNotEmpty(inPlaceEditor.getCancelText())) {
                inPlaceEditorParams.append(", cancel: '" + inPlaceEditor.getCancelText() + "'");
            } else {
                inPlaceEditorParams.append(", cancel: 'Cancel'");
            }
            if (UtilValidate.isNotEmpty(inPlaceEditor.getClickToEditText())) {
                inPlaceEditorParams.append(", tooltip: '" + inPlaceEditor.getClickToEditText() + "'");
            }
            if (UtilValidate.isNotEmpty(inPlaceEditor.getFormClassName())) {
                inPlaceEditorParams.append(", cssclass: '" + inPlaceEditor.getFormClassName() + "'");
            } else {
                inPlaceEditorParams.append(", cssclass: 'inplaceeditor-form'");
            }
            if (UtilValidate.isNotEmpty(inPlaceEditor.getLoadingText())) {
                inPlaceEditorParams.append(", indicator: '" + inPlaceEditor.getLoadingText() + "'");
            }
            if (UtilValidate.isNotEmpty(inPlaceEditor.getOkControl())) {
                inPlaceEditorParams.append(", submit: ");
                if (!"false".equals(inPlaceEditor.getOkControl())) {
                    inPlaceEditorParams.append("'");
                }
                inPlaceEditorParams.append(inPlaceEditor.getOkControl());
                if (!"false".equals(inPlaceEditor.getOkControl())) {
                    inPlaceEditorParams.append("'");
                }
            } else {
                inPlaceEditorParams.append(", submit: 'OK'");
            }
            if (UtilValidate.isNotEmpty(inPlaceEditor.getRows())) {
                inPlaceEditorParams.append(", rows: '" + inPlaceEditor.getRows() + "'");
            }
            if (UtilValidate.isNotEmpty(inPlaceEditor.getCols())) {
                inPlaceEditorParams.append(", cols: '" + inPlaceEditor.getCols() + "'");
            }
            inPlaceEditorParams.append("}");
            sr.append(inPlaceEditorParams.toString());
        }
        sr.append("\" />");
        executeMacro(writer, sr.toString());
        if (displayField instanceof DisplayEntityField) {
            makeHyperlinkString(writer, ((DisplayEntityField) displayField).getSubHyperlink(), context);
        }
        this.appendTooltip(writer, context, modelFormField);
    }

    public void renderHyperlinkField(Appendable writer, Map<String, Object> context, HyperlinkField hyperlinkField) throws IOException {
        this.request.setAttribute("image", hyperlinkField.getImageLocation(context));
        ModelFormField modelFormField = hyperlinkField.getModelFormField();
        String encodedAlternate = encode(hyperlinkField.getAlternate(context), modelFormField, context);
        String encodedImageTitle = encode(hyperlinkField.getImageTitle(context), modelFormField, context);
        this.request.setAttribute("alternate", encodedAlternate);
        this.request.setAttribute("imageTitle", encodedImageTitle);
        this.request.setAttribute("descriptionSize", hyperlinkField.getSize());
        this.request.setAttribute("id", hyperlinkField.getId(context));
        this.request.setAttribute("width", hyperlinkField.getWidth());
        this.request.setAttribute("height", hyperlinkField.getHeight());
        makeHyperlinkByType(writer, hyperlinkField.getLinkType(), modelFormField.getWidgetStyle(), hyperlinkField.getUrlMode(), hyperlinkField.getTarget(context),
                hyperlinkField.getParameterMap(context, modelFormField.getEntityName(), modelFormField.getServiceName()), 
                hyperlinkField.getDescription(context), hyperlinkField.getTargetWindow(context),
                hyperlinkField.getConfirmation(context), modelFormField, this.request, this.response, context);
        this.appendTooltip(writer, context, modelFormField);
        this.request.removeAttribute("image");
        this.request.removeAttribute("descriptionSize");
    }

    public void renderMenuField(Appendable writer, Map<String, Object> context, MenuField menuField) throws IOException {
        menuField.renderFieldString(writer, context, null);
    }

    public void renderTextField(Appendable writer, Map<String, Object> context, TextField textField) throws IOException {
        ModelFormField modelFormField = textField.getModelFormField();
        String name = modelFormField.getParameterName(context);
        String className = "";
        String alert = "false";
        String mask = "";
        String placeholder = textField.getPlaceholder(context);
        if (UtilValidate.isNotEmpty(modelFormField.getWidgetStyle())) {
            className = modelFormField.getWidgetStyle();
            if (modelFormField.shouldBeRed(context)) {
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
        String id = modelFormField.getCurrentContainerId(context);
        String clientAutocomplete = "false";
        //check for required field style on single forms
        if ("single".equals(modelFormField.getModelForm().getType()) && modelFormField.getRequiredField()) {
            String requiredStyle = modelFormField.getRequiredFieldStyle();
            if (UtilValidate.isEmpty(requiredStyle))
                requiredStyle = "required";
            if (UtilValidate.isEmpty(className))
                className = requiredStyle;
            else
                className = requiredStyle + " " + className;
        }
        List<ModelForm.UpdateArea> updateAreas = modelFormField.getOnChangeUpdateAreas();
        boolean ajaxEnabled = updateAreas != null && this.javaScriptEnabled;
        if (textField.getClientAutocompleteField() || ajaxEnabled) {
            clientAutocomplete = "true";
        }
        if (UtilValidate.isNotEmpty(textField.getMask())) {
            mask = textField.getMask();
        }
        String ajaxUrl = createAjaxParamsFromUpdateAreas(updateAreas, "", context);
        boolean disabled = textField.getDisabled();
        boolean readonly = textField.getReadonly();
        String tabindex = modelFormField.getTabindex();
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
        if (event != null) {
            sr.append(event);
        }
        sr.append("\" action=\"");
        if (action != null) {
            sr.append(action);
        }
        sr.append("\" disabled=");
        sr.append(Boolean.toString(disabled));
        sr.append(" readonly=");
        sr.append(Boolean.toString(readonly));
        sr.append(" clientAutocomplete=\"");
        sr.append(clientAutocomplete);
        sr.append("\" ajaxUrl=\"");
        sr.append(ajaxUrl);
        sr.append("\" ajaxEnabled=");
        sr.append(Boolean.toString(ajaxEnabled));
        sr.append(" mask=\"");
        sr.append(mask);
        sr.append("\" placeholder=\"");
        sr.append(placeholder);
        sr.append("\" tabindex=\"");
        sr.append(tabindex);
        sr.append("\" />");
        executeMacro(writer, sr.toString());
        ModelFormField.SubHyperlink subHyperlink = textField.getSubHyperlink();
        if (subHyperlink != null && subHyperlink.shouldUse(context)) {
            makeHyperlinkString(writer, subHyperlink, context);
        }
        this.addAsterisks(writer, context, modelFormField);
        this.appendTooltip(writer, context, modelFormField);
    }

    public void renderTextareaField(Appendable writer, Map<String, Object> context, TextareaField textareaField) throws IOException {
        ModelFormField modelFormField = textareaField.getModelFormField();
        String name = modelFormField.getParameterName(context);
        String cols = Integer.toString(textareaField.getCols());
        String rows = Integer.toString(textareaField.getRows());
        String id = modelFormField.getCurrentContainerId(context);
        String className = "";
        String alert = "false";
        if (UtilValidate.isNotEmpty(modelFormField.getWidgetStyle())) {
            className = modelFormField.getWidgetStyle();
            if (modelFormField.shouldBeRed(context)) {
                alert = "true";
            }
        }
        //check for required field style on single forms
        if ("single".equals(modelFormField.getModelForm().getType()) && modelFormField.getRequiredField()) {
            String requiredStyle = modelFormField.getRequiredFieldStyle();
            if (UtilValidate.isEmpty(requiredStyle))
                requiredStyle = "required";
            if (UtilValidate.isEmpty(className))
                className = requiredStyle;
            else
                className = requiredStyle + " " + className;
        }
        String visualEditorEnable = "";
        String buttons = "";
        if (textareaField.getVisualEditorEnable()) {
            visualEditorEnable = "true";
            buttons = textareaField.getVisualEditorButtons(context);
            if (UtilValidate.isEmpty(buttons)) {
                buttons = "maxi";
            }
        }
        String readonly = "";
        if (textareaField.isReadOnly()) {
            readonly = "readonly";
        }
        Map<String, Object> userLogin = UtilGenerics.checkMap(context.get("userLogin"));
        String language = "en";
        if (userLogin != null) {
            language = UtilValidate.isEmpty((String) userLogin.get("lastLocale")) ? "en" : (String) userLogin.get("lastLocale");
        }
        String maxlength = "";
        if (textareaField.getMaxlength() != null) {
            maxlength = Integer.toString(textareaField.getMaxlength());
        }
        String tabindex = modelFormField.getTabindex();
        String value = modelFormField.getEntry(context, textareaField.getDefaultValue(context));
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
        sr.append("\" maxlength=\"");
        sr.append(maxlength);
        sr.append("\" id=\"");
        sr.append(id);
        sr.append("\" readonly=\"");
        sr.append(readonly);
        sr.append("\" visualEditorEnable=\"");
        sr.append(visualEditorEnable);
        sr.append("\" language=\"");
        sr.append(language);
        sr.append("\" buttons=\"");
        sr.append(buttons);
        sr.append("\" tabindex=\"");
        sr.append(tabindex);
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
        String formattedMask = "";
        String event = modelFormField.getEvent();
        String action = modelFormField.getAction(context);
        if (UtilValidate.isNotEmpty(modelFormField.getWidgetStyle())) {
            className = modelFormField.getWidgetStyle();
            if (modelFormField.shouldBeRed(context)) {
                alert = "true";
            }
        }
        boolean useTimeDropDown = "time-dropdown".equals(dateTimeField.getInputMethod());
        String stepString = dateTimeField.getStep();
        int step = 1;
        StringBuilder timeValues = new StringBuilder();
        if (useTimeDropDown && UtilValidate.isNotEmpty(step)) {
            try {
                step = Integer.valueOf(stepString).intValue();
            } catch (IllegalArgumentException e) {
                Debug.logWarning("Invalid value for step property for field[" + paramName + "] with input-method=\"time-dropdown\" " + " Found Value [" + stepString + "]  " + e.getMessage(), module);
            }
            timeValues.append("[");
            for (int i = 0; i <= 59;) {
                if (i != 0) {
                    timeValues.append(", ");
                }
                timeValues.append(i);
                i += step;
            }
            timeValues.append("]");
        }
        Map<String, String> uiLabelMap = UtilGenerics.checkMap(context.get("uiLabelMap"));
        if (uiLabelMap == null) {
            Debug.logWarning("Could not find uiLabelMap in context", module);
        }
        String localizedInputTitle = "", localizedIconTitle = "";
        // whether the date field is short form, yyyy-mm-dd
        boolean shortDateInput = ("date".equals(dateTimeField.getType()) || useTimeDropDown ? true : false);
        if (useTimeDropDown) {
            name = UtilHttp.makeCompositeParam(paramName, "date");
        } else {
            name = paramName;
        }
        // the default values for a timestamp
        int size = 25;
        int maxlength = 30;
        if (shortDateInput) {
            size = maxlength = 10;
            if (uiLabelMap != null) {
                localizedInputTitle = uiLabelMap.get("CommonFormatDate");
            }
        } else if ("time".equals(dateTimeField.getType())) {
            size = maxlength = 8;
            if (uiLabelMap != null) {
                localizedInputTitle = uiLabelMap.get("CommonFormatTime");
            }
        } else {
            if (uiLabelMap != null) {
                localizedInputTitle = uiLabelMap.get("CommonFormatDateTime");
            }
        }
        /*
         * FIXME: Using a builder here is a hack. Replace the builder with appropriate code.
         */
        ModelFormFieldBuilder builder = new ModelFormFieldBuilder(modelFormField);
        boolean memEncodeOutput = modelFormField.getEncodeOutput();
        if (useTimeDropDown)
            // If time-dropdown deactivate encodingOutput for found hour and minutes
            // FIXME: Encoding should be controlled by the renderer, not by the model.
            builder.setEncodeOutput(false);
        // FIXME: modelFormField.getEntry ignores shortDateInput when converting Date objects to Strings.
        if (useTimeDropDown) {
            builder.setEncodeOutput(memEncodeOutput);
        }
        modelFormField = builder.build();
        String contextValue = modelFormField.getEntry(context, dateTimeField.getDefaultValue(context));
        String value = contextValue;
        if (UtilValidate.isNotEmpty(value)) {
            if (value.length() > maxlength) {
                value = value.substring(0, maxlength);
            }
        }
        String id = modelFormField.getCurrentContainerId(context);
        ModelForm modelForm = modelFormField.getModelForm();
        String formName = FormRenderer.getCurrentFormName(modelForm, context);
        String timeDropdown = dateTimeField.getInputMethod();
        String timeDropdownParamName = "";
        String classString = "";
        boolean isTwelveHour = false;
        String timeHourName = "";
        int hour2 = 0, hour1 = 0, minutes = 0;
        String timeMinutesName = "";
        String amSelected = "", pmSelected = "", ampmName = "";
        String compositeType = "";
        // search for a localized label for the icon
        if (uiLabelMap != null) {
            localizedIconTitle = uiLabelMap.get("CommonViewCalendar");
        }
        if (!"time".equals(dateTimeField.getType())) {
            String tempParamName;
            if (useTimeDropDown) {
                tempParamName = UtilHttp.makeCompositeParam(paramName, "date");
            } else {
                tempParamName = paramName;
            }
            timeDropdownParamName = tempParamName;
            defaultDateTimeString = UtilHttp.encodeBlanks(modelFormField.getEntry(context, defaultDateTimeString));
        }
        // if we have an input method of time-dropdown, then render two
        // dropdowns
        if (useTimeDropDown) {
            className = modelFormField.getWidgetStyle();
            classString = (className != null ? className : "");
            isTwelveHour = "12".equals(dateTimeField.getClock());
            // set the Calendar to the default time of the form or now()
            Calendar cal = null;
            try {
                Timestamp defaultTimestamp = Timestamp.valueOf(contextValue);
                cal = Calendar.getInstance();
                cal.setTime(defaultTimestamp);
            } catch (IllegalArgumentException e) {
                Debug.logWarning("Form widget field [" + paramName + "] with input-method=\"time-dropdown\" was not able to understand the default time [" + defaultDateTimeString + "]. The parsing error was: " + e.getMessage(), module);
            }
            timeHourName = UtilHttp.makeCompositeParam(paramName, "hour");
            if (cal != null) {
                int hour = cal.get(Calendar.HOUR_OF_DAY);
                hour2 = hour;
                if (hour == 0) {
                    hour = 12;
                }
                if (hour > 12) {
                    hour -= 12;
                }
                hour1 = hour;
                minutes = cal.get(Calendar.MINUTE);
            }
            timeMinutesName = UtilHttp.makeCompositeParam(paramName, "minutes");
            compositeType = UtilHttp.makeCompositeParam(paramName, "compositeType");
            // if 12 hour clock, write the AM/PM selector
            if (isTwelveHour) {
                amSelected = ((cal != null && cal.get(Calendar.AM_PM) == Calendar.AM) ? "selected" : "");
                pmSelected = ((cal != null && cal.get(Calendar.AM_PM) == Calendar.PM) ? "selected" : "");
                ampmName = UtilHttp.makeCompositeParam(paramName, "ampm");
            }
        }
        //check for required field style on single forms
        if ("single".equals(modelFormField.getModelForm().getType()) && modelFormField.getRequiredField()) {
            String requiredStyle = modelFormField.getRequiredFieldStyle();
            if (UtilValidate.isEmpty(requiredStyle))
                requiredStyle = "required";
            if (UtilValidate.isEmpty(className))
                className = requiredStyle;
            else
                className = requiredStyle + " " + className;
        }
        String mask = dateTimeField.getMask();
        if ("Y".equals(mask)) {
            if ("date".equals(dateTimeField.getType())) {
                formattedMask = "9999-99-99";
            } else if ("time".equals(dateTimeField.getType())) {
                formattedMask = "99:99:99";
            } else if ("timestamp".equals(dateTimeField.getType())) {
                formattedMask = "9999-99-99 99:99:99";
            }
        }
        String tabindex = modelFormField.getTabindex();
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
        sr.append("\" size=\"");
        sr.append(Integer.toString(size));
        sr.append("\" maxlength=\"");
        sr.append(Integer.toString(maxlength));
        sr.append("\" step=\"");
        sr.append(Integer.toString(step));
        sr.append("\" timeValues=\"");
        sr.append(timeValues.toString());
        sr.append("\" id=\"");
        sr.append(id);
        sr.append("\" event=\"");
        sr.append(event);
        sr.append("\" action=\"");
        sr.append(action);
        sr.append("\" dateType=\"");
        sr.append(dateTimeField.getType());
        sr.append("\" shortDateInput=");
        sr.append(Boolean.toString(shortDateInput));
        sr.append(" timeDropdownParamName=\"");
        sr.append(timeDropdownParamName);
        sr.append("\" defaultDateTimeString=\"");
        sr.append(defaultDateTimeString);
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
        sr.append("\" mask=\"");
        sr.append(formattedMask);
        sr.append("\" tabindex=\"");
        sr.append(tabindex);
        sr.append("\" />");
        executeMacro(writer, sr.toString());
        this.addAsterisks(writer, context, modelFormField);
        this.appendTooltip(writer, context, modelFormField);
    }

    public void renderDropDownField(Appendable writer, Map<String, Object> context, DropDownField dropDownField) throws IOException {
        ModelFormField modelFormField = dropDownField.getModelFormField();
        ModelForm modelForm = modelFormField.getModelForm();
        String currentValue = modelFormField.getEntry(context);
        List<ModelFormField.OptionValue> allOptionValues = dropDownField.getAllOptionValues(context, WidgetWorker.getDelegator(context));
        ModelFormField.AutoComplete autoComplete = dropDownField.getAutoComplete();
        String event = modelFormField.getEvent();
        String action = modelFormField.getAction(context);
        Integer textSize = Integer.valueOf(0);
        if (UtilValidate.isNotEmpty(dropDownField.getTextSize())) {
            try {
                textSize = Integer.parseInt(dropDownField.getTextSize());
            } catch (NumberFormatException nfe) {
                Debug.logError(nfe, "Error reading size of a field fieldName=" + dropDownField.getModelFormField().getFieldName() + " FormName= " + dropDownField.getModelFormField().getModelForm().getName(), module);
            }
            if (textSize > 0 && UtilValidate.isNotEmpty(currentValue) && currentValue.length() > textSize) {
                currentValue = currentValue.substring(0, textSize - 8) + "..." + currentValue.substring(currentValue.length() - 5);
            }
        }
        boolean ajaxEnabled = autoComplete != null && this.javaScriptEnabled;
        String className = "";
        String alert = "false";
        String name = modelFormField.getParameterName(context);
        String id = modelFormField.getCurrentContainerId(context);
        String multiple = dropDownField.getAllowMultiple() ? "multiple" : "";
        String otherFieldName = "";
        String formName = modelForm.getName();
        String size = dropDownField.getSize();
        String dDFCurrent = dropDownField.getCurrent();
        String firstInList = "";
        String explicitDescription = "";
        String allowEmpty = "";
        StringBuilder options = new StringBuilder();
        StringBuilder ajaxOptions = new StringBuilder();
        if (UtilValidate.isNotEmpty(modelFormField.getWidgetStyle())) {
            className = modelFormField.getWidgetStyle();
            if (modelFormField.shouldBeRed(context)) {
                alert = "true";
            }
        }
        //check for required field style on single forms
        if ("single".equals(modelFormField.getModelForm().getType()) && modelFormField.getRequiredField()) {
            String requiredStyle = modelFormField.getRequiredFieldStyle();
            if (UtilValidate.isEmpty(requiredStyle))
                requiredStyle = "required";
            if (UtilValidate.isEmpty(className))
                className = requiredStyle;
            else
                className = requiredStyle + " " + className;
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
        explicitDescription = (currentDescription != null ? currentDescription : dropDownField.getCurrentDescription(context));
        if (UtilValidate.isEmpty(explicitDescription)) {
            explicitDescription = (FieldInfoWithOptions.getDescriptionForOptionKey(currentValue, allOptionValues));
        }
        if (textSize > 0 && UtilValidate.isNotEmpty(explicitDescription) && explicitDescription.length() > textSize) {
            explicitDescription = explicitDescription.substring(0, textSize - 8) + "..." + explicitDescription.substring(explicitDescription.length() - 5);
        }
        explicitDescription = encode(explicitDescription, modelFormField, context);
        // if allow empty is true, add an empty option
        if (dropDownField.getAllowEmpty()) {
            allowEmpty = "Y";
        }
        List<String> currentValueList = null;
        if (UtilValidate.isNotEmpty(currentValue) && dropDownField.getAllowMultiple()) {
            // If currentValue is Array, it will start with [
            if (currentValue.startsWith("[")) {
                currentValueList = StringUtil.toList(currentValue);
            } else {
                currentValueList = UtilMisc.toList(currentValue);
            }
        }
        options.append("[");
        Iterator<ModelFormField.OptionValue> optionValueIter = allOptionValues.iterator();
        int count = 0;
        while (optionValueIter.hasNext()) {
            ModelFormField.OptionValue optionValue = optionValueIter.next();
            if (options.length() > 1) {
                options.append(",");
            }
            options.append("{'key':'");
            String key = encode(optionValue.getKey(), modelFormField, context);
            options.append(key);
            options.append("'");
            options.append(",'description':'");
            String description = optionValue.getDescription();
            if (textSize > 0 && description.length() > textSize) {
                description = description.substring(0, textSize - 8) + "..." + description.substring(description.length() - 5);
            }
            options.append(encode(description.replaceAll("'", "\\\\\'"), modelFormField, context));  // replaceAll("'", "\\\\\'") related to OFBIZ-6504

            if (UtilValidate.isNotEmpty(currentValueList)) {
                options.append("'");
                options.append(",'selected':'");
                if (currentValueList.contains(optionValue.getKey())) {
                    options.append("selected");
                } else {
                    options.append("");
                }
            }

            options.append("'}");
            if (ajaxEnabled) {
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
            Map<String, ? extends Object> dataMap = modelFormField.getMap(context);
            if (dataMap == null) {
                dataMap = context;
            }
            Object otherValueObj = dataMap.get(otherFieldName);
            otherValue = (otherValueObj == null) ? "" : otherValueObj.toString();
        }
        String frequency = "";
        String minChars = "";
        String choices = "";
        String autoSelect = "";
        String partialSearch = "";
        String partialChars = "";
        String ignoreCase = "";
        String fullSearch = "";
        if (ajaxEnabled) {
            frequency = autoComplete.getFrequency();
            minChars = autoComplete.getMinChars();
            choices = autoComplete.getChoices();
            autoSelect = autoComplete.getAutoSelect();
            partialSearch = autoComplete.getPartialSearch();
            partialChars = autoComplete.getPartialChars();
            ignoreCase = autoComplete.getIgnoreCase();
            fullSearch = autoComplete.getFullSearch();
        }
        String tabindex = modelFormField.getTabindex();
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
        if (event != null) {
            sr.append(event);
        }
        sr.append("\" action=\"");
        if (action != null) {
            sr.append(action);
        }
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
        sr.append("\" tabindex=\"");
        sr.append(tabindex);
        sr.append("\" />");
        executeMacro(writer, sr.toString());
        ModelFormField.SubHyperlink subHyperlink = dropDownField.getSubHyperlink();
        if (subHyperlink != null && subHyperlink.shouldUse(context)) {
            makeHyperlinkString(writer, subHyperlink, context);
        }
        this.appendTooltip(writer, context, modelFormField);
    }

    public void renderCheckField(Appendable writer, Map<String, Object> context, CheckField checkField) throws IOException {
        ModelFormField modelFormField = checkField.getModelFormField();
        modelFormField.getModelForm();
        String currentValue = modelFormField.getEntry(context);
        Boolean allChecked = checkField.isAllChecked(context);
        String id = modelFormField.getCurrentContainerId(context);
        String className = "";
        String alert = "false";
        String name = modelFormField.getParameterName(context);
        String event = modelFormField.getEvent();
        String action = modelFormField.getAction(context);
        StringBuilder items = new StringBuilder();
        if (UtilValidate.isNotEmpty(modelFormField.getWidgetStyle())) {
            className = modelFormField.getWidgetStyle();
            if (modelFormField.shouldBeRed(context)) {
                alert = "true";
            }
        }
        String tabindex = modelFormField.getTabindex();
        List<ModelFormField.OptionValue> allOptionValues = checkField.getAllOptionValues(context, WidgetWorker.getDelegator(context));
        items.append("[");
        for (ModelFormField.OptionValue optionValue : allOptionValues) {
            if (items.length() > 1) {
                items.append(",");
            }
            items.append("{'value':'");
            items.append(optionValue.getKey());
            items.append("', 'description':'" + encode(optionValue.getDescription(), modelFormField, context));
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
        sr.append("\" id=\"");
        sr.append(id);
        sr.append("\" allChecked=");
        sr.append((allChecked != null ? Boolean.toString(allChecked) : "\"\""));
        sr.append(" currentValue=\"");
        sr.append(currentValue);
        sr.append("\" name=\"");
        sr.append(name);
        sr.append("\" event=\"");
        if (event != null) {
            sr.append(event);
        }
        sr.append("\" action=\"");
        if (action != null) {
            sr.append(action);
        }
        sr.append("\" tabindex=\"");
        sr.append(tabindex);
        sr.append("\" />");
        executeMacro(writer, sr.toString());
        this.appendTooltip(writer, context, modelFormField);
    }

    public void renderRadioField(Appendable writer, Map<String, Object> context, RadioField radioField) throws IOException {
        ModelFormField modelFormField = radioField.getModelFormField();
        modelFormField.getModelForm();
        List<ModelFormField.OptionValue> allOptionValues = radioField.getAllOptionValues(context, WidgetWorker.getDelegator(context));
        String currentValue = modelFormField.getEntry(context);
        String className = "";
        String alert = "false";
        String name = modelFormField.getParameterName(context);
        String event = modelFormField.getEvent();
        String action = modelFormField.getAction(context);
        StringBuilder items = new StringBuilder();
        if (UtilValidate.isNotEmpty(modelFormField.getWidgetStyle())) {
            className = modelFormField.getWidgetStyle();
            if (modelFormField.shouldBeRed(context)) {
                alert = "true";
            }
        }
        String noCurrentSelectedKey = radioField.getNoCurrentSelectedKey(context);
        String tabindex = modelFormField.getTabindex();
        items.append("[");
        for (ModelFormField.OptionValue optionValue : allOptionValues) {
            if (items.length() > 1) {
                items.append(",");
            }
            items.append("{'key':'");
            items.append(optionValue.getKey());
            items.append("', 'description':'" + encode(optionValue.getDescription(), modelFormField, context));
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
        sr.append("\" currentValue=\"");
        sr.append(currentValue);
        sr.append("\" noCurrentSelectedKey=\"");
        sr.append(noCurrentSelectedKey);
        sr.append("\" name=\"");
        sr.append(name);
        sr.append("\" event=\"");
        if (event != null) {
            sr.append(event);
        }
        sr.append("\" action=\"");
        if (action != null) {
            sr.append(action);
        }
        sr.append("\" tabindex=\"");
        sr.append(tabindex);
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
        String buttonType = submitField.getButtonType();
        String formName = FormRenderer.getCurrentFormName(modelForm, context);
        String imgSrc = submitField.getImageLocation(context);
        String confirmation = submitField.getConfirmation(context);
        String className = "";
        String alert = "false";
        if (UtilValidate.isNotEmpty(modelFormField.getWidgetStyle())) {
            className = modelFormField.getWidgetStyle();
            if (modelFormField.shouldBeRed(context)) {
                alert = "true";
            }
        }
        String formId = FormRenderer.getCurrentContainerId(modelForm, context);
        List<ModelForm.UpdateArea> updateAreas = modelForm.getOnSubmitUpdateAreas();
        // This is here for backwards compatibility. Use on-event-update-area
        // elements instead.
        String backgroundSubmitRefreshTarget = submitField.getBackgroundSubmitRefreshTarget(context);
        if (UtilValidate.isNotEmpty(backgroundSubmitRefreshTarget)) {
            if (updateAreas == null) {
                updateAreas = new LinkedList<ModelForm.UpdateArea>();
            }
            updateAreas.add(new ModelForm.UpdateArea("submit", formId, backgroundSubmitRefreshTarget));
        }
        boolean ajaxEnabled = (UtilValidate.isNotEmpty(updateAreas) || UtilValidate.isNotEmpty(backgroundSubmitRefreshTarget)) && this.javaScriptEnabled;
        String ajaxUrl = "";
        if (ajaxEnabled) {
            ajaxUrl = createAjaxParamsFromUpdateAreas(updateAreas, "", context);
        }
        String tabindex = modelFormField.getTabindex();
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
        sr.append(encode(title, modelFormField, context));
        sr.append("\" name=\"");
        sr.append(name);
        sr.append("\" event=\"");
        if (event != null) {
            sr.append(event);
        }
        sr.append("\" action=\"");
        if (action != null) {
            sr.append(action);
        }
        sr.append("\" imgSrc=\"");
        sr.append(imgSrc);
        sr.append("\" containerId=\"");
        if (ajaxEnabled) {
            sr.append(formId);
        }
        sr.append("\" confirmation =\"");
        sr.append(confirmation);
        sr.append("\" ajaxUrl=\"");
        if (ajaxEnabled) {
            sr.append(ajaxUrl);
        }
        sr.append("\" tabindex=\"");
        sr.append(tabindex);
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
            if (modelFormField.shouldBeRed(context)) {
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
        sr.append("\" title=\"");
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
        String action = modelFormField.getAction(context);
        String event = modelFormField.getEvent();
        String id = modelFormField.getCurrentContainerId(context);
        StringWriter sr = new StringWriter();
        sr.append("<@renderHiddenField ");
        sr.append(" name=\"");
        sr.append(name);
        sr.append("\" value=\"");
        sr.append(value);
        sr.append("\" id=\"");
        sr.append(id);
        sr.append("\" event=\"");
        if (event != null) {
            sr.append(event);
        }
        sr.append("\" action=\"");
        if (action != null) {
            sr.append(action);
        }
        sr.append("\" />");
        executeMacro(writer, sr.toString());
    }

    public void renderIgnoredField(Appendable writer, Map<String, Object> context, IgnoredField ignoredField) {
        // do nothing, it's an ignored field; could add a comment or something if we wanted to
    }

    public void renderFieldTitle(Appendable writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
        String titleText = modelFormField.getTitle(context);
        String style = modelFormField.getTitleStyle();
        String id = modelFormField.getCurrentContainerId(context);
        StringBuilder sb = new StringBuilder();
        if (UtilValidate.isNotEmpty(titleText)) {
            if (" ".equals(titleText)) {
                executeMacro(writer, "<@renderFormatEmptySpace />");
            } else {
                titleText = UtilHttp.encodeAmpersands(titleText);
                titleText = encode(titleText, modelFormField, context);
                if (UtilValidate.isNotEmpty(modelFormField.getHeaderLink())) {
                    StringBuilder targetBuffer = new StringBuilder();
                    FlexibleStringExpander target = FlexibleStringExpander.getInstance(modelFormField.getHeaderLink());
                    String fullTarget = target.expandString(context);
                    targetBuffer.append(fullTarget);
                    String targetType = CommonWidgetModels.Link.DEFAULT_URL_MODE;
                    if (UtilValidate.isNotEmpty(targetBuffer.toString()) && targetBuffer.toString().toLowerCase().startsWith("javascript:")) {
                        targetType = "plain";
                    }
                    StringWriter sr = new StringWriter();
                    makeHyperlinkString(sr, modelFormField.getHeaderLinkStyle(), targetType, targetBuffer.toString(), null, titleText, "", modelFormField, this.request, this.response, context, "");
                    String title = sr.toString().replace("\"", "\'");
                    sr = new StringWriter();
                    sr.append("<@renderHyperlinkTitle ");
                    sr.append(" name=\"");
                    sr.append(modelFormField.getModelForm().getName());
                    sr.append("\" title=\"");
                    sr.append(FreeMarkerWorker.encodeDoubleQuotes(title));
                    sr.append("\" />");
                    executeMacro(writer, sr.toString());
                } else if (modelFormField.isSortField()) {
                    renderSortField(writer, context, modelFormField, titleText);
                } else if (modelFormField.isRowSubmit()) {
                    StringWriter sr = new StringWriter();
                    sr.append("<@renderHyperlinkTitle ");
                    sr.append(" name=\"");
                    sr.append(modelFormField.getModelForm().getName());
                    sr.append("\" title=\"");
                    sr.append(titleText);
                    sr.append("\" showSelectAll=\"Y\"/>");
                    executeMacro(writer, sr.toString());
                } else {
                    sb.append(titleText);
                }
            }
        }
        if (!sb.toString().isEmpty()) {
            //check for required field style on single forms
            if ("single".equals(modelFormField.getModelForm().getType()) && modelFormField.getRequiredField()) {
                String requiredStyle = modelFormField.getRequiredFieldStyle();
                if (UtilValidate.isNotEmpty(requiredStyle)) {
                    style = requiredStyle;
                }
            }
            StringWriter sr = new StringWriter();
            sr.append("<@renderFieldTitle ");
            sr.append(" style=\"");
            sr.append(style);
            String displayHelpText = UtilProperties.getPropertyValue("widget", "widget.form.displayhelpText");
            if ("Y".equals(displayHelpText)) {
                Delegator delegator = WidgetWorker.getDelegator(context);
                Locale locale = (Locale) context.get("locale");
                String entityName = modelFormField.getEntityName();
                String fieldName = modelFormField.getFieldName();
                String helpText = UtilHelpText.getEntityFieldDescription(entityName, fieldName, delegator, locale);

                sr.append("\" fieldHelpText=\"");
                sr.append(FreeMarkerWorker.encodeDoubleQuotes(helpText));
            }
            sr.append("\" title=\"");
            sr.append(sb.toString());
            if (UtilValidate.isNotEmpty(id)) {
                sr.append("\" id=\"");
                sr.append(id);
                sr.append("_title");
                // Render "for"
                sr.append("\" for=\"");
                sr.append(id);
            }
            sr.append("\" />");
            executeMacro(writer, sr.toString());
        }
    }

    public void renderSingleFormFieldTitle(Appendable writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
        renderFieldTitle(writer, context, modelFormField);
    }

    public void renderFormOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        this.widgetCommentsEnabled = ModelWidget.widgetBoundaryCommentsEnabled(context);
        if (modelForm instanceof ModelSingleForm) {
            renderBeginningBoundaryComment(writer, "Form Widget - Form Element", modelForm);
        } else {
            renderBeginningBoundaryComment(writer, "Grid Widget - Grid Element", modelForm);
        }
        String targetType = modelForm.getTargetType();
        String targ = modelForm.getTarget(context, targetType);
        StringBuilder linkUrl = new StringBuilder();
        if (UtilValidate.isNotEmpty(targ)) {
            //this.appendOfbizUrl(writer, "/" + targ);
            WidgetWorker.buildHyperlinkUrl(linkUrl, targ, targetType, null, null, false, false, true, request, response, context);
        }
        String formType = modelForm.getType();
        String targetWindow = modelForm.getTargetWindow(context);
        String containerId = FormRenderer.getCurrentContainerId(modelForm, context);
        String containerStyle = modelForm.getContainerStyle();
        String autocomplete = "";
        String name = FormRenderer.getCurrentFormName(modelForm, context);
        String viewIndexField = modelForm.getMultiPaginateIndexField(context);
        String viewSizeField = modelForm.getMultiPaginateSizeField(context);
        int viewIndex = Paginator.getViewIndex(modelForm, context);
        int viewSize = Paginator.getViewSize(modelForm, context);
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
        sr.append("\" viewIndexField=\"");
        sr.append(viewIndexField);
        sr.append("\" viewSizeField=\"");
        sr.append(viewSizeField);
        sr.append("\" viewIndex=\"");
        sr.append(Integer.toString(viewIndex));
        sr.append("\" viewSize=\"");
        sr.append(Integer.toString(viewSize));
        sr.append("\" useRowSubmit=");
        sr.append(Boolean.toString(useRowSubmit));
        sr.append(" />");
        executeMacro(writer, sr.toString());
    }

    public void renderFormClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        String focusFieldName = modelForm.getFocusFieldName();
        String formName = FormRenderer.getCurrentFormName(modelForm, context);
        String containerId = FormRenderer.getCurrentContainerId(modelForm, context);
        String hasRequiredField = "";
        for (ModelFormField formField : modelForm.getFieldList()) {
            if (formField.getRequiredField()) {
                hasRequiredField = "Y";
                break;
            }
        }
        StringWriter sr = new StringWriter();
        sr.append("<@renderFormClose ");
        sr.append(" focusFieldName=\"");
        sr.append(focusFieldName);
        sr.append("\" formName=\"");
        sr.append(formName);
        sr.append("\" containerId=\"");
        sr.append(containerId);
        sr.append("\" hasRequiredField=\"");
        sr.append(hasRequiredField);
        sr.append("\" />");
        executeMacro(writer, sr.toString());
        if (modelForm instanceof ModelSingleForm) {
            renderEndingBoundaryComment(writer, "Form Widget - Form Element", modelForm);
        } else {
            renderEndingBoundaryComment(writer, "Grid Widget - Grid Element", modelForm);
        }
    }

    public void renderMultiFormClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        //FIXME copy from HtmlFormRenderer.java (except for the closing form tag itself, that is now converted)
        Iterator<ModelFormField> submitFields = modelForm.getMultiSubmitFields().iterator();
        while (submitFields.hasNext()) {
            ModelFormField submitField = submitFields.next();
            if (submitField != null && submitField.shouldUse(context)) {
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
        StringWriter sr = new StringWriter();
        sr.append("<@renderMultiFormClose />");
        executeMacro(writer, sr.toString());
        // see if there is anything that needs to be added outside of the multi-form
        Map<String, Object> wholeFormContext = UtilGenerics.checkMap(context.get("wholeFormContext"));
        Appendable postMultiFormWriter = wholeFormContext != null ? (Appendable) wholeFormContext.get("postMultiFormWriter") : null;
        if (postMultiFormWriter != null) {
            writer.append(postMultiFormWriter.toString());
        }
        if (modelForm instanceof ModelSingleForm) {
            renderEndingBoundaryComment(writer, "Form Widget - Form Element", modelForm);
        } else {
            renderEndingBoundaryComment(writer, "Grid Widget - Grid Element", modelForm);
        }
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
        if (modelForm instanceof ModelSingleForm) {
            renderBeginningBoundaryComment(writer, "Form Widget - Form Element", modelForm);
        } else {
            renderBeginningBoundaryComment(writer, "Grid Widget - Grid Element", modelForm);
        }
        if (this.renderPagination) {
            this.renderNextPrev(writer, context, modelForm);
        }
        List<ModelFormField> childFieldList = modelForm.getFieldList();
        List<String> columnStyleList = new LinkedList<String>();
        List<String> fieldNameList = new LinkedList<String>();
        for (ModelFormField childField : childFieldList) {
            int childFieldType = childField.getFieldInfo().getFieldType();
            if (childFieldType == FieldInfo.HIDDEN || childFieldType == FieldInfo.IGNORED) {
                continue;
            }
            String areaStyle = childField.getTitleAreaStyle();
            if (UtilValidate.isEmpty(areaStyle)) {
                areaStyle = "";
            }
            if (fieldNameList.contains(childField.getName())) {
                if (UtilValidate.isNotEmpty(areaStyle)) {
                    columnStyleList.set(fieldNameList.indexOf(childField.getName()), areaStyle);
                }
            } else {
                columnStyleList.add(areaStyle);
                fieldNameList.add(childField.getName());
            }
        }
        columnStyleList = StringUtil.quoteStrList(columnStyleList);
        String columnStyleListString = StringUtil.join(columnStyleList, ", ");
        StringWriter sr = new StringWriter();
        sr.append("<@renderFormatListWrapperOpen ");
        sr.append(" formName=\"");
        sr.append(modelForm.getName());
        sr.append("\" style=\"");
        sr.append(FlexibleStringExpander.expandString(modelForm.getDefaultTableStyle(), context));
        sr.append("\" columnStyles=[");
        if (UtilValidate.isNotEmpty(columnStyleListString)) {
            // this is a fix for forms with no fields
            sr.append(columnStyleListString);
        }
        sr.append("] />");
        executeMacro(writer, sr.toString());

    }

    public void renderFormatListWrapperClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        StringWriter sr = new StringWriter();
        sr.append("<@renderFormatListWrapperClose");
        sr.append(" formName=\"");
        sr.append(modelForm.getName());
        sr.append("\" />");
        executeMacro(writer, sr.toString());
        if (this.renderPagination) {
            this.renderNextPrev(writer, context, modelForm);
        }
        if (modelForm instanceof ModelSingleForm) {
            renderEndingBoundaryComment(writer, "Form Widget - Form Element", modelForm);
        } else {
            renderEndingBoundaryComment(writer, "Grid Widget - Grid Element", modelForm);
        }
    }
    
    public void renderFormatHeaderOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        StringWriter sr = new StringWriter();
        sr.append("<@renderFormatHeaderOpen ");
        sr.append(" />");
        executeMacro(writer, sr.toString());
    }
    
    public void renderFormatHeaderClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        StringWriter sr = new StringWriter();
        sr.append("<@renderFormatHeaderClose");
        sr.append(" />");
        executeMacro(writer, sr.toString());
    }

    public void renderFormatHeaderRowOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        String headerStyle = FlexibleStringExpander.expandString(modelForm.getHeaderRowStyle(), context);
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
        Integer itemIndex = (Integer) context.get("itemIndex");
        String altRowStyles = "";
        String evenRowStyle = "";
        String oddRowStyle = "";
        if (itemIndex != null) {
            altRowStyles = modelForm.getStyleAltRowStyle(context);
            if (itemIndex.intValue() % 2 == 0) {
                evenRowStyle = modelForm.getEvenRowStyle();
            } else {
                oddRowStyle = FlexibleStringExpander.expandString(modelForm.getOddRowStyle(), context);
            }
        }
        StringWriter sr = new StringWriter();
        sr.append("<@renderFormatItemRowOpen ");
        sr.append(" formName=\"");
        sr.append(modelForm.getName());
        sr.append("\" itemIndex=");
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
        sr.append("<@renderFormatItemRowClose ");
        sr.append(" formName=\"");
        sr.append(modelForm.getName());
        sr.append("\"/>");
        executeMacro(writer, sr.toString());
    }

    public void renderFormatItemRowCellOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField, int positionSpan) throws IOException {
        String areaStyle = modelFormField.getWidgetAreaStyle();
        StringWriter sr = new StringWriter();
        sr.append("<@renderFormatItemRowCellOpen ");
        sr.append(" fieldName=\"");
        sr.append(modelFormField.getName());
        sr.append("\" style=\"");
        sr.append(areaStyle);
        sr.append("\" positionSpan=");
        sr.append(Integer.toString(positionSpan));
        sr.append(" />");
        executeMacro(writer, sr.toString());
    }

    public void renderFormatItemRowCellClose(Appendable writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField) throws IOException {
        StringWriter sr = new StringWriter();
        sr.append("<@renderFormatItemRowCellClose");
        sr.append(" fieldName=\"");
        sr.append(modelFormField.getName());
        sr.append("\"/>");
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
        String style = FlexibleStringExpander.expandString(modelForm.getDefaultTableStyle(), context);
        StringWriter sr = new StringWriter();
        sr.append("<@renderFormatSingleWrapperOpen ");
        sr.append(" formName=\"");
        sr.append(modelForm.getName());
        sr.append("\" style=\"");
        sr.append(style);
        sr.append("\" />");
        executeMacro(writer, sr.toString());
    }

    public void renderFormatSingleWrapperClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        StringWriter sr = new StringWriter();
        sr.append("<@renderFormatSingleWrapperClose");
        sr.append(" formName=\"");
        sr.append(modelForm.getName());
        sr.append("\"/>");
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
        String defaultOption = textFindField.getDefaultOption(context);
        String className = "";
        String alert = "false";
        String opEquals = "";
        String opBeginsWith = "";
        String opContains = "";
        String opIsEmpty = "";
        String opNotEqual = "";
        String name = modelFormField.getParameterName(context);
        String size = Integer.toString(textFindField.getSize());
        String maxlength = "";
        String autocomplete = "";
        if (UtilValidate.isNotEmpty(modelFormField.getWidgetStyle())) {
            className = modelFormField.getWidgetStyle();
            if (modelFormField.shouldBeRed(context)) {
                alert = "true";
            }
        }
        Locale locale = (Locale) context.get("locale");
        if (!textFindField.getHideOptions()) {
            opEquals = UtilProperties.getMessage("conditional", "equals", locale);
            opBeginsWith = UtilProperties.getMessage("conditional", "begins_with", locale);
            opContains = UtilProperties.getMessage("conditional", "contains", locale);
            opIsEmpty = UtilProperties.getMessage("conditional", "is_empty", locale);
            opNotEqual = UtilProperties.getMessage("conditional", "not_equal", locale);
        }
        String value = modelFormField.getEntry(context, textFindField.getDefaultValue(context));
        if (value == null) {
            value = "";
        }
        if (textFindField.getMaxlength() != null) {
            maxlength = textFindField.getMaxlength().toString();
        }
        if (!textFindField.getClientAutocompleteField()) {
            autocomplete = "off";
        }
        String titleStyle = "";
        if (UtilValidate.isNotEmpty(modelFormField.getTitleStyle())) {
            titleStyle = modelFormField.getTitleStyle();
        }
        String ignoreCase = UtilProperties.getMessage("conditional", "ignore_case", locale);
        boolean ignCase = textFindField.getIgnoreCase(context);
        boolean hideIgnoreCase = textFindField.getHideIgnoreCase();
        String tabindex = modelFormField.getTabindex();
        StringWriter sr = new StringWriter();
        sr.append("<@renderTextFindField ");
        sr.append(" name=\"");
        sr.append(name);
        sr.append("\" value=\"");
        sr.append(value);
        sr.append("\" defaultOption=\"");
        sr.append(defaultOption);
        sr.append("\" opEquals=\"");
        sr.append(opEquals);
        sr.append("\" opBeginsWith=\"");
        sr.append(opBeginsWith);
        sr.append("\" opContains=\"");
        sr.append(opContains);
        sr.append("\" opIsEmpty=\"");
        sr.append(opIsEmpty);
        sr.append("\" opNotEqual=\"");
        sr.append(opNotEqual);
        sr.append("\" className=\"");
        sr.append(className);
        sr.append("\" alert=\"");
        sr.append(alert);
        sr.append("\" size=\"");
        sr.append(size);
        sr.append("\" maxlength=\"");
        sr.append(maxlength);
        sr.append("\" autocomplete=\"");
        sr.append(autocomplete);
        sr.append("\" titleStyle=\"");
        sr.append(titleStyle);
        sr.append("\" hideIgnoreCase=");
        sr.append(Boolean.toString(hideIgnoreCase));
        sr.append(" ignCase=");
        sr.append(Boolean.toString(ignCase));
        sr.append(" ignoreCase=\"");
        sr.append(ignoreCase);
        sr.append("\" tabindex=\"");
        sr.append(tabindex);
        sr.append("\" />");
        executeMacro(writer, sr.toString());
        this.appendTooltip(writer, context, modelFormField);
    }

    public void renderRangeFindField(Appendable writer, Map<String, Object> context, RangeFindField rangeFindField) throws IOException {
        ModelFormField modelFormField = rangeFindField.getModelFormField();
        Locale locale = (Locale) context.get("locale");
        String opEquals = UtilProperties.getMessage("conditional", "equals", locale);
        String opGreaterThan = UtilProperties.getMessage("conditional", "greater_than", locale);
        String opGreaterThanEquals = UtilProperties.getMessage("conditional", "greater_than_equals", locale);
        String opLessThan = UtilProperties.getMessage("conditional", "less_than", locale);
        String opLessThanEquals = UtilProperties.getMessage("conditional", "less_than_equals", locale);
        //String opIsEmpty = UtilProperties.getMessage("conditional", "is_empty", locale);
        String className = "";
        String alert = "false";
        if (UtilValidate.isNotEmpty(modelFormField.getWidgetStyle())) {
            className = modelFormField.getWidgetStyle();
            if (modelFormField.shouldBeRed(context)) {
                alert = "true";
            }
        }
        String name = modelFormField.getParameterName(context);
        String size = Integer.toString(rangeFindField.getSize());
        String value = modelFormField.getEntry(context, rangeFindField.getDefaultValue(context));
        if (value == null) {
            value = "";
        }
        Integer maxlength = rangeFindField.getMaxlength();
        String autocomplete = "";

        if (!rangeFindField.getClientAutocompleteField()) {
            autocomplete = "off";
        }
        String titleStyle = modelFormField.getTitleStyle();

        if (titleStyle == null) {
            titleStyle = "";
        }
        String defaultOptionFrom = rangeFindField.getDefaultOptionFrom();
        String value2 = modelFormField.getEntry(context);
        if (value2 == null) {
            value2 = "";
        }
        String defaultOptionThru = rangeFindField.getDefaultOptionThru();
        String tabindex = modelFormField.getTabindex();
        StringWriter sr = new StringWriter();
        sr.append("<@renderRangeFindField ");
        sr.append(" className=\"");
        sr.append(className);
        sr.append("\" alert=\"");
        sr.append(alert);
        sr.append("\" name=\"");
        sr.append(name);
        sr.append("\" value=\"");
        sr.append(value);
        sr.append("\" size=\"");
        sr.append(size);
        sr.append("\" maxlength=\"");
        if (maxlength != null) {
            sr.append(Integer.toString(maxlength));
        }
        sr.append("\" autocomplete=\"");
        sr.append(autocomplete);
        sr.append("\" titleStyle=\"");
        sr.append(titleStyle);
        sr.append("\" defaultOptionFrom=\"");
        sr.append(defaultOptionFrom);
        sr.append("\" opEquals=\"");
        sr.append(opEquals);
        sr.append("\" opGreaterThan=\"");
        sr.append(opGreaterThan);
        sr.append("\" opGreaterThanEquals=\"");
        sr.append(opGreaterThanEquals);
        sr.append("\" opLessThan=\"");
        sr.append(opLessThan);
        sr.append("\" opLessThanEquals=\"");
        sr.append(opLessThanEquals);
        sr.append("\" value2=\"");
        sr.append(value2);
        sr.append("\" defaultOptionThru=\"");
        sr.append(defaultOptionThru);
        sr.append("\" tabindex=\"");
        sr.append(tabindex);
        sr.append("\" />");
        executeMacro(writer, sr.toString());
        this.appendTooltip(writer, context, modelFormField);
    }

    public void renderDateFindField(Appendable writer, Map<String, Object> context, DateFindField dateFindField) throws IOException {
        ModelFormField modelFormField = dateFindField.getModelFormField();
        Locale locale = (Locale) context.get("locale");
        String opEquals = UtilProperties.getMessage("conditional", "equals", locale);
        String opGreaterThan = UtilProperties.getMessage("conditional", "greater_than", locale);
        String opSameDay = UtilProperties.getMessage("conditional", "same_day", locale);
        String opGreaterThanFromDayStart = UtilProperties.getMessage("conditional", "greater_than_from_day_start", locale);
        String opLessThan = UtilProperties.getMessage("conditional", "less_than", locale);
        String opUpToDay = UtilProperties.getMessage("conditional", "up_to_day", locale);
        String opUpThruDay = UtilProperties.getMessage("conditional", "up_thru_day", locale);
        String opIsEmpty = UtilProperties.getMessage("conditional", "is_empty", locale);
        Map<String, String> uiLabelMap = UtilGenerics.checkMap(context.get("uiLabelMap"));
        if (uiLabelMap == null) {
            Debug.logWarning("Could not find uiLabelMap in context", module);
        }
        String localizedInputTitle = "", localizedIconTitle = "";
        String className = "";
        String alert = "false";
        if (UtilValidate.isNotEmpty(modelFormField.getWidgetStyle())) {
            className = modelFormField.getWidgetStyle();
            if (modelFormField.shouldBeRed(context)) {
                alert = "true";
            }
        }
        String name = modelFormField.getParameterName(context);
        // the default values for a timestamp
        int size = 25;
        int maxlength = 30;
        String dateType = dateFindField.getType();
        if ("date".equals(dateType)) {
            size = maxlength = 10;
            if (uiLabelMap != null) {
                localizedInputTitle = uiLabelMap.get("CommonFormatDate");
            }
        } else if ("time".equals(dateFindField.getType())) {
            size = maxlength = 8;
            if (uiLabelMap != null) {
                localizedInputTitle = uiLabelMap.get("CommonFormatTime");
            }
        } else {
            if (uiLabelMap != null) {
                localizedInputTitle = uiLabelMap.get("CommonFormatDateTime");
            }
        }
        String value = modelFormField.getEntry(context, dateFindField.getDefaultValue(context));
        if (value == null) {
            value = "";
        }
        // search for a localized label for the icon
        if (uiLabelMap != null) {
            localizedIconTitle = uiLabelMap.get("CommonViewCalendar");
        }
        String formName = "";
        String defaultDateTimeString = "";
        StringBuilder imgSrc = new StringBuilder();
        // add calendar pop-up button and seed data IF this is not a "time" type date-find
        if (!"time".equals(dateFindField.getType())) {
            ModelForm modelForm = modelFormField.getModelForm();
            formName = FormRenderer.getCurrentFormName(modelForm, context);
            defaultDateTimeString = UtilHttp.encodeBlanks(modelFormField.getEntry(context, dateFindField.getDefaultDateTimeString(context)));
            this.appendContentUrl(imgSrc, "/images/cal.gif");
        }
        String defaultOptionFrom = dateFindField.getDefaultOptionFrom(context);
        String defaultOptionThru = dateFindField.getDefaultOptionThru(context);
        String value2 = modelFormField.getEntry(context);
        if (value2 == null) {
            value2 = "";
        }
        if (context.containsKey("parameters")) {
            Map<String, Object> parameters = UtilGenerics.checkMap(context.get("parameters"));
            if (parameters.containsKey(name + "_fld0_value")) {
                value = (String) parameters.get(name + "_fld0_value");
            }
            if (parameters.containsKey(name + "_fld1_value")) {
                value2 = (String) parameters.get(name + "_fld1_value");
            }
        }
        
        String titleStyle = "";
        if (UtilValidate.isNotEmpty(modelFormField.getTitleStyle())) {
            titleStyle = modelFormField.getTitleStyle();
        }
        String tabindex = modelFormField.getTabindex();
        StringWriter sr = new StringWriter();
        sr.append("<@renderDateFindField ");
        sr.append(" className=\"");
        sr.append(className);
        sr.append("\" alert=\"");
        sr.append(alert);
        sr.append("\" name=\"");
        sr.append(name);
        sr.append("\" localizedInputTitle=\"");
        sr.append(localizedInputTitle);
        sr.append("\" value=\"");
        sr.append(value);
        sr.append("\" value2=\"");
        sr.append(value2);
        sr.append("\" size=\"");
        sr.append(Integer.toString(size));
        sr.append("\" maxlength=\"");
        sr.append(Integer.toString(maxlength));
        sr.append("\" dateType=\"");
        sr.append(dateType);
        sr.append("\" formName=\"");
        sr.append(formName);
        sr.append("\" defaultDateTimeString=\"");
        sr.append(defaultDateTimeString);
        sr.append("\" imgSrc=\"");
        sr.append(imgSrc.toString());
        sr.append("\" localizedIconTitle=\"");
        sr.append(localizedIconTitle);
        sr.append("\" titleStyle=\"");
        sr.append(titleStyle);
        sr.append("\" defaultOptionFrom=\"");
        sr.append(defaultOptionFrom);
        sr.append("\" defaultOptionThru=\"");
        sr.append(defaultOptionThru);
        sr.append("\" opEquals=\"");
        sr.append(opEquals);
        sr.append("\" opSameDay=\"");
        sr.append(opSameDay);
        sr.append("\" opGreaterThanFromDayStart=\"");
        sr.append(opGreaterThanFromDayStart);
        sr.append("\" opGreaterThan=\"");
        sr.append(opGreaterThan);
        sr.append("\" opGreaterThan=\"");
        sr.append(opGreaterThan);
        sr.append("\" opLessThan=\"");
        sr.append(opLessThan);
        sr.append("\" opUpToDay=\"");
        sr.append(opUpToDay);
        sr.append("\" opUpThruDay=\"");
        sr.append(opUpThruDay);
        sr.append("\" opIsEmpty=\"");
        sr.append(opIsEmpty);
        sr.append("\" tabindex=\"");
        sr.append(tabindex);
        sr.append("\" />");
        executeMacro(writer, sr.toString());
        this.appendTooltip(writer, context, modelFormField);
    }

    public void renderLookupField(Appendable writer, Map<String, Object> context, LookupField lookupField) throws IOException {
        ModelFormField modelFormField = lookupField.getModelFormField();
        String lookupFieldFormName = lookupField.getFormName(context);
        String className = "";
        String alert = "false";
        if (UtilValidate.isNotEmpty(modelFormField.getWidgetStyle())) {
            className = modelFormField.getWidgetStyle();
            if (modelFormField.shouldBeRed(context)) {
                alert = "true";
            }
        }
        //check for required field style on single forms
        if ("single".equals(modelFormField.getModelForm().getType()) && modelFormField.getRequiredField()) {
            String requiredStyle = modelFormField.getRequiredFieldStyle();
            if (UtilValidate.isEmpty(requiredStyle))
                requiredStyle = "required";
            if (UtilValidate.isEmpty(className))
                className = requiredStyle;
            else
                className = requiredStyle + " " + className;
        }
        String name = modelFormField.getParameterName(context);
        String value = modelFormField.getEntry(context, lookupField.getDefaultValue(context));
        if (value == null) {
            value = "";
        }
        String size = Integer.toString(lookupField.getSize());
        Integer maxlength = lookupField.getMaxlength();
        String id = modelFormField.getCurrentContainerId(context);
        List<ModelForm.UpdateArea> updateAreas = modelFormField.getOnChangeUpdateAreas();
        //add default ajax auto completer to all lookup fields
        if (UtilValidate.isEmpty(updateAreas) && UtilValidate.isNotEmpty(lookupFieldFormName)) {
            String autoCompleterTarget = null;
            if (lookupFieldFormName.indexOf('?') == -1) {
                autoCompleterTarget = lookupFieldFormName + "?";
            } else {
                autoCompleterTarget = lookupFieldFormName + "&amp;amp;";
            }
            autoCompleterTarget = autoCompleterTarget + "ajaxLookup=Y";
            updateAreas = new LinkedList<ModelForm.UpdateArea>();
            updateAreas.add(new ModelForm.UpdateArea("change", id, autoCompleterTarget));
        }
        boolean ajaxEnabled = UtilValidate.isNotEmpty(updateAreas) && this.javaScriptEnabled;
        String autocomplete = "";
        if (!lookupField.getClientAutocompleteField() || !ajaxEnabled) {
            autocomplete = "off";
        }
        String event = modelFormField.getEvent();
        String action = modelFormField.getAction(context);
        boolean readonly = lookupField.getReadonly();
        // add lookup pop-up button
        String descriptionFieldName = lookupField.getDescriptionFieldName();
        ModelForm modelForm = modelFormField.getModelForm();
        String formName = modelFormField.getParentFormName();
        if (UtilValidate.isEmpty(formName)) {
            formName = FormRenderer.getCurrentFormName(modelForm, context);
        }
        StringBuilder targetParameterIter = new StringBuilder();
        StringBuilder imgSrc = new StringBuilder();
        // FIXME: refactor using the StringUtils methods
        List<String> targetParameterList = lookupField.getTargetParameterList();
        targetParameterIter.append("[");
        for (String targetParameter : targetParameterList) {
            if (targetParameterIter.length() > 1) {
                targetParameterIter.append(",");
            }
            targetParameterIter.append("'");
            targetParameterIter.append(targetParameter);
            targetParameterIter.append("'");
        }
        targetParameterIter.append("]");
        this.appendContentUrl(imgSrc, "/images/fieldlookup.gif");
        String ajaxUrl = "";
        if (ajaxEnabled) {
            ajaxUrl = createAjaxParamsFromUpdateAreas(updateAreas, "", context);
        }
        String lookupPresentation = lookupField.getLookupPresentation();
        if (UtilValidate.isEmpty(lookupPresentation)) {
            lookupPresentation = "";
        }
        String lookupHeight = lookupField.getLookupHeight();
        if (UtilValidate.isEmpty(lookupHeight)) {
            lookupHeight = "";
        }
        String lookupWidth = lookupField.getLookupWidth();
        if (UtilValidate.isEmpty(lookupWidth)) {
            lookupWidth = "";
        }
        String lookupPosition = lookupField.getLookupPosition();
        if (UtilValidate.isEmpty(lookupPosition)) {
            lookupPosition = "";
        }
        String fadeBackground = lookupField.getFadeBackground();
        if (UtilValidate.isEmpty(fadeBackground)) {
            fadeBackground = "false";
        }
        Boolean isInitiallyCollapsed = lookupField.getInitiallyCollapsed();
        String clearText = "";
        Map<String, Object> uiLabelMap = UtilGenerics.checkMap(context.get("uiLabelMap"));
        if (uiLabelMap != null) {
            clearText = (String) uiLabelMap.get("CommonClear");
        } else {
            Debug.logWarning("Could not find uiLabelMap in context", module);
        }
        Boolean showDescription = lookupField.getShowDescription();
        if (showDescription == null) {
            showDescription = "Y".equals(UtilProperties.getPropertyValue("widget", "widget.lookup.showDescription", "Y"));
        }
        // lastViewName, used by lookup to remember the real last view name
        String lastViewName = request.getParameter("_LAST_VIEW_NAME_"); // Try to get it from parameters firstly
        if (UtilValidate.isEmpty(lastViewName)) { // get from session
            lastViewName = (String) request.getSession().getAttribute("_LAST_VIEW_NAME_");
        }
        if (UtilValidate.isEmpty(lastViewName)) {
            lastViewName = "";
        }
        String tabindex = modelFormField.getTabindex();
        StringWriter sr = new StringWriter();
        sr.append("<@renderLookupField ");
        sr.append(" className=\"");
        sr.append(className);
        sr.append("\" alert=\"");
        sr.append(alert);
        sr.append("\" name=\"");
        sr.append(name);
        sr.append("\" value=\"");
        sr.append(value);
        sr.append("\" size=\"");
        sr.append(size);
        sr.append("\" maxlength=\"");
        sr.append((maxlength != null ? Integer.toString(maxlength) : ""));
        sr.append("\" id=\"");
        sr.append(id);
        sr.append("\" event=\"");
        if (event != null) {
            sr.append(event);
        }
        sr.append("\" action=\"");
        if (action != null) {
            sr.append(action);
        }
        sr.append("\" readonly=");
        sr.append(Boolean.toString(readonly));
        sr.append(" autocomplete=\"");
        sr.append(autocomplete);
        sr.append("\" descriptionFieldName=\"");
        sr.append(descriptionFieldName);
        sr.append("\" formName=\"");
        sr.append(formName);
        sr.append("\" fieldFormName=\"");
        sr.append(lookupFieldFormName);
        sr.append("\" targetParameterIter=");
        sr.append(targetParameterIter.toString());
        sr.append(" imgSrc=\"");
        sr.append(imgSrc.toString());
        sr.append("\" ajaxUrl=\"");
        sr.append(ajaxUrl);
        sr.append("\" ajaxEnabled=");
        sr.append(Boolean.toString(ajaxEnabled));
        sr.append(" presentation=\"");
        sr.append(lookupPresentation);
        sr.append("\" height=\"");
        sr.append(lookupHeight);
        sr.append("\" width=\"");
        sr.append(lookupWidth);
        sr.append("\" position=\"");
        sr.append(lookupPosition);
        sr.append("\" fadeBackground=\"");
        sr.append(fadeBackground);
        sr.append("\" clearText=\"");
        sr.append(clearText);
        sr.append("\" showDescription=\"");
        sr.append(Boolean.toString(showDescription));
        sr.append("\" initiallyCollapsed=\"");
        sr.append(Boolean.toString(isInitiallyCollapsed));
        sr.append("\" lastViewName=\"");
        sr.append(lastViewName);
        sr.append("\" tabindex=\"");
        sr.append(tabindex);
        sr.append("\" />");
        executeMacro(writer, sr.toString());
        this.addAsterisks(writer, context, modelFormField);
        this.makeHyperlinkString(writer, lookupField.getSubHyperlink(), context);
        this.appendTooltip(writer, context, modelFormField);
    }

    public void renderNextPrev(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
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
        // get the parameterized pagination index and size fields
        int paginatorNumber = WidgetWorker.getPaginatorNumber(context);
        String viewIndexParam = modelForm.getMultiPaginateIndexField(context);
        String viewSizeParam = modelForm.getMultiPaginateSizeField(context);
        int viewIndex = Paginator.getViewIndex(modelForm, context);
        int viewSize = Paginator.getViewSize(modelForm, context);
        int listSize = Paginator.getListSize(context);
        int lowIndex = Paginator.getLowIndex(context);
        int highIndex = Paginator.getHighIndex(context);
        int actualPageSize = Paginator.getActualPageSize(context);
        // needed for the "Page" and "rows" labels
        Map<String, String> uiLabelMap = UtilGenerics.checkMap(context.get("uiLabelMap"));
        String pageLabel = "";
        String commonDisplaying = "";
        if (uiLabelMap == null) {
            Debug.logWarning("Could not find uiLabelMap in context", module);
        } else {
            pageLabel = uiLabelMap.get("CommonPage");
            Map<String, Integer> messageMap = UtilMisc.toMap("lowCount", Integer.valueOf(lowIndex + 1), "highCount", Integer.valueOf(lowIndex + actualPageSize), "total", Integer.valueOf(listSize));
            commonDisplaying = UtilProperties.getMessage("CommonUiLabels", "CommonDisplaying", messageMap, (Locale) context.get("locale"));
        }
        // for legacy support, the viewSizeParam is VIEW_SIZE and viewIndexParam is VIEW_INDEX when the fields are "viewSize" and "viewIndex"
        if (viewIndexParam.equals("viewIndex" + "_" + paginatorNumber))
            viewIndexParam = "VIEW_INDEX" + "_" + paginatorNumber;
        if (viewSizeParam.equals("viewSize" + "_" + paginatorNumber))
            viewSizeParam = "VIEW_SIZE" + "_" + paginatorNumber;
        String str = (String) context.get("_QBESTRING_");
        // strip legacy viewIndex/viewSize params from the query string
        String queryString = UtilHttp.stripViewParamsFromQueryString(str, "" + paginatorNumber);
        // strip parameterized index/size params from the query string
        HashSet<String> paramNames = new HashSet<String>();
        paramNames.add(viewIndexParam);
        paramNames.add(viewSizeParam);
        queryString = UtilHttp.stripNamedParamsFromQueryString(queryString, paramNames);
        String anchor = "";
        String paginateAnchor = modelForm.getPaginateTargetAnchor();
        if (UtilValidate.isNotEmpty(paginateAnchor))
            anchor = "#" + paginateAnchor;
        // Create separate url path String and request parameters String,
        // add viewIndex/viewSize parameters to request parameter String
        String urlPath = UtilHttp.removeQueryStringFromTarget(targetService);
        String prepLinkText = UtilHttp.getQueryStringFromTarget(targetService);
        String prepLinkSizeText;
        if (UtilValidate.isNotEmpty(queryString)) {
            queryString = UtilHttp.encodeAmpersands(queryString);
        }
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
        prepLinkSizeText = prepLinkText + viewSizeParam + "='+this.value+'" + "&amp;" + viewIndexParam + "=0";
        prepLinkText += viewSizeParam + "=" + viewSize + "&amp;" + viewIndexParam + "=";
        if (ajaxEnabled) {
            // Prepare params for prototype.js
            prepLinkText = prepLinkText.replace("?", "");
            prepLinkText = prepLinkText.replace("&amp;", "&");
        }
        String linkText;
        String paginateStyle = modelForm.getPaginateStyle();
        String paginateFirstStyle = modelForm.getPaginateFirstStyle();
        String paginateFirstLabel = modelForm.getPaginateFirstLabel(context);
        String firstUrl = "";
        String ajaxFirstUrl = "";
        String paginatePreviousStyle = modelForm.getPaginatePreviousStyle();
        String paginatePreviousLabel = modelForm.getPaginatePreviousLabel(context);
        String previousUrl = "";
        String ajaxPreviousUrl = "";
        String selectUrl = "";
        String ajaxSelectUrl = "";
        String paginateViewSizeLabel = modelForm.getPaginateViewSizeLabel(context);
        String selectSizeUrl = "";
        String ajaxSelectSizeUrl = "";
        String paginateNextStyle = modelForm.getPaginateNextStyle();
        String paginateNextLabel = modelForm.getPaginateNextLabel(context);
        String nextUrl = "";
        String ajaxNextUrl = "";
        String paginateLastStyle = modelForm.getPaginateLastStyle();
        String paginateLastLabel = modelForm.getPaginateLastLabel(context);
        String lastUrl = "";
        String ajaxLastUrl = "";
        if (viewIndex > 0) {
            if (ajaxEnabled) {
                ajaxFirstUrl = createAjaxParamsFromUpdateAreas(updateAreas, prepLinkText + 0 + anchor, context);
            } else {
                linkText = prepLinkText + 0 + anchor;
                firstUrl = rh.makeLink(this.request, this.response, urlPath + linkText);
            }
        }
        if (viewIndex > 0) {
            if (ajaxEnabled) {
                ajaxPreviousUrl = createAjaxParamsFromUpdateAreas(updateAreas, prepLinkText + (viewIndex - 1) + anchor, context);
            } else {
                linkText = prepLinkText + (viewIndex - 1) + anchor;
                previousUrl = rh.makeLink(this.request, this.response, urlPath + linkText);
            }
        }
        // Page select dropdown
        if (listSize > 0 && this.javaScriptEnabled) {
            if (ajaxEnabled) {
                ajaxSelectUrl = createAjaxParamsFromUpdateAreas(updateAreas, prepLinkText + "' + this.value + '", context);
            } else {
                linkText = prepLinkText;
                if (linkText.startsWith("/")) {
                    linkText = linkText.substring(1);
                }
                selectUrl = rh.makeLink(this.request, this.response, urlPath + linkText);
            }
        }
        // Next button
        if (highIndex < listSize) {
            if (ajaxEnabled) {
                ajaxNextUrl = createAjaxParamsFromUpdateAreas(updateAreas, prepLinkText + (viewIndex + 1) + anchor, context);
            } else {
                linkText = prepLinkText + (viewIndex + 1) + anchor;
                nextUrl = rh.makeLink(this.request, this.response, urlPath + linkText);
            }
        }
        // Last button
        if (highIndex < listSize) {
            int lastIndex = UtilMisc.getViewLastIndex(listSize, viewSize);
            if (ajaxEnabled) {
                ajaxLastUrl = createAjaxParamsFromUpdateAreas(updateAreas, prepLinkText + lastIndex + anchor, context);
            } else {
                linkText = prepLinkText + lastIndex + anchor;
                lastUrl = rh.makeLink(this.request, this.response, urlPath + linkText);
            }
        }
        // Page size select dropdown
        if (listSize > 0 && this.javaScriptEnabled) {
            if (ajaxEnabled) {
                ajaxSelectSizeUrl = createAjaxParamsFromUpdateAreas(updateAreas, prepLinkSizeText + anchor, context);
            } else {
                linkText = prepLinkSizeText;
                if (linkText.startsWith("/")) {
                    linkText = linkText.substring(1);
                }
                selectSizeUrl = rh.makeLink(this.request, this.response, urlPath + linkText);
            }
        }
        StringWriter sr = new StringWriter();
        sr.append("<@renderNextPrev ");
        sr.append(" paginateStyle=\"");
        sr.append(paginateStyle);
        sr.append("\" paginateFirstStyle=\"");
        sr.append(paginateFirstStyle);
        sr.append("\" viewIndex=");
        sr.append(Integer.toString(viewIndex));
        sr.append(" highIndex=");
        sr.append(Integer.toString(highIndex));
        sr.append(" listSize=");
        sr.append(Integer.toString(listSize));
        sr.append(" viewSize=");
        sr.append(Integer.toString(viewSize));
        sr.append(" ajaxEnabled=");
        sr.append(Boolean.toString(ajaxEnabled));
        sr.append(" javaScriptEnabled=");
        sr.append(Boolean.toString(javaScriptEnabled));
        sr.append(" ajaxFirstUrl=\"");
        sr.append(ajaxFirstUrl);
        sr.append("\" ajaxFirstUrl=\"");
        sr.append(ajaxFirstUrl);
        sr.append("\" ajaxFirstUrl=\"");
        sr.append(ajaxFirstUrl);
        sr.append("\" firstUrl=\"");
        sr.append(firstUrl);
        sr.append("\" paginateFirstLabel=\"");
        sr.append(paginateFirstLabel);
        sr.append("\" paginatePreviousStyle=\"");
        sr.append(paginatePreviousStyle);
        sr.append("\" ajaxPreviousUrl=\"");
        sr.append(ajaxPreviousUrl);
        sr.append("\" previousUrl=\"");
        sr.append(previousUrl);
        sr.append("\" paginatePreviousLabel=\"");
        sr.append(paginatePreviousLabel);
        sr.append("\" pageLabel=\"");
        sr.append(pageLabel);
        sr.append("\" ajaxSelectUrl=\"");
        sr.append(ajaxSelectUrl);
        sr.append("\" selectUrl=\"");
        sr.append(selectUrl);
        sr.append("\" ajaxSelectSizeUrl=\"");
        sr.append(ajaxSelectSizeUrl);
        sr.append("\" selectSizeUrl=\"");
        sr.append(selectSizeUrl);
        sr.append("\" commonDisplaying=\"");
        sr.append(commonDisplaying);
        sr.append("\" paginateNextStyle=\"");
        sr.append(paginateNextStyle);
        sr.append("\" ajaxNextUrl=\"");
        sr.append(ajaxNextUrl);
        sr.append("\" nextUrl=\"");
        sr.append(nextUrl);
        sr.append("\" paginateNextLabel=\"");
        sr.append(paginateNextLabel);
        sr.append("\" paginateLastStyle=\"");
        sr.append(paginateLastStyle);
        sr.append("\" ajaxLastUrl=\"");
        sr.append(ajaxLastUrl);
        sr.append("\" lastUrl=\"");
        sr.append(lastUrl);
        sr.append("\" paginateLastLabel=\"");
        sr.append(paginateLastLabel);
        sr.append("\" paginateViewSizeLabel=\"");
        sr.append(paginateViewSizeLabel);
        sr.append("\" />");
        executeMacro(writer, sr.toString());
    }

    public void renderFileField(Appendable writer, Map<String, Object> context, FileField textField) throws IOException {
        ModelFormField modelFormField = textField.getModelFormField();
        String className = "";
        String alert = "false";
        String name = modelFormField.getParameterName(context);
        String value = modelFormField.getEntry(context, textField.getDefaultValue(context));
        String size = Integer.toString(textField.getSize());
        String maxlength = "";
        String autocomplete = "";
        if (UtilValidate.isNotEmpty(modelFormField.getWidgetStyle())) {
            className = modelFormField.getWidgetStyle();
            if (modelFormField.shouldBeRed(context)) {
                alert = "true";
            }
        }
        if (UtilValidate.isEmpty(value)) {
            value = "";
        }
        if (textField.getMaxlength() != null) {
            maxlength = textField.getMaxlength().toString();
        }
        if (!textField.getClientAutocompleteField()) {
            autocomplete = "off";
        }
        String tabindex = modelFormField.getTabindex();
        StringWriter sr = new StringWriter();
        sr.append("<@renderFileField ");
        sr.append(" className=\"");
        sr.append(className);
        sr.append("\" alert=\"");
        sr.append(alert);
        sr.append("\" name=\"");
        sr.append(name);
        sr.append("\" value=\"");
        sr.append(value);
        sr.append("\" size=\"");
        sr.append(size);
        sr.append("\" maxlength=\"");
        sr.append(maxlength);
        sr.append("\" autocomplete=\"");
        sr.append(autocomplete);
        sr.append("\" tabindex=\"");
        sr.append(tabindex);
        sr.append("\" />");
        executeMacro(writer, sr.toString());
        this.makeHyperlinkString(writer, textField.getSubHyperlink(), context);
        this.appendTooltip(writer, context, modelFormField);
    }

    public void renderPasswordField(Appendable writer, Map<String, Object> context, PasswordField passwordField) throws IOException {
        ModelFormField modelFormField = passwordField.getModelFormField();
        String className = "";
        String alert = "false";
        String name = modelFormField.getParameterName(context);
        String size = Integer.toString(passwordField.getSize());
        String maxlength = "";
        String id = modelFormField.getCurrentContainerId(context);
        String autocomplete = "";
        if (UtilValidate.isNotEmpty(modelFormField.getWidgetStyle())) {
            className = modelFormField.getWidgetStyle();
            if (modelFormField.shouldBeRed(context)) {
                alert = "true";
            }
        }
        String value = modelFormField.getEntry(context, passwordField.getDefaultValue(context));
        if (value == null) {
            value = "";
        }
        if (passwordField.getMaxlength() != null) {
            maxlength = passwordField.getMaxlength().toString();
        }
        if (id == null) {
            id = "";
        }
        if (!passwordField.getClientAutocompleteField()) {
            autocomplete = "off";
        }
        String tabindex = modelFormField.getTabindex();
        StringWriter sr = new StringWriter();
        sr.append("<@renderPasswordField ");
        sr.append(" className=\"");
        sr.append(className);
        sr.append("\" alert=\"");
        sr.append(alert);
        sr.append("\" name=\"");
        sr.append(name);
        sr.append("\" value=\"");
        sr.append(value);
        sr.append("\" size=\"");
        sr.append(size);
        sr.append("\" maxlength=\"");
        sr.append(maxlength);
        sr.append("\" id=\"");
        sr.append(id);
        sr.append("\" autocomplete=\"");
        sr.append(autocomplete);
        sr.append("\" tabindex=\"");
        sr.append(tabindex);
        sr.append("\" />");
        executeMacro(writer, sr.toString());
        this.addAsterisks(writer, context, modelFormField);
        this.makeHyperlinkString(writer, passwordField.getSubHyperlink(), context);
        this.appendTooltip(writer, context, modelFormField);
    }

    public void renderImageField(Appendable writer, Map<String, Object> context, ImageField imageField) throws IOException {
        ModelFormField modelFormField = imageField.getModelFormField();
        String value = modelFormField.getEntry(context, imageField.getValue(context));
        String description = imageField.getDescription(context);
        String alternate = imageField.getAlternate(context);
        String style = imageField.getStyle(context);
        if (UtilValidate.isEmpty(description)) {
            description = imageField.getModelFormField().getTitle(context);
        }
        if (UtilValidate.isEmpty(alternate)) {
            alternate = description;
        }
        if (UtilValidate.isNotEmpty(value)) {
            if (!value.startsWith("http")) {
                StringBuilder buffer = new StringBuilder();
                ContentUrlTag.appendContentPrefix(request, buffer);
                buffer.append(value);
                value = buffer.toString();
            }
        } else if (value == null) {
            value = "";
        }
        String event = modelFormField.getEvent();
        String action = modelFormField.getAction(context);
        StringWriter sr = new StringWriter();
        sr.append("<@renderImageField ");
        sr.append(" value=\"");
        sr.append(value);
        sr.append("\" description=\"");
        sr.append(encode(description, modelFormField, context));
        sr.append("\" alternate=\"");
        sr.append(encode(alternate, modelFormField, context));
        sr.append("\" style=\"");
        sr.append(style);
        sr.append("\" event=\"");
        sr.append(event == null ? "" : event);
        sr.append("\" action=\"");
        sr.append(action == null ? "" : action);
        sr.append("\" />");
        executeMacro(writer, sr.toString());
        this.makeHyperlinkString(writer, imageField.getSubHyperlink(), context);
        this.appendTooltip(writer, context, modelFormField);
    }

    public void renderFieldGroupOpen(Appendable writer, Map<String, Object> context, ModelForm.FieldGroup fieldGroup) throws IOException {
        String style = fieldGroup.getStyle();
        String id = fieldGroup.getId();
        FlexibleStringExpander titleNotExpanded = FlexibleStringExpander.getInstance(fieldGroup.getTitle());
        String title = titleNotExpanded.expandString(context);
        Boolean collapsed = fieldGroup.initiallyCollapsed();
        String collapsibleAreaId = fieldGroup.getId() + "_body";
        Boolean collapsible = fieldGroup.collapsible();
        String expandToolTip = "";
        String collapseToolTip = "";
        if (UtilValidate.isNotEmpty(style) || UtilValidate.isNotEmpty(id) || UtilValidate.isNotEmpty(title)) {
            if (fieldGroup.collapsible()) {
                Map<String, Object> uiLabelMap = UtilGenerics.checkMap(context.get("uiLabelMap"));
                //Map<String, Object> paramMap = UtilGenerics.checkMap(context.get("requestParameters"));
                if (uiLabelMap != null) {
                    expandToolTip = (String) uiLabelMap.get("CommonExpand");
                    collapseToolTip = (String) uiLabelMap.get("CommonCollapse");
                }
            }
        }
        StringWriter sr = new StringWriter();
        sr.append("<@renderFieldGroupOpen ");
        sr.append(" style=\"");
        if (style != null) {
            sr.append(style);
        }
        sr.append("\" id=\"");
        sr.append(id);
        sr.append("\" title=\"");
        sr.append(title);
        sr.append("\" collapsed=");
        sr.append(Boolean.toString(collapsed));
        sr.append(" collapsibleAreaId=\"");
        sr.append(collapsibleAreaId);
        sr.append("\" collapsible=");
        sr.append(Boolean.toString(collapsible));
        sr.append(" expandToolTip=\"");
        sr.append(expandToolTip);
        sr.append("\" collapseToolTip=\"");
        sr.append(collapseToolTip);
        sr.append("\" />");
        executeMacro(writer, sr.toString());
    }

    public void renderFieldGroupClose(Appendable writer, Map<String, Object> context, ModelForm.FieldGroup fieldGroup) throws IOException {
        String style = fieldGroup.getStyle();
        String id = fieldGroup.getId();
        FlexibleStringExpander titleNotExpanded = FlexibleStringExpander.getInstance(fieldGroup.getTitle());
        String title = titleNotExpanded.expandString(context);
        StringWriter sr = new StringWriter();
        sr.append("<@renderFieldGroupClose ");
        sr.append(" style=\"");
        if (style != null) {
            sr.append(style);
        }
        sr.append("\" id=\"");
        if (id != null) {
            sr.append(id);
        }
        sr.append("\" title=\"");
        if (title != null) {
            sr.append(title);
        }
        sr.append("\" />");
        executeMacro(writer, sr.toString());
    }

    public void renderBanner(Appendable writer, Map<String, Object> context, ModelForm.Banner banner) throws IOException {
        String style = banner.getStyle(context);
        String leftStyle = banner.getLeftTextStyle(context);
        if (UtilValidate.isEmpty(leftStyle))
            leftStyle = style;
        String rightStyle = banner.getRightTextStyle(context);
        if (UtilValidate.isEmpty(rightStyle))
            rightStyle = style;
        String leftText = banner.getLeftText(context);
        if (leftText == null) {
            leftText = "";
        }
        String text = banner.getText(context);
        if (text == null) {
            text = "";
        }
        String rightText = banner.getRightText(context);
        if (rightText == null) {
            rightText = "";
        }
        StringWriter sr = new StringWriter();
        sr.append("<@renderBanner ");
        sr.append(" style=\"");
        sr.append(style);
        sr.append("\" leftStyle=\"");
        sr.append(leftStyle);
        sr.append("\" rightStyle=\"");
        sr.append(rightStyle);
        sr.append("\" leftText=\"");
        sr.append(leftText);
        sr.append("\" text=\"");
        sr.append(text);
        sr.append("\" rightText=\"");
        sr.append(rightText);
        sr.append("\" />");
        executeMacro(writer, sr.toString());
    }

    /**
     * Renders the beginning boundary comment string.
     * @param writer The writer to write to
     * @param widgetType The widget type: "Screen Widget", "Form Widget", etc.
     * @param modelWidget The widget
     */
    public void renderBeginningBoundaryComment(Appendable writer, String widgetType, ModelWidget modelWidget) throws IOException {
        if (this.widgetCommentsEnabled) {
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
        if (this.widgetCommentsEnabled) {
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

    public void renderSortField(Appendable writer, Map<String, Object> context, ModelFormField modelFormField, String titleText) throws IOException {
        boolean ajaxEnabled = false;
        ModelForm modelForm = modelFormField.getModelForm();
        List<ModelForm.UpdateArea> updateAreas = modelForm.getOnSortColumnUpdateAreas();
        if (updateAreas == null) {
            // For backward compatibility.
            updateAreas = modelForm.getOnPaginateUpdateAreas();
        }
        if (this.javaScriptEnabled) {
            if (UtilValidate.isNotEmpty(updateAreas)) {
                ajaxEnabled = true;
            }
        }
        String paginateTarget = modelForm.getPaginateTarget(context);
        if (paginateTarget.isEmpty() && updateAreas == null) {
            Debug.logWarning("Cannot sort because the paginate target URL is empty for the form: " + modelForm.getName(), module);
            return;
        }
        String oldSortField = modelForm.getSortField(context);
        String sortFieldStyle = modelFormField.getSortFieldStyle();
        // if the entry-name is defined use this instead of field name
        String columnField = modelFormField.getEntryName();
        if (UtilValidate.isEmpty(columnField)) {
            columnField = modelFormField.getFieldName();
        }
        // switch between asc/desc order
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
        String queryString = UtilHttp.getQueryStringFromTarget(paginateTarget).replace("?", "");
        Map<String, Object> paramMap = UtilHttp.getQueryStringOnlyParameterMap(queryString);
        String qbeString = (String) context.get("_QBESTRING_");
        if (qbeString != null) {
            qbeString = qbeString.replaceAll("&amp;", "&");
            paramMap.putAll(UtilHttp.getQueryStringOnlyParameterMap(qbeString));
        }
        paramMap.put(modelForm.getSortFieldParameterName(), newSortField);
        UtilHttp.canonicalizeParameterMap(paramMap);
        String linkUrl = null;
        if (ajaxEnabled) {
            linkUrl = createAjaxParamsFromUpdateAreas(updateAreas, paramMap, null, context);
        } else {
            StringBuilder sb = new StringBuilder("?");
            Iterator<Map.Entry<String, Object>> iter = paramMap.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, Object> entry = iter.next();
                if (entry.getKey().contains("externalLoginKey")) continue; 
                sb.append(entry.getKey()).append("=").append(entry.getValue());
                if (iter.hasNext()) {
                    sb.append("&amp;");
                }
            }
            String newQueryString = sb.toString();
            String urlPath = UtilHttp.removeQueryStringFromTarget(paginateTarget);
            linkUrl = rh.makeLink(this.request, this.response, urlPath.concat(newQueryString));
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
        String tooltip = modelFormField.getSortFieldHelpText(context);
        if (!tooltip.isEmpty()) {
            sr.append(" tooltip=\"").append(tooltip).append("\"");
        }
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
    private String createAjaxParamsFromUpdateAreas(List<ModelForm.UpdateArea> updateAreas, Map<String, Object> extraParams, String anchor, Map<String, ? extends Object> context) {
        StringBuilder sb = new StringBuilder();
        Iterator<ModelForm.UpdateArea> updateAreaIter = updateAreas.iterator();
        while (updateAreaIter.hasNext()) {
            ModelForm.UpdateArea updateArea = updateAreaIter.next();
            sb.append(updateArea.getAreaId()).append(",");
            String ajaxTarget = updateArea.getAreaTarget(context);
            String urlPath = UtilHttp.removeQueryStringFromTarget(ajaxTarget);
            sb.append(this.rh.makeLink(this.request, this.response,urlPath)).append(",");
            String queryString = UtilHttp.getQueryStringFromTarget(ajaxTarget).replace("?", "");
            Map<String, Object> parameters = UtilHttp.getQueryStringOnlyParameterMap(queryString);
            Map<String, Object> ctx = UtilGenerics.checkMap(context);
            Map<String, Object> updateParams = UtilGenerics.checkMap(updateArea.getParameterMap(ctx));
            parameters.putAll(updateParams);
            UtilHttp.canonicalizeParameterMap(parameters);
            parameters.putAll(extraParams);
            Iterator<Map.Entry<String, Object>> paramIter = parameters.entrySet().iterator();
            while (paramIter.hasNext()) {
                Map.Entry<String, Object> entry = paramIter.next();
                sb.append(entry.getKey()).append("=").append(entry.getValue());
                if (paramIter.hasNext()) {
                    sb.append("&");
                }
            }
            if (anchor != null) {
                sb.append("#").append(anchor);
            }
            if (updateAreaIter.hasNext()) {
                sb.append(",");
            }
        }
        Locale locale = UtilMisc.ensureLocale(context.get("locale"));
        return FlexibleStringExpander.expandString(sb.toString(), context, locale);
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
            Map<String, Object> ctx = UtilGenerics.checkMap(context);
            Map<String, String> parameters = updateArea.getParameterMap(ctx);
            String targetUrl = updateArea.getAreaTarget(context);
            String ajaxParams = getAjaxParamsFromTarget(targetUrl);
            //add first parameters from updateArea parameters
            if (UtilValidate.isNotEmpty(parameters)) {
                if (UtilValidate.isEmpty(ajaxParams)) {
                    ajaxParams = "";
                }
                for (Map.Entry<String, String> entry : parameters.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    //test if ajax parameters are not already into extraParams, if so do not add it
                    if (UtilValidate.isNotEmpty(extraParams) && extraParams.contains(value)) {
                        continue;
                    }
                    if (ajaxParams.length() > 0 && ajaxParams.indexOf(key) < 0) {
                        ajaxParams += "&";
                    }
                    if (ajaxParams.indexOf(key) < 0) {
                        ajaxParams += key + "=" + value;
                    }
                }
            }
            //then add parameters from request. Those parameters could end with an anchor so we must set ajax parameters first
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
        Locale locale = UtilMisc.ensureLocale(context.get("locale"));
        return FlexibleStringExpander.expandString(ajaxUrl, context, locale);
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
        sr.append(FreeMarkerWorker.encodeDoubleQuotes(tooltip));
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
            if (UtilValidate.isNotEmpty(subHyperlink.getWidth())) this.request.setAttribute("width", subHyperlink.getWidth());
            if (UtilValidate.isNotEmpty(subHyperlink.getHeight())) this.request.setAttribute("height", subHyperlink.getHeight());
            writer.append(' ');
            makeHyperlinkByType(writer, subHyperlink.getLinkType(), subHyperlink.getStyle(context), subHyperlink.getUrlMode(),
                    subHyperlink.getTarget(context), subHyperlink.getParameterMap(context, subHyperlink.getModelFormField().getEntityName(), subHyperlink.getModelFormField().getServiceName()), subHyperlink.getDescription(context),
                    subHyperlink.getTargetWindow(context), "", subHyperlink.getModelFormField(), this.request, this.response,
                    context);
        }
    }

    public void addAsterisks(Appendable writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
        String requiredField = "false";
        String requiredStyle = "";
        if (modelFormField.getRequiredField()) {
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
        StringBuilder buffer = new StringBuilder();
        ContentUrlTag.appendContentPrefix(this.request, buffer);
        writer.append(buffer.toString());
        writer.append(location);
    }

    public void makeHyperlinkByType(Appendable writer, String linkType, String linkStyle, String targetType, String target, Map<String, String> parameterMap, String description, String targetWindow, String confirmation, ModelFormField modelFormField, HttpServletRequest request,
            HttpServletResponse response, Map<String, Object> context) throws IOException {
        String realLinkType = WidgetWorker.determineAutoLinkType(linkType, target, targetType, request);
        String encodedDescription = encode(description, modelFormField, context);
        // get the parameterized pagination index and size fields
        int paginatorNumber = WidgetWorker.getPaginatorNumber(context);
        ModelForm modelForm = modelFormField.getModelForm();
        String viewIndexField = modelForm.getMultiPaginateIndexField(context);
        String viewSizeField = modelForm.getMultiPaginateSizeField(context);
        int viewIndex = Paginator.getViewIndex(modelForm, context);
        int viewSize = Paginator.getViewSize(modelForm, context);
        if (viewIndexField.equals("viewIndex" + "_" + paginatorNumber)) {
            viewIndexField = "VIEW_INDEX" + "_" + paginatorNumber;
        }
        if (viewSizeField.equals("viewSize" + "_" + paginatorNumber)) {
            viewSizeField = "VIEW_SIZE" + "_" + paginatorNumber;
        }
        if ("hidden-form".equals(realLinkType)) {
            parameterMap.put(viewIndexField, Integer.toString(viewIndex));
            parameterMap.put(viewSizeField, Integer.toString(viewSize));
            if (modelFormField != null && "multi".equals(modelForm.getType())) {
                WidgetWorker.makeHiddenFormLinkAnchor(writer, linkStyle, encodedDescription, confirmation, modelFormField, request, response, context);
                // this is a bit trickier, since we can't do a nested form we'll have to put the link to submit the form in place, but put the actual form def elsewhere, ie after the big form is closed
                Map<String, Object> wholeFormContext = UtilGenerics.checkMap(context.get("wholeFormContext"));
                Appendable postMultiFormWriter = wholeFormContext != null ? (Appendable) wholeFormContext.get("postMultiFormWriter") : null;
                if (postMultiFormWriter == null) {
                    postMultiFormWriter = new StringWriter();
                    wholeFormContext.put("postMultiFormWriter", postMultiFormWriter);
                }
                WidgetWorker.makeHiddenFormLinkForm(postMultiFormWriter, target, targetType, targetWindow, parameterMap, modelFormField, request, response, context);
            } else {
                WidgetWorker.makeHiddenFormLinkForm(writer, target, targetType, targetWindow, parameterMap, modelFormField, request, response, context);
                WidgetWorker.makeHiddenFormLinkAnchor(writer, linkStyle, encodedDescription, confirmation, modelFormField, request, response, context);
            }
        } else {
            if ("layered-modal".equals(realLinkType)) {
                String uniqueItemName = "Modal_".concat(UUID.randomUUID().toString().replace("-", "_"));
                String width = (String) this.request.getAttribute("width");
                if (UtilValidate.isEmpty(width)) {
                    width = String.valueOf(UtilProperties.getPropertyValue("widget", "widget.link.default.layered-modal.width", "800"));
                    this.request.setAttribute("width", width);
                }
                String height = (String) this.request.getAttribute("height");
                if (UtilValidate.isEmpty(height)) {
                    height = String.valueOf(UtilProperties.getPropertyValue("widget", "widget.link.default.layered-modal.height", "600"));
                    this.request.setAttribute("height", height);
                }
                this.request.setAttribute("uniqueItemName", uniqueItemName);
                makeHyperlinkString(writer, linkStyle, targetType, target, parameterMap, encodedDescription, confirmation, modelFormField, request, response, context, targetWindow);
                this.request.removeAttribute("uniqueItemName");
                this.request.removeAttribute("height");
                this.request.removeAttribute("width");
            } else {
                makeHyperlinkString(writer, linkStyle, targetType, target, parameterMap, encodedDescription, confirmation, modelFormField, request, response, context, targetWindow);
            }
        }
    }

    public void makeHyperlinkString(Appendable writer, String linkStyle, String targetType, String target, Map<String, String> parameterMap, String description, String confirmation, ModelFormField modelFormField, HttpServletRequest request, HttpServletResponse response, Map<String, Object> context,
            String targetWindow) throws IOException {
        if (description != null || UtilValidate.isNotEmpty(request.getAttribute("image"))) {
            StringBuilder linkUrl = new StringBuilder();
            WidgetWorker.buildHyperlinkUrl(linkUrl, target, targetType, UtilValidate.isEmpty(request.getAttribute("uniqueItemName"))?parameterMap:null, null, false, false, true, request, response, context);
            String event = "";
            String action = "";
            String imgSrc = "";
            String alt = "";
            String id = "";
            String uniqueItemName = "";
            String width = "";
            String height = "";
            String imgTitle = "";
            String hiddenFormName = WidgetWorker.makeLinkHiddenFormName(context, modelFormField);
            if (UtilValidate.isNotEmpty(modelFormField.getEvent()) && UtilValidate.isNotEmpty(modelFormField.getAction(context))) {
                event = modelFormField.getEvent();
                action = modelFormField.getAction(context);
            }
            if (UtilValidate.isNotEmpty(request.getAttribute("image"))) {
                imgSrc = request.getAttribute("image").toString();
            }
            if (UtilValidate.isNotEmpty(request.getAttribute("alternate"))) {
                alt = request.getAttribute("alternate").toString();
            }
            if (UtilValidate.isNotEmpty(request.getAttribute("imageTitle"))) {
                imgTitle = request.getAttribute("imageTitle").toString();
            }
            Integer size = Integer.valueOf("0");
            if (UtilValidate.isNotEmpty(request.getAttribute("descriptionSize"))) {
                size = Integer.valueOf(request.getAttribute("descriptionSize").toString());
            }
            if (UtilValidate.isNotEmpty(description) && size > 0 && description.length() > size) {
                imgTitle = description;
                description = description.substring(0, size - 8) + "..." + description.substring(description.length() - 5);
            }
            if (UtilValidate.isEmpty(imgTitle)) {
                imgTitle = modelFormField.getTitle(context);
            }
            if (UtilValidate.isNotEmpty(request.getAttribute("id"))) {
                id = request.getAttribute("id").toString();
            }
            if (UtilValidate.isNotEmpty(request.getAttribute("uniqueItemName"))) {
                uniqueItemName = request.getAttribute("uniqueItemName").toString();
                width = request.getAttribute("width").toString();
                height = request.getAttribute("height").toString();
            }
            StringBuilder targetParameters = new StringBuilder();
            if (UtilValidate.isNotEmpty(parameterMap) ) {
                targetParameters.append("{");
                for (Map.Entry<String, String> parameter : parameterMap.entrySet()) {
                    if (targetParameters.length() > 1) {
                        targetParameters.append(",");
                    }
                    targetParameters.append("'");
                    targetParameters.append(parameter.getKey());
                    targetParameters.append("':'");
                    targetParameters.append(parameter.getValue());
                    targetParameters.append("'");
                }
                targetParameters.append("}");
            }
            StringWriter sr = new StringWriter();
            sr.append("<@makeHyperlinkString ");
            sr.append("linkStyle=\"");
            sr.append(linkStyle == null ? "" : linkStyle);
            sr.append("\" hiddenFormName=\"");
            sr.append(hiddenFormName == null ? "" : hiddenFormName);
            sr.append("\" event=\"");
            sr.append(event);
            sr.append("\" action=\"");
            sr.append(action);
            sr.append("\" imgSrc=\"");
            sr.append(imgSrc);
            sr.append("\" title=\"");
            sr.append(imgTitle);
            sr.append("\" alternate=\"");
            sr.append(alt);
            sr.append("\" targetParameters=\"");
            sr.append(targetParameters.toString());
            sr.append("\" linkUrl=\"");
            sr.append(linkUrl.toString());
            sr.append("\" targetWindow=\"");
            sr.append(targetWindow);
            sr.append("\" description=\"");
            sr.append(description);
            sr.append("\" confirmation =\"");
            sr.append(confirmation);
            sr.append("\" uniqueItemName=\"");
            sr.append(uniqueItemName);
            sr.append("\" height=\"");
            sr.append(height);
            sr.append("\" width=\"");
            sr.append(width);
            sr.append("\" id=\"");
            sr.append(id);
            sr.append("\" />");
            executeMacro(writer, sr.toString());
        }
    }

    public void makeHiddenFormLinkAnchor(Appendable writer, String linkStyle, String description, String confirmation, ModelFormField modelFormField, HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) throws IOException {
        if (UtilValidate.isNotEmpty(description) || UtilValidate.isNotEmpty(request.getAttribute("image"))) {
            String hiddenFormName = WidgetWorker.makeLinkHiddenFormName(context, modelFormField);
            String event = "";
            String action = "";
            String imgSrc = "";
            if (UtilValidate.isNotEmpty(modelFormField.getEvent()) && UtilValidate.isNotEmpty(modelFormField.getAction(context))) {
                event = modelFormField.getEvent();
                action = modelFormField.getAction(context);
            }
            if (UtilValidate.isNotEmpty(request.getAttribute("image"))) {
                imgSrc = request.getAttribute("image").toString();
            }
            StringWriter sr = new StringWriter();
            sr.append("<@makeHiddenFormLinkAnchor ");
            sr.append("linkStyle=\"");
            sr.append(linkStyle == null ? "" : linkStyle);
            sr.append("\" hiddenFormName=\"");
            sr.append(hiddenFormName == null ? "" : hiddenFormName);
            sr.append("\" event=\"");
            sr.append(event);
            sr.append("\" action=\"");
            sr.append(action);
            sr.append("\" imgSrc=\"");
            sr.append(imgSrc);
            sr.append("\" description=\"");
            sr.append(description);
            sr.append("\" confirmation =\"");
            sr.append(confirmation);
            sr.append("\" />");
            executeMacro(writer, sr.toString());
        }
    }

    public void makeHiddenFormLinkForm(Appendable writer, String target, String targetType, String targetWindow, List<CommonWidgetModels.Parameter> parameterList, ModelFormField modelFormField, HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) throws IOException {
        StringBuilder actionUrl = new StringBuilder();
        WidgetWorker.buildHyperlinkUrl(actionUrl, target, targetType, null, null, false, false, true, request, response, context);
        String name = WidgetWorker.makeLinkHiddenFormName(context, modelFormField);
        StringBuilder parameters = new StringBuilder();
        parameters.append("[");
        for (CommonWidgetModels.Parameter parameter : parameterList) {
            if (parameters.length() > 1) {
                parameters.append(",");
            }
            parameters.append("{'name':'");
            parameters.append(parameter.getName());
            parameters.append("'");
            parameters.append(",'value':'");
            parameters.append(UtilCodec.getEncoder("html").encode(parameter.getValue(context)));
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

    public void renderContainerFindField(Appendable writer, Map<String, Object> context, ContainerField containerField) throws IOException {
        String id = containerField.getModelFormField().getIdName();
        String className = UtilFormatOut.checkNull(containerField.getModelFormField().getWidgetStyle());
        StringWriter sr = new StringWriter();
        sr.append("<@renderContainerField ");
        sr.append("id=\"");
        sr.append(id);
        sr.append("\" className=\"");
        sr.append(className);
        sr.append("\" />");
        executeMacro(writer, sr.toString());
    }
}
