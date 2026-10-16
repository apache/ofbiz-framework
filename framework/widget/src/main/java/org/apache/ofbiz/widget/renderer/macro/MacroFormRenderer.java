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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilCodec;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.webapp.control.RequestHandler;
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
import org.apache.ofbiz.widget.renderer.FormStringRenderer;
import org.apache.ofbiz.widget.renderer.UtilHelpText;
import org.apache.ofbiz.widget.renderer.VisualTheme;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtl;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtlMacroCall;

/**
 * Widget Library - Form Renderer implementation based on Freemarker macros
 */
public final class MacroFormRenderer implements FormStringRenderer {

    private static final String MODULE = MacroFormRenderer.class.getName();
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
        String encodedAlternate = encodeForMacro(hyperlinkField.getAlternate(context), modelFormField, context);
        String encodedImageTitle = encodeForMacro(hyperlinkField.getImageTitle(context), modelFormField, context);
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
        writeFtlElement(writer, renderableFtlFormElementsBuilder.textFindField(context, textFindField));
        this.appendTooltip(writer, context, textFindField.getModelFormField());
    }

    @Override
    public void renderRangeFindField(Appendable writer, Map<String, Object> context, RangeFindField rangeFindField) throws IOException {
        writeFtlElement(writer, renderableFtlFormElementsBuilder.rangeFindField(context, rangeFindField));
        this.appendTooltip(writer, context, rangeFindField.getModelFormField());
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
        final ModelFormField modelFormField = lookupField.getModelFormField();
        writeFtlElement(writer, renderableFtlFormElementsBuilder.lookupField(context, lookupField, this.javaScriptEnabled));
        this.addAsterisks(writer, context, modelFormField);
        this.makeHyperlinkString(writer, lookupField.getSubHyperlink(), context);
        this.appendTooltip(writer, context, modelFormField);
    }

    public void renderNextPrev(Appendable writer, Map<String, Object> context, ModelForm modelForm) {
        writeFtlElement(writer, renderableFtlFormElementsBuilder.nextPrev(context, modelForm, this.javaScriptEnabled));
    }

    @Override
    public void renderFileField(Appendable writer, Map<String, Object> context, FileField textField) throws IOException {
        writeFtlElement(writer, renderableFtlFormElementsBuilder.fileField(context, textField));
        this.makeHyperlinkString(writer, textField.getSubHyperlink(), context);
        this.appendTooltip(writer, context, textField.getModelFormField());
    }

    @Override
    public void renderPasswordField(Appendable writer, Map<String, Object> context, PasswordField passwordField) throws IOException {
        final ModelFormField modelFormField = passwordField.getModelFormField();
        writeFtlElement(writer, renderableFtlFormElementsBuilder.passwordField(context, passwordField));
        this.addAsterisks(writer, context, modelFormField);
        this.makeHyperlinkString(writer, passwordField.getSubHyperlink(), context);
        this.appendTooltip(writer, context, modelFormField);
    }

    @Override
    public void renderImageField(Appendable writer, Map<String, Object> context, ImageField imageField) throws IOException {
        writeFtlElement(writer, renderableFtlFormElementsBuilder.imageField(context, imageField));
        this.makeHyperlinkString(writer, imageField.getSubHyperlink(), context);
        this.appendTooltip(writer, context, imageField.getModelFormField());
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
        writeFtlElement(writer, renderableFtlFormElementsBuilder.banner(context, banner));
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

    @Override
    public void renderContainerFindField(Appendable writer, Map<String, Object> context, ContainerField containerField) throws IOException {
        final RenderableFtlMacroCall containerMc = renderableFtlFormElementsBuilder.containerMacroCall(context, containerField);
        writeFtlElement(writer, containerMc);
    }
}
