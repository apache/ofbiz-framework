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
import java.net.URI;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.ibm.icu.util.Calendar;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilCodec;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.webapp.control.RequestHandler;
import org.apache.ofbiz.widget.WidgetWorker;
import org.apache.ofbiz.widget.content.StaticContentUrlProvider;
import org.apache.ofbiz.widget.model.ModelForm;
import org.apache.ofbiz.widget.model.ModelFormField;
import org.apache.ofbiz.widget.model.ModelFormField.ContainerField;
import org.apache.ofbiz.widget.model.ModelFormField.DisplayField;
import org.apache.ofbiz.widget.model.ModelScreenWidget.Label;
import org.apache.ofbiz.widget.model.ModelTheme;
import org.apache.ofbiz.widget.renderer.FormRenderer;
import org.apache.ofbiz.widget.renderer.Paginator;
import org.apache.ofbiz.widget.renderer.VisualTheme;
import org.apache.ofbiz.widget.renderer.macro.model.Option;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtl;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtlMacroCall;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtlMacroCall.RenderableFtlMacroCallBuilder;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtlNoop;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtlSequence;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtlString;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtlString.RenderableFtlStringBuilder;
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

    private final UtilCodec.SimpleEncoder internalEncoder;

    public RenderableFtlFormElementsBuilder(final VisualTheme visualTheme, final RequestHandler requestHandler,
                                            final HttpServletRequest request, final HttpServletResponse response,
                                            final StaticContentUrlProvider staticContentUrlProvider) {
        this.visualTheme = visualTheme;
        this.requestHandler = requestHandler;
        this.request = request;
        this.response = response;
        this.staticContentUrlProvider = staticContentUrlProvider;
        this.internalEncoder = UtilCodec.getEncoder("string");
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
        String requiredStyle = "";
        if (modelFormField.getRequiredField()) {
            requiredField = "true";
            requiredStyle = modelFormField.getRequiredFieldStyle();
        }

        return RenderableFtlMacroCall.builder()
                .name("renderAsterisks")
                .stringParameter("requiredField", requiredField)
                .stringParameter("requiredStyle", requiredStyle)
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
            description = description.substring(0, size - 8) + "..." + description.substring(description.length() - 5);
        }

        final RenderableFtlMacroCallBuilder builder = RenderableFtlMacroCall.builder()
                .name("renderDisplayField")
                .stringParameter("type", type)
                .stringParameter("imageLocation", imageLocation)
                .stringParameter("idName", idName)
                .stringParameter("description", description)
                .stringParameter("title", title)
                .stringParameter("class", modelFormField.getWidgetStyle())
                .stringParameter("alert", modelFormField.shouldBeRed(context) ? "true" : "false");

        StringWriter sr = new StringWriter();
        sr.append("<@renderDisplayField ");
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
            if (UtilValidate.isEmpty(requiredStyle)) {
                requiredStyle = "required";
            }
            if (UtilValidate.isEmpty(className)) {
                className = requiredStyle;
            } else {
                className = requiredStyle + " " + className;
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
                .stringParameter("className", className)
                .stringParameter("alert", alert)
                .stringParameter("value", value)
                .stringParameter("textSize", textSize)
                .stringParameter("maxlength", maxlength)
                .stringParameter("id", id)
                .stringParameter("event", event != null ? event : "")
                .stringParameter("action", action != null ? action : "")
                .booleanParameter("disabled", disabled)
                .booleanParameter("readonly", readonly)
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

            String buttons = textareaField.getVisualEditorButtons(context);
            builder.stringParameter("buttons", UtilValidate.isEmpty(buttons) ? "maxi" : buttons);
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

            final RenderableFtlStringBuilder renderableFtlStringBuilder = RenderableFtlString.builder();
            final StringBuilder htmlStringBuilder = renderableFtlStringBuilder.getStringBuilder();

            if ("multi".equals(modelForm.getType())) {
                final Element anchorElement = WidgetWorker.makeHiddenFormLinkAnchorElement(linkStyle,
                        description, confirmation, modelFormField, request, context);
                htmlStringBuilder.append(anchorElement.outerHtml());

                // this is a bit trickier, since we can't do a nested form we'll have to put the link to submit the
                // form in place, but put the actual form def elsewhere, ie after the big form is closed
                final RenderableFtlString postFormRenderableFtlString = RenderableFtlString.withStringBuilder(sb -> {
                    final Element hiddenFormElement = WidgetWorker.makeHiddenFormLinkFormElement(target, targetType,
                            targetWindow, parameterMap, modelFormField, request, response, context);
                    sb.append(hiddenFormElement.outerHtml());
                });
                appendToPostFormRenderableFtl(postFormRenderableFtlString, context);

            } else {
                final Element hiddenFormElement = WidgetWorker.makeHiddenFormLinkFormElement(target, targetType,
                        targetWindow, parameterMap, modelFormField, request, response, context);
                htmlStringBuilder.append(hiddenFormElement.outerHtml());
                final Element anchorElement = WidgetWorker.makeHiddenFormLinkAnchorElement(linkStyle,
                        description, confirmation, modelFormField, request, context);
                htmlStringBuilder.append(anchorElement.outerHtml());
            }

            return renderableFtlStringBuilder.build();

        } else {
            if ("layered-modal".equals(realLinkType)) {
                String uniqueItemName = "Modal_".concat(UUID.randomUUID().toString().replace("-", "_"));
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
            String hiddenFormName = WidgetWorker.makeLinkHiddenFormName(context, modelFormField);
            if (UtilValidate.isNotEmpty(modelFormField.getEvent())
                    && UtilValidate.isNotEmpty(modelFormField.getAction(context))) {
                event = modelFormField.getEvent();
                action = modelFormField.getAction(context);
            }
            if (UtilValidate.isNotEmpty(request.getAttribute("image"))) {
                imgSrc = request.getAttribute("image").toString();
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
                description = description.substring(0, size) + "…";
            } else if (UtilValidate.isNotEmpty(request.getAttribute("title"))) {
                title = request.getAttribute("title").toString();
            }
            if (UtilValidate.isNotEmpty(request.getAttribute("id"))) {
                id = request.getAttribute("id").toString();
            }
            if (UtilValidate.isNotEmpty(request.getAttribute("uniqueItemName"))) {
                uniqueItemName = request.getAttribute("uniqueItemName").toString();
                width = request.getAttribute("width").toString();
                height = request.getAttribute("height").toString();
            }

            return RenderableFtlMacroCall.builder()
                    .name("makeHyperlinkString")
                    .stringParameter("linkStyle", linkStyle == null ? "" : linkStyle)
                    .stringParameter("hiddenFormName", hiddenFormName)
                    .stringParameter("event", event)
                    .stringParameter("action", action)
                    .stringParameter("imgSrc", imgSrc)
                    .stringParameter("imgTitle", imgTitle)
                    .stringParameter("title", title)
                    .stringParameter("alternate", alt)
                    .mapParameter("targetParameters", parameterMap)
                    .stringParameter("linkUrl", linkUrl.toString())
                    .stringParameter("targetWindow", targetWindow)
                    .stringParameter("description", description)
                    .stringParameter("confirmation", confirmation)
                    .stringParameter("uniqueItemName", uniqueItemName)
                    .stringParameter("height", height)
                    .stringParameter("width", width)
                    .stringParameter("id", id)
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

        var optionsList = allOptionValues.stream()
                .map(optionValue -> {
                    var encodedKey = encode(optionValue.getKey(), modelFormField, context);
                    var truncatedDescription = truncate(optionValue.getDescription(), textSizeOptional);
                    var selected = currentValuesList.contains(optionValue.getKey());

                    return new Option(encodedKey, truncatedDescription, selected);
                })
                .collect(Collectors.toList());

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
        } else {
            value = internalEncoder.encode(value);
        }
        return value;
    }

    private String truncate(String value, int maxCharacterLength) {
        if (maxCharacterLength > 8 && value.length() > maxCharacterLength) {
            return value.substring(0, maxCharacterLength - 8) + "..." + value.substring(value.length() - 5);
        }
        return value;
    }

    private String truncate(String value, Optional<Integer> maxCharacterLengthOptional) {
        return maxCharacterLengthOptional
                .map(maxCharacterLength -> truncate(value, maxCharacterLength))
                .orElse(value);
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
