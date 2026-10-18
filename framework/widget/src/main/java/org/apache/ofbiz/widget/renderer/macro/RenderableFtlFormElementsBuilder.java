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
package org.apache.ofbiz.widget.renderer.macro;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.ibm.icu.util.Calendar;
import org.apache.ofbiz.base.lang.JSON;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilCodec;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilRandom;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.security.CsrfUtil;
import org.apache.ofbiz.webapp.control.RequestHandler;
import org.apache.ofbiz.widget.WidgetWorker;
import org.apache.ofbiz.widget.content.StaticContentUrlProvider;
import org.apache.ofbiz.widget.model.CommonWidgetModels;
import org.apache.ofbiz.widget.model.FieldInfo;
import org.apache.ofbiz.widget.model.ModelForm;
import org.apache.ofbiz.widget.model.ModelFormField;
import org.apache.ofbiz.widget.model.ModelFormField.ContainerField;
import org.apache.ofbiz.widget.model.ModelFormField.DisplayField;
import org.apache.ofbiz.widget.model.ModelScreenWidget.Label;
import org.apache.ofbiz.widget.model.ModelTheme;
import org.apache.ofbiz.widget.renderer.FormRenderer;
import org.apache.ofbiz.widget.renderer.Paginator;
import org.apache.ofbiz.widget.renderer.VisualTheme;
import org.apache.ofbiz.widget.renderer.macro.model.GroupOption;
import org.apache.ofbiz.widget.renderer.macro.model.Option;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtl;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtlMacroCall;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtlMacroCall.RenderableFtlMacroCallBuilder;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtlNoop;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtlSequence;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtlVerbatim;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtlVerbatim.RenderableFtlVerbatimBuilder;
import org.jsoup.nodes.Element;

/**
 * Creates RenderableFtl objects used to render the various elements of a form.
 */
public final class RenderableFtlFormElementsBuilder {
    private static final String MODULE = RenderableFtlFormElementsBuilder.class.getName();
    private final VisualTheme visualTheme;
    private final RequestHandler requestHandler;
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    private final StaticContentUrlProvider staticContentUrlProvider;

    public RenderableFtlFormElementsBuilder(final VisualTheme visualTheme, final RequestHandler requestHandler,
                                            final HttpServletRequest request, final HttpServletResponse response,
                                            final StaticContentUrlProvider staticContentUrlProvider) {
        this.visualTheme = visualTheme;
        this.requestHandler = requestHandler;
        this.request = request;
        this.response = response;
        this.staticContentUrlProvider = staticContentUrlProvider;
    }

    public RenderableFtl tooltip(final Map<String, Object> context, final ModelFormField modelFormField) {
        final String tooltip = modelFormField.getTooltip(context);
        return RenderableFtlMacroCall.builder()
                .name("renderTooltip")
                .stringParameter("tooltip", tooltip)
                .stringParameter("tooltipStyle", modelFormField.getTooltipStyle())
                .build();
    }

    public RenderableFtl asterisks(final Map<String, Object> context, final ModelFormField modelFormField) {
        String requiredField = "false";
        if (modelFormField.getRequiredField()) {
            requiredField = "true";
        }

        return RenderableFtlMacroCall.builder()
                .name("renderAsterisks")
                .stringParameter("requiredField", requiredField)
                .build();
    }

    public RenderableFtl label(final Map<String, Object> context, final Label label) {
        final String labelText = label.getText(context);

        if (UtilValidate.isEmpty(labelText)) {
            // nothing to render
            return RenderableFtlNoop.INSTANCE;
        }
        return RenderableFtlMacroCall.builder()
                .name("renderLabel")
                .stringParameter("text", labelText)
                .build();
    }

