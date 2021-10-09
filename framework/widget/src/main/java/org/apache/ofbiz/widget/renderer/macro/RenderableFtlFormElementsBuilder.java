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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.webapp.control.RequestHandler;
import org.apache.ofbiz.widget.WidgetWorker;
import org.apache.ofbiz.widget.model.ModelForm;
import org.apache.ofbiz.widget.model.ModelFormField;
import org.apache.ofbiz.widget.model.ModelFormField.ContainerField;
import org.apache.ofbiz.widget.model.ModelFormField.DisplayField;
import org.apache.ofbiz.widget.model.ModelScreenWidget.Label;
import org.apache.ofbiz.widget.model.ModelTheme;
import org.apache.ofbiz.widget.renderer.Paginator;
import org.apache.ofbiz.widget.renderer.VisualTheme;
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

    public RenderableFtlFormElementsBuilder(final VisualTheme visualTheme, final RequestHandler requestHandler,
                                            final HttpServletRequest request, final HttpServletResponse response) {
        this.visualTheme = visualTheme;
        this.requestHandler = requestHandler;
        this.request = request;
        this.response = response;
    }

    public RenderableFtl tooltip(final Map<String, Object> context, final ModelFormField modelFormField) {
        final String tooltip = modelFormField.getTooltip(context);
        return RenderableFtlMacroCall.builder()
                .name("renderTooltip")
                .stringParameter("tooltip", tooltip)
                .stringParameter("tooltipStyle", modelFormField.getTitleStyle())
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
        boolean disabled = modelFormField.getDisabled();
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
            String alt = "";
            String id = "";
            String uniqueItemName = "";
            String width = "";
            String height = "";
            String imgTitle = "";
            String hiddenFormName = WidgetWorker.makeLinkHiddenFormName(context, modelFormField);
            if (UtilValidate.isNotEmpty(modelFormField.getEvent())
                    && UtilValidate.isNotEmpty(modelFormField.getAction(context))) {
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
                description = description.substring(0, size - 8) + "..."
                        + description.substring(description.length() - 5);
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

            return RenderableFtlMacroCall.builder()
                    .name("makeHyperlinkString")
                    .stringParameter("linkStyle", linkStyle == null ? "" : linkStyle)
                    .stringParameter("hiddenFormName", hiddenFormName)
                    .stringParameter("event", event)
                    .stringParameter("action", action)
                    .stringParameter("imgSrc", imgSrc)
                    .stringParameter("title", imgTitle)
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
}
