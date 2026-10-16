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

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilCodec;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.security.CsrfUtil;
import org.apache.ofbiz.webapp.control.RequestHandler;
import org.apache.ofbiz.webapp.taglib.ContentUrlTag;
import org.apache.ofbiz.widget.WidgetWorker;
import org.apache.ofbiz.widget.content.StaticContentUrlProvider;
import org.apache.ofbiz.widget.model.CommonWidgetModels;
import org.apache.ofbiz.widget.model.ModelForm;
import org.apache.ofbiz.widget.model.ModelFormField;
import org.apache.ofbiz.widget.model.ModelFormField.CheckField;
import org.apache.ofbiz.widget.model.ModelFormField.ContainerField;
import org.apache.ofbiz.widget.model.ModelFormField.DateFindField;
import org.apache.ofbiz.widget.model.ModelFormField.DateTimeField;
import org.apache.ofbiz.widget.model.ModelFormField.DisplayEntityField;
import org.apache.ofbiz.widget.model.ModelFormField.DisplayField;
import org.apache.ofbiz.widget.model.ModelFormField.DropDownField;
import org.apache.ofbiz.widget.model.ModelFormField.FileField;
import org.apache.ofbiz.widget.model.ModelFormField.HiddenField;
import org.apache.ofbiz.widget.model.ModelFormField.HyperlinkField;
import org.apache.ofbiz.widget.model.ModelFormField.IgnoredField;
import org.apache.ofbiz.widget.model.ModelFormField.ImageField;
import org.apache.ofbiz.widget.model.ModelFormField.LookupField;
import org.apache.ofbiz.widget.model.ModelFormField.MenuField;
import org.apache.ofbiz.widget.model.ModelFormField.PasswordField;
import org.apache.ofbiz.widget.model.ModelFormField.RadioField;
import org.apache.ofbiz.widget.model.ModelFormField.RangeFindField;
import org.apache.ofbiz.widget.model.ModelFormField.ResetField;
import org.apache.ofbiz.widget.model.ModelFormField.SubmitField;
import org.apache.ofbiz.widget.model.ModelFormField.TextField;
import org.apache.ofbiz.widget.model.ModelFormField.TextFindField;
import org.apache.ofbiz.widget.model.ModelFormField.TextareaField;
import org.apache.ofbiz.widget.model.ModelScreenWidget;
import org.apache.ofbiz.widget.model.ModelSingleForm;
import org.apache.ofbiz.widget.model.ModelWidget;
import org.apache.ofbiz.widget.model.ThemeFactory;
import org.apache.ofbiz.widget.renderer.FormRenderer;
import org.apache.ofbiz.widget.renderer.FormStringRenderer;
import org.apache.ofbiz.widget.renderer.Paginator;
import org.apache.ofbiz.widget.renderer.UtilHelpText;
import org.apache.ofbiz.widget.renderer.VisualTheme;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtl;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtlMacroCall;

/**
 * Widget Library - Form Renderer implementation based on Freemarker macros
 */
public final class MacroFormRenderer implements FormStringRenderer {

    private static final String MODULE = MacroFormRenderer.class.getName();
    private final UtilCodec.SimpleEncoder internalEncoder;
    private final RequestHandler rh;
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final RenderableFtlFormElementsBuilder renderableFtlFormElementsBuilder;
    private final boolean javaScriptEnabled;
    private final VisualTheme visualTheme;
    private final FtlWriter ftlWriter;
    private boolean renderPagination = true;
    private boolean widgetCommentsEnabled = false;

    public MacroFormRenderer(String macroLibraryPath, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        this(macroLibraryPath, request, response, null, null);
    }

    public MacroFormRenderer(String macroLibraryPath, HttpServletRequest request, HttpServletResponse response,
                             FtlWriter ftlWriter, RenderableFtlFormElementsBuilder renderableFtlFormElementsBuilder)
            throws IOException {
        this.request = request;
        this.response = response;
        this.visualTheme = ThemeFactory.resolveVisualTheme(request);
        this.rh = RequestHandler.from(request);
        this.javaScriptEnabled = UtilHttp.isJavaScriptEnabled(request);
        internalEncoder = UtilCodec.getEncoder("string");
        this.ftlWriter = ftlWriter != null ? ftlWriter : new FtlWriter(macroLibraryPath, this.visualTheme);
        final StaticContentUrlProvider staticContentUrlProvider = new StaticContentUrlProvider(request);
        this.renderableFtlFormElementsBuilder = renderableFtlFormElementsBuilder != null
                ? renderableFtlFormElementsBuilder
                : new RenderableFtlFormElementsBuilder(this.visualTheme, rh, request, response, staticContentUrlProvider);
    }

    public boolean getRenderPagination() {
        return this.renderPagination;
    }

    public void setRenderPagination(boolean renderPagination) {
        this.renderPagination = renderPagination;
    }

    public void writeFtlElement(final Appendable writer, final RenderableFtl renderableFtl) {
        ftlWriter.processFtl(writer, null, renderableFtl);
    }

    public void writeFtlElement(final Appendable writer, Locale locale, final RenderableFtl renderableFtl) {
        ftlWriter.processFtl(writer, locale, renderableFtl);
    }

    private void executeMacro(Appendable writer, String macro) {
        ftlWriter.processFtlString(writer, null, macro);
    }