    public RenderableFtl displayField(final Map<String, Object> context, final DisplayField displayField,
                                      final boolean javaScriptEnabled) {
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
                Debug.logError(nfe, "Error reading size of a field fieldName="
                        + displayField.getModelFormField().getFieldName() + " FormName= "
                        + displayField.getModelFormField().getModelForm().getName(), MODULE);
            }
        }
        ModelFormField.InPlaceEditor inPlaceEditor = displayField.getInPlaceEditor();
        boolean ajaxEnabled = inPlaceEditor != null && javaScriptEnabled;
        if (UtilValidate.isNotEmpty(description) && size > 0 && description.length() > size) {
            title = description;
            description = StringUtil.truncateEncodedStringToLength(description, size);
        }

        final RenderableFtlMacroCallBuilder builder = RenderableFtlMacroCall.builder()
                .name("renderDisplayField")
                .stringParameter("type", type)
                .stringParameter("imageLocation", encode(imageLocation, modelFormField, context))
                .stringParameter("idName", idName)
                .stringParameter("description", description)
                .stringParameter("title", title)
                .stringParameter("class", modelFormField.getWidgetStyle())
                .stringParameter("alert", modelFormField.shouldBeRed(context) ? "true" : "false");

        if (ajaxEnabled) {
            String url = inPlaceEditor.getUrl(context);
            StringBuffer extraParameterBuffer = new StringBuffer();
            String extraParameter;

            Map<String, Object> fieldMap = inPlaceEditor.getFieldMap(context);
            Set<Map.Entry<String, Object>> fieldSet = fieldMap.entrySet();
            Iterator<Map.Entry<String, Object>> fieldIterator = fieldSet.iterator();
            int count = 0;
            extraParameterBuffer.append("{");
            while (fieldIterator.hasNext()) {
                count++;
                Map.Entry<String, Object> field = fieldIterator.next();
                extraParameterBuffer.append(field.getKey() + ":'" + (String) field.getValue() + "'");
                if (count < fieldSet.size()) {
                    extraParameterBuffer.append(',');
                }
            }

            extraParameterBuffer.append("}");
            extraParameter = extraParameterBuffer.toString();
            builder.stringParameter("inPlaceEditorUrl", url);

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
            inPlaceEditorParams.append(", onreset: function(){jQuery('#cc_" + idName
                    + "').css('background-color', 'transparent');}");
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
            builder.stringParameter("inPlaceEditorParams", inPlaceEditorParams.toString());
        }

        return builder.build();
    }

    public RenderableFtl textField(final Map<String, Object> context, final ModelFormField.TextField textField,
                                   final boolean javaScriptEnabled) {
        ModelFormField modelFormField = textField.getModelFormField();
        String name = modelFormField.getParameterName(context);
        String type = textField.getType();
        if (UtilValidate.isEmpty(type)) {
            type = "text";
        }
        String pattern = "";
        if (List.of("text", "email", "url", "tel").contains(type)) {
            pattern = textField.getPattern();
        }
        String step = "";
        String min = "";
        String max = "";
        if (List.of("number", "range").contains(type)) {
            min = textField.getMin();
            max = textField.getMax();
            step = textField.getStep();
            if (UtilValidate.isEmpty(step)) {
                step = "any";
            }
        }
        List<String> classes = new ArrayList<>();
        String alert = "false";
        String mask = "";
        String placeholder = textField.getPlaceholder(context);
        if (UtilValidate.isNotEmpty(modelFormField.getWidgetStyle())) {
            classes.add(modelFormField.getWidgetStyle());
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
            // kept for backward compatibility with existing CSS/JS
            // maybe unused if jQuery Validation is no longer used
            // for styling we should rely on "required" attribute
            classes.add("required");
            String requiredStyle = modelFormField.getRequiredFieldStyle();
            if (UtilValidate.isNotEmpty(requiredStyle)) {
                classes.add(requiredStyle);
            }
        }
        List<ModelForm.UpdateArea> updateAreas = modelFormField.getOnChangeUpdateAreas();
        boolean ajaxEnabled = updateAreas != null && javaScriptEnabled;
        if (textField.getClientAutocompleteField() || ajaxEnabled) {
            clientAutocomplete = "true";
        }
        if (UtilValidate.isNotEmpty(textField.getMask())) {
            mask = textField.getMask();
        }
        String ajaxUrl = createAjaxParamsFromUpdateAreas(updateAreas, "", context);
        boolean disabled = modelFormField.getDisabled(context);
        boolean readonly = textField.getReadonly();
        String tabindex = modelFormField.getTabindex();

        return RenderableFtlMacroCall.builder()
                .name("renderTextField")
                .stringParameter("name", name)
                .stringParameter("className", String.join(" ", classes))
                .stringParameter("type", type)
                .stringParameter("pattern", pattern)
                .stringParameter("step", step)
                .stringParameter("min", min)
                .stringParameter("max", max)
                .stringParameter("alert", alert)
                .stringParameter("value", value)
                .stringParameter("textSize", textSize)
                .stringParameter("maxlength", maxlength)
                .stringParameter("id", id)
                .stringParameter("event", event != null ? event : "")
                .stringParameter("action", action != null ? action : "")
                .booleanParameter("disabled", disabled)
                .booleanParameter("readonly", readonly)
                .booleanParameter("required", modelFormField.getRequiredField())
                .stringParameter("clientAutocomplete", clientAutocomplete)
                .stringParameter("ajaxUrl", ajaxUrl)
                .booleanParameter("ajaxEnabled", ajaxEnabled)
                .stringParameter("mask", mask)
                .stringParameter("placeholder", placeholder)
                .stringParameter("tabindex", tabindex)
                .stringParameter("delegatorName", ((HttpSession) context.get("session"))
                        .getAttribute("delegatorName").toString())
                .build();
    }

    public RenderableFtl textArea(final Map<String, Object> context, final ModelFormField.TextareaField textareaField) {

        final ModelFormField modelFormField = textareaField.getModelFormField();

        final RenderableFtlMacroCallBuilder builder = RenderableFtlMacroCall.builder()
                .name("renderTextareaField")
                .stringParameter("name", modelFormField.getParameterName(context));

        builder.intParameter("cols", textareaField.getCols());
        builder.intParameter("rows", textareaField.getRows());

        builder.stringParameter("id", modelFormField.getCurrentContainerId(context));

        builder.stringParameter("alert", "false");

        ArrayList<String> classNames = new ArrayList<>();
        if (UtilValidate.isNotEmpty(modelFormField.getWidgetStyle())) {
            classNames.add(modelFormField.getWidgetStyle());
            if (modelFormField.shouldBeRed(context)) {
                builder.stringParameter("alert", "true");
            }
        }

        if (shouldApplyRequiredField(modelFormField)) {
            String requiredStyle = modelFormField.getRequiredFieldStyle();
            if (UtilValidate.isEmpty(requiredStyle)) {
                requiredStyle = "required";
            }
            classNames.add(requiredStyle);
        }
        builder.stringParameter("className", String.join(" ", classNames));

        if (textareaField.getVisualEditorEnable()) {
            builder.booleanParameter("visualEditorEnable", true);
            builder.stringParameter("buttons", textareaField.getVisualEditorButtons(context));
        }

        if (textareaField.isReadOnly()) {
            builder.stringParameter("readonly", "readonly");
        }

        Map<String, Object> userLogin = UtilGenerics.cast(context.get("userLogin"));
        String language = "en";
        if (userLogin != null) {
            language = UtilValidate.isEmpty((String) userLogin.get("lastLocale")) ? "en" : (String) userLogin.get("lastLocale");
        }
        builder.stringParameter("language", language);

        if (textareaField.getMaxlength() != null) {
            builder.intParameter("maxlength", textareaField.getMaxlength());
        }

        builder.stringParameter("placeholder", textareaField.getPlaceholder(context));

        builder.stringParameter("tabindex", modelFormField.getTabindex());

        builder.stringParameter("value", modelFormField.getEntry(context, textareaField.getDefaultValue(context)));

        builder.booleanParameter("disabled", modelFormField.getDisabled(context));

        return builder.build();
    }

    public RenderableFtl dateTime(final Map<String, Object> context, final ModelFormField.DateTimeField dateTimeField) {

        final ModelFormField modelFormField = dateTimeField.getModelFormField();
        final ModelForm modelForm = modelFormField.getModelForm();

        // Determine whether separate drop down select inputs be used for the hour/minute/am_pm components of the date-time.
        boolean useTimeDropDown = "time-dropdown".equals(dateTimeField.getInputMethod());

        final String paramName = modelFormField.getParameterName(context);

        final RenderableFtlMacroCallBuilder macroCallBuilder = RenderableFtlMacroCall.builder()
                .name("renderDateTimeField")
                .booleanParameter("disabled", modelFormField.getDisabled(context))
                .stringParameter("name", useTimeDropDown ? UtilHttp.makeCompositeParam(paramName, "date") : paramName)
                .stringParameter("id", modelFormField.getCurrentContainerId(context))
                .booleanParameter("isXMLHttpRequest", "XMLHttpRequest".equals(request.getHeader("X-Requested-With")))
                .stringParameter("tabindex", modelFormField.getTabindex())
                .stringParameter("event", modelFormField.getEvent())
                .stringParameter("formName", FormRenderer.getCurrentFormName(modelForm, context))
                .booleanParameter("alert", false)
                .stringParameter("action", modelFormField.getAction(context));

        // Set names for the various input components that might be rendered for this date-time field.
        macroCallBuilder.stringParameter("timeHourName", UtilHttp.makeCompositeParam(paramName, "hour"))
                .stringParameter("timeMinutesName", UtilHttp.makeCompositeParam(paramName, "minutes"))
                .stringParameter("compositeType", UtilHttp.makeCompositeParam(paramName, "compositeType"))
                .stringParameter("ampmName", UtilHttp.makeCompositeParam(paramName, "ampm"));

        ArrayList<String> classNames = new ArrayList<>();
        if (UtilValidate.isNotEmpty(modelFormField.getWidgetStyle())) {
            classNames.add(modelFormField.getWidgetStyle());

            if (modelFormField.shouldBeRed(context)) {
                macroCallBuilder.booleanParameter("alert", true);
            }
        }

        if (shouldApplyRequiredField(modelFormField)) {
            String requiredStyle = modelFormField.getRequiredFieldStyle();
            if (UtilValidate.isEmpty(requiredStyle)) {
                requiredStyle = "required";
            }
            classNames.add(requiredStyle);
        }
        macroCallBuilder.stringParameter("className", String.join(" ", classNames));

        String defaultDateTimeString = dateTimeField.getDefaultDateTimeString(context);

        if (useTimeDropDown) {
            final int step = dateTimeField.getStep();
            final String timeValues = IntStream.range(0, 60)
                    .filter(i -> i % step == 0)
                    .mapToObj(Integer::toString)
                    .collect(Collectors.joining(", ", "[", "]"));
            macroCallBuilder.stringParameter("timeValues", timeValues)
                    .intParameter("step", step);
        }

        Map<String, String> uiLabelMap = UtilGenerics.cast(context.get("uiLabelMap"));
        if (uiLabelMap == null) {
            Debug.logWarning("Could not find uiLabelMap in context", MODULE);
        }

        // whether the date field is short form, yyyy-mm-dd
        boolean shortDateInput = dateTimeField.isDateType() || useTimeDropDown;
        macroCallBuilder.booleanParameter("shortDateInput", shortDateInput);

        // Set render properties based on the date-time field's type.
        final int size;
        final int maxlength;
        final String formattedMask;
        final String titleLabelMapKey;

        if (shortDateInput) {
            size = 10;
            maxlength = 10;
            formattedMask = "9999-99-99";
            titleLabelMapKey = "CommonFormatDate";
        } else if (dateTimeField.isTimeType()) {
            size = 8;
            maxlength = 8;
            formattedMask = "99:99:99";
            titleLabelMapKey = "CommonFormatTime";

            macroCallBuilder.booleanParameter("isTimeType", true);
        } else {
            size = 25;
            maxlength = 30;
            formattedMask = "9999-99-99 99:99:99";
            titleLabelMapKey = "CommonFormatDateTime";
        }

        macroCallBuilder.intParameter("size", size)
                .intParameter("maxlength", maxlength);

        if (dateTimeField.useMask()) {
            macroCallBuilder.stringParameter("mask", formattedMask);
        }

        if (uiLabelMap != null) {
            macroCallBuilder.stringParameter("title", uiLabelMap.get(titleLabelMapKey))
                    .stringParameter("localizedIconTitle", uiLabelMap.get("CommonViewCalendar"));
        }

        final String contextValue = modelFormField.getEntry(context, dateTimeField.getDefaultValue(context));
        final String value = UtilValidate.isNotEmpty(contextValue) && contextValue.length() > maxlength
                ? contextValue.substring(0, maxlength)
                : contextValue;

        String timeDropdown = dateTimeField.getInputMethod();
        String timeDropdownParamName = "";

        if (!dateTimeField.isTimeType()) {
            String tempParamName;
            if (useTimeDropDown) {
                tempParamName = UtilHttp.makeCompositeParam(paramName, "date");
            } else {
                tempParamName = paramName;
            }
            timeDropdownParamName = tempParamName;
            defaultDateTimeString = UtilHttp.encodeBlanks(modelFormField.getEntry(context, defaultDateTimeString));
        }

        // If we have an input method of time-dropdown, then render two dropdowns
        if (useTimeDropDown) {
            // Set the class to apply to the time input components.
            final String widgetStyle = modelFormField.getWidgetStyle();
            macroCallBuilder.stringParameter("classString", widgetStyle != null ? widgetStyle : "");

            // Set the Calendar to the field's context value, or the field's default if no context value exists.
            final Calendar cal = Calendar.getInstance();
            try {
                if (contextValue != null) {
                    Timestamp contextValueTimestamp = Timestamp.valueOf(contextValue);
                    cal.setTime(contextValueTimestamp);
                }
            } catch (IllegalArgumentException e) {
                Debug.logWarning("Form widget field [" + paramName
                        + "] with input-method=\"time-dropdown\" was not able to understand the time [" + contextValue
                        + "]. The parsing error was: " + e.getMessage(), MODULE);
            }

            if (cal != null) {
                int hourOfDay = cal.get(Calendar.HOUR_OF_DAY);
                int minutesOfHour = cal.get(Calendar.MINUTE);

                // Set the hour value for when in 12-hour clock mode.
                macroCallBuilder.intParameter("hour1", hourOfDay % 12);

                // Set the hour value for when in 24-hour clock mode.
                macroCallBuilder.intParameter("hour2", hourOfDay);

                macroCallBuilder.intParameter("minutes", minutesOfHour);
            }

            boolean isTwelveHourClock = dateTimeField.isTwelveHour();
            macroCallBuilder.booleanParameter("isTwelveHour", isTwelveHourClock);

            // if using a 12-hour clock, write the AM/PM selector
            if (isTwelveHourClock) {
                macroCallBuilder.booleanParameter("amSelected", cal.get(Calendar.AM_PM) == Calendar.AM)
                        .booleanParameter("pmSelected", cal.get(Calendar.AM_PM) == Calendar.PM);

            }
        }

        macroCallBuilder.stringParameter("value", value)
                .stringParameter("timeDropdownParamName", timeDropdownParamName)
                .stringParameter("defaultDateTimeString", defaultDateTimeString)
                .stringParameter("timeDropdown", timeDropdown);

        return macroCallBuilder.build();
    }

    public RenderableFtl dateFind(final Map<String, Object> context, final ModelFormField.DateFindField dateFindField) {
        final ModelFormField modelFormField = dateFindField.getModelFormField();
        final ModelForm modelForm = modelFormField.getModelForm();
        final String name = modelFormField.getParameterName(context);

        final Locale locale = (Locale) context.get("locale");

        final Map<String, String> uiLabelMap = UtilGenerics.cast(context.get("uiLabelMap"));
        if (uiLabelMap == null) {
            Debug.logWarning("Could not find uiLabelMap in context", MODULE);
        }

        final Function<String, String> getOpLabel = (label) -> UtilProperties.getMessage("conditionalUiLabels",
                label, locale);

        final RenderableFtlMacroCallBuilder macroCallBuilder = RenderableFtlMacroCall.builder()
                .name("renderDateFindField")
                .stringParameter("name", name)
                .stringParameter("id", modelFormField.getCurrentContainerId(context))
                .stringParameter("formName", FormRenderer.getCurrentFormName(modelForm, context))
                .booleanParameter("disabled", modelFormField.getDisabled(context))
                .booleanParameter("isDateType", dateFindField.isDateType())
                .booleanParameter("isTimeType", dateFindField.isTimeType())
                .stringParameter("opEquals", getOpLabel.apply("equals"))
                .stringParameter("opSameDay", getOpLabel.apply("same_day"))
                .stringParameter("opGreaterThanFromDayStart", getOpLabel.apply("greater_than_from_day_start"))
                .stringParameter("opGreaterThan", getOpLabel.apply("greater_than"))
                .stringParameter("opLessThan", getOpLabel.apply("less_than"))
                .stringParameter("opUpToDay", getOpLabel.apply("up_to_day"))
                .stringParameter("opUpThruDay", getOpLabel.apply("up_thru_day"))
                .stringParameter("opIsEmpty", getOpLabel.apply("is_empty"))
                .stringParameter("tabindex", modelFormField.getTabindex())
                .stringParameter("conditionGroup", modelFormField.getConditionGroup())
                .stringParameter("defaultOptionFrom", dateFindField.getDefaultOptionFrom(context))
                .stringParameter("defaultOptionThru", dateFindField.getDefaultOptionThru(context))
                .stringParameter("language", locale.getLanguage());

        macroCallBuilder.booleanParameter("alert", false);
        if (UtilValidate.isNotEmpty(modelFormField.getWidgetStyle())) {
            macroCallBuilder.stringParameter("className", modelFormField.getWidgetStyle());
            if (modelFormField.shouldBeRed(context)) {
                macroCallBuilder.booleanParameter("alert", true);
            }
        }

        // Set render properties based on the date-finds field's type.
        final String localizedInputTitleLabelMapKey;
        if (dateFindField.isDateType()) {
            macroCallBuilder.intParameter("size", 10)
                    .intParameter("maxlength", 20);

            localizedInputTitleLabelMapKey = "CommonFormatDate";
        } else if (dateFindField.isTimeType()) {
            macroCallBuilder.intParameter("size", 8)
                    .intParameter("maxlength", 8);

            localizedInputTitleLabelMapKey = "CommonFormatTime";
        } else {
            macroCallBuilder.intParameter("size", 25)
                    .intParameter("maxlength", 30);

            localizedInputTitleLabelMapKey = "CommonFormatDateTime";
        }

        if (uiLabelMap != null) {
            // search for a localized label for the icon
            macroCallBuilder.stringParameter("localizedInputTitle", uiLabelMap.get(localizedInputTitleLabelMapKey));
        }

        // add calendar pop-up button IF this is not a "time" type date-find
        if (!dateFindField.isTimeType()) {
            macroCallBuilder.stringParameter("imgSrc", pathAsContentUrl("/images/cal.gif"));
        }

        macroCallBuilder.stringParameter("value",
                        modelFormField.getEntry(context, dateFindField.getDefaultValue(context)))
                .stringParameter("value2", modelFormField.getEntry(context));

        if (context.containsKey("parameters")) {
            final Map<String, Object> parameters = UtilGenerics.cast(context.get("parameters"));
            if (parameters.containsKey(name + "_fld0_value")) {
                macroCallBuilder.stringParameter("value", (String) parameters.get(name + "_fld0_value"));
            }
            if (parameters.containsKey(name + "_fld1_value")) {
                macroCallBuilder.stringParameter("value2", (String) parameters.get(name + "_fld1_value"));
            }
        }

        if (UtilValidate.isNotEmpty(modelFormField.getTitleStyle())) {
            macroCallBuilder.stringParameter("titleStyle", modelFormField.getTitleStyle());
        }

        return macroCallBuilder.build();
    }

    public RenderableFtl dateRangePicker(final Map<String, Object> context, final ModelFormField.DateRangePickerField dateRangePickerField) {
        final ModelFormField modelFormField = dateRangePickerField.getModelFormField();
        final ModelForm modelForm = modelFormField.getModelForm();
        final String name = modelFormField.getParameterName(context);
        final Locale locale = (Locale) context.get("locale");
        String className = UtilFormatOut.checkNull(dateRangePickerField.getModelFormField().getWidgetStyle());

        String event = "";
        String action = "";
        if (UtilValidate.isNotEmpty(modelFormField.getEvent()) && UtilValidate.isNotEmpty(modelFormField.getAction(context))) {
            event = modelFormField.getEvent();
            action = modelFormField.getAction(context);
        }

        final Map<String, String> uiLabelMap = UtilGenerics.cast(context.get("uiLabelMap"));
        if (uiLabelMap == null) {
            Debug.logWarning("Could not find uiLabelMap in context", MODULE);
        }

        final Function<String, String> getOpLabel = (label) -> UtilProperties.getMessage("conditionalUiLabels", label, locale);

        String applyLabel = Optional.ofNullable(dateRangePickerField.getApplyLabel(context))
                .filter(s -> !s.isEmpty())
                .orElse(UtilProperties.getMessage("CommonUiLabels", "CommonApply", locale));
        String cancelLabel = Optional.ofNullable(dateRangePickerField.getCancelLabel(context))
                .filter(s -> !s.isEmpty())
                .orElse(UtilProperties.getMessage("CommonUiLabels", "CommonCancel", locale));
        String clearTitle = Optional.ofNullable(dateRangePickerField.getClearTitle(context))
                .filter(s -> !s.isEmpty())
                .orElse(UtilProperties.getMessage("CommonUiLabels", "CommonClear", locale));

        final RenderableFtlMacroCallBuilder macroCallBuilder = RenderableFtlMacroCall.builder()
                .name("renderDateRangePicker")
                .stringParameter("name", name)
                .stringParameter("id", modelFormField.getCurrentContainerId(context))
                .stringParameter("className", className)
                .stringParameter("formName", FormRenderer.getCurrentFormName(modelForm, context))
                .stringParameter("event", event)
                .stringParameter("action", action)
                .stringParameter("locale", locale.toString())
                .stringParameter("conditionGroup", modelFormField.getConditionGroup())
                .stringParameter("tabindex", modelFormField.getTabindex())

                .booleanParameter("alwaysShowCalendars", dateRangePickerField.getAlwaysShowCalendars())
                .stringParameter("applyButtonClasses", dateRangePickerField.getApplyButtonClasses(context))
                .stringParameter("applyLabel", applyLabel)
                .booleanParameter("autoApply", dateRangePickerField.getAutoApply())
                .stringParameter("buttonClasses", dateRangePickerField.getButtonClasses(context))
                .stringParameter("cancelButtonClasses", dateRangePickerField.getCancelButtonClasses(context))
                .stringParameter("cancelLabel", cancelLabel)
                .stringParameter("clearTitle", clearTitle)
                .stringParameter("drops", dateRangePickerField.getDrops())
                .booleanParameter("linkedCalendars", dateRangePickerField.getLinkedCalendars())
                .stringParameter("maxSpan", Optional.ofNullable(dateRangePickerField.getMaxSpan()).map(Objects::toString).orElse(""))
                .stringParameter("maxYear", Optional.ofNullable(dateRangePickerField.getMaxYear()).map(Objects::toString).orElse(""))
                .stringParameter("minYear", Optional.ofNullable(dateRangePickerField.getMinYear()).map(Objects::toString).orElse(""))
                .stringParameter("opens", dateRangePickerField.getOpens())
                .stringParameter("rangeThisWeekLabel", getOpLabel.apply("this_week"))
                .stringParameter("rangeLastWeekLabel", getOpLabel.apply("last_week"))
                .stringParameter("rangeNextWeekLabel", getOpLabel.apply("next_week"))
                .stringParameter("rangeThisMonthLabel", getOpLabel.apply("this_month"))
                .stringParameter("rangeLastMonthLabel", getOpLabel.apply("last_month"))
                .stringParameter("rangeNextMonthLabel", getOpLabel.apply("next_month"))
                .booleanParameter("showDropdowns", dateRangePickerField.getShowDropdowns())
                .booleanParameter("showIsoWeekNumbers", dateRangePickerField.getShowIsoWeekNumbers())
                .booleanParameter("showRanges", dateRangePickerField.getShowRanges())
                .booleanParameter("showWeekNumbers", dateRangePickerField.getShowWeekNumbers())
                .booleanParameter("singleDatePicker", dateRangePickerField.getSingleDatePicker())
                .booleanParameter("timePicker", dateRangePickerField.getTimePicker())
                .booleanParameter("timePicker24Hour", dateRangePickerField.getTimePicker24Hour())
                .stringParameter("timePickerIncrement",
                        Optional.ofNullable(dateRangePickerField.getTimePickerIncrement()).map(Objects::toString).orElse(""))
                .booleanParameter("timePickerSeconds", dateRangePickerField.getTimePickerSeconds());

        macroCallBuilder.stringParameter("alert", "false");
        if (UtilValidate.isNotEmpty(modelFormField.getWidgetStyle())) {
            macroCallBuilder.stringParameter("className", modelFormField.getWidgetStyle());
            if (modelFormField.shouldBeRed(context)) {
                macroCallBuilder.stringParameter("alert", "true");
            }
        }

        macroCallBuilder
                .stringParameter("value", modelFormField.getEntry(context, dateRangePickerField.getDefaultValue(context)))
                .stringParameter("value2", modelFormField.getEntry(context));

        if (context.containsKey("parameters")) {
            final Map<String, Object> parameters = UtilGenerics.cast(context.get("parameters"));
            if (parameters.containsKey(name + "_fld0_value")) {
                macroCallBuilder.stringParameter("value", (String) parameters.get(name + "_fld0_value"));
            }
            if (parameters.containsKey(name + "_fld1_value")) {
                macroCallBuilder.stringParameter("value2", (String) parameters.get(name + "_fld1_value"));
            }
        }

        if (UtilValidate.isNotEmpty(modelFormField.getTitleStyle())) {
            macroCallBuilder.stringParameter("titleStyle", modelFormField.getTitleStyle());
        }

        return macroCallBuilder.build();
    }

    public RenderableFtl makeHyperlinkString(final ModelFormField.SubHyperlink subHyperlink,
                                             final Map<String, Object> context) {
        if (subHyperlink == null || !subHyperlink.shouldUse(context)) {
            return RenderableFtlNoop.INSTANCE;
        }

        if (UtilValidate.isNotEmpty(subHyperlink.getWidth())) {
            request.setAttribute("width", subHyperlink.getWidth());
        }
        if (UtilValidate.isNotEmpty(subHyperlink.getHeight())) {
            request.setAttribute("height", subHyperlink.getHeight());
        }

        return makeHyperlinkByType(subHyperlink.getLinkType(), subHyperlink.getStyle(context),
                subHyperlink.getUrlMode(), subHyperlink.getTarget(context),
                subHyperlink.getParameterMap(context, subHyperlink.getModelFormField().getEntityName(),
                        subHyperlink.getModelFormField().getServiceName()),
                subHyperlink.getDescription(context), subHyperlink.getTargetWindow(context), "",
                subHyperlink.getModelFormField(), request, response, context);
    }

    public RenderableFtl makeHyperlinkByType(String linkType, String linkStyle, String targetType, String target,
                                             Map<String, String> parameterMap, String description, String targetWindow,
                                             String confirmation, ModelFormField modelFormField,
                                             HttpServletRequest request, HttpServletResponse response,
                                             Map<String, Object> context) {
        String realLinkType = WidgetWorker.determineAutoLinkType(linkType, target, targetType, request);
        // get the parameterized pagination index and size fields
        int paginatorNumber = WidgetWorker.getPaginatorNumber(context);
        ModelForm modelForm = modelFormField.getModelForm();
        ModelTheme modelTheme = visualTheme.getModelTheme();
        String viewIndexField = modelForm.getMultiPaginateIndexField(context);
        String viewSizeField = modelForm.getMultiPaginateSizeField(context);
        int viewIndex = Paginator.getViewIndex(modelForm, context);
        int viewSize = Paginator.getViewSize(modelForm, context);
        if (("viewIndex" + "_" + paginatorNumber).equals(viewIndexField)) {
            viewIndexField = "VIEW_INDEX" + "_" + paginatorNumber;
        }
        if (("viewSize" + "_" + paginatorNumber).equals(viewSizeField)) {
            viewSizeField = "VIEW_SIZE" + "_" + paginatorNumber;
        }
        if ("hidden-form".equals(realLinkType)) {
            parameterMap.put(viewIndexField, Integer.toString(viewIndex));
            parameterMap.put(viewSizeField, Integer.toString(viewSize));

            final RenderableFtlVerbatimBuilder renderableFtlVerbatimBuilder = RenderableFtlVerbatim.builder();
            final StringBuilder htmlStringBuilder = renderableFtlVerbatimBuilder.getStringBuilder();

            if ("multi".equals(modelForm.getType())) {
                final Element anchorElement = WidgetWorker.makeHiddenFormLinkAnchorElement(linkStyle,
                        description, confirmation, modelFormField, request, context);
                if (anchorElement != null) {
                    htmlStringBuilder.append(anchorElement.outerHtml());
                }

                // this is a bit trickier, since we can't do a nested form we'll have to put the link to submit the
                // form in place, but put the actual form def elsewhere, ie after the big form is closed
                final RenderableFtlVerbatim postFormRenderableFtlVerbatim = RenderableFtlVerbatim.withStringBuilder(sb -> {
                    final Element hiddenFormElement = WidgetWorker.makeHiddenFormLinkFormElement(target, targetType,
                            targetWindow, parameterMap, modelFormField, request, response, context);
                    sb.append(hiddenFormElement.outerHtml());
                });
                appendToPostFormRenderableFtl(postFormRenderableFtlVerbatim, context);

            } else {
                final Element hiddenFormElement = WidgetWorker.makeHiddenFormLinkFormElement(target, targetType,
                        targetWindow, parameterMap, modelFormField, request, response, context);
                htmlStringBuilder.append(hiddenFormElement.outerHtml());
                final Element anchorElement = WidgetWorker.makeHiddenFormLinkAnchorElement(linkStyle,
                        description, confirmation, modelFormField, request, context);
                if (anchorElement != null) {
                    htmlStringBuilder.append(anchorElement.outerHtml());
                }
            }

            return renderableFtlVerbatimBuilder.build();

        } else {
            if ("layered-modal".equals(realLinkType)) {
                String uniqueItemName = UtilRandom.getUnique("Modal_", true);
                String width = (String) request.getAttribute("width");
                if (UtilValidate.isEmpty(width)) {
                    width = String.valueOf(modelTheme.getLinkDefaultLayeredModalWidth());
                    request.setAttribute("width", width);
                }
                String height = (String) request.getAttribute("height");
                if (UtilValidate.isEmpty(height)) {
                    height = String.valueOf(modelTheme.getLinkDefaultLayeredModalHeight());
                    request.setAttribute("height", height);
                }
                request.setAttribute("uniqueItemName", uniqueItemName);
                RenderableFtl renderableFtl = hyperlinkMacroCall(linkStyle, targetType, target, parameterMap,
                        description, confirmation, modelFormField, request, response, context, targetWindow);
                request.removeAttribute("uniqueItemName");
                request.removeAttribute("height");
                request.removeAttribute("width");
                return renderableFtl;
            } else {
                return hyperlinkMacroCall(linkStyle, targetType, target, parameterMap, description, confirmation,
                        modelFormField, request, response, context, targetWindow);
            }
        }
    }

    public RenderableFtl hyperlinkMacroCall(String linkStyle, String targetType, String target,
                                            Map<String, String> parameterMap, String description, String confirmation,
                                            ModelFormField modelFormField,
                                            HttpServletRequest request, HttpServletResponse response,
                                            Map<String, Object> context, String targetWindow) {
        if (description != null || UtilValidate.isNotEmpty(request.getAttribute("image"))) {
            StringBuilder linkUrl = new StringBuilder();
            final URI linkUri = WidgetWorker.buildHyperlinkUri(target, targetType,
                    UtilValidate.isEmpty(request.getAttribute("uniqueItemName")) ? parameterMap : null,
                    null, false, false, true, request, response);
            linkUrl.append(linkUri.toString());
            String event = "";
            String action = "";
            String imgSrc = "";
            String imgTitle = "";
            String alt = "";
            String id = "";
            String uniqueItemName = "";
            String width = "";
            String height = "";
            String title = "";
            String text = "";
            String hiddenFormName = WidgetWorker.makeLinkHiddenFormName(context, modelFormField);
            if (UtilValidate.isNotEmpty(modelFormField.getEvent())
                    && UtilValidate.isNotEmpty(modelFormField.getAction(context))) {
                event = modelFormField.getEvent();
                action = modelFormField.getAction(context);
            }
            if (UtilValidate.isNotEmpty(request.getAttribute("image"))) {
                imgSrc = encode(request.getAttribute("image").toString(), modelFormField, context);
            }
            if (UtilValidate.isNotEmpty(request.getAttribute("imageTitle"))) {
                imgTitle = request.getAttribute("imageTitle").toString();
            }
            if (UtilValidate.isNotEmpty(request.getAttribute("alternate"))) {
                alt = request.getAttribute("alternate").toString();
            }
            Integer size = Integer.valueOf("0");
            if (UtilValidate.isNotEmpty(request.getAttribute("descriptionSize"))) {
                size = Integer.valueOf(request.getAttribute("descriptionSize").toString());
            }
            if (UtilValidate.isNotEmpty(description) && size > 0 && description.length() > size) {
                title = description;
                description = StringUtil.truncateEncodedStringToLength(description, size);
            } else if (UtilValidate.isNotEmpty(request.getAttribute("title"))) {
                title = request.getAttribute("title").toString();
            }
            if (UtilValidate.isNotEmpty(request.getAttribute("id"))) {
                id = request.getAttribute("id").toString();
            }
            if (UtilValidate.isNotEmpty(request.getAttribute("text"))) {
                text = request.getAttribute("text").toString();
            }
            if (UtilValidate.isNotEmpty(request.getAttribute("uniqueItemName"))) {
                uniqueItemName = request.getAttribute("uniqueItemName").toString();
                width = request.getAttribute("width").toString();
                height = request.getAttribute("height").toString();
            }
            // the macro expects the parameters of the link as a JSON string
            String targetParameters = "";
            if (UtilValidate.isNotEmpty(parameterMap) || UtilValidate.isNotEmpty(uniqueItemName)) {
                try {
                    final Map<String, Object> params = new java.util.TreeMap<>();
                    if (UtilValidate.isNotEmpty(parameterMap)) {
                        params.putAll(parameterMap);
                    }
                    if (UtilValidate.isNotEmpty(uniqueItemName)) {
                        params.put("presentation", "layer");
                    }
                    targetParameters = JSON.from(params).toString();
                } catch (Exception e) {
                    Debug.logError(e, "Error converting dialog params to JSON", MODULE);
                }
            }

            return RenderableFtlMacroCall.builder()
                    .name("makeHyperlinkString")
                    .stringParameter("linkStyle", linkStyle == null ? "" : linkStyle)
                    .stringParameter("hiddenFormName", nonNull(hiddenFormName))
                    .stringParameter("event", event)
                    .stringParameter("action", action)
                    .stringParameter("imgSrc", imgSrc)
                    .stringParameter("imgTitle", imgTitle)
                    .stringParameter("title", title)
                    .stringParameter("alternate", alt)
                    .stringParameter("targetParameters", targetParameters)
                    .stringParameter("linkUrl", linkUrl.toString())
                    .stringParameter("targetWindow", nonNull(targetWindow))
                    .stringParameter("description", nonNull(description))
                    .stringParameter("confirmation", nonNull(confirmation))
                    .stringParameter("uniqueItemName", uniqueItemName)
                    .stringParameter("height", height)
                    .stringParameter("width", width)
                    .stringParameter("id", id)
                    .stringParameter("text", text)
                    .build();
        } else {
            return RenderableFtlNoop.INSTANCE;
        }
    }

    public RenderableFtlMacroCall containerMacroCall(final Map<String, Object> context,
                                                     final ContainerField containerField) {
        final String id = containerField.getModelFormField().getCurrentContainerId(context);
        String className = UtilFormatOut.checkNull(containerField.getModelFormField().getWidgetStyle());

        return RenderableFtlMacroCall.builder()
                .name("renderContainerField")
                .stringParameter("id", id)
                .stringParameter("className", className)
                .build();
    }

    public RenderableFtlMacroCall fieldGroupOpen(final Map<String, Object> context, final ModelForm.FieldGroup fieldGroup) {
        final String style = fieldGroup.getStyle();
        final String id = fieldGroup.getId();
        final FlexibleStringExpander titleNotExpanded = FlexibleStringExpander.getInstance(fieldGroup.getTitle());
        final String title = titleNotExpanded.expandString(context);

        String expandToolTip = "";
        String collapseToolTip = "";
        if (UtilValidate.isNotEmpty(style) || UtilValidate.isNotEmpty(id) || UtilValidate.isNotEmpty(title)) {
            if (fieldGroup.collapsible()) {
                Map<String, Object> uiLabelMap = UtilGenerics.cast(context.get("uiLabelMap"));
                if (uiLabelMap != null) {
                    expandToolTip = (String) uiLabelMap.get("CommonExpand");
                    collapseToolTip = (String) uiLabelMap.get("CommonCollapse");
                }
            }
        }

        final RenderableFtlMacroCallBuilder macroCallBuilder = RenderableFtlMacroCall.builder().name("renderFieldGroupOpen")
                .stringParameter("id", id)
                .stringParameter("title", title)
                .stringParameter("style", style)
                .stringParameter("collapsibleAreaId", fieldGroup.getId() + "_body")
                .booleanParameter("collapsible", fieldGroup.collapsible())
                .booleanParameter("collapsed", fieldGroup.initiallyCollapsed())
                .stringParameter("expandToolTip", expandToolTip)
                .stringParameter("collapseToolTip", collapseToolTip);

        return macroCallBuilder.build();
    }

    public RenderableFtlMacroCall fieldGroupClose(final Map<String, Object> context, final ModelForm.FieldGroup fieldGroup) {
        FlexibleStringExpander titleNotExpanded = FlexibleStringExpander.getInstance(fieldGroup.getTitle());
        final String title = titleNotExpanded.expandString(context);

        final RenderableFtlMacroCallBuilder macroCallBuilder = RenderableFtlMacroCall.builder().name("renderFieldGroupClose")
                .stringParameter("id", fieldGroup.getId())
                .stringParameter("title", title)
                .stringParameter("style", fieldGroup.getStyle());

        return macroCallBuilder.build();
    }

    public RenderableFtl dropDownField(final Map<String, Object> context,
                                       final ModelFormField.DropDownField dropDownField,
                                       final boolean javaScriptEnabled) {

        final var builder = RenderableFtlMacroCall.builder().name("renderDropDownField");

        final ModelFormField modelFormField = dropDownField.getModelFormField();
        final ModelForm modelForm = modelFormField.getModelForm();
        final var currentValue = modelFormField.getEntry(context);
        final var autoComplete = dropDownField.getAutoComplete();
        final var textSizeOptional = dropDownField.getTextSize();

        applyCommonStyling(modelFormField, context, builder);

        builder
                .stringParameter("name", modelFormField.getParameterName(context))
                .stringParameter("id", modelFormField.getCurrentContainerId(context))
                .stringParameter("formName", modelForm.getName())
                .stringParameter("size", dropDownField.getSize())
                .booleanParameter("multiple", dropDownField.getAllowMultiple())
                .stringParameter("currentValue", currentValue)
                .stringParameter("conditionGroup", modelFormField.getConditionGroup())
                .booleanParameter("disabled", modelFormField.getDisabled(context))
                .booleanParameter("ajaxEnabled", autoComplete != null && javaScriptEnabled)
                .stringParameter("noCurrentSelectedKey", dropDownField.getNoCurrentSelectedKey(context))
                .stringParameter("tabindex", modelFormField.getTabindex())
                .booleanParameter("allowEmpty", dropDownField.getAllowEmpty())
                .stringParameter("dDFCurrent", dropDownField.getCurrent())
                .booleanParameter("placeCurrentValueAsFirstOption",
                        "first-in-list".equals(dropDownField.getCurrent()));

        final var event = modelFormField.getEvent();
        final var action = modelFormField.getAction(context);
        if (event != null && action != null) {
            builder.stringParameter("event", event).stringParameter("action", action);
        }

        final var allOptionValues = dropDownField.getAllOptionValues(context, WidgetWorker.getDelegator(context));
        final var allGroupValues = dropDownField.getGroupOptions();
        final var explicitDescription =
                // Populate explicitDescription with the description from the option associated with the current value.
                allOptionValues.stream()
                .filter(optionValue -> optionValue.getKey().equals(currentValue))
                .map(ModelFormField.OptionValue::getDescription)
                .findFirst()

                // If no matching option is found, use the current description from the field.
                .or(() -> Optional.ofNullable(dropDownField.getCurrentDescription(context)))
                .filter(UtilValidate::isNotEmpty)

                // If no description has been found, fall back to the description determined by the ModelFormField.
                .or(() -> Optional.of(ModelFormField.FieldInfoWithOptions.getDescriptionForOptionKey(currentValue,
                        allOptionValues)))

                // Truncate and encode the description as needed.
                .map(description -> encode(truncate(description, textSizeOptional), modelFormField, context));

        builder.stringParameter("explicitDescription", explicitDescription.orElse(""));

        // Take the field's current value and convert it to a list containing a single item.
        // If the field allows multiple values, the current value is expected to be a string encoded list of values
        // which it will be converted to a list of strings.
        final List<String> currentValuesList = (UtilValidate.isNotEmpty(currentValue) && dropDownField.getAllowMultiple())
                        ? (currentValue.startsWith("[")
                            ? StringUtil.toList(currentValue)
                            : UtilMisc.toList(currentValue))
                        : Collections.emptyList();

        var optionsList = new ArrayList<>();
        if (UtilValidate.isNotEmpty(allGroupValues)) {
            optionsList.addAll(populateGroupAndOptions(context, allGroupValues, modelFormField, textSizeOptional, currentValuesList));
        }
        optionsList.addAll(populateOptions(context, allOptionValues, modelFormField, textSizeOptional, currentValuesList));

        builder.objectParameter("options", optionsList);

        int otherFieldSize = dropDownField.getOtherFieldSize();
        if (otherFieldSize > 0) {
            var otherFieldName = dropDownField.getParameterNameOther(context);

            var dataMap = modelFormField.getMap(context);
            if (dataMap == null) {
                dataMap = context;
            }
            var otherValueObj = dataMap.get(otherFieldName);
            var otherValue = (otherValueObj == null) ? "" : otherValueObj.toString();

            builder
                    .stringParameter("otherFieldName", otherFieldName)
                    .stringParameter("otherValue", otherValue)
                    .intParameter("otherFieldSize", otherFieldSize);
        }

        return builder.build();
    }

    private List<Object> populateGroupAndOptions(Map<String, Object> context, List<ModelFormField.GroupOptions> allGroupOptions,
                                         ModelFormField modelFormField, Optional<Integer> textSizeOptional, List<String> currentValuesList) {
        if (UtilValidate.isEmpty(allGroupOptions)) {
            return new ArrayList<>();
        }
        return UtilGenerics.cast(allGroupOptions.stream()
                .map(groupOptions -> {
                    var groupOptionId = groupOptions.getId(context);
                    var truncatedDescription = truncate(groupOptions.getDescription(context), textSizeOptional);
                    var widgetStyle = groupOptions.getWidgetStyle(context);
                    List<Object> optionsInGroupList = new ArrayList<>();
                    optionsInGroupList.addAll(populateGroupAndOptions(context,
                            groupOptions.getGroupOptions(),
                            modelFormField, textSizeOptional, currentValuesList));
                    optionsInGroupList.addAll(populateOptions(context,
                            groupOptions.getAllOptionValues(context, WidgetWorker.getDelegator(context)),
                            modelFormField, textSizeOptional, currentValuesList));
                    return new GroupOption(groupOptionId, truncatedDescription, widgetStyle, optionsInGroupList);
                })
        .toList());
    }
    private List<Object> populateOptions(Map<String, Object> context, List<ModelFormField.OptionValue> allOptionValues,
                                         ModelFormField modelFormField, Optional<Integer> textSizeOptional, List<String> currentValuesList) {
        if (UtilValidate.isEmpty(allOptionValues)) {
            return new ArrayList<>();
        }
        return UtilGenerics.cast(allOptionValues.stream()
                .map(optionValue -> {
                    var encodedKey = encode(optionValue.getKey(), modelFormField, context);
                    var truncatedDescription = truncate(optionValue.getDescription(), textSizeOptional);
                    var selected = currentValuesList.contains(optionValue.getKey());

                    return new Option(encodedKey, truncatedDescription, selected);
                })
        .toList());
    }

    /**
     * Create an ajaxXxxx JavaScript CSV string from a list of UpdateArea objects. See
     * <code>OfbizUtil.js</code>.
     *
     * @param updateAreas
     * @param extraParams Renderer-supplied additional target parameters
     * @param context
     * @return Parameter string or empty string if no UpdateArea objects were found
     */
    private String createAjaxParamsFromUpdateAreas(final List<ModelForm.UpdateArea> updateAreas,
                                                   final String extraParams,
                                                   final Map<String, Object> context) {
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
            Map<String, Object> ctx = UtilGenerics.cast(context);
            Map<String, String> parameters = updateArea.getParameterMap(ctx);
            String targetUrl = updateArea.getAreaTarget(context);
            String ajaxParams;
            StringBuffer ajaxParamsBuffer = new StringBuffer();
            ajaxParamsBuffer.append(getAjaxParamsFromTarget(targetUrl));
            //add first parameters from updateArea parameters
            if (UtilValidate.isNotEmpty(parameters)) {
                for (Map.Entry<String, String> entry : parameters.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    //test if ajax parameters are not already into extraParams, if so do not add it
                    if (UtilValidate.isNotEmpty(extraParams) && extraParams.contains(value)) {
                        continue;
                    }
                    if (ajaxParamsBuffer.length() > 0 && ajaxParamsBuffer.indexOf(key) < 0) {
                        ajaxParamsBuffer.append("&");
                    }
                    if (ajaxParamsBuffer.indexOf(key) < 0) {
                        ajaxParamsBuffer.append(key).append("=").append(value);
                    }
                }
            }
            //then add parameters from request. Those parameters could end with an anchor so we must set ajax parameters first
            if (UtilValidate.isNotEmpty(extraParams)) {
                if (ajaxParamsBuffer.length() > 0 && !extraParams.startsWith("&")) {
                    ajaxParamsBuffer.append("&");
                }
                ajaxParamsBuffer.append(extraParams);
            }
            ajaxParams = ajaxParamsBuffer.toString();
            ajaxUrl += updateArea.getAreaId() + ",";
            ajaxUrl += requestHandler.makeLink(request, response, UtilHttp.removeQueryStringFromTarget(targetUrl));
            ajaxUrl += "," + ajaxParams;
        }
        Locale locale = UtilMisc.ensureLocale(context.get("locale"));
        return FlexibleStringExpander.expandString(ajaxUrl, context, locale);
    }

    private void appendToPostFormRenderableFtl(final RenderableFtl renderableFtl, final Map<String, Object> context) {
        // If there is already a Post Form RenderableFtl, wrap it in a sequence with the given RenderableFtl
        // appended. This ensures we don't overwrite any other elements to be rendered after the main form.
        final RenderableFtl current = getPostMultiFormRenderableFtl(context);

        if (current == null) {
            setPostMultiFormRenderableFtl(renderableFtl, context);
        } else {
            final RenderableFtlSequence wrapper = RenderableFtlSequence.builder()
                    .renderableFtl(current)
                    .renderableFtl(renderableFtl)
                    .build();
            setPostMultiFormRenderableFtl(wrapper, context);
        }
    }

    private RenderableFtl getPostMultiFormRenderableFtl(final Map<String, Object> context) {
        final Map<String, Object> wholeFormContext = getWholeFormContext(context);
        return (RenderableFtl) wholeFormContext.get("postMultiFormRenderableFtl");
    }

    private void setPostMultiFormRenderableFtl(final RenderableFtl postMultiFormRenderableFtl,
                                               final Map<String, Object> context) {
        final Map<String, Object> wholeFormContext = getWholeFormContext(context);
        wholeFormContext.put("postMultiFormRenderableFtl", postMultiFormRenderableFtl);
    }

    private Map<String, Object> getWholeFormContext(final Map<String, Object> context) {
        final Map<String, Object> wholeFormContext = UtilGenerics.cast(context.get("wholeFormContext"));
        if (wholeFormContext == null) {
            throw new RuntimeException("Cannot access whole form context");
        }
        return wholeFormContext;
    }

    private static boolean shouldApplyRequiredField(ModelFormField modelFormField) {
        return ("single".equals(modelFormField.getModelForm().getType())
                || "upload".equals(modelFormField.getModelForm().getType()))
                && modelFormField.getRequiredField();
    }

    /**
     * Extracts parameters from a target URL string, prepares them for an Ajax
     * JavaScript call. This method is currently set to return a parameter string
     * suitable for the Prototype.js library.
     *
     * @param target Target URL string
     * @return Parameter string
     */
    private static String getAjaxParamsFromTarget(String target) {
        String targetParams = UtilHttp.getQueryStringFromTarget(target);
        targetParams = targetParams.replace("?", "");
        targetParams = targetParams.replace("&amp;", "&");
        return targetParams;
    }

    private String pathAsContentUrl(final String path) {
        return staticContentUrlProvider.pathAsContentUrlString(path);
    }

    private String encode(String value, ModelFormField modelFormField, Map<String, Object> context) {
        if (UtilValidate.isEmpty(value)) {
            return value;
        }
        UtilCodec.SimpleEncoder encoder = (UtilCodec.SimpleEncoder) context.get("simpleEncoder");
        if (modelFormField.getEncodeOutput() && encoder != null) {
            value = encoder.encode(value);
        }
        return value;
    }

    private String truncate(String value, int maxCharacterLength) {
        if (maxCharacterLength > 8 && value.length() > maxCharacterLength) {
            return StringUtil.truncateEncodedStringToLength(value, maxCharacterLength);
        }
        return value;
    }

    private String truncate(String value, Optional<Integer> maxCharacterLengthOptional) {
        return maxCharacterLengthOptional
                .map(maxCharacterLength -> truncate(value, maxCharacterLength))
                .orElse(value);
    }

    /**
     * Creates the macro call that renders the check boxes of a check field.
     * @param context the context
     * @param checkField the check field
     * @return the RenderableFtl
     */
    public RenderableFtl checkField(final Map<String, Object> context, final ModelFormField.CheckField checkField) {
        final ModelFormField modelFormField = checkField.getModelFormField();
        final String currentValue = modelFormField.getEntry(context);
        final Boolean allChecked = checkField.isAllChecked(context);
        final String checkBox = modelFormField.getAttributeName();

        List<String> checkedByDefault = Collections.emptyList();
        final Object checkBoxValue = context.get(checkBox);
        if (checkBoxValue != null && !checkBoxValue.getClass().equals(String.class)) {
            checkedByDefault = StringUtil.toList(checkBoxValue.toString());
        }

        List<String> currentValueList = null;
        if (UtilValidate.isNotEmpty(currentValue)) {
            currentValueList = currentValue.startsWith("[") ? StringUtil.toList(currentValue) : UtilMisc.toList(currentValue);
        }

        final List<Map<String, String>> items = new ArrayList<>();
        for (final ModelFormField.OptionValue optionValue : checkField.getAllOptionValues(context, WidgetWorker.getDelegator(context))) {
            final boolean checked;
            if (UtilValidate.isNotEmpty(currentValueList)) {
                checked = currentValueList.contains(optionValue.getKey());
            } else if (UtilValidate.isNotEmpty(checkedByDefault)) {
                checked = checkedByDefault.contains(optionValue.getKey());
            } else {
                checked = allChecked;
            }
            items.add(Map.of(
                    "value", nonNull(optionValue.getKey()),
                    "description", nonNull(encode(optionValue.getDescription(), modelFormField, context)),
                    "checked", String.valueOf(checked)));
        }

        final var builder = RenderableFtlMacroCall.builder().name("renderCheckField");
        applyWidgetStyleAndAlert(modelFormField, context, builder);
        return builder
                .objectParameter("items", items)
                .stringParameter("id", nonNull(modelFormField.getCurrentContainerId(context)))
                .stringParameter("conditionGroup", nonNull(modelFormField.getConditionGroup()))
                .stringParameter("currentValue", nonNull(currentValue))
                .stringParameter("name", nonNull(modelFormField.getParameterName(context)))
                .stringParameter("event", nonNull(modelFormField.getEvent()))
                .stringParameter("action", nonNull(modelFormField.getAction(context)))
                .stringParameter("tabindex", nonNull(modelFormField.getTabindex()))
                .booleanParameter("disabled", modelFormField.getDisabled(context))
                .build();
    }

    /**
     * Creates the macro call that renders the radio buttons of a radio field.
     * @param context the context
     * @param radioField the radio field
     * @return the RenderableFtl
     */
    public RenderableFtl radioField(final Map<String, Object> context, final ModelFormField.RadioField radioField) {
        final ModelFormField modelFormField = radioField.getModelFormField();

        final List<Map<String, String>> items = new ArrayList<>();
        for (final ModelFormField.OptionValue optionValue : radioField.getAllOptionValues(context, WidgetWorker.getDelegator(context))) {
            items.add(Map.of(
                    "key", nonNull(optionValue.getKey()),
                    "description", nonNull(encode(optionValue.getDescription(), modelFormField, context))));
        }

        final var builder = RenderableFtlMacroCall.builder().name("renderRadioField");
        applyWidgetStyleAndAlert(modelFormField, context, builder);
        return builder
                .objectParameter("items", items)
                .stringParameter("currentValue", nonNull(modelFormField.getEntry(context)))
                .stringParameter("noCurrentSelectedKey", nonNull(radioField.getNoCurrentSelectedKey(context)))
                .stringParameter("name", nonNull(modelFormField.getParameterName(context)))
                .stringParameter("event", nonNull(modelFormField.getEvent()))
                .stringParameter("action", nonNull(modelFormField.getAction(context)))
                .stringParameter("conditionGroup", nonNull(modelFormField.getConditionGroup()))
                .stringParameter("tabindex", nonNull(modelFormField.getTabindex()))
                .booleanParameter("disabled", modelFormField.getDisabled(context))
                .build();
    }

    /**
     * Creates the macro call that renders a submit button.
     * @param context the context
     * @param submitField the submit field
     * @param javaScriptEnabled whether JavaScript is enabled in the browser of the user
     * @return the RenderableFtl
     */
    public RenderableFtl submitField(final Map<String, Object> context, final ModelFormField.SubmitField submitField,
                                     final boolean javaScriptEnabled) {
        final ModelFormField modelFormField = submitField.getModelFormField();
        final ModelForm modelForm = modelFormField.getModelForm();
        final String formId = FormRenderer.getCurrentContainerId(modelForm, context);

        List<ModelForm.UpdateArea> updateAreas = new ArrayList<>();
        final List<ModelForm.UpdateArea> onSubmitUpdateAreas = modelForm.getOnSubmitUpdateAreas();
        if (UtilValidate.isNotEmpty(onSubmitUpdateAreas)) {
            updateAreas.addAll(onSubmitUpdateAreas);
        }

        // Retrieve on click event for submit field
        final List<ModelForm.UpdateArea> onClickUpdateAreas = modelFormField.getOnClickUpdateAreas();
        if (UtilValidate.isNotEmpty(onClickUpdateAreas)) {
            updateAreas.addAll(onClickUpdateAreas);
        }

        // This is here for backwards compatibility. Use on-event-update-area elements instead.
        final String backgroundSubmitRefreshTarget = submitField.getBackgroundSubmitRefreshTarget(context);
        final ModelForm.UpdateArea jwtCallback = ModelForm.UpdateArea.fromJwtToken(context);
        if (UtilValidate.isNotEmpty(backgroundSubmitRefreshTarget)) {
            updateAreas.add(ModelForm.UpdateArea.literal("submit", formId, backgroundSubmitRefreshTarget));
        }

        // In context a callback is present and no other update area to call after the submit, so trigger it.
        if (UtilValidate.isEmpty(updateAreas) && jwtCallback != null && !submitField.getPropagateCallback()) {
            updateAreas = UtilMisc.toList(jwtCallback);
        }
        final boolean ajaxEnabled = UtilValidate.isNotEmpty(updateAreas) && javaScriptEnabled;
        String ajaxUrl = "";
        if (ajaxEnabled) {
            final Map<String, Object> extraParams = CommonWidgetModels.propagateCallbackInParameterMap(context,
                    submitField.getPropagateCallback(), jwtCallback);
            ajaxUrl = MacroCommonRenderer.createAjaxParamsFromUpdateAreas(updateAreas, extraParams, modelForm, "", context);
        }

        final var builder = RenderableFtlMacroCall.builder().name("renderSubmitField");
        applyWidgetStyleAndAlert(modelFormField, context, builder);
        return builder
                .stringParameter("buttonType", nonNull(submitField.getButtonType()))
                .stringParameter("formName", nonNull(FormRenderer.getCurrentFormName(modelForm, context)))
                .stringParameter("title", nonNull(encode(modelFormField.getTitle(context), modelFormField, context)))
                .stringParameter("name", nonNull(modelFormField.getParameterName(context)))
                .stringParameter("id", nonNull(modelFormField.getCurrentContainerId(context)))
                .stringParameter("event", nonNull(modelFormField.getEvent()))
                .stringParameter("action", nonNull(modelFormField.getAction(context)))
                .stringParameter("imgSrc", nonNull(encode(submitField.getImageLocation(context), modelFormField, context)))
                .stringParameter("containerId", ajaxEnabled ? nonNull(formId) : "")
                .stringParameter("confirmation", nonNull(submitField.getConfirmation(context)))
                .stringParameter("ajaxUrl", ajaxEnabled ? nonNull(ajaxUrl) : "")
                .stringParameter("tabindex", nonNull(modelFormField.getTabindex()))
                .booleanParameter("disabled", modelFormField.getDisabled(context))
                .stringParameter("closeOnSubmit", String.valueOf(!submitField.getPropagateCallback()))
                .build();
    }

    /**
     * Creates the macro call that renders a reset button.
     * @param context the context
     * @param resetField the reset field
     * @return the RenderableFtl
     */
    public RenderableFtl resetField(final Map<String, Object> context, final ModelFormField.ResetField resetField) {
        final ModelFormField modelFormField = resetField.getModelFormField();
        final var builder = RenderableFtlMacroCall.builder().name("renderResetField");
        applyWidgetStyleAndAlert(modelFormField, context, builder);
        return builder
                .stringParameter("name", nonNull(modelFormField.getParameterName(context)))
                .stringParameter("title", nonNull(modelFormField.getTitle(context)))
                .build();
    }

    /**
     * Creates the macro call that renders a hidden field.
     * @param context the context
     * @param modelFormField the form field
     * @param value the value of the field, which may contain the quotes escaped for a FreeMarker string literal
     * @return the RenderableFtl
     */
    public RenderableFtl hiddenField(final Map<String, Object> context, final ModelFormField modelFormField, final String value) {
        return RenderableFtlMacroCall.builder()
                .name("renderHiddenField")
                .stringParameter("name", nonNull(modelFormField.getParameterName(context)))
                .stringParameter("conditionGroup", nonNull(modelFormField.getConditionGroup()))
                // the form fields escape the double quotes with a backslash because the value was a FreeMarker string literal
                .stringParameter("value", nonNull(value).replace("\\\"", "\""))
                .stringParameter("id", nonNull(modelFormField.getCurrentContainerId(context)))
                .stringParameter("event", nonNull(modelFormField.getEvent()))
                .stringParameter("action", nonNull(modelFormField.getAction(context)))
                .booleanParameter("disabled", modelFormField.getDisabled(context))
                .build();
    }

    /**
     * Creates the macro call that renders an empty field title.
     * @return the RenderableFtl
     */
    public RenderableFtl formatEmptySpace() {
        return RenderableFtlMacroCall.builder().name("renderFormatEmptySpace").build();
    }

    /**
     * Creates the macro call that renders a field title made of a link or of a select all check box.
     * @param formName the name of the form
     * @param title the title, which can contain HTML
     * @param showSelectAll true to show the select all check box
     * @return the RenderableFtl
     */
    public RenderableFtl hyperlinkTitle(final String formName, final String title, final boolean showSelectAll) {
        final var builder = RenderableFtlMacroCall.builder()
                .name("renderHyperlinkTitle")
                .stringParameter("name", nonNull(formName))
                .stringParameter("title", nonNull(title));
        if (showSelectAll) {
            builder.stringParameter("showSelectAll", "Y");
        }
        return builder.build();
    }

    /**
     * Creates the macro call that renders a field title.
     * @param style the style of the title
     * @param title the title
     * @param id the id of the field, or empty if the title has no id and does not refer to a field
     * @param fieldHelpText the help text of the field, or null if there is none to show
     * @return the RenderableFtl
     */
    public RenderableFtl fieldTitle(final String style, final String title, final String id, final String fieldHelpText) {
        final var builder = RenderableFtlMacroCall.builder()
                .name("renderFieldTitle")
                .stringParameter("style", nonNull(style))
                .stringParameter("title", nonNull(title));
        if (fieldHelpText != null) {
            builder.stringParameter("fieldHelpText", fieldHelpText);
        }
        if (UtilValidate.isNotEmpty(id)) {
            builder.stringParameter("id", id + "_title").stringParameter("for", id);
        }
        return builder.build();
    }

    /**
     * Creates the macro call that renders a field title which sorts the list when clicked.
     * @param style the style of the link
     * @param title the title
     * @param linkUrl the url of the link
     * @param ajaxEnabled true if the link updates areas of the page
     * @param tooltip the tooltip, or empty if there is none
     * @return the RenderableFtl
     */
    public RenderableFtl sortField(final String style, final String title, final String linkUrl, final boolean ajaxEnabled,
                                   final String tooltip) {
        final var builder = RenderableFtlMacroCall.builder()
                .name("renderSortField")
                .stringParameter("style", nonNull(style))
                .stringParameter("title", nonNull(title))
                .stringParameter("linkUrl", nonNull(linkUrl))
                .booleanParameter("ajaxEnabled", ajaxEnabled);
        if (UtilValidate.isNotEmpty(tooltip)) {
            builder.stringParameter("tooltip", tooltip);
        }
        return builder.build();
    }

    /**
     * Creates the macro call that opens a form.
     * @param context the context
     * @param modelForm the form
     * @return the RenderableFtl
     */
    public RenderableFtl formOpen(final Map<String, Object> context, final ModelForm modelForm) {
        final String targetType = modelForm.getTargetType();
        final String target = modelForm.getTarget(context, targetType);
        String linkUrl = "";
        if (UtilValidate.isNotEmpty(target)) {
            linkUrl = WidgetWorker.buildHyperlinkUri(target, targetType, null, null, false, false, true,
                    request, response).toString();
        }
        String hasRequiredField = "";
        for (final ModelFormField formField : modelForm.getFieldList()) {
            if (formField.getRequiredField()) {
                hasRequiredField = "Y";
                break;
            }
        }
        // Generate CSRF name & value for form
        final String csrfNameValue = CsrfUtil.getTokenNameNonAjax() + " " + CsrfUtil.generateTokenForNonAjax(request, target);
        return RenderableFtlMacroCall.builder()
                .name("renderFormOpen")
                .stringParameter("linkUrl", linkUrl)
                .stringParameter("formType", nonNull(modelForm.getType()))
                .stringParameter("targetWindow", nonNull(modelForm.getTargetWindow(context)))
                .stringParameter("containerId", nonNull(FormRenderer.getCurrentContainerId(modelForm, context)))
                .stringParameter("containerStyle", nonNull(modelForm.getContainerStyle()))
                .stringParameter("autocomplete", modelForm.getClientAutocompleteFields() ? "" : "off")
                .stringParameter("name", nonNull(FormRenderer.getCurrentFormName(modelForm, context)))
                .stringParameter("focusFieldName", nonNull(modelForm.getFocusFieldName()))
                .stringParameter("hasRequiredField", hasRequiredField)
                .stringParameter("viewIndexField", nonNull(modelForm.getMultiPaginateIndexField(context)))
                .stringParameter("viewSizeField", nonNull(modelForm.getMultiPaginateSizeField(context)))
                .stringParameter("viewIndex", Integer.toString(Paginator.getViewIndex(modelForm, context)))
                .stringParameter("viewSize", Integer.toString(Paginator.getViewSize(modelForm, context)))
                .booleanParameter("useRowSubmit", modelForm.getUseRowSubmit())
                .stringParameter("csrfNameValue", csrfNameValue)
                .build();
    }

    /**
     * Creates the macro call that closes a form.
     * @return the RenderableFtl
     */
    public RenderableFtl formClose() {
        return macroWithoutParameters("renderFormClose");
    }

    /**
     * Creates the macro call that closes a multi form.
     * @return the RenderableFtl
     */
    public RenderableFtl multiFormClose() {
        return macroWithoutParameters("renderMultiFormClose");
    }

    /**
     * Gets what has to be rendered after the closing of a multi form, such as the forms of the hidden form links.
     * @param context the context
     * @return the RenderableFtl, which renders nothing if there is none
     */
    public RenderableFtl postMultiFormContent(final Map<String, Object> context) {
        final Map<String, Object> wholeFormContext = UtilGenerics.cast(context.get("wholeFormContext"));
        final RenderableFtl postMultiFormContent = wholeFormContext == null ? null
                : (RenderableFtl) wholeFormContext.get("postMultiFormRenderableFtl");
        return postMultiFormContent == null ? RenderableFtlNoop.INSTANCE : postMultiFormContent;
    }

    /**
     * Creates the macro call that opens the wrapper of a list form.
     * @param context the context
     * @param modelForm the form
     * @return the RenderableFtl
     */
    public RenderableFtl formatListWrapperOpen(final Map<String, Object> context, final ModelForm modelForm) {
        final List<String> columnStyleList = new ArrayList<>();
        final List<String> fieldNameList = new ArrayList<>();
        for (final ModelFormField childField : modelForm.getFieldList()) {
            final int childFieldType = childField.getFieldInfo().getFieldType();
            if (childFieldType == FieldInfo.HIDDEN || childFieldType == FieldInfo.IGNORED) {
                continue;
            }
            final String areaStyle = nonNull(childField.getTitleAreaStyle());
            if (fieldNameList.contains(childField.getName())) {
                if (UtilValidate.isNotEmpty(areaStyle)) {
                    columnStyleList.set(fieldNameList.indexOf(childField.getName()), areaStyle);
                }
            } else {
                columnStyleList.add(areaStyle);
                fieldNameList.add(childField.getName());
            }
        }
        return RenderableFtlMacroCall.builder()
                .name("renderFormatListWrapperOpen")
                .stringParameter("formName", nonNull(modelForm.getName()))
                .stringParameter("style", FlexibleStringExpander.expandString(modelForm.getDefaultTableStyle(), context))
                .objectParameter("columnStyles", columnStyleList)
                .build();
    }

    /**
     * Creates the macro call that renders the message of a form without data.
     * @param context the context
     * @param modelForm the form
     * @return the RenderableFtl
     */
    public RenderableFtl emptyFormDataMessage(final Map<String, Object> context, final ModelForm modelForm) {
        return RenderableFtlMacroCall.builder()
                .name("renderEmptyFormDataMessage")
                .stringParameter("message", nonNull(modelForm.getEmptyFormDataMessage(context)))
                .build();
    }

    /**
     * Creates the macro call that closes the wrapper of a list form.
     * @param modelForm the form
     * @return the RenderableFtl
     */
    public RenderableFtl formatListWrapperClose(final ModelForm modelForm) {
        return RenderableFtlMacroCall.builder()
                .name("renderFormatListWrapperClose")
                .stringParameter("formName", nonNull(modelForm.getName()))
                .build();
    }

    public RenderableFtl formatHeaderOpen() {
        return macroWithoutParameters("renderFormatHeaderOpen");
    }

    public RenderableFtl formatHeaderClose() {
        return macroWithoutParameters("renderFormatHeaderClose");
    }

    public RenderableFtl formatHeaderRowOpen(final Map<String, Object> context, final ModelForm modelForm) {
        return RenderableFtlMacroCall.builder()
                .name("renderFormatHeaderRowOpen")
                .stringParameter("style", FlexibleStringExpander.expandString(modelForm.getHeaderRowStyle(), context))
                .build();
    }

    public RenderableFtl formatHeaderRowClose() {
        return macroWithoutParameters("renderFormatHeaderRowClose");
    }

    public RenderableFtl formatHeaderRowCellOpen(final ModelFormField modelFormField, final int positionSpan) {
        return RenderableFtlMacroCall.builder()
                .name("renderFormatHeaderRowCellOpen")
                .stringParameter("style", nonNull(modelFormField.getTitleAreaStyle()))
                .intParameter("positionSpan", positionSpan)
                .build();
    }

    public RenderableFtl formatHeaderRowCellClose() {
        return macroWithoutParameters("renderFormatHeaderRowCellClose");
    }

    public RenderableFtl formatHeaderRowFormCellOpen(final ModelForm modelForm) {
        return RenderableFtlMacroCall.builder()
                .name("renderFormatHeaderRowFormCellOpen")
                .stringParameter("style", nonNull(modelForm.getFormTitleAreaStyle()))
                .build();
    }

    public RenderableFtl formatHeaderRowFormCellClose() {
        return macroWithoutParameters("renderFormatHeaderRowFormCellClose");
    }

    public RenderableFtl formatHeaderRowFormCellTitleSeparator(final ModelFormField modelFormField, final boolean isLast) {
        return RenderableFtlMacroCall.builder()
                .name("renderFormatHeaderRowFormCellTitleSeparator")
                .stringParameter("style", nonNull(modelFormField.getTitleStyle()))
                .booleanParameter("isLast", isLast)
                .build();
    }

    public RenderableFtl formatItemRowOpen(final Map<String, Object> context, final ModelForm modelForm) {
        final Integer itemIndex = (Integer) context.get("itemIndex");
        String altRowStyles = "";
        String evenRowStyle = "";
        String oddRowStyle = "";
        if (itemIndex != null) {
            altRowStyles = modelForm.getStyleAltRowStyle(context);
            if (itemIndex % 2 == 0) {
                evenRowStyle = modelForm.getEvenRowStyle();
            } else {
                oddRowStyle = FlexibleStringExpander.expandString(modelForm.getOddRowStyle(), context);
            }
        }
        final RenderableFtlMacroCallBuilder builder = RenderableFtlMacroCall.builder()
                .name("renderFormatItemRowOpen")
                .stringParameter("formName", nonNull(modelForm.getName()))
                .stringParameter("altRowStyles", nonNull(altRowStyles))
                .stringParameter("evenRowStyle", nonNull(evenRowStyle))
                .stringParameter("oddRowStyle", nonNull(oddRowStyle));
        if (itemIndex != null) {
            builder.intParameter("itemIndex", itemIndex);
        }
        return builder.build();
    }

    public RenderableFtl formatItemRowClose(final ModelForm modelForm) {
        return RenderableFtlMacroCall.builder()
                .name("renderFormatItemRowClose")
                .stringParameter("formName", nonNull(modelForm.getName()))
                .build();
    }

    public RenderableFtl formatItemRowCellOpen(final ModelFormField modelFormField, final int positionSpan) {
        return RenderableFtlMacroCall.builder()
                .name("renderFormatItemRowCellOpen")
                .stringParameter("fieldName", nonNull(modelFormField.getName()))
                .stringParameter("style", nonNull(modelFormField.getWidgetAreaStyle()))
                .intParameter("positionSpan", positionSpan)
                .build();
    }

    public RenderableFtl formatItemRowCellClose(final ModelFormField modelFormField) {
        return RenderableFtlMacroCall.builder()
                .name("renderFormatItemRowCellClose")
                .stringParameter("fieldName", nonNull(modelFormField.getName()))
                .build();
    }

    public RenderableFtl formatItemRowFormCellOpen(final ModelForm modelForm) {
        return RenderableFtlMacroCall.builder()
                .name("renderFormatItemRowFormCellOpen")
                .stringParameter("style", nonNull(modelForm.getFormTitleAreaStyle()))
                .build();
    }

    public RenderableFtl formatItemRowFormCellClose() {
        return macroWithoutParameters("renderFormatItemRowFormCellClose");
    }

    public RenderableFtl formatSingleWrapperOpen(final Map<String, Object> context, final ModelForm modelForm) {
        return RenderableFtlMacroCall.builder()
                .name("renderFormatSingleWrapperOpen")
                .stringParameter("formName", nonNull(modelForm.getName()))
                .stringParameter("style", FlexibleStringExpander.expandString(modelForm.getDefaultTableStyle(), context))
                .build();
    }

    public RenderableFtl formatSingleWrapperClose(final ModelForm modelForm) {
        return RenderableFtlMacroCall.builder()
                .name("renderFormatSingleWrapperClose")
                .stringParameter("formName", nonNull(modelForm.getName()))
                .build();
    }

    public RenderableFtl formatFieldRowOpen() {
        return macroWithoutParameters("renderFormatFieldRowOpen");
    }

    public RenderableFtl formatFieldRowClose() {
        return macroWithoutParameters("renderFormatFieldRowClose");
    }

    public RenderableFtl formatFieldRowTitleCellOpen(final ModelFormField modelFormField) {
        return RenderableFtlMacroCall.builder()
                .name("renderFormatFieldRowTitleCellOpen")
                .stringParameter("style", nonNull(modelFormField.getTitleAreaStyle()))
                .build();
    }

    public RenderableFtl formatFieldRowTitleCellClose() {
        return macroWithoutParameters("renderFormatFieldRowTitleCellClose");
    }

    public RenderableFtl formatFieldRowWidgetCellOpen(final ModelFormField modelFormField, final int positionSpan) {
        return RenderableFtlMacroCall.builder()
                .name("renderFormatFieldRowWidgetCellOpen")
                .intParameter("positionSpan", positionSpan)
                .stringParameter("style", nonNull(modelFormField.getWidgetAreaStyle()))
                .build();
    }

    public RenderableFtl formatFieldRowWidgetCellClose() {
        return macroWithoutParameters("renderFormatFieldRowWidgetCellClose");
    }

    /**
     * Creates the macro call that renders a boundary comment.
     * @param boundaryType Begin or End
     * @param widgetType the type of the widget, such as Form Widget - Form Element
     * @param widgetName the name of the widget
     * @return the RenderableFtl
     */
    public RenderableFtl boundaryComment(final String boundaryType, final String widgetType, final String widgetName) {
        return RenderableFtlMacroCall.builder()
                .name("formatBoundaryComment")
                .stringParameter("boundaryType", boundaryType)
                .stringParameter("widgetType", widgetType)
                .stringParameter("widgetName", nonNull(widgetName))
                .build();
    }

    /**
     * Creates the macro call that renders a text find field.
     * @param context the context
     * @param textFindField the text find field
     * @return the RenderableFtl
     */
    public RenderableFtl textFindField(final Map<String, Object> context, final ModelFormField.TextFindField textFindField) {
        final ModelFormField modelFormField = textFindField.getModelFormField();
        final Locale locale = (Locale) context.get("locale");
        String opEquals = "";
        String opBeginsWith = "";
        String opContains = "";
        String opIsEmpty = "";
        String opNotEqual = "";
        if (!textFindField.getHideOptions()) {
            opEquals = UtilProperties.getMessage("conditionalUiLabels", "equals", locale);
            opBeginsWith = UtilProperties.getMessage("conditionalUiLabels", "begins_with", locale);
            opContains = UtilProperties.getMessage("conditionalUiLabels", "contains", locale);
            opIsEmpty = UtilProperties.getMessage("conditionalUiLabels", "is_empty", locale);
            opNotEqual = UtilProperties.getMessage("conditionalUiLabels", "not_equal", locale);
        }
        final String value = modelFormField.getEntry(context, textFindField.getDefaultValue(context));
        final var builder = RenderableFtlMacroCall.builder().name("renderTextFindField");
        applyWidgetStyleAndAlert(modelFormField, context, builder);
        return builder
                .stringParameter("name", nonNull(modelFormField.getParameterName(context)))
                .stringParameter("value", nonNull(value))
                .stringParameter("defaultOption", nonNull(textFindField.getDefaultOption(context)))
                .stringParameter("opEquals", nonNull(opEquals))
                .stringParameter("opBeginsWith", nonNull(opBeginsWith))
                .stringParameter("opContains", nonNull(opContains))
                .stringParameter("opIsEmpty", nonNull(opIsEmpty))
                .stringParameter("opNotEqual", nonNull(opNotEqual))
                .stringParameter("size", Integer.toString(textFindField.getSize()))
                .stringParameter("maxlength", textFindField.getMaxlength() == null ? "" : textFindField.getMaxlength().toString())
                .stringParameter("autocomplete", textFindField.getClientAutocompleteField() ? "" : "off")
                .stringParameter("titleStyle", nonNull(modelFormField.getTitleStyle()))
                .booleanParameter("hideIgnoreCase", textFindField.getHideIgnoreCase())
                .booleanParameter("ignCase", textFindField.getIgnoreCase(context))
                .stringParameter("ignoreCase", nonNull(UtilProperties.getMessage("conditionalUiLabels", "ignore_case", locale)))
                .stringParameter("tabindex", nonNull(modelFormField.getTabindex()))
                .stringParameter("conditionGroup", nonNull(modelFormField.getConditionGroup()))
                .booleanParameter("disabled", modelFormField.getDisabled(context))
                .build();
    }

    /**
     * Creates the macro call that renders a range find field.
     * @param context the context
     * @param rangeFindField the range find field
     * @return the RenderableFtl
     */
    public RenderableFtl rangeFindField(final Map<String, Object> context, final ModelFormField.RangeFindField rangeFindField) {
        final ModelFormField modelFormField = rangeFindField.getModelFormField();
        final Locale locale = (Locale) context.get("locale");
        final String value = modelFormField.getEntry(context, rangeFindField.getDefaultValue(context));
        final String value2 = modelFormField.getEntry(context);
        final Integer maxlength = rangeFindField.getMaxlength();
        final var builder = RenderableFtlMacroCall.builder().name("renderRangeFindField");
        applyWidgetStyleAndAlert(modelFormField, context, builder);
        return builder
                .stringParameter("name", nonNull(modelFormField.getParameterName(context)))
                .stringParameter("value", nonNull(value))
                .stringParameter("size", Integer.toString(rangeFindField.getSize()))
                .stringParameter("maxlength", maxlength == null ? "" : Integer.toString(maxlength))
                .stringParameter("autocomplete", rangeFindField.getClientAutocompleteField() ? "" : "off")
                .stringParameter("titleStyle", nonNull(modelFormField.getTitleStyle()))
                .stringParameter("defaultOptionFrom", nonNull(rangeFindField.getDefaultOptionFrom()))
                .stringParameter("opEquals", nonNull(UtilProperties.getMessage("conditionalUiLabels", "equals", locale)))
                .stringParameter("opGreaterThan", nonNull(UtilProperties.getMessage("conditionalUiLabels", "greater_than", locale)))
                .stringParameter("opGreaterThanEquals",
                        nonNull(UtilProperties.getMessage("conditionalUiLabels", "greater_than_equals", locale)))
                .stringParameter("opLessThan", nonNull(UtilProperties.getMessage("conditionalUiLabels", "less_than", locale)))
                .stringParameter("opLessThanEquals", nonNull(UtilProperties.getMessage("conditionalUiLabels", "less_than_equals", locale)))
                .stringParameter("value2", nonNull(value2))
                .stringParameter("defaultOptionThru", nonNull(rangeFindField.getDefaultOptionThru()))
                .stringParameter("conditionGroup", nonNull(modelFormField.getConditionGroup()))
                .stringParameter("tabindex", nonNull(modelFormField.getTabindex()))
                .booleanParameter("disabled", modelFormField.getDisabled(context))
                .build();
    }

    /**
     * Creates the macro call that renders a lookup field.
     * @param context the context
     * @param lookupField the lookup field
     * @param javaScriptEnabled whether JavaScript is enabled in the browser of the user
     * @return the RenderableFtl
     * @throws UnsupportedEncodingException if the last view name cannot be encoded
     */
    public RenderableFtl lookupField(final Map<String, Object> context, final ModelFormField.LookupField lookupField,
                                     final boolean javaScriptEnabled) throws UnsupportedEncodingException {
        final ModelFormField modelFormField = lookupField.getModelFormField();
        final String lookupFieldFormName = lookupField.getFormName(context);
        final String id = modelFormField.getCurrentContainerId(context);
        List<ModelForm.UpdateArea> updateAreas = modelFormField.getOnChangeUpdateAreas();
        //add default ajax auto completer to all lookup fields
        if (UtilValidate.isEmpty(updateAreas) && UtilValidate.isNotEmpty(lookupFieldFormName)) {
            final String autoCompleterTarget = (lookupFieldFormName.indexOf('?') == -1 ? lookupFieldFormName + "?"
                    : lookupFieldFormName + "&amp;amp;") + "ajaxLookup=Y";
            updateAreas = new ArrayList<>();
            updateAreas.add(ModelForm.UpdateArea.literal("change", id, autoCompleterTarget));
        }
        final boolean ajaxEnabled = UtilValidate.isNotEmpty(updateAreas) && javaScriptEnabled;
        final ModelForm modelForm = modelFormField.getModelForm();
        String formName = modelFormField.getParentFormName();
        if (UtilValidate.isEmpty(formName)) {
            formName = FormRenderer.getCurrentFormName(modelForm, context);
        }
        final String ajaxUrl = ajaxEnabled ? MacroCommonRenderer.createAjaxParamsFromUpdateAreas(updateAreas, null, modelForm, "", context) : "";
        String clearText = "";
        final Map<String, Object> uiLabelMap = UtilGenerics.cast(context.get("uiLabelMap"));
        if (uiLabelMap != null) {
            clearText = (String) uiLabelMap.get("CommonClear");
        } else {
            Debug.logWarning("Could not find uiLabelMap in context", MODULE);
        }
        Boolean showDescription = lookupField.getShowDescription();
        if (showDescription == null) {
            showDescription = "Y".equals(visualTheme.getModelTheme().getLookupShowDescription());
        }
        // lastViewName, used by lookup to remember the real last view name; read only from session (set by RequestHandler) to prevent user input
        String lastViewName = (String) request.getSession().getAttribute("_LAST_VIEW_NAME_");
        if (UtilValidate.isEmpty(lastViewName)) {
            lastViewName = "";
        }
        lastViewName = UtilHttp.getEncodedParameter(lastViewName);

        String className = nonNull(modelFormField.getWidgetStyle());
        final String alert = UtilValidate.isNotEmpty(className) && modelFormField.shouldBeRed(context) ? "true" : "false";
        className = withRequiredStyle(modelFormField, className);
        final Integer maxlength = lookupField.getMaxlength();
        final String fadeBackground = lookupField.getFadeBackground();
        final var builder = RenderableFtlMacroCall.builder()
                .name("renderLookupField")
                .stringParameter("className", className)
                .stringParameter("alert", alert)
                .stringParameter("name", nonNull(modelFormField.getParameterName(context)))
                .stringParameter("value", nonNull(modelFormField.getEntry(context, lookupField.getDefaultValue(context))))
                .stringParameter("size", Integer.toString(lookupField.getSize()))
                .stringParameter("maxlength", maxlength == null ? "" : Integer.toString(maxlength))
                .stringParameter("id", nonNull(id))
                .stringParameter("event", nonNull(modelFormField.getEvent()))
                .stringParameter("action", nonNull(modelFormField.getAction(context)))
                .booleanParameter("readonly", lookupField.getReadonly())
                .stringParameter("autocomplete", !lookupField.getClientAutocompleteField() || !ajaxEnabled ? "off" : "")
                .stringParameter("descriptionFieldName", nonNull(lookupField.getDescriptionFieldName()))
                .stringParameter("formName", nonNull(formName))
                .stringParameter("fieldFormName", nonNull(lookupFieldFormName))
                .objectParameter("targetParameterIter", new ArrayList<>(lookupField.getTargetParameterList(context)))
                .stringParameter("imgSrc", pathAsContentUrl("/images/fieldlookup.gif"))
                .stringParameter("ajaxUrl", ajaxUrl)
                .booleanParameter("ajaxEnabled", ajaxEnabled)
                .stringParameter("presentation", nonNull(lookupField.getLookupPresentation()));
        if (UtilValidate.isNotEmpty(lookupField.getLookupHeight())) {
            builder.stringParameter("height", lookupField.getLookupHeight());
        }
        if (UtilValidate.isNotEmpty(lookupField.getLookupWidth())) {
            builder.stringParameter("width", lookupField.getLookupWidth());
        }
        if (UtilValidate.isNotEmpty(lookupField.getLookupPosition())) {
            builder.stringParameter("position", lookupField.getLookupPosition());
        }
        return builder
                .stringParameter("fadeBackground", UtilValidate.isEmpty(fadeBackground) ? "false" : fadeBackground)
                .stringParameter("clearText", nonNull(clearText))
                .stringParameter("showDescription", Boolean.toString(showDescription))
                .stringParameter("initiallyCollapsed", Boolean.toString(lookupField.getInitiallyCollapsed()))
                .stringParameter("lastViewName", lastViewName)
                .stringParameter("conditionGroup", nonNull(modelFormField.getConditionGroup()))
                .stringParameter("tabindex", nonNull(modelFormField.getTabindex()))
                .booleanParameter("disabled", modelFormField.getDisabled(context))
                .stringParameter("delegatorName", ((HttpSession) context.get("session")).getAttribute("delegatorName").toString())
                .build();
    }

    /**
     * Creates the macro call that renders the pagination of a list form.
     * @param context the context
     * @param modelForm the form
     * @param javaScriptEnabled whether JavaScript is enabled in the browser of the user
     * @return the RenderableFtl, which renders nothing if the form cannot paginate
     */
    public RenderableFtl nextPrev(final Map<String, Object> context, final ModelForm modelForm, final boolean javaScriptEnabled) {
        boolean ajaxEnabled = false;
        final List<ModelForm.UpdateArea> updateAreas = modelForm.getOnPaginateUpdateAreas();
        String targetService = modelForm.getPaginateTarget(context);
        if (javaScriptEnabled && UtilValidate.isNotEmpty(updateAreas)) {
            ajaxEnabled = true;
        }
        if (targetService == null) {
            targetService = "${targetService}";
        }
        if (UtilValidate.isEmpty(targetService) && updateAreas == null) {
            Debug.logWarning("Cannot paginate because TargetService is empty for the form: " + modelForm.getName(), MODULE);
            return RenderableFtlNoop.INSTANCE;
        }
        // get the parameterized pagination index and size fields
        final int paginatorNumber = WidgetWorker.getPaginatorNumber(context);
        String viewIndexParam = modelForm.getMultiPaginateIndexField(context);
        String viewSizeParam = modelForm.getMultiPaginateSizeField(context);
        final int viewIndex = Paginator.getViewIndex(modelForm, context);
        final int viewSize = Paginator.getViewSize(modelForm, context);
        final int listSize = Paginator.getListSize(context);
        final int lowIndex = Paginator.getLowIndex(context);
        final int highIndex = Paginator.getHighIndex(context);
        final int actualPageSize = Paginator.getActualPageSize(context);
        // needed for the "Page" and "rows" labels
        final Map<String, String> uiLabelMap = UtilGenerics.cast(context.get("uiLabelMap"));
        String pageLabel = "";
        String commonDisplaying = "";
        if (uiLabelMap == null) {
            Debug.logWarning("Could not find uiLabelMap in context", MODULE);
        } else {
            pageLabel = uiLabelMap.get("CommonPage");
            final Map<String, Integer> messageMap = UtilMisc.toMap("lowCount", lowIndex + 1, "highCount", lowIndex + actualPageSize,
                    "total", listSize);
            commonDisplaying = UtilProperties.getMessage("CommonUiLabels", "CommonDisplaying", messageMap, (Locale) context.get("locale"));
        }
        // for legacy support, the viewSizeParam is VIEW_SIZE and viewIndexParam is VIEW_INDEX when the fields are "viewSize" and "viewIndex"
        if (("viewIndex" + "_" + paginatorNumber).equals(viewIndexParam)) {
            viewIndexParam = "VIEW_INDEX" + "_" + paginatorNumber;
        }
        if (("viewSize" + "_" + paginatorNumber).equals(viewSizeParam)) {
            viewSizeParam = "VIEW_SIZE" + "_" + paginatorNumber;
        }
        String str = (String) context.get("_QBESTRING_");

        // refresh any csrf token in the query string for pagination
        final String tokenValue = CsrfUtil.generateTokenForNonAjax(request, targetService);
        str = CsrfUtil.addOrUpdateTokenInQueryString(str, tokenValue);

        // strip legacy viewIndex/viewSize params from the query string
        String queryString = UtilHttp.stripViewParamsFromQueryString(str, "" + paginatorNumber);
        // strip parameterized index/size params from the query string
        final Set<String> paramNames = new HashSet<>();
        paramNames.add(viewIndexParam);
        paramNames.add(viewSizeParam);
        queryString = UtilHttp.stripNamedParamsFromQueryString(queryString, paramNames);
        String anchor = "";
        final String paginateAnchor = modelForm.getPaginateTargetAnchor();
        if (UtilValidate.isNotEmpty(paginateAnchor)) {
            anchor = "#" + paginateAnchor;
        }
        // Create separate url path String and request parameters String,
        // add viewIndex/viewSize parameters to request parameter String
        final String urlPath = UtilHttp.removeQueryStringFromTarget(targetService);
        String prepLinkText = UtilHttp.getQueryStringFromTarget(targetService);
        if (UtilValidate.isNotEmpty(queryString)) {
            queryString = UtilHttp.encodeAmpersands(queryString);
        }
        if (prepLinkText.indexOf('?') < 0) {
            prepLinkText += "?";
        } else if (!prepLinkText.endsWith("?")) {
            prepLinkText += "&amp;";
        }
        if (UtilValidate.isNotEmpty(queryString) && !"null".equals(queryString)) {
            prepLinkText += queryString + "&amp;";
        }
        final String prepLinkSizeText = prepLinkText + viewSizeParam + "='+this.value+'" + "&amp;" + viewIndexParam + "=0";
        prepLinkText += viewSizeParam + "=" + viewSize + "&amp;" + viewIndexParam + "=";
        if (ajaxEnabled) {
            // Prepare params for prototype.js
            prepLinkText = prepLinkText.replace("?", "");
            prepLinkText = prepLinkText.replace("&amp;", "&");
        }
        String firstUrl = "";
        String ajaxFirstUrl = "";
        String previousUrl = "";
        String ajaxPreviousUrl = "";
        String selectUrl = "";
        String ajaxSelectUrl = "";
        String selectSizeUrl = "";
        String ajaxSelectSizeUrl = "";
        String nextUrl = "";
        String ajaxNextUrl = "";
        String lastUrl = "";
        String ajaxLastUrl = "";
        if (viewIndex > 0) {
            if (ajaxEnabled) {
                ajaxFirstUrl = MacroCommonRenderer.createAjaxParamsFromUpdateAreas(updateAreas, null, modelForm, prepLinkText + 0 + anchor, context);
                ajaxPreviousUrl = MacroCommonRenderer.createAjaxParamsFromUpdateAreas(updateAreas, null, modelForm,
                        prepLinkText + (viewIndex - 1) + anchor, context);
            } else {
                firstUrl = requestHandler.makeLink(request, response, urlPath + prepLinkText + 0 + anchor);
                previousUrl = requestHandler.makeLink(request, response, urlPath + prepLinkText + (viewIndex - 1) + anchor);
            }
        }
        // Page select dropdown
        if (listSize > 0 && javaScriptEnabled) {
            if (ajaxEnabled) {
                ajaxSelectUrl = MacroCommonRenderer.createAjaxParamsFromUpdateAreas(updateAreas, null, modelForm,
                        prepLinkText + "' + (this.value - 1) + '", context);
            } else {
                final String linkText = prepLinkText.startsWith("/") ? prepLinkText.substring(1) : prepLinkText;
                selectUrl = requestHandler.makeLink(request, response, urlPath + linkText);
            }
        }
        if (highIndex < listSize) {
            // Next button and last button
            final int lastIndex = UtilMisc.getViewLastIndex(listSize, viewSize);
            if (ajaxEnabled) {
                ajaxNextUrl = MacroCommonRenderer.createAjaxParamsFromUpdateAreas(updateAreas, null,
                        modelForm, prepLinkText + (viewIndex + 1) + anchor, context);
                ajaxLastUrl = MacroCommonRenderer.createAjaxParamsFromUpdateAreas(updateAreas, null,
                        modelForm, prepLinkText + lastIndex + anchor, context);
            } else {
                nextUrl = requestHandler.makeLink(request, response, urlPath + prepLinkText + (viewIndex + 1) + anchor);
                lastUrl = requestHandler.makeLink(request, response, urlPath + prepLinkText + lastIndex + anchor);
            }
        }
        // Page size select dropdown
        if (listSize > 0 && javaScriptEnabled) {
            if (ajaxEnabled) {
                ajaxSelectSizeUrl = MacroCommonRenderer.createAjaxParamsFromUpdateAreas(updateAreas, null,
                        modelForm, prepLinkSizeText + anchor, context);
            } else {
                final String linkText = prepLinkSizeText.startsWith("/") ? prepLinkSizeText.substring(1) : prepLinkSizeText;
                selectSizeUrl = requestHandler.makeLink(request, response, urlPath + linkText);
            }
        }
        return RenderableFtlMacroCall.builder()
                .name("renderNextPrev")
                .stringParameter("paginateStyle", nonNull(modelForm.getPaginateStyle()))
                .stringParameter("paginateFirstStyle", nonNull(modelForm.getPaginateFirstStyle()))
                .intParameter("viewIndex", viewIndex)
                .intParameter("highIndex", highIndex)
                .intParameter("listSize", listSize)
                .intParameter("viewSize", viewSize)
                .booleanParameter("ajaxEnabled", ajaxEnabled)
                .booleanParameter("javaScriptEnabled", javaScriptEnabled)
                .stringParameter("ajaxFirstUrl", ajaxFirstUrl)
                .stringParameter("firstUrl", nonNull(firstUrl))
                .stringParameter("paginateFirstLabel", nonNull(modelForm.getPaginateFirstLabel(context)))
                .stringParameter("paginatePreviousStyle", nonNull(modelForm.getPaginatePreviousStyle()))
                .stringParameter("ajaxPreviousUrl", ajaxPreviousUrl)
                .stringParameter("previousUrl", nonNull(previousUrl))
                .stringParameter("paginatePreviousLabel", nonNull(modelForm.getPaginatePreviousLabel(context)))
                .stringParameter("pageLabel", nonNull(pageLabel))
                .stringParameter("ajaxSelectUrl", ajaxSelectUrl)
                .stringParameter("selectUrl", nonNull(selectUrl))
                .stringParameter("ajaxSelectSizeUrl", ajaxSelectSizeUrl)
                .stringParameter("selectSizeUrl", nonNull(selectSizeUrl))
                .stringParameter("commonDisplaying", nonNull(commonDisplaying))
                .stringParameter("paginateNextStyle", nonNull(modelForm.getPaginateNextStyle()))
                .stringParameter("ajaxNextUrl", ajaxNextUrl)
                .stringParameter("nextUrl", nonNull(nextUrl))
                .stringParameter("paginateNextLabel", nonNull(modelForm.getPaginateNextLabel(context)))
                .stringParameter("paginateLastStyle", nonNull(modelForm.getPaginateLastStyle()))
                .stringParameter("ajaxLastUrl", ajaxLastUrl)
                .stringParameter("lastUrl", nonNull(lastUrl))
                .stringParameter("paginateLastLabel", nonNull(modelForm.getPaginateLastLabel(context)))
                .stringParameter("paginateViewSizeLabel", nonNull(modelForm.getPaginateViewSizeLabel(context)))
                .build();
    }

    /**
     * Creates the macro call that renders a file field.
     * @param context the context
     * @param fileField the file field
     * @return the RenderableFtl
     */
    public RenderableFtl fileField(final Map<String, Object> context, final ModelFormField.FileField fileField) {
        final ModelFormField modelFormField = fileField.getModelFormField();
        final var builder = RenderableFtlMacroCall.builder().name("renderFileField");
        applyWidgetStyleAndAlert(modelFormField, context, builder);
        return builder
                .stringParameter("name", nonNull(modelFormField.getParameterName(context)))
                .stringParameter("value", nonNull(modelFormField.getEntry(context, fileField.getDefaultValue(context))))
                .stringParameter("size", Integer.toString(fileField.getSize()))
                .stringParameter("maxlength", fileField.getMaxlength() == null ? "" : fileField.getMaxlength().toString())
                .stringParameter("autocomplete", fileField.getClientAutocompleteField() ? "" : "off")
                .stringParameter("tabindex", nonNull(modelFormField.getTabindex()))
                .booleanParameter("disabled", modelFormField.getDisabled(context))
                .build();
    }

    /**
     * Creates the macro call that renders a password field.
     * @param context the context
     * @param passwordField the password field
     * @return the RenderableFtl
     */
    public RenderableFtl passwordField(final Map<String, Object> context, final ModelFormField.PasswordField passwordField) {
        final ModelFormField modelFormField = passwordField.getModelFormField();
        String className = nonNull(modelFormField.getWidgetStyle());
        final String alert = UtilValidate.isNotEmpty(className) && modelFormField.shouldBeRed(context) ? "true" : "false";
        className = withRequiredStyle(modelFormField, className);
        return RenderableFtlMacroCall.builder()
                .name("renderPasswordField")
                .stringParameter("className", className)
                .stringParameter("alert", alert)
                .stringParameter("name", nonNull(modelFormField.getParameterName(context)))
                .stringParameter("value", nonNull(modelFormField.getEntry(context, passwordField.getDefaultValue(context))))
                .stringParameter("size", Integer.toString(passwordField.getSize()))
                .stringParameter("maxlength", passwordField.getMaxlength() == null ? "" : passwordField.getMaxlength().toString())
                .stringParameter("id", nonNull(modelFormField.getCurrentContainerId(context)))
                .stringParameter("autocomplete", passwordField.getClientAutocompleteField() ? "" : "off")
                .stringParameter("tabindex", nonNull(modelFormField.getTabindex()))
                .booleanParameter("disabled", modelFormField.getDisabled(context))
                .build();
    }

    /**
     * Creates the macro call that renders an image field.
     * @param context the context
     * @param imageField the image field
     * @return the RenderableFtl
     */
    public RenderableFtl imageField(final Map<String, Object> context, final ModelFormField.ImageField imageField) {
        final ModelFormField modelFormField = imageField.getModelFormField();
        String value = modelFormField.getEntry(context, imageField.getValue(context));
        String description = imageField.getDescription(context);
        String alternate = imageField.getAlternate(context);
        if (UtilValidate.isEmpty(description)) {
            description = modelFormField.getTitle(context);
        }
        if (UtilValidate.isEmpty(alternate)) {
            alternate = description;
        }
        if (UtilValidate.isNotEmpty(value) && !value.startsWith("http")) {
            value = pathAsContentUrl(value);
        }
        return RenderableFtlMacroCall.builder()
                .name("renderImageField")
                .stringParameter("value", nonNull(value))
                .stringParameter("description", nonNull(encode(description, modelFormField, context)))
                .stringParameter("alternate", nonNull(encode(alternate, modelFormField, context)))
                .stringParameter("style", nonNull(imageField.getStyle(context)))
                .stringParameter("event", nonNull(modelFormField.getEvent()))
                .stringParameter("action", nonNull(modelFormField.getAction(context)))
                .build();
    }

    /**
     * Creates the macro call that renders a banner.
     * @param context the context
     * @param banner the banner
     * @return the RenderableFtl
     */
    public RenderableFtl banner(final Map<String, Object> context, final ModelForm.Banner banner) {
        final String style = banner.getStyle(context);
        final String leftStyle = banner.getLeftTextStyle(context);
        final String rightStyle = banner.getRightTextStyle(context);
        return RenderableFtlMacroCall.builder()
                .name("renderBanner")
                .stringParameter("style", nonNull(style))
                .stringParameter("leftStyle", nonNull(UtilValidate.isEmpty(leftStyle) ? style : leftStyle))
                .stringParameter("rightStyle", nonNull(UtilValidate.isEmpty(rightStyle) ? style : rightStyle))
                .stringParameter("leftText", nonNull(banner.getLeftText(context)))
                .stringParameter("text", nonNull(banner.getText(context)))
                .stringParameter("rightText", nonNull(banner.getRightText(context)))
                .build();
    }

    // the class name of a field that is required in a single form gets the required style first
    private static String withRequiredStyle(final ModelFormField modelFormField, final String className) {
        if (!shouldApplyRequiredField(modelFormField)) {
            return className;
        }
        String requiredStyle = modelFormField.getRequiredFieldStyle();
        if (UtilValidate.isEmpty(requiredStyle)) {
            requiredStyle = "required";
        }
        return UtilValidate.isEmpty(className) ? requiredStyle : requiredStyle + " " + className;
    }

    private static RenderableFtl macroWithoutParameters(final String macroName) {
        return RenderableFtlMacroCall.builder().name(macroName).build();
    }

    private static String nonNull(final String value) {
        return value == null ? "" : value;
    }

    private static void applyWidgetStyleAndAlert(final ModelFormField modelFormField, final Map<String, Object> context,
                                                 final RenderableFtlMacroCallBuilder builder) {
        String className = "";
        String alert = "false";
        if (UtilValidate.isNotEmpty(modelFormField.getWidgetStyle())) {
            className = modelFormField.getWidgetStyle();
            if (modelFormField.shouldBeRed(context)) {
                alert = "true";
            }
        }
        builder.stringParameter("className", className).stringParameter("alert", alert);
    }

    private static void applyCommonStyling(final ModelFormField modelFormField, final Map<String, Object> context,
                                           final RenderableFtlMacroCallBuilder builder) {
        final var classNames = new ArrayList<String>();
        if (UtilValidate.isNotEmpty(modelFormField.getWidgetStyle())) {
            classNames.add(modelFormField.getWidgetStyle());
            if (modelFormField.shouldBeRed(context)) {
                builder.stringParameter("alert", "true");
            }
        }

        if (shouldApplyRequiredField(modelFormField)) {
            var requiredStyle = modelFormField.getRequiredFieldStyle();
            if (UtilValidate.isEmpty(requiredStyle)) {
                requiredStyle = "required";
            }
            classNames.add(requiredStyle);
        }

        builder.stringParameter("className", String.join(" ", classNames));
    }
}