    /**
     * Make locale available before executing macro
     * @param writer
     * @param locale
     * @param macro
     */
    @SuppressWarnings("unused")
    private void executeMacro(Appendable writer, Locale locale, String macro) {
        ftlWriter.processFtlString(writer, locale, macro);
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

    private String encodeForMacro(String value, ModelFormField modelFormField, Map<String, Object> context) {
        if (UtilValidate.isEmpty(value)) {
            return value;
        }
        UtilCodec.SimpleEncoder encoder = (UtilCodec.SimpleEncoder) context.get("simpleEncoder");
        if (modelFormField.getEncodeOutput() && encoder != null) {
            value = encoder.encode(value);
        }
        return value;
    }

    public void renderLabel(Appendable writer, Map<String, Object> context, ModelScreenWidget.Label label) {
        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.label(context, label);
        writeFtlElement(writer, renderableFtl);
    }

    @Override
    public void renderDisplayField(Appendable writer, Map<String, Object> context, DisplayField displayField)
            throws IOException {
        if (displayField instanceof DisplayEntityField
                && ((DisplayEntityField) displayField).needConvertAsHyperlink(context)) {

            // When we have a subHyperlink on a display entity, display all as a hyperlink
            renderHyperlinkField(writer, context, ((DisplayEntityField) displayField).asHyperlink(context));
        } else {
            writeFtlElement(writer,
                    renderableFtlFormElementsBuilder.displayField(context, displayField, this.javaScriptEnabled));
            if (displayField instanceof DisplayEntityField) {
                writeFtlElement(writer,
                        renderableFtlFormElementsBuilder.makeHyperlinkString(((DisplayEntityField) displayField).getSubHyperlink(),
                                context));
            }
        }

        final ModelFormField modelFormField = displayField.getModelFormField();
        appendTooltip(writer, context, modelFormField);
    }

    @Override
    public void renderHyperlinkField(Appendable writer, Map<String, Object> context, HyperlinkField hyperlinkField)
            throws IOException {
        this.request.setAttribute("image", hyperlinkField.getImageLocation(context));
        ModelFormField modelFormField = hyperlinkField.getModelFormField();
        String encodedAlternate = encode(hyperlinkField.getAlternate(context), modelFormField, context);
        String encodedImageTitle = encode(hyperlinkField.getImageTitle(context), modelFormField, context);
        this.request.setAttribute("alternate", encodedAlternate);
        this.request.setAttribute("imageTitle", encodedImageTitle);
        this.request.setAttribute("descriptionSize", hyperlinkField.getSize());
        this.request.setAttribute("id", UtilValidate.isNotEmpty(hyperlinkField.getId(context)) ? hyperlinkField.getId(context)
                : modelFormField.getCurrentContainerId(context));
        this.request.setAttribute("title", hyperlinkField.getTitle());
        this.request.setAttribute("width", hyperlinkField.getWidth());
        this.request.setAttribute("height", hyperlinkField.getHeight());
        this.request.setAttribute("text", hyperlinkField.getText(context));
        writeFtlElement(writer, renderableFtlFormElementsBuilder.makeHyperlinkByType(hyperlinkField.getLinkType(),
                modelFormField.getWidgetStyle(), hyperlinkField.getUrlMode(), hyperlinkField.getTarget(context),
                hyperlinkField.getParameterMap(context, modelFormField.getEntityName(), modelFormField.getServiceName()),
                hyperlinkField.getDescription(context), hyperlinkField.getTargetWindow(context),
                hyperlinkField.getConfirmation(context), modelFormField, this.request, this.response, context));
        this.appendTooltip(writer, context, modelFormField);
        this.request.removeAttribute("image");
        this.request.removeAttribute("descriptionSize");
    }

    @Override
    public void renderMenuField(Appendable writer, Map<String, Object> context, MenuField menuField) throws IOException {
        menuField.renderFieldString(writer, context, null);
    }

    @Override
    public void renderTextField(Appendable writer, Map<String, Object> context, TextField textField) {
        writeFtlElement(writer, renderableFtlFormElementsBuilder.textField(context, textField, javaScriptEnabled));

        writeFtlElement(writer, renderableFtlFormElementsBuilder.makeHyperlinkString(textField.getSubHyperlink(), context));

        final ModelFormField modelFormField = textField.getModelFormField();
        this.addAsterisks(writer, context, modelFormField);
        this.appendTooltip(writer, context, modelFormField);
    }

    @Override
    public void renderTextareaField(Appendable writer, Map<String, Object> context, TextareaField textareaField) {
        writeFtlElement(writer, renderableFtlFormElementsBuilder.textArea(context, textareaField));

        final ModelFormField modelFormField = textareaField.getModelFormField();
        this.addAsterisks(writer, context, modelFormField);
        this.appendTooltip(writer, context, modelFormField);
    }

    private boolean shouldApplyRequiredField(ModelFormField modelFormField) {
        return ("single".equals(modelFormField.getModelForm().getType())
                || "upload".equals(modelFormField.getModelForm().getType()))
                && modelFormField.getRequiredField();
    }

    @Override
    public void renderDateTimeField(Appendable writer, Map<String, Object> context, DateTimeField dateTimeField) {
        writeFtlElement(writer, (Locale) context.get("locale"), renderableFtlFormElementsBuilder.dateTime(context, dateTimeField));

        final ModelFormField modelFormField = dateTimeField.getModelFormField();
        this.addAsterisks(writer, context, modelFormField);
        this.appendTooltip(writer, context, modelFormField);
    }

    @Override
    public void renderDropDownField(Appendable writer, Map<String, Object> context, DropDownField dropDownField) throws IOException {
        writeFtlElement(writer, renderableFtlFormElementsBuilder.dropDownField(context, dropDownField, this.javaScriptEnabled));

        ModelFormField.SubHyperlink subHyperlink = dropDownField.getSubHyperlink();
        if (subHyperlink != null && subHyperlink.shouldUse(context)) {
            makeHyperlinkString(writer, subHyperlink, context);
        }

        ModelFormField modelFormField = dropDownField.getModelFormField();
        this.appendTooltip(writer, context, modelFormField);
    }

    @Override
    public void renderCheckField(Appendable writer, Map<String, Object> context, CheckField checkField) throws IOException {
        writeFtlElement(writer, renderableFtlFormElementsBuilder.checkField(context, checkField));
        this.appendTooltip(writer, context, checkField.getModelFormField());
    }

    @Override
    public void renderRadioField(Appendable writer, Map<String, Object> context, RadioField radioField) throws IOException {
        writeFtlElement(writer, renderableFtlFormElementsBuilder.radioField(context, radioField));
        this.appendTooltip(writer, context, radioField.getModelFormField());
    }

    @Override
    public void renderSubmitField(Appendable writer, Map<String, Object> context, SubmitField submitField) throws IOException {
        writeFtlElement(writer, renderableFtlFormElementsBuilder.submitField(context, submitField, this.javaScriptEnabled));
        this.appendTooltip(writer, context, submitField.getModelFormField());
    }

    @Override
    public void renderResetField(Appendable writer, Map<String, Object> context, ResetField resetField) throws IOException {
        writeFtlElement(writer, renderableFtlFormElementsBuilder.resetField(context, resetField));
        this.appendTooltip(writer, context, resetField.getModelFormField());
    }

    @Override
    public void renderHiddenField(Appendable writer, Map<String, Object> context, HiddenField hiddenField) throws IOException {
        ModelFormField modelFormField = hiddenField.getModelFormField();
        String value = hiddenField.getValue(context);
        this.renderHiddenField(writer, context, modelFormField, value);
    }

    @Override
    public void renderHiddenField(Appendable writer, Map<String, Object> context, ModelFormField modelFormField, String value) throws IOException {
        writeFtlElement(writer, renderableFtlFormElementsBuilder.hiddenField(context, modelFormField, value));
    }

    @Override
    public void renderIgnoredField(Appendable writer, Map<String, Object> context, IgnoredField ignoredField) {
        // do nothing, it's an ignored field; could add a comment or something if we wanted to
        Debug.logVerbose("do nothing, it's an ignored field; could add a comment or something if we wanted to", MODULE);

    }

    @Override
    public void renderFieldTitle(Appendable writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
        String titleText = modelFormField.getTitle(context);
        String style = modelFormField.getTitleStyle();
        String id = modelFormField.getCurrentContainerId(context);
        String title = "";
        if (UtilValidate.isNotEmpty(titleText)) {
            if (" ".equals(titleText)) {
                writeFtlElement(writer, renderableFtlFormElementsBuilder.formatEmptySpace());
            } else {
                titleText = UtilHttp.encodeAmpersands(titleText);
                final String encodedTitleText = encodeForMacro(titleText, modelFormField, context);
                if (UtilValidate.isNotEmpty(modelFormField.getHeaderLink())) {
                    StringBuilder targetBuffer = new StringBuilder();
                    FlexibleStringExpander target = FlexibleStringExpander.getInstance(modelFormField.getHeaderLink());
                    String fullTarget = target.expandString(context);
                    targetBuffer.append(fullTarget);
                    String targetType = CommonWidgetModels.Link.DEFAULT_URL_MODE;
                    if (UtilValidate.isNotEmpty(targetBuffer.toString()) && targetBuffer.toString().toLowerCase(Locale
                            .getDefault()).startsWith("javascript:")) {
                        targetType = "plain";
                    }
                    StringWriter sr = new StringWriter();
                    writeFtlElement(sr, renderableFtlFormElementsBuilder.hyperlinkMacroCall(modelFormField.getHeaderLinkStyle(), targetType,
                            targetBuffer.toString(), null, encodedTitleText, "", modelFormField, this.request, this.response, context, ""));
                    String hyperlinkTitle = sr.toString().replace("\"", "\'");
                    writeFtlElement(writer, renderableFtlFormElementsBuilder.hyperlinkTitle(modelFormField.getModelForm().getName(),
                            hyperlinkTitle, false));
                } else if (modelFormField.isSortField()) {
                    renderSortField(writer, context, modelFormField, encodedTitleText);
                } else if (modelFormField.isRowSubmit()) {
                    writeFtlElement(writer, renderableFtlFormElementsBuilder.hyperlinkTitle(modelFormField.getModelForm().getName(),
                            encodedTitleText, true));
                } else {
                    title = encodedTitleText;
                }
            }
        }
        if (!title.isEmpty()) {
            //check for required field style on single forms
            if (shouldApplyRequiredField(modelFormField)) {
                String requiredStyle = modelFormField.getRequiredFieldStyle();
                if (UtilValidate.isNotEmpty(requiredStyle)) {
                    style = requiredStyle;
                }
            }
            String fieldHelpText = null;
            String displayHelpText = UtilProperties.getPropertyValue("widget", "widget.form.displayhelpText");
            if ("Y".equals(displayHelpText)) {
                Delegator delegator = WidgetWorker.getDelegator(context);
                Locale locale = (Locale) context.get("locale");
                String entityName = modelFormField.getEntityName();
                String fieldName = modelFormField.getFieldName();
                String helpText = UtilHelpText.getEntityFieldDescription(entityName, fieldName, delegator, locale);
                fieldHelpText = helpText == null ? "" : helpText;
            }
            writeFtlElement(writer, renderableFtlFormElementsBuilder.fieldTitle(style, title, id, fieldHelpText));
        }
    }

    @Override
    public void renderSingleFormFieldTitle(Appendable writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
        renderFieldTitle(writer, context, modelFormField);
    }

    @Override
    public void renderFormOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        this.widgetCommentsEnabled = ModelWidget.widgetBoundaryCommentsEnabled(context);
        if (modelForm instanceof ModelSingleForm) {
            renderBeginningBoundaryComment(writer, "Form Widget - Form Element", modelForm);
        } else {
            renderBeginningBoundaryComment(writer, "Grid Widget - Grid Element", modelForm);
        }
        writeFtlElement(writer, renderableFtlFormElementsBuilder.formOpen(context, modelForm));
    }

    @Override
    public void renderFormClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writeFtlElement(writer, renderableFtlFormElementsBuilder.formClose());
        if (modelForm instanceof ModelSingleForm) {
            renderEndingBoundaryComment(writer, "Form Widget - Form Element", modelForm);
        } else {
            renderEndingBoundaryComment(writer, "Grid Widget - Grid Element", modelForm);
        }
    }

    @Override
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
        writeFtlElement(writer, renderableFtlFormElementsBuilder.multiFormClose());
        // see if there is anything that needs to be added outside of the multi-form
        writeFtlElement(writer, renderableFtlFormElementsBuilder.postMultiFormContent(context));
        if (modelForm instanceof ModelSingleForm) {
            renderEndingBoundaryComment(writer, "Form Widget - Form Element", modelForm);
        } else {
            renderEndingBoundaryComment(writer, "Grid Widget - Grid Element", modelForm);
        }
    }

    @Override
    public void renderFormatListWrapperOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        Map<String, Object> inputFields = UtilGenerics.cast(context.get("requestParameters"));
        Object obj = context.get("queryStringMap");
        Map<String, Object> queryStringMap = (obj instanceof Map) ? UtilGenerics.cast(obj) : null;
        if (UtilValidate.isNotEmpty(queryStringMap)) {
            inputFields.putAll(queryStringMap);
        }
        if ("multi".equals(modelForm.getType())) {
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
        writeFtlElement(writer, renderableFtlFormElementsBuilder.formatListWrapperOpen(context, modelForm));
    }

    @Override
    public void renderEmptyFormDataMessage(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writeFtlElement(writer, renderableFtlFormElementsBuilder.emptyFormDataMessage(context, modelForm));
    }

    @Override
    public void renderFormatListWrapperClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writeFtlElement(writer, renderableFtlFormElementsBuilder.formatListWrapperClose(modelForm));
        if (this.renderPagination) {
            this.renderNextPrev(writer, context, modelForm);
        }
        if (modelForm instanceof ModelSingleForm) {
            renderEndingBoundaryComment(writer, "Form Widget - Form Element", modelForm);
        } else {
            renderEndingBoundaryComment(writer, "Grid Widget - Grid Element", modelForm);
        }
    }

    @Override
    public void renderFormatHeaderOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writeFtlElement(writer, renderableFtlFormElementsBuilder.formatHeaderOpen());
    }

    @Override
    public void renderFormatHeaderClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writeFtlElement(writer, renderableFtlFormElementsBuilder.formatHeaderClose());
    }

    @Override
    public void renderFormatHeaderRowOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writeFtlElement(writer, renderableFtlFormElementsBuilder.formatHeaderRowOpen(context, modelForm));
    }

    @Override
    public void renderFormatHeaderRowClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writeFtlElement(writer, renderableFtlFormElementsBuilder.formatHeaderRowClose());
    }

    @Override
    public void renderFormatHeaderRowCellOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField,
                                              int positionSpan) throws IOException {
        writeFtlElement(writer, renderableFtlFormElementsBuilder.formatHeaderRowCellOpen(modelFormField, positionSpan));
    }

    @Override
    public void renderFormatHeaderRowCellClose(Appendable writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField)
            throws IOException {
        writeFtlElement(writer, renderableFtlFormElementsBuilder.formatHeaderRowCellClose());
    }

    @Override
    public void renderFormatHeaderRowFormCellOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writeFtlElement(writer, renderableFtlFormElementsBuilder.formatHeaderRowFormCellOpen(modelForm));
    }

    @Override
    public void renderFormatHeaderRowFormCellClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writeFtlElement(writer, renderableFtlFormElementsBuilder.formatHeaderRowFormCellClose());
    }

    @Override
    public void renderFormatHeaderRowFormCellTitleSeparator(Appendable writer, Map<String, Object> context, ModelForm modelForm,
                                                            ModelFormField modelFormField, boolean isLast) throws IOException {
        writeFtlElement(writer, renderableFtlFormElementsBuilder.formatHeaderRowFormCellTitleSeparator(modelFormField, isLast));
    }

    @Override
    public void renderFormatItemRowOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writeFtlElement(writer, renderableFtlFormElementsBuilder.formatItemRowOpen(context, modelForm));
    }

    @Override
    public void renderFormatItemRowClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writeFtlElement(writer, renderableFtlFormElementsBuilder.formatItemRowClose(modelForm));
    }

    @Override
    public void renderFormatItemRowCellOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField,
                                            int positionSpan) throws IOException {
        writeFtlElement(writer, renderableFtlFormElementsBuilder.formatItemRowCellOpen(modelFormField, positionSpan));
    }

    @Override
    public void renderFormatItemRowCellClose(Appendable writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField)
            throws IOException {
        writeFtlElement(writer, renderableFtlFormElementsBuilder.formatItemRowCellClose(modelFormField));
    }

    @Override
    public void renderFormatItemRowFormCellOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writeFtlElement(writer, renderableFtlFormElementsBuilder.formatItemRowFormCellOpen(modelForm));
    }

    @Override
    public void renderFormatItemRowFormCellClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writeFtlElement(writer, renderableFtlFormElementsBuilder.formatItemRowFormCellClose());
    }

    @Override
    public void renderFormatSingleWrapperOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writeFtlElement(writer, renderableFtlFormElementsBuilder.formatSingleWrapperOpen(context, modelForm));
    }

    @Override
    public void renderFormatSingleWrapperClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writeFtlElement(writer, renderableFtlFormElementsBuilder.formatSingleWrapperClose(modelForm));
    }

    @Override
    public void renderFormatFieldRowOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writeFtlElement(writer, renderableFtlFormElementsBuilder.formatFieldRowOpen());
    }

    @Override
    public void renderFormatFieldRowClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writeFtlElement(writer, renderableFtlFormElementsBuilder.formatFieldRowClose());
    }

    @Override
    public void renderFormatFieldRowTitleCellOpen(Appendable writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
        writeFtlElement(writer, renderableFtlFormElementsBuilder.formatFieldRowTitleCellOpen(modelFormField));
    }

    @Override
    public void renderFormatFieldRowTitleCellClose(Appendable writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
        writeFtlElement(writer, renderableFtlFormElementsBuilder.formatFieldRowTitleCellClose());
    }

    @Override
    public void renderFormatFieldRowSpacerCell(Appendable writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
    }

    @Override
    public void renderFormatFieldRowWidgetCellOpen(Appendable writer, Map<String, Object> context, ModelFormField modelFormField, int positions,
                                                   int positionSpan, Integer nextPositionInRow) throws IOException {
        writeFtlElement(writer, renderableFtlFormElementsBuilder.formatFieldRowWidgetCellOpen(modelFormField, positionSpan));
    }

    @Override
    public void renderFormatFieldRowWidgetCellClose(Appendable writer, Map<String, Object> context, ModelFormField modelFormField, int positions,
                                                    int positionSpan, Integer nextPositionInRow) throws IOException {
        writeFtlElement(writer, renderableFtlFormElementsBuilder.formatFieldRowWidgetCellClose());
    }

    @Override
    public void renderFormatEmptySpace(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writeFtlElement(writer, renderableFtlFormElementsBuilder.formatEmptySpace());
    }

    @Override
    public void renderTextFindField(Appendable writer, Map<String, Object> context, TextFindField textFindField) throws IOException {
        ModelFormField modelFormField = textFindField.getModelFormField();
        String defaultOption = textFindField.getDefaultOption(context);
        String conditionGroup = modelFormField.getConditionGroup();
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
            opEquals = UtilProperties.getMessage("conditionalUiLabels", "equals", locale);
            opBeginsWith = UtilProperties.getMessage("conditionalUiLabels", "begins_with", locale);
            opContains = UtilProperties.getMessage("conditionalUiLabels", "contains", locale);
            opIsEmpty = UtilProperties.getMessage("conditionalUiLabels", "is_empty", locale);
            opNotEqual = UtilProperties.getMessage("conditionalUiLabels", "not_equal", locale);
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
        String ignoreCase = UtilProperties.getMessage("conditionalUiLabels", "ignore_case", locale);
        boolean ignCase = textFindField.getIgnoreCase(context);
        boolean hideIgnoreCase = textFindField.getHideIgnoreCase();
        String tabindex = modelFormField.getTabindex();
        boolean disabled = modelFormField.getDisabled(context);
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
        sr.append("\" conditionGroup=\"");
        sr.append(conditionGroup);
        sr.append("\" disabled=");
        sr.append(Boolean.toString(disabled));
        sr.append(" />");
        executeMacro(writer, sr.toString());
        this.appendTooltip(writer, context, modelFormField);
    }

    @Override
    public void renderRangeFindField(Appendable writer, Map<String, Object> context, RangeFindField rangeFindField) throws IOException {
        ModelFormField modelFormField = rangeFindField.getModelFormField();
        Locale locale = (Locale) context.get("locale");
        String opEquals = UtilProperties.getMessage("conditionalUiLabels", "equals", locale);
        String opGreaterThan = UtilProperties.getMessage("conditionalUiLabels", "greater_than", locale);
        String opGreaterThanEquals = UtilProperties.getMessage("conditionalUiLabels", "greater_than_equals", locale);
        String opLessThan = UtilProperties.getMessage("conditionalUiLabels", "less_than", locale);
        String opLessThanEquals = UtilProperties.getMessage("conditionalUiLabels", "less_than_equals", locale);
        String conditionGroup = modelFormField.getConditionGroup();
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
        boolean disabled = modelFormField.getDisabled(context);
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
        sr.append("\" conditionGroup=\"");
        sr.append(conditionGroup);
        sr.append("\" tabindex=\"");
        sr.append(tabindex);
        sr.append("\" disabled=");
        sr.append(Boolean.toString(disabled));
        sr.append(" />");
        executeMacro(writer, sr.toString());
        this.appendTooltip(writer, context, modelFormField);
    }

    @Override
    public void renderDateFindField(Appendable writer, Map<String, Object> context, DateFindField dateFindField) {
        writeFtlElement(writer, (Locale) context.get("locale"), renderableFtlFormElementsBuilder.dateFind(context, dateFindField));

        final ModelFormField modelFormField = dateFindField.getModelFormField();
        this.appendTooltip(writer, context, modelFormField);
    }

    @Override
    public void renderDateRangePickerField(Appendable writer, Map<String, Object> context, ModelFormField.DateRangePickerField dateRangePickerField) {
        writeFtlElement(writer, (Locale) context.get("locale"), renderableFtlFormElementsBuilder.dateRangePicker(context, dateRangePickerField));

        final ModelFormField modelFormField = dateRangePickerField.getModelFormField();
        this.appendTooltip(writer, context, modelFormField);
    }

    @Override
    public void renderLookupField(Appendable writer, Map<String, Object> context, LookupField lookupField) throws IOException {
        ModelFormField modelFormField = lookupField.getModelFormField();
        String lookupFieldFormName = lookupField.getFormName(context);
        String conditionGroup = modelFormField.getConditionGroup();
        String className = "";
        String alert = "false";
        if (UtilValidate.isNotEmpty(modelFormField.getWidgetStyle())) {
            className = modelFormField.getWidgetStyle();
            if (modelFormField.shouldBeRed(context)) {
                alert = "true";
            }
        }
        //check for required field style on single forms
        if (shouldApplyRequiredField(modelFormField)) {
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
            updateAreas = new LinkedList<>();
            updateAreas.add(ModelForm.UpdateArea.literal("change", id, autoCompleterTarget));
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
        List<String> targetParameterList = lookupField.getTargetParameterList(context);
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
            ajaxUrl = MacroCommonRenderer.createAjaxParamsFromUpdateAreas(updateAreas, null, modelForm, "", context);
        }
        String lookupPresentation = lookupField.getLookupPresentation();
        if (UtilValidate.isEmpty(lookupPresentation)) {
            lookupPresentation = "";
        }
        String lookupHeight = lookupField.getLookupHeight();
        String lookupWidth = lookupField.getLookupWidth();
        String lookupPosition = lookupField.getLookupPosition();
        String fadeBackground = lookupField.getFadeBackground();
        if (UtilValidate.isEmpty(fadeBackground)) {
            fadeBackground = "false";
        }
        Boolean isInitiallyCollapsed = lookupField.getInitiallyCollapsed();
        String clearText = "";
        Map<String, Object> uiLabelMap = UtilGenerics.cast(context.get("uiLabelMap"));
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
        String tabindex = modelFormField.getTabindex();
        boolean disabled = modelFormField.getDisabled(context);
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
        if (UtilValidate.isNotEmpty(lookupHeight)) {
            sr.append("\" height=\"");
            sr.append(lookupHeight);
        }
        if (UtilValidate.isNotEmpty(lookupWidth)) {
            sr.append("\" width=\"");
            sr.append(lookupWidth);
        }
        if (UtilValidate.isNotEmpty(lookupPosition)) {
            sr.append("\" position=\"");
            sr.append(lookupPosition);
        }
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
        sr.append("\" conditionGroup=\"");
        sr.append(conditionGroup);
        sr.append("\" tabindex=\"");
        sr.append(tabindex);
        sr.append("\" disabled=");
        sr.append(Boolean.toString(disabled));
        sr.append(" delegatorName=\"");
        sr.append(((HttpSession) context.get("session")).getAttribute("delegatorName").toString());
        sr.append("\" />");
        executeMacro(writer, sr.toString());
        this.addAsterisks(writer, context, modelFormField);
        this.makeHyperlinkString(writer, lookupField.getSubHyperlink(), context);
        this.appendTooltip(writer, context, modelFormField);
    }

    public void renderNextPrev(Appendable writer, Map<String, Object> context, ModelForm modelForm) {
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
            Debug.logWarning("Cannot paginate because TargetService is empty for the form: " + modelForm.getName(), MODULE);
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
        Map<String, String> uiLabelMap = UtilGenerics.cast(context.get("uiLabelMap"));
        String pageLabel = "";
        String commonDisplaying = "";
        if (uiLabelMap == null) {
            Debug.logWarning("Could not find uiLabelMap in context", MODULE);
        } else {
            pageLabel = uiLabelMap.get("CommonPage");
            Map<String, Integer> messageMap = UtilMisc.toMap("lowCount", lowIndex + 1, "highCount", lowIndex + actualPageSize, "total", listSize);
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
        String tokenValue = CsrfUtil.generateTokenForNonAjax(request, targetService);
        str = CsrfUtil.addOrUpdateTokenInQueryString(str, tokenValue);

        // strip legacy viewIndex/viewSize params from the query string
        String queryString = UtilHttp.stripViewParamsFromQueryString(str, "" + paginatorNumber);
        // strip parameterized index/size params from the query string
        Set<String> paramNames = new HashSet<>();
        paramNames.add(viewIndexParam);
        paramNames.add(viewSizeParam);
        queryString = UtilHttp.stripNamedParamsFromQueryString(queryString, paramNames);
        String anchor = "";
        String paginateAnchor = modelForm.getPaginateTargetAnchor();
        if (UtilValidate.isNotEmpty(paginateAnchor)) {
            anchor = "#" + paginateAnchor;
        }
        // Create separate url path String and request parameters String,
        // add viewIndex/viewSize parameters to request parameter String
        String urlPath = UtilHttp.removeQueryStringFromTarget(targetService);
        String prepLinkText = UtilHttp.getQueryStringFromTarget(targetService);
        String prepLinkSizeText;
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
                ajaxFirstUrl = MacroCommonRenderer.createAjaxParamsFromUpdateAreas(updateAreas, null, modelForm, prepLinkText + 0 + anchor, context);
            } else {
                linkText = prepLinkText + 0 + anchor;
                firstUrl = rh.makeLink(this.request, this.response, urlPath + linkText);
            }
        }
        if (viewIndex > 0) {
            if (ajaxEnabled) {
                ajaxPreviousUrl = MacroCommonRenderer.createAjaxParamsFromUpdateAreas(updateAreas, null, modelForm,
                        prepLinkText + (viewIndex - 1) + anchor, context);
            } else {
                linkText = prepLinkText + (viewIndex - 1) + anchor;
                previousUrl = rh.makeLink(this.request, this.response, urlPath + linkText);
            }
        }
        // Page select dropdown
        if (listSize > 0 && this.javaScriptEnabled) {
            if (ajaxEnabled) {
                ajaxSelectUrl = MacroCommonRenderer.createAjaxParamsFromUpdateAreas(updateAreas, null, modelForm,
                        prepLinkText + "' + (this.value - 1) + '", context);
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
                ajaxNextUrl = MacroCommonRenderer.createAjaxParamsFromUpdateAreas(updateAreas, null,
                        modelForm, prepLinkText + (viewIndex + 1) + anchor, context);
            } else {
                linkText = prepLinkText + (viewIndex + 1) + anchor;
                nextUrl = rh.makeLink(this.request, this.response, urlPath + linkText);
            }
        }
        // Last button
        if (highIndex < listSize) {
            int lastIndex = UtilMisc.getViewLastIndex(listSize, viewSize);
            if (ajaxEnabled) {
                ajaxLastUrl = MacroCommonRenderer.createAjaxParamsFromUpdateAreas(updateAreas, null,
                        modelForm, prepLinkText + lastIndex + anchor, context);
            } else {
                linkText = prepLinkText + lastIndex + anchor;
                lastUrl = rh.makeLink(this.request, this.response, urlPath + linkText);
            }
        }
        // Page size select dropdown
        if (listSize > 0 && this.javaScriptEnabled) {
            if (ajaxEnabled) {
                ajaxSelectSizeUrl = MacroCommonRenderer.createAjaxParamsFromUpdateAreas(updateAreas, null,
                        modelForm, prepLinkSizeText + anchor, context);
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

    @Override
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
        boolean disabled = modelFormField.getDisabled(context);
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
        sr.append("\" disabled=");
        sr.append(Boolean.toString(disabled));
        sr.append(" />");
        executeMacro(writer, sr.toString());
        this.makeHyperlinkString(writer, textField.getSubHyperlink(), context);
        this.appendTooltip(writer, context, modelFormField);
    }

    @Override
    public void renderPasswordField(Appendable writer, Map<String, Object> context, PasswordField passwordField) throws IOException {
        ModelFormField modelFormField = passwordField.getModelFormField();
        String className = "";
        String alert = "false";
        String name = modelFormField.getParameterName(context);
        String size = Integer.toString(passwordField.getSize());
        String maxlength = "";
        String id = modelFormField.getCurrentContainerId(context);
        boolean disabled = modelFormField.getDisabled(context);
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

        //check for required field style on single forms
        if (shouldApplyRequiredField(modelFormField)) {
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
        sr.append("\" disabled=");
        sr.append(Boolean.toString(disabled));
        sr.append(" />");
        executeMacro(writer, sr.toString());
        this.addAsterisks(writer, context, modelFormField);
        this.makeHyperlinkString(writer, passwordField.getSubHyperlink(), context);
        this.appendTooltip(writer, context, modelFormField);
    }

    @Override
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

    @Override
    public void renderFieldGroupOpen(Appendable writer, Map<String, Object> context, ModelForm.FieldGroup fieldGroup) {
        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.fieldGroupOpen(context, fieldGroup);
        ftlWriter.processFtl(writer, (Locale) context.get("locale"), renderableFtl);
    }

    @Override
    public void renderFieldGroupClose(Appendable writer, Map<String, Object> context, ModelForm.FieldGroup fieldGroup) {
        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.fieldGroupClose(context, fieldGroup);
        ftlWriter.processFtl(writer, (Locale) context.get("locale"), renderableFtl);
    }

    @Override
    public void renderBanner(Appendable writer, Map<String, Object> context, ModelForm.Banner banner) throws IOException {
        String style = banner.getStyle(context);
        String leftStyle = banner.getLeftTextStyle(context);
        if (UtilValidate.isEmpty(leftStyle)) {
            leftStyle = style;
        }
        String rightStyle = banner.getRightTextStyle(context);
        if (UtilValidate.isEmpty(rightStyle)) {
            rightStyle = style;
        }
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
     * @param writer      The writer to write to
     * @param widgetType  The widget type: "Screen Widget", "Form Widget", etc.
     * @param modelWidget The widget
     */
    public void renderBeginningBoundaryComment(Appendable writer, String widgetType, ModelWidget modelWidget) {
        if (this.widgetCommentsEnabled) {
            writeFtlElement(writer, renderableFtlFormElementsBuilder.boundaryComment("Begin", widgetType,
                    modelWidget.getBoundaryCommentName()));
        }
    }

    /**
     * Renders the ending boundary comment string.
     * @param writer      The writer to write to
     * @param widgetType  The widget type: "Screen Widget", "Form Widget", etc.
     * @param modelWidget The widget
     */
    public void renderEndingBoundaryComment(Appendable writer, String widgetType, ModelWidget modelWidget) {
        if (this.widgetCommentsEnabled) {
            writeFtlElement(writer, renderableFtlFormElementsBuilder.boundaryComment("End", widgetType,
                    modelWidget.getBoundaryCommentName()));
        }
    }

    public void renderSortField(Appendable writer, Map<String, Object> context, ModelFormField modelFormField, String titleText)
            throws UnsupportedEncodingException {
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
            Debug.logWarning("Cannot sort because the paginate target URL is empty for the form: " + modelForm.getName(), MODULE);
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
            } else if (("-" + columnField).equals(oldSortField)) {
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
            linkUrl = MacroCommonRenderer.createAjaxParamsFromUpdateAreas(updateAreas, paramMap, modelForm, null, context);
        } else {
            StringBuilder sb = new StringBuilder("?");
            Iterator<Map.Entry<String, Object>> iter = paramMap.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, Object> entry = iter.next();
                sb.append(entry.getKey()).append("=").append(entry.getValue());
                if (iter.hasNext()) {
                    sb.append("&amp;");
                }
            }
            String newQueryString = sb.toString();
            String urlPath = UtilHttp.removeQueryStringFromTarget(paginateTarget);
            if (newQueryString.contains("?null=")) { // FIXME, not sure how to handle URL encoding in tests
                newQueryString = newQueryString.replace("?null=LinkFromQBEString", "?sortField=LinkFromQBEString");
                linkUrl = rh.makeLink(this.request, this.response, urlPath.concat(newQueryString));
            } else {
                linkUrl = rh.makeLink(this.request, this.response, urlPath.concat(UtilCodec.encodeUrl(newQueryString, context)));
            }
        }
        String tooltip = modelFormField.getSortFieldHelpText(context);
        writeFtlElement(writer, renderableFtlFormElementsBuilder.sortField(sortFieldStyle, titleText, linkUrl, ajaxEnabled, tooltip));
    }

    private void appendTooltip(Appendable writer, Map<String, Object> context, ModelFormField modelFormField) {
        // render the tooltip, in other methods too
        writeFtlElement(writer, renderableFtlFormElementsBuilder.tooltip(context, modelFormField));
    }

    private void makeHyperlinkString(Appendable writer, ModelFormField.SubHyperlink subHyperlink, Map<String, Object> context) throws IOException {
        if (subHyperlink != null && subHyperlink.shouldUse(context)) {
            writer.append(' ');
            writeFtlElement(writer, renderableFtlFormElementsBuilder.makeHyperlinkString(subHyperlink, context));
        }
    }

    private void addAsterisks(Appendable writer, Map<String, Object> context, ModelFormField modelFormField) {
        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.asterisks(context, modelFormField);
        writeFtlElement(writer, renderableFtl);
    }

    public void appendContentUrl(Appendable writer, String location) throws IOException {
        StringBuilder buffer = new StringBuilder();
        ContentUrlTag.appendContentPrefix(this.request, buffer);
        writer.append(buffer.toString());
        writer.append(location);
    }

    @Override
    public void renderContainerFindField(Appendable writer, Map<String, Object> context, ContainerField containerField) throws IOException {
        final RenderableFtlMacroCall containerMc = renderableFtlFormElementsBuilder.containerMacroCall(context, containerField);
        writeFtlElement(writer, containerMc);
    }
}
