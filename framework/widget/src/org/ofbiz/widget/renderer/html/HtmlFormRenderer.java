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
package org.ofbiz.widget.renderer.html;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilCodec;
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
import org.ofbiz.widget.model.ModelWidget;
import org.ofbiz.widget.renderer.FormRenderer;
import org.ofbiz.widget.renderer.FormStringRenderer;
import org.ofbiz.widget.renderer.Paginator;
import org.ofbiz.widget.renderer.UtilHelpText;
import org.ofbiz.widget.renderer.macro.MacroFormRenderer;

import freemarker.template.TemplateException;

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
    private UtilCodec.SimpleEncoder internalEncoder;

    protected HtmlFormRenderer() {}

    public HtmlFormRenderer(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
        ServletContext ctx = (ServletContext) request.getAttribute("servletContext");
        this.rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");
        this.javaScriptEnabled = UtilHttp.isJavaScriptEnabled(request);
        internalEncoder = UtilCodec.getEncoder("string");
    }

    public boolean getRenderPagination() {
        return this.renderPagination;
    }

    public void setRenderPagination(boolean renderPagination) {
        this.renderPagination = renderPagination;
    }

    public void appendOfbizUrl(Appendable writer, String location) throws IOException {
        writer.append(this.rh.makeLink(this.request, this.response, location));
    }

    public void appendContentUrl(Appendable writer, String location) throws IOException {
        ContentUrlTag.appendContentPrefix(this.request, writer);
        writer.append(location);
    }

    public void appendTooltip(Appendable writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
        // render the tooltip, in other methods too
        String tooltip = modelFormField.getTooltip(context);
        if (UtilValidate.isNotEmpty(tooltip)) {
            writer.append("<span class=\"");
            String tooltipStyle = modelFormField.getTooltipStyle();
            if (UtilValidate.isNotEmpty(tooltipStyle)) {
                writer.append(tooltipStyle);
            } else {
                writer.append("tooltip");
            }
            writer.append("\">");
            writer.append(tooltip);
            writer.append("</span>");
        }
    }

    public void addAsterisks(Appendable writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {

        boolean requiredField = modelFormField.getRequiredField();
        if (requiredField) {
            String requiredStyle = modelFormField.getRequiredFieldStyle();

            if (UtilValidate.isEmpty(requiredStyle)) {
               writer.append("*");
            }
        }
    }

    public void appendClassNames(Appendable writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
        String className = modelFormField.getWidgetStyle();
        if (UtilValidate.isNotEmpty(className) || modelFormField.shouldBeRed(context)) {
            writer.append(" class=\"");
            writer.append(className);
            // add a style of red if redWhen is true
            if (modelFormField.shouldBeRed(context)) {
                writer.append(" alert");
            }
            writer.append('"');
        }
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderDisplayField(java.io.Writer, java.util.Map, org.ofbiz.widget.model.ModelFormField.DisplayField)
     */
    public void renderDisplayField(Appendable writer, Map<String, Object> context, DisplayField displayField) throws IOException {
        ModelFormField modelFormField = displayField.getModelFormField();

        StringBuilder str = new StringBuilder();

        String idName = modelFormField.getCurrentContainerId(context);

        if (UtilValidate.isNotEmpty(modelFormField.getWidgetStyle()) || modelFormField.shouldBeRed(context)) {
            str.append("<span class=\"");
            str.append(modelFormField.getWidgetStyle());
            // add a style of red if this is a date/time field and redWhen is true
            if (modelFormField.shouldBeRed(context)) {
                str.append(" alert");
            }
            str.append('"');
            if (UtilValidate.isNotEmpty(idName)) {
                str.append(" id=\"");
                str.append(idName+"_sp");
                str.append('"');
            }
            str.append('>');
        }

        if (str.length() > 0) {
            writer.append(str.toString());
        }
        String description = displayField.getDescription(context);
        //Replace new lines with <br/>
        description = description.replaceAll("\n", "<br/>");

        if (UtilValidate.isEmpty(description)) {
            this.renderFormatEmptySpace(writer, context, modelFormField.getModelForm());
        } else {
            writer.append(description);
        }

        if (str.length() > 0) {
            writer.append("</span>");
        }

        ModelFormField.InPlaceEditor inPlaceEditor = displayField.getInPlaceEditor();
        boolean ajaxEnabled = inPlaceEditor != null && this.javaScriptEnabled;

        if (ajaxEnabled) {
            writer.append("<script language=\"JavaScript\" type=\"text/javascript\">");
            StringBuilder url = new StringBuilder(inPlaceEditor.getUrl(context));
            Map<String, Object> fieldMap = inPlaceEditor.getFieldMap(context);
            if (fieldMap != null) {
                url.append('?');
                int count = 0;
                for (Entry<String, Object> field: fieldMap.entrySet()) {
                    count++;
                    url.append(field.getKey()).append('=').append(field.getValue());
                    if (count < fieldMap.size()) {
                        url.append('&');
                    }
                }
            }
            writer.append("ajaxInPlaceEditDisplayField('");
            writer.append(idName).append("', '").append(url).append("', {");
            if (UtilValidate.isNotEmpty(inPlaceEditor.getParamName())) {
                writer.append("name: '").append(inPlaceEditor.getParamName()).append("'");
            } else {
                writer.append("name: '").append(modelFormField.getFieldName()).append("'");
            }
            if (UtilValidate.isNotEmpty(inPlaceEditor.getCancelControl())) {
                writer.append(", cancelControl: ");
                if (!"false".equals(inPlaceEditor.getCancelControl())) {
                    writer.append("'");
                }
                writer.append(inPlaceEditor.getCancelControl());
                if (!"false".equals(inPlaceEditor.getCancelControl())) {
                    writer.append("'");
                }
            }
            if (UtilValidate.isNotEmpty(inPlaceEditor.getCancelText())) {
                writer.append(", cancel: '").append(inPlaceEditor.getCancelText()).append("'");
            }
            if (UtilValidate.isNotEmpty(inPlaceEditor.getClickToEditText())) {
                writer.append(", tooltip: '").append(inPlaceEditor.getClickToEditText()).append("'");
            }
            if (UtilValidate.isNotEmpty(inPlaceEditor.getFieldPostCreation())) {
                writer.append(", fieldPostCreation: ");
                if (!"false".equals(inPlaceEditor.getFieldPostCreation())) {
                    writer.append("'");
                }
                writer.append(inPlaceEditor.getFieldPostCreation());
                if (!"false".equals(inPlaceEditor.getFieldPostCreation())) {
                    writer.append("'");
                }
            }
            if (UtilValidate.isNotEmpty(inPlaceEditor.getFormClassName())) {
                writer.append(", cssclass: '").append(inPlaceEditor.getFormClassName()).append("'");
            }
            if (UtilValidate.isNotEmpty(inPlaceEditor.getHighlightColor())) {
                writer.append(", highlightColor: '").append(inPlaceEditor.getHighlightColor()).append("'");
            }
            if (UtilValidate.isNotEmpty(inPlaceEditor.getHighlightEndColor())) {
                writer.append(", highlightEndColor: '").append(inPlaceEditor.getHighlightEndColor()).append("'");
            }
            if (UtilValidate.isNotEmpty(inPlaceEditor.getHoverClassName())) {
                writer.append(", hoverClassName: '").append(inPlaceEditor.getHoverClassName()).append("'");
            }
            if (UtilValidate.isNotEmpty(inPlaceEditor.getHtmlResponse())) {
                writer.append(", htmlResponse: ").append(inPlaceEditor.getHtmlResponse());
            }
            if (UtilValidate.isNotEmpty(inPlaceEditor.getLoadingClassName())) {
                writer.append(", loadingClassName: '").append(inPlaceEditor.getLoadingClassName()).append("'");
            }
            if (UtilValidate.isNotEmpty(inPlaceEditor.getLoadingText())) {
                writer.append(", indicator: '").append(inPlaceEditor.getLoadingText()).append("'");
            }
            if (UtilValidate.isNotEmpty(inPlaceEditor.getOkControl())) {
                writer.append(", submit: ");
                if (!"false".equals(inPlaceEditor.getOkControl())) {
                    writer.append("'");
                }
                writer.append(inPlaceEditor.getOkControl());
                if (!"false".equals(inPlaceEditor.getOkControl())) {
                    writer.append("'");
                }
            }
            if (UtilValidate.isNotEmpty(inPlaceEditor.getOkText())) {
                writer.append(", okText: '").append(inPlaceEditor.getOkText()).append("'");
            }
            if (UtilValidate.isNotEmpty(inPlaceEditor.getSavingClassName())) {
                writer.append(", savingClassName: '").append(inPlaceEditor.getSavingClassName()).append("', ");
            }
            if (UtilValidate.isNotEmpty(inPlaceEditor.getSavingText())) {
                writer.append(", savingText: '").append(inPlaceEditor.getSavingText()).append("'");
            }
            if (UtilValidate.isNotEmpty(inPlaceEditor.getSubmitOnBlur())) {
                writer.append(", submitOnBlur: ").append(inPlaceEditor.getSubmitOnBlur());
            }
            if (UtilValidate.isNotEmpty(inPlaceEditor.getTextBeforeControls())) {
                writer.append(", textBeforeControls: '").append(inPlaceEditor.getTextBeforeControls()).append("'");
            }
            if (UtilValidate.isNotEmpty(inPlaceEditor.getTextAfterControls())) {
                writer.append(", textAfterControls: '").append(inPlaceEditor.getTextAfterControls()).append("'");
            }
            if (UtilValidate.isNotEmpty(inPlaceEditor.getTextBetweenControls())) {
                writer.append(", textBetweenControls: '").append(inPlaceEditor.getTextBetweenControls()).append("'");
            }
            if (UtilValidate.isNotEmpty(inPlaceEditor.getUpdateAfterRequestCall())) {
                writer.append(", updateAfterRequestCall: ").append(inPlaceEditor.getUpdateAfterRequestCall());
            }
            if (UtilValidate.isNotEmpty(inPlaceEditor.getRows())) {
                writer.append(", rows: '").append(inPlaceEditor.getRows()).append("'");
            }
            if (UtilValidate.isNotEmpty(inPlaceEditor.getCols())) {
                writer.append(", cols: '").append(inPlaceEditor.getCols()).append("'");
            }
            writer.append("});");
            writer.append("</script>");
        }

        if (displayField instanceof DisplayEntityField) {
            this.makeHyperlinkString(writer, ((DisplayEntityField) displayField).getSubHyperlink(), context);
        }

        this.appendTooltip(writer, context, modelFormField);

        //appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderHyperlinkField(java.io.Writer, java.util.Map, org.ofbiz.widget.model.ModelFormField.HyperlinkField)
     */
    public void renderHyperlinkField(Appendable writer, Map<String, Object> context, HyperlinkField hyperlinkField) throws IOException {
        this.request.setAttribute("image", hyperlinkField.getImageLocation(context));
        ModelFormField modelFormField = hyperlinkField.getModelFormField();
        String description = encode(hyperlinkField.getDescription(context), modelFormField, context);
        String confirmation = encode(hyperlinkField.getConfirmation(context), modelFormField, context);
        WidgetWorker.makeHyperlinkByType(writer, hyperlinkField.getLinkType(), modelFormField.getWidgetStyle(), hyperlinkField.getUrlMode(), hyperlinkField.getTarget(context),
                hyperlinkField.getParameterMap(context), description, hyperlinkField.getTargetWindow(context), confirmation, modelFormField,
                this.request, this.response, context);
        this.appendTooltip(writer, context, modelFormField);
        //appendWhitespace(writer);
    }

    public void makeHyperlinkString(Appendable writer, ModelFormField.SubHyperlink subHyperlink, Map<String, Object> context) throws IOException {
        if (subHyperlink == null) {
            return;
        }
        if (subHyperlink.shouldUse(context)) {
            writer.append(' ');
            String description = encode(subHyperlink.getDescription(context), subHyperlink.getModelFormField(), context);
            WidgetWorker.makeHyperlinkByType(writer, subHyperlink.getLinkType(), subHyperlink.getStyle(context), subHyperlink.getUrlMode(), subHyperlink.getTarget(context),
                    subHyperlink.getParameterMap(context), description, subHyperlink.getTargetWindow(context), null, subHyperlink.getModelFormField(),
                    this.request, this.response, context);
        }
    }

    private String encode(String value, ModelFormField modelFormField, Map<String, Object> context) {
        if (UtilValidate.isEmpty(value)) {
            return value;
        }
        UtilCodec.SimpleEncoder encoder = (UtilCodec.SimpleEncoder)context.get("simpleEncoder");
        if (modelFormField.getEncodeOutput() && encoder != null) {
            value = encoder.encode(value);
        } else {
            value = internalEncoder.encode(value);
        }
        return value;
    }

    public void renderMenuField(Appendable writer, Map<String, Object> context, MenuField menuField) throws IOException {
        menuField.renderFieldString(writer, context, null);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderTextField(java.io.Writer, java.util.Map, org.ofbiz.widget.model.ModelFormField.TextField)
     */
    public void renderTextField(Appendable writer, Map<String, Object> context, TextField textField) throws IOException {
        ModelFormField modelFormField = textField.getModelFormField();

        writer.append("<input type=\"text\"");

        appendClassNames(writer, context, modelFormField);

        writer.append(" name=\"");
        writer.append(modelFormField.getParameterName(context));
        writer.append('"');

        String value = modelFormField.getEntry(context, textField.getDefaultValue(context));
        if (UtilValidate.isNotEmpty(value)) {
            writer.append(" value=\"");
            writer.append(value);
            writer.append('"');
        }

        writer.append(" size=\"");
        writer.append(Integer.toString(textField.getSize()));
        writer.append('"');

        Integer maxlength = textField.getMaxlength();
        if (maxlength != null) {
            writer.append(" maxlength=\"");
            writer.append(maxlength.toString());
            writer.append('"');
        }

        String idName = modelFormField.getCurrentContainerId(context);
        if (UtilValidate.isNotEmpty(idName)) {
            writer.append(" id=\"");
            writer.append(idName);
            writer.append('"');
        }

        String event = modelFormField.getEvent();
        String action = modelFormField.getAction(context);
        if (UtilValidate.isNotEmpty(event) && UtilValidate.isNotEmpty(action)) {
            writer.append(" ");
            writer.append(event);
            writer.append("=\"");
            writer.append(action);
            writer.append('"');
        }

        List<ModelForm.UpdateArea> updateAreas = modelFormField.getOnChangeUpdateAreas();
        boolean ajaxEnabled = updateAreas != null && this.javaScriptEnabled;
        if (!textField.getClientAutocompleteField() || ajaxEnabled) {
            writer.append(" autocomplete=\"off\"");
        }

        writer.append("/>");

        this.addAsterisks(writer, context, modelFormField);

        this.makeHyperlinkString(writer, textField.getSubHyperlink(), context);

        this.appendTooltip(writer, context, modelFormField);

        if (ajaxEnabled) {
            appendWhitespace(writer);
            writer.append("<script language=\"JavaScript\" type=\"text/javascript\">");
            appendWhitespace(writer);
            writer.append("ajaxAutoCompleter('").append(createAjaxParamsFromUpdateAreas(updateAreas, null, context)).append("');");
            appendWhitespace(writer);
            writer.append("</script>");
        }
        appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderTextareaField(java.io.Writer, java.util.Map, org.ofbiz.widget.model.ModelFormField.TextareaField)
     */
    public void renderTextareaField(Appendable writer, Map<String, Object> context, TextareaField textareaField) throws IOException {
        ModelFormField modelFormField = textareaField.getModelFormField();

        writer.append("<textarea");

        appendClassNames(writer, context, modelFormField);

        writer.append(" name=\"");
        writer.append(modelFormField.getParameterName(context));
        writer.append('"');

        writer.append(" cols=\"");
        writer.append(Integer.toString(textareaField.getCols()));
        writer.append('"');

        writer.append(" rows=\"");
        writer.append(Integer.toString(textareaField.getRows()));
        writer.append('"');

        Integer maxlength = textareaField.getMaxlength();
        if(maxlength != null) {
            writer.append(" maxlength=\"");
            writer.append(Integer.toString(textareaField.getMaxlength()));
            writer.append('"');
        }

        String idName = modelFormField.getCurrentContainerId(context);
        if (UtilValidate.isNotEmpty(idName)) {
            writer.append(" id=\"");
            writer.append(idName);
            writer.append('"');
        } else if (textareaField.getVisualEditorEnable()) {
            writer.append(" id=\"");
            writer.append("htmlEditArea");
            writer.append('"');
        }

        if (textareaField.isReadOnly()) {
            writer.append(" readonly");
        }

        writer.append('>');

        String value = modelFormField.getEntry(context, textareaField.getDefaultValue(context));
        if (UtilValidate.isNotEmpty(value)) {
            writer.append(value);
        }

        writer.append("</textarea>");

        if (textareaField.getVisualEditorEnable()) {
            writer.append("<script language=\"javascript\" src=\"/images/jquery/plugins/elrte-1.3/js/elrte.min.js\" type=\"text/javascript\"></script>");
            writer.append("<link href=\"/images/jquery/plugins/elrte-1.3/css/elrte.min.css\" rel=\"stylesheet\" type=\"text/css\">");
            writer.append("<script language=\"javascript\" type=\"text/javascript\"> var opts = { cssClass : 'el-rte', toolbar : ");
            // define the toolsbar
            String buttons = textareaField.getVisualEditorButtons(context);
            if (UtilValidate.isNotEmpty(buttons)) {
                writer.append(buttons);
            } else {
                writer.append("maxi");
            }
            writer.append(", doctype  : '<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">', //'<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\">'");
            writer.append(", cssfiles : ['/images/jquery/plugins/elrte-1.3/css/elrte-inner.css'] ");
            writer.append("}");
            // load the wysiwyg editor
            writer.append("jQuery('#");
            if (UtilValidate.isNotEmpty(idName)) {
                writer.append(idName);
            } else {
                writer.append("htmlEditArea");
            }
            writer.append("').elrte(opts);");
            writer.append("</script>");
        }

        this.addAsterisks(writer, context, modelFormField);

        this.appendTooltip(writer, context, modelFormField);

        //appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderDateTimeField(java.io.Writer, java.util.Map, org.ofbiz.widget.model.ModelFormField.DateTimeField)
     */
    public void renderDateTimeField(Appendable writer, Map<String, Object> context, DateTimeField dateTimeField) throws IOException {
        String macroLibraryPath = UtilProperties.getPropertyValue("widget", "screen.formrenderer");
        try {
            MacroFormRenderer macroFormRenderer = new MacroFormRenderer(macroLibraryPath, this.request, this.response);
            macroFormRenderer.renderDateTimeField(writer, context, dateTimeField);
        } catch (TemplateException e) {
            Debug.logError(e, "Error rendering screen thru ftl macro: renderDateTimeField", module);
        } catch (IOException e) {
            Debug.logError(e, "Error rendering screen thru ftl, macro: renderDateTimeField", module);
        }
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderDropDownField(java.io.Writer, java.util.Map, org.ofbiz.widget.model.ModelFormField.DropDownField)
     */
    public void renderDropDownField(Appendable writer, Map<String, Object> context, DropDownField dropDownField) throws IOException {
        ModelFormField modelFormField = dropDownField.getModelFormField();
        ModelForm modelForm = modelFormField.getModelForm();
        ModelFormField.AutoComplete autoComplete = dropDownField.getAutoComplete();
        boolean ajaxEnabled = autoComplete != null && this.javaScriptEnabled;
        List<ModelFormField.OptionValue> allOptionValues = dropDownField.getAllOptionValues(context, WidgetWorker.getDelegator(context));

        String event = modelFormField.getEvent();
        String action = modelFormField.getAction(context);

        String currentValue = modelFormField.getEntry(context);
        // Get the current value's description from the option value. If there
        // is a localized version it will be there.
        String currentDescription = null;
        if (UtilValidate.isNotEmpty(currentValue)) {
            for (ModelFormField.OptionValue optionValue : allOptionValues) {
                if (encode(optionValue.getKey(), modelFormField, context).equals(currentValue)) {
                    currentDescription = optionValue.getDescription();
                    break;
                }
            }
        }

        if (ajaxEnabled) {
            writer.append("<input type=\"text\"");
        } else {
            writer.append("<select");
        }

        appendClassNames(writer, context, modelFormField);

        writer.append(" name=\"");
        writer.append(modelFormField.getParameterName(context));

        String idName = modelFormField.getCurrentContainerId(context);

        if (ajaxEnabled) {
            writer.append("_description\"");

            String textFieldIdName = idName;
            if (UtilValidate.isNotEmpty(textFieldIdName)) {
                textFieldIdName += "_description";
                writer.append(" id=\"");
                writer.append(textFieldIdName);
                writer.append('"');
            }

            if (UtilValidate.isNotEmpty(currentValue)) {
                writer.append(" value=\"");
                String explicitDescription = null;
                if (currentDescription != null) {
                    explicitDescription = currentDescription;
                } else {
                    explicitDescription = dropDownField.getCurrentDescription(context);
                }
                if (UtilValidate.isEmpty(explicitDescription)) {
                    explicitDescription = FieldInfoWithOptions.getDescriptionForOptionKey(currentValue, allOptionValues);
                }
                explicitDescription = encode(explicitDescription, modelFormField, context);
                writer.append(explicitDescription);
                writer.append('"');
            }
            writer.append("/>");

            appendWhitespace(writer);
            writer.append("<input type=\"hidden\" name=\"");
            writer.append(modelFormField.getParameterName(context));
            writer.append('"');
            if (UtilValidate.isNotEmpty(idName)) {
                writer.append(" id=\"");
                writer.append(idName);
                writer.append('"');
            }

            if (UtilValidate.isNotEmpty(currentValue)) {
                writer.append(" value=\"");
                //String explicitDescription = dropDownField.getCurrentDescription(context);
                writer.append(currentValue);
                writer.append('"');
            }

            writer.append("/>");

            appendWhitespace(writer);
            writer.append("<script language=\"JavaScript\" type=\"text/javascript\">");
            appendWhitespace(writer);
            writer.append("var data = {");
            int count = 0;
            for (ModelFormField.OptionValue optionValue: allOptionValues) {
                count++;
                writer.append(optionValue.getKey()).append(": ");
                writer.append(" '").append(optionValue.getDescription()).append("'");
                if (count != allOptionValues.size()) {
                    writer.append(", ");
                }
            }
            writer.append("};");
            appendWhitespace(writer);
            writer.append("ajaxAutoCompleteDropDown('").append(textFieldIdName).append("', '").append(idName).append("', data, {autoSelect: ").append(
                    autoComplete.getAutoSelect()).append(", frequency: ").append(autoComplete.getFrequency()).append(", minChars: ").append(autoComplete.getMinChars()).append(
                    ", choices: ").append(autoComplete.getChoices()).append(", partialSearch: ").append(autoComplete.getPartialSearch()).append(
                    ", partialChars: ").append(autoComplete.getPartialChars()).append(", ignoreCase: ").append(autoComplete.getIgnoreCase()).append(
                    ", fullSearch: ").append(autoComplete.getFullSearch()).append("});");
            appendWhitespace(writer);
            writer.append("</script>");
        } else {
            writer.append('"');

            if (UtilValidate.isNotEmpty(idName)) {
                writer.append(" id=\"");
                writer.append(idName);
                writer.append('"');
            }

            if (dropDownField.getAllowMultiple()) {
                writer.append(" multiple=\"multiple\"");
            }

            int otherFieldSize = dropDownField.getOtherFieldSize();
            String otherFieldName = dropDownField.getParameterNameOther(context);
            if (otherFieldSize > 0) {
                //writer.append(" onchange=\"alert('ONCHANGE, process_choice:' + process_choice)\"");
                //writer.append(" onchange='test_js()' ");
                writer.append(" onchange=\"process_choice(this,document.");
                writer.append(modelForm.getName());
                writer.append(".");
                writer.append(otherFieldName);
                writer.append(")\" ");
            }

            if (UtilValidate.isNotEmpty(event) && UtilValidate.isNotEmpty(action)) {
                writer.append(" ");
                writer.append(event);
                writer.append("=\"");
                writer.append(action);
                writer.append('"');
            }

            writer.append(" size=\"").append(dropDownField.getSize()).append("\">");

            // if the current value should go first, stick it in
            if (UtilValidate.isNotEmpty(currentValue) && "first-in-list".equals(dropDownField.getCurrent())) {
                writer.append("<option");
                writer.append(" selected=\"selected\"");
                writer.append(" value=\"");
                writer.append(currentValue);
                writer.append("\">");
                String explicitDescription = (currentDescription != null ? currentDescription : dropDownField.getCurrentDescription(context));
                if (UtilValidate.isNotEmpty(explicitDescription)) {
                    writer.append(encode(explicitDescription, modelFormField, context));
                } else {
                    String description = FieldInfoWithOptions.getDescriptionForOptionKey(currentValue, allOptionValues);
                    writer.append(encode(description, modelFormField, context));
                }
                writer.append("</option>");

                // add a "separator" option
                writer.append("<option value=\"");
                writer.append(currentValue);
                writer.append("\">---</option>");
            }

            // if allow empty is true, add an empty option
            if (dropDownField.getAllowEmpty()) {
                writer.append("<option value=\"\">&nbsp;</option>");
            }

            // list out all options according to the option list
            for (ModelFormField.OptionValue optionValue: allOptionValues) {
                String noCurrentSelectedKey = dropDownField.getNoCurrentSelectedKey(context);
                writer.append("<option");
                // if current value should be selected in the list, select it
                if (UtilValidate.isNotEmpty(currentValue) && currentValue.equals(optionValue.getKey()) && "selected".equals(dropDownField.getCurrent())) {
                    writer.append(" selected=\"selected\"");
                } else if (UtilValidate.isEmpty(currentValue) && noCurrentSelectedKey != null && noCurrentSelectedKey.equals(optionValue.getKey())) {
                    writer.append(" selected=\"selected\"");
                }
                writer.append(" value=\"");
                writer.append(encode(optionValue.getKey(), modelFormField, context));
                writer.append("\">");
                writer.append(encode(optionValue.getDescription(), modelFormField, context));
                writer.append("</option>");
            }

            writer.append("</select>");


            // Adapted from work by Yucca Korpela
            // http://www.cs.tut.fi/~jkorpela/forms/combo.html
            if (otherFieldSize > 0) {

                String fieldName = modelFormField.getParameterName(context);
                Map<String, Object> dataMap = UtilGenerics.checkMap(modelFormField.getMap(context));
                if (dataMap == null) {
                    dataMap = context;
                }
                Object otherValueObj = dataMap.get(otherFieldName);
                String otherValue = (otherValueObj == null) ? "" : otherValueObj.toString();

                writer.append("<noscript>");
                writer.append("<input type='text' name='");
                writer.append(otherFieldName);
                writer.append("'/> ");
                writer.append("</noscript>");
                writer.append("\n<script type='text/javascript' language='JavaScript'><!--");
                writer.append("\ndisa = ' disabled';");
                writer.append("\nif (other_choice(document.");
                writer.append(modelForm.getName());
                writer.append(".");
                writer.append(fieldName);
                writer.append(")) disa = '';");
                writer.append("\ndocument.write(\"<input type=");
                writer.append("'text' name='");
                writer.append(otherFieldName);
                writer.append("' value='");
                writer.append(otherValue);
                writer.append("' size='");
                writer.append(Integer.toString(otherFieldSize));
                writer.append("' ");
                writer.append("\" +disa+ \" onfocus='check_choice(document.");
                writer.append(modelForm.getName());
                writer.append(".");
                writer.append(fieldName);
                writer.append(")'/>\");");
                writer.append("\nif (disa && document.styleSheets)");
                writer.append(" document.");
                writer.append(modelForm.getName());
                writer.append(".");
                writer.append(otherFieldName);
                writer.append(".style.visibility  = 'hidden';");
                writer.append("\n//--></script>");
            }
        }

        this.makeHyperlinkString(writer, dropDownField.getSubHyperlink(), context);

        this.appendTooltip(writer, context, modelFormField);

        //appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderCheckField(java.io.Writer, java.util.Map, org.ofbiz.widget.model.ModelFormField.CheckField)
     */
    public void renderCheckField(Appendable writer, Map<String, Object> context, CheckField checkField) throws IOException {
        ModelFormField modelFormField = checkField.getModelFormField();
        String currentValue = modelFormField.getEntry(context);
        Boolean allChecked = checkField.isAllChecked(context);

        List<ModelFormField.OptionValue> allOptionValues = checkField.getAllOptionValues(context, WidgetWorker.getDelegator(context));
        String event = modelFormField.getEvent();
        String action = modelFormField.getAction(context);

        // list out all options according to the option list
        for (ModelFormField.OptionValue optionValue: allOptionValues) {

            writer.append("<input type=\"checkbox\"");

            appendClassNames(writer, context, modelFormField);

            // if current value should be selected in the list, select it
            if (Boolean.TRUE.equals(allChecked)) {
                writer.append(" checked=\"checked\"");
            } else if (Boolean.FALSE.equals(allChecked)) {
                // do nothing
            } else if (UtilValidate.isNotEmpty(currentValue) && currentValue.equals(optionValue.getKey())) {
                writer.append(" checked=\"checked\"");
            }
            writer.append(" name=\"");
            writer.append(modelFormField.getParameterName(context));
            writer.append('"');
            writer.append(" value=\"");
            writer.append(encode(optionValue.getKey(), modelFormField, context));
            writer.append("\"");

            if (UtilValidate.isNotEmpty(event) && UtilValidate.isNotEmpty(action)) {
                writer.append(" ");
                writer.append(event);
                writer.append("=\"");
                writer.append(action);
                writer.append('"');
            }

            writer.append("/>");

            writer.append(encode(optionValue.getDescription(), modelFormField, context));
        }

        this.appendTooltip(writer, context, modelFormField);

        //appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderRadioField(java.io.Writer, java.util.Map, org.ofbiz.widget.model.ModelFormField.RadioField)
     */
    public void renderRadioField(Appendable writer, Map<String, Object> context, RadioField radioField) throws IOException {
        ModelFormField modelFormField = radioField.getModelFormField();
        List<ModelFormField.OptionValue> allOptionValues = radioField.getAllOptionValues(context, WidgetWorker.getDelegator(context));
        String currentValue = modelFormField.getEntry(context);
        String event = modelFormField.getEvent();
        String action = modelFormField.getAction(context);

        // list out all options according to the option list
        for (ModelFormField.OptionValue optionValue: allOptionValues) {

            writer.append("<span");

            appendClassNames(writer, context, modelFormField);

            writer.append("><input type=\"radio\"");

            // if current value should be selected in the list, select it
            String noCurrentSelectedKey = radioField.getNoCurrentSelectedKey(context);
            if (UtilValidate.isNotEmpty(currentValue) && currentValue.equals(optionValue.getKey())) {
                writer.append(" checked=\"checked\"");
            } else if (UtilValidate.isEmpty(currentValue) && noCurrentSelectedKey != null && noCurrentSelectedKey.equals(optionValue.getKey())) {
                writer.append(" checked=\"checked\"");
            }
            writer.append(" name=\"");
            writer.append(modelFormField.getParameterName(context));
            writer.append('"');
            writer.append(" value=\"");
            writer.append(encode(optionValue.getKey(), modelFormField, context));
            writer.append("\"");

            if (UtilValidate.isNotEmpty(event) && UtilValidate.isNotEmpty(action)) {
                writer.append(" ");
                writer.append(event);
                writer.append("=\"");
                writer.append(action);
                writer.append('"');
            }

            writer.append("/>");

            writer.append(encode(optionValue.getDescription(), modelFormField, context));
            writer.append("</span>");
        }

        this.appendTooltip(writer, context, modelFormField);

        //appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderSubmitField(java.io.Writer, java.util.Map, org.ofbiz.widget.model.ModelFormField.SubmitField)
     */
    public void renderSubmitField(Appendable writer, Map<String, Object> context, SubmitField submitField) throws IOException {
        ModelFormField modelFormField = submitField.getModelFormField();
        ModelForm modelForm = modelFormField.getModelForm();
        String event = null;
        String action = null;
        String confirmation =  encode(submitField.getConfirmation(context), modelFormField, context);

        if ("text-link".equals(submitField.getButtonType())) {
            writer.append("<a");

            appendClassNames(writer, context, modelFormField);
            if (UtilValidate.isNotEmpty(confirmation)) {
                writer.append(" onclick=\"return confirm('");
                writer.append(confirmation);
                writer.append("'); \" ");
            }

            writer.append(" href=\"javascript:document.");
            writer.append(FormRenderer.getCurrentFormName(modelForm, context));
            writer.append(".submit()\">");

            writer.append(encode(modelFormField.getTitle(context), modelFormField, context));

            writer.append("</a>");
        } else if ("image".equals(submitField.getButtonType())) {
            writer.append("<input type=\"image\"");

            appendClassNames(writer, context, modelFormField);

            writer.append(" name=\"");
            writer.append(modelFormField.getParameterName(context));
            writer.append('"');

            String title = modelFormField.getTitle(context);
            if (UtilValidate.isNotEmpty(title)) {
                writer.append(" alt=\"");
                writer.append(encode(title, modelFormField, context));
                writer.append('"');
            }

            writer.append(" src=\"");
            this.appendContentUrl(writer, submitField.getImageLocation(context));
            writer.append('"');

            event = modelFormField.getEvent();
            action = modelFormField.getAction(context);
            if (UtilValidate.isNotEmpty(event) && UtilValidate.isNotEmpty(action)) {
                writer.append(" ");
                writer.append(event);
                writer.append("=\"");
                writer.append(action);
                writer.append('"');
            }

            if (UtilValidate.isNotEmpty(confirmation)) {
                writer.append("onclick=\" return confirm('");
                writer.append(confirmation);
                writer.append("); \" ");
            }

            writer.append("/>");
        } else {
            // default to "button"

            String formId = modelForm.getContainerId();
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
            if (ajaxEnabled) {
                writer.append("<input type=\"button\"");
            } else {
                writer.append("<input type=\"submit\"");
            }

            appendClassNames(writer, context, modelFormField);

            writer.append(" name=\"");
            writer.append(modelFormField.getParameterName(context));
            writer.append('"');

            String title = modelFormField.getTitle(context);
            if (UtilValidate.isNotEmpty(title)) {
                writer.append(" value=\"");
                writer.append(encode(title, modelFormField, context));
                writer.append('"');
            }


            event = modelFormField.getEvent();
            action = modelFormField.getAction(context);
            if (UtilValidate.isNotEmpty(event) && UtilValidate.isNotEmpty(action)) {
                writer.append(" ");
                writer.append(event);
                writer.append("=\"");
                writer.append(action);
                writer.append('"');
            } else {
                //add single click JS onclick
                // disabling for now, using form onSubmit action instead: writer.append(singleClickAction);
            }

            if (ajaxEnabled) {
                writer.append(" onclick=\"");
                if (UtilValidate.isNotEmpty(confirmation)) {
                    writer.append("if  (confirm('");
                    writer.append(confirmation);
                    writer.append(");) ");
                }
                writer.append("ajaxSubmitFormUpdateAreas('");
                writer.append(formId);
                writer.append("', '").append(createAjaxParamsFromUpdateAreas(updateAreas, null, context));
                writer.append("')\"");
            }

            writer.append("/>");
        }

        this.appendTooltip(writer, context, modelFormField);

        //appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderResetField(java.io.Writer, java.util.Map, org.ofbiz.widget.model.ModelFormField.ResetField)
     */
    public void renderResetField(Appendable writer, Map<String, Object> context, ResetField resetField) throws IOException {
        ModelFormField modelFormField = resetField.getModelFormField();

        writer.append("<input type=\"reset\"");

        appendClassNames(writer, context, modelFormField);

        writer.append(" name=\"");
        writer.append(modelFormField.getParameterName(context));
        writer.append('"');

        String title = modelFormField.getTitle(context);
        if (UtilValidate.isNotEmpty(title)) {
            writer.append(" value=\"");
            writer.append(encode(title, modelFormField, context));
            writer.append('"');
        }

        writer.append("/>");

        this.appendTooltip(writer, context, modelFormField);

        //appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderHiddenField(java.io.Writer, java.util.Map, org.ofbiz.widget.model.ModelFormField.HiddenField)
     */
    public void renderHiddenField(Appendable writer, Map<String, Object> context, HiddenField hiddenField) throws IOException {
        ModelFormField modelFormField = hiddenField.getModelFormField();
        String value = hiddenField.getValue(context);
        this.renderHiddenField(writer, context, modelFormField, value);
    }

    public void renderHiddenField(Appendable writer, Map<String, Object> context, ModelFormField modelFormField, String value) throws IOException {
        writer.append("<input type=\"hidden\"");

        writer.append(" name=\"");
        writer.append(modelFormField.getParameterName(context));
        writer.append('"');

        if (UtilValidate.isNotEmpty(value)) {
            writer.append(" value=\"");
            writer.append(value);
            writer.append('"');
        }

        writer.append("/>");
        appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderIgnoredField(java.io.Writer, java.util.Map, org.ofbiz.widget.model.ModelFormField.IgnoredField)
     */
    public void renderIgnoredField(Appendable writer, Map<String, Object> context, IgnoredField ignoredField) throws IOException {
        // do nothing, it's an ignored field; could add a comment or something if we wanted to
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFieldTitle(java.io.Writer, java.util.Map, org.ofbiz.widget.model.ModelFormField)
     */
    public void renderFieldTitle(Appendable writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
        String tempTitleText = modelFormField.getTitle(context);
        String titleText = UtilHttp.encodeAmpersands(tempTitleText);

        if (UtilValidate.isNotEmpty(titleText)) {
            // copied from MacroFormRenderer renderFieldTitle
            String displayHelpText = UtilProperties.getPropertyValue("widget.properties", "widget.form.displayhelpText");
            String helpText = null;
            if ("Y".equals(displayHelpText)) {
                Delegator delegator = WidgetWorker.getDelegator(context);
                Locale locale = (Locale)context.get("locale");
                String entityName = modelFormField.getEntityName();
                String fieldName = modelFormField.getFieldName();
                helpText = UtilHelpText.getEntityFieldDescription(entityName, fieldName, delegator, locale);
            }
            if (UtilValidate.isNotEmpty(modelFormField.getTitleStyle()) || UtilValidate.isNotEmpty(helpText)) {
                writer.append("<span");
                if (UtilValidate.isNotEmpty(modelFormField.getTitleStyle())){
                    writer.append(" class=\"");
                    writer.append(modelFormField.getTitleStyle());
                }
                if (UtilValidate.isNotEmpty(helpText)){
                    writer.append(" title=\"");
                    writer.append(FreeMarkerWorker.encodeDoubleQuotes(helpText));
                }
                writer.append("\">");
            }
            if (" ".equals(titleText)) {
                // If the title content is just a blank then render it colling renderFormatEmptySpace:
                // the method will set its content to work fine in most browser
                this.renderFormatEmptySpace(writer, context, modelFormField.getModelForm());
            } else {
                renderHyperlinkTitle(writer, context, modelFormField, titleText);
            }

            if (UtilValidate.isNotEmpty(modelFormField.getTitleStyle())) {
                writer.append("</span>");
            }

            //appendWhitespace(writer);
        }
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFieldTitle(java.io.Writer, java.util.Map, org.ofbiz.widget.model.ModelFormField)
     */
    public void renderSingleFormFieldTitle(Appendable writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
        boolean requiredField = modelFormField.getRequiredField();
        if (requiredField) {

            String requiredStyle = modelFormField.getRequiredFieldStyle();
            if (UtilValidate.isEmpty(requiredStyle)) {
                requiredStyle = modelFormField.getTitleStyle();
            }

            if (UtilValidate.isNotEmpty(requiredStyle)) {
                writer.append("<span class=\"");
                writer.append(requiredStyle);
                writer.append("\">");
            }
            renderHyperlinkTitle(writer, context, modelFormField, modelFormField.getTitle(context));
            if (UtilValidate.isNotEmpty(requiredStyle)) {
                writer.append("</span>");
            }

            //appendWhitespace(writer);
        } else {
            renderFieldTitle(writer, context, modelFormField);
        }
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormOpen(java.io.Writer, java.util.Map, org.ofbiz.widget.form.model.ModelForm)
     */
    public void renderFormOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        this.widgetCommentsEnabled = ModelWidget.widgetBoundaryCommentsEnabled(context);
        renderBeginningBoundaryComment(writer, "Form Widget - Form Element", modelForm);
        writer.append("<form method=\"post\" ");
        String targetType = modelForm.getTargetType();
        String targ = modelForm.getTarget(context, targetType);
        // The 'action' attribute is mandatory in a form definition,
        // even if it is empty.
        writer.append(" action=\"");
        if (UtilValidate.isNotEmpty(targ)) {
            //this.appendOfbizUrl(writer, "/" + targ);
            WidgetWorker.buildHyperlinkUrl(writer, targ, targetType, null, null, false, false, true, request, response, context);
        }
        writer.append("\" ");

        String formType = modelForm.getType();
        if (formType.equals("upload")) {
            writer.append(" enctype=\"multipart/form-data\"");
        }

        String targetWindow = modelForm.getTargetWindow(context);
        if (UtilValidate.isNotEmpty(targetWindow)) {
            writer.append(" target=\"");
            writer.append(targetWindow);
            writer.append("\"");
        }

        String containerId = FormRenderer.getCurrentContainerId(modelForm, context);
        if (UtilValidate.isNotEmpty(containerId)) {
            writer.append(" id=\"");
            writer.append(containerId);
            writer.append("\"");
        }

        writer.append(" class=\"");
        String containerStyle =  modelForm.getContainerStyle();
        if (UtilValidate.isNotEmpty(containerStyle)) {
            writer.append(containerStyle);
        } else {
            writer.append("basic-form");
        }
        writer.append("\"");

        writer.append(" onsubmit=\"javascript:submitFormDisableSubmits(this)\"");

        if (!modelForm.getClientAutocompleteFields()) {
            writer.append(" autocomplete=\"off\"");
        }

        writer.append(" name=\"");
        writer.append(FormRenderer.getCurrentContainerId(modelForm, context));
        writer.append("\">");

        boolean useRowSubmit = modelForm.getUseRowSubmit();
        if (useRowSubmit) {
            writer.append("<input type=\"hidden\" name=\"_useRowSubmit\" value=\"Y\"/>");
        }

        appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormClose(java.io.Writer, java.util.Map, org.ofbiz.widget.form.model.ModelForm)
     */
    public void renderFormClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.append("</form>");
        String focusFieldName = FormRenderer.getFocusFieldName(modelForm, context);
        if (UtilValidate.isNotEmpty(focusFieldName)) {
            appendWhitespace(writer);
            writer.append("<script language=\"JavaScript\" type=\"text/javascript\">");
            appendWhitespace(writer);
            writer.append("document.").append(FormRenderer.getCurrentFormName(modelForm, context)).append(".");
            writer.append(focusFieldName).append(".focus();");
            appendWhitespace(writer);
            writer.append("</script>");
        }
        appendWhitespace(writer);
        renderEndingBoundaryComment(writer, "Form Widget - Form Element", modelForm);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormClose(java.io.Writer, java.util.Map, org.ofbiz.widget.form.model.ModelForm)
     */
    public void renderMultiFormClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        for (ModelFormField submitField: modelForm.getMultiSubmitFields()) {
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

        this.widgetCommentsEnabled = ModelWidget.widgetBoundaryCommentsEnabled(context);
        renderBeginningBoundaryComment(writer, "Form Widget", modelForm);

        if (this.renderPagination) {
            this.renderNextPrev(writer, context, modelForm);
        }
        writer.append(" <table cellspacing=\"0\" class=\"");
        if (UtilValidate.isNotEmpty(modelForm.getDefaultTableStyle())) {
            writer.append(FlexibleStringExpander.expandString(modelForm.getDefaultTableStyle(), context));
        } else {
            writer.append("basic-table form-widget-table dark-grid");
        }
        writer.append("\">");
        appendWhitespace(writer);
    }

    public void renderFormatListWrapperClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.append(" </table>");

        appendWhitespace(writer);
        if (this.renderPagination) {
            this.renderNextPrev(writer, context, modelForm);
        }
        renderEndingBoundaryComment(writer, "Form Widget - Formal List Wrapper", modelForm);
    }
    
    public void renderFormatHeaderOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.append("  <thead");
        writer.append(">");
        appendWhitespace(writer);
    }
        
    public void renderFormatHeaderClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.append("  </thead");
        writer.append(">");
        appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatHeaderRowOpen(java.io.Writer, java.util.Map, org.ofbiz.widget.form.model.ModelForm)
     */
    public void renderFormatHeaderRowOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.append("  <tr");
        String headerStyle = FlexibleStringExpander.expandString(modelForm.getHeaderRowStyle(), context);
        writer.append(" class=\"");
        if (UtilValidate.isNotEmpty(headerStyle)) {
            writer.append(headerStyle);
        } else {
            writer.append("header-row");
        }
        writer.append("\">");
        appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatHeaderRowClose(java.io.Writer, java.util.Map, org.ofbiz.widget.form.model.ModelForm)
     */
    public void renderFormatHeaderRowClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.append("  </tr>");

        appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatHeaderRowCellOpen(java.io.Writer, java.util.Map, org.ofbiz.widget.form.model.ModelForm, org.ofbiz.widget.model.ModelFormField, int positionSpan)
     */
    public void renderFormatHeaderRowCellOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField, int positionSpan) throws IOException {
        writer.append("   <td");
        String areaStyle = modelFormField.getTitleAreaStyle();
        if (positionSpan > 1) {
            writer.append(" colspan=\"");
            writer.append(Integer.toString(positionSpan));
            writer.append("\"");
        }
        if (UtilValidate.isNotEmpty(areaStyle)) {
            writer.append(" class=\"");
            writer.append(areaStyle);
            writer.append("\"");
        }
        writer.append(">");
        //appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatHeaderRowCellClose(java.io.Writer, java.util.Map, org.ofbiz.widget.form.model.ModelForm, org.ofbiz.widget.model.ModelFormField)
     */
    public void renderFormatHeaderRowCellClose(Appendable writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField) throws IOException {
        writer.append("</td>");
        appendWhitespace(writer);
    }

    public void renderFormatHeaderRowFormCellOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.append("   <td");
        String areaStyle = modelForm.getFormTitleAreaStyle();
        if (UtilValidate.isNotEmpty(areaStyle)) {
            writer.append(" class=\"");
            writer.append(areaStyle);
            writer.append("\"");
        }
        writer.append(">");
        //appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatHeaderRowFormCellClose(java.io.Writer, java.util.Map, org.ofbiz.widget.form.model.ModelForm)
     */
    public void renderFormatHeaderRowFormCellClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.append("</td>");
        appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatHeaderRowFormCellTitleSeparator(java.io.Writer, java.util.Map, org.ofbiz.widget.form.model.ModelForm, boolean)
     */
    public void renderFormatHeaderRowFormCellTitleSeparator(Appendable writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField, boolean isLast) throws IOException {

        String titleStyle = modelFormField.getTitleStyle();
        if (UtilValidate.isNotEmpty(titleStyle)) {
            writer.append("<span class=\"");
            writer.append(titleStyle);
            writer.append("\">");
        }
        if (isLast) {
            writer.append(" - ");
        } else {
            writer.append(" - ");
        }
        if (UtilValidate.isNotEmpty(titleStyle)) {
            writer.append("</span>");
        }
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatItemRowOpen(java.io.Writer, java.util.Map, org.ofbiz.widget.form.model.ModelForm)
     */
    public void renderFormatItemRowOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        Integer itemIndex = (Integer)context.get("itemIndex");

        writer.append("  <tr");
        if (itemIndex!=null) {

            String altRowStyles = modelForm.getStyleAltRowStyle(context);
            if (itemIndex.intValue() % 2 == 0) {
                String evenRowStyle = modelForm.getEvenRowStyle();
                if (UtilValidate.isNotEmpty(evenRowStyle)) {
                    writer.append(" class=\"");
                    writer.append(evenRowStyle);
                    if (UtilValidate.isNotEmpty(altRowStyles)) {
                        writer.append(altRowStyles);
                    }
                    writer.append("\"");
                } else {
                    if (UtilValidate.isNotEmpty(altRowStyles)) {
                        writer.append(" class=\"");
                        writer.append(altRowStyles);
                        writer.append("\"");
                    }
                }
            } else {
                String oddRowStyle = FlexibleStringExpander.expandString(modelForm.getOddRowStyle(), context);
                if (UtilValidate.isNotEmpty(oddRowStyle)) {
                    writer.append(" class=\"");
                    writer.append(oddRowStyle);
                    if (UtilValidate.isNotEmpty(altRowStyles)) {
                        writer.append(altRowStyles);
                    }
                    writer.append("\"");
                } else {
                    if (UtilValidate.isNotEmpty(altRowStyles)) {
                        writer.append(" class=\"");
                        writer.append(altRowStyles);
                        writer.append("\"");
                    }
                }
            }
        }
        writer.append(">");
        appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatItemRowClose(java.io.Writer, java.util.Map, org.ofbiz.widget.form.model.ModelForm)
     */
    public void renderFormatItemRowClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.append("  </tr>");

        appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatItemRowCellOpen(java.io.Writer, java.util.Map, org.ofbiz.widget.form.model.ModelForm, org.ofbiz.widget.model.ModelFormField)
     */
    public void renderFormatItemRowCellOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField, int positionSpan) throws IOException {
        writer.append("   <td");
        String areaStyle = modelFormField.getWidgetAreaStyle();
        if (positionSpan > 1) {
            writer.append(" colspan=\"");
            writer.append(Integer.toString(positionSpan));
            writer.append("\"");
        }
        if (UtilValidate.isNotEmpty(areaStyle)) {
            writer.append(" class=\"");
            writer.append(areaStyle);
            writer.append("\"");
        }
        writer.append(">");
        //appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatItemRowCellClose(java.io.Writer, java.util.Map, org.ofbiz.widget.form.model.ModelForm, org.ofbiz.widget.model.ModelFormField)
     */
    public void renderFormatItemRowCellClose(Appendable writer, Map<String, Object> context, ModelForm modelForm, ModelFormField modelFormField) throws IOException {
        writer.append("</td>");
        appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatItemRowFormCellOpen(java.io.Writer, java.util.Map, org.ofbiz.widget.form.model.ModelForm)
     */
    public void renderFormatItemRowFormCellOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.append("   <td");
        String areaStyle = modelForm.getFormWidgetAreaStyle();
        if (UtilValidate.isNotEmpty(areaStyle)) {
            writer.append(" class=\"");
            writer.append(areaStyle);
            writer.append("\"");
        }
        writer.append(">");
        //appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatItemRowFormCellClose(java.io.Writer, java.util.Map, org.ofbiz.widget.form.model.ModelForm)
     */
    public void renderFormatItemRowFormCellClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.append("</td>");
        appendWhitespace(writer);
    }

    public void renderFormatSingleWrapperOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.append(" <table cellspacing=\"0\"");
        if (UtilValidate.isNotEmpty(modelForm.getDefaultTableStyle())) {
            writer.append(" class=\"").append(FlexibleStringExpander.expandString(modelForm.getDefaultTableStyle(), context)).append("\"");
        }
        writer.append(">");
        appendWhitespace(writer);
    }

    public void renderFormatSingleWrapperClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.append(" </table>");
        appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatFieldRowOpen(java.io.Writer, java.util.Map, org.ofbiz.widget.form.model.ModelForm)
     */
    public void renderFormatFieldRowOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.append("  <tr>");
        appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatFieldRowClose(java.io.Writer, java.util.Map, org.ofbiz.widget.form.model.ModelForm)
     */
    public void renderFormatFieldRowClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.append("  </tr>");
        appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatFieldRowTitleCellOpen(java.io.Writer, java.util.Map, org.ofbiz.widget.model.ModelFormField)
     */
    public void renderFormatFieldRowTitleCellOpen(Appendable writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
        writer.append("   <td");
        String areaStyle = modelFormField.getTitleAreaStyle();
        if (UtilValidate.isNotEmpty(areaStyle)) {
            writer.append(" class=\"");
            writer.append(areaStyle);
            writer.append("\"");
        } else {
            writer.append(" class=\"label\"");
        }
        writer.append(">");
        //appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatFieldRowTitleCellClose(java.io.Writer, java.util.Map, org.ofbiz.widget.model.ModelFormField)
     */
    public void renderFormatFieldRowTitleCellClose(Appendable writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
        writer.append("</td>");
        appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatFieldRowSpacerCell(java.io.Writer, java.util.Map, org.ofbiz.widget.model.ModelFormField)
     */
    public void renderFormatFieldRowSpacerCell(Appendable writer, Map<String, Object> context, ModelFormField modelFormField) throws IOException {
        // Embedded styling - bad idea
        //writer.append("<td>&nbsp;</td>");

        //appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatFieldRowWidgetCellOpen(java.io.Writer, java.util.Map, org.ofbiz.widget.model.ModelFormField, int)
     */
    public void renderFormatFieldRowWidgetCellOpen(Appendable writer, Map<String, Object> context, ModelFormField modelFormField, int positions, int positionSpan, Integer nextPositionInRow) throws IOException {
//        writer.append("<td width=\"");
//        if (nextPositionInRow != null || modelFormField.getPosition() > 1) {
//            writer.append("30");
//        } else {
//            writer.append("80");
//        }
//        writer.append("%\"");
        writer.append("   <td");
        if (positionSpan > 0) {
            writer.append(" colspan=\"");
            // do a span of 1 for this column, plus 3 columns for each spanned
            //position or each blank position that this will be filling in
            writer.append(Integer.toString(1 + (positionSpan * 3)));
            writer.append("\"");
        }
        String areaStyle = modelFormField.getWidgetAreaStyle();
        if (UtilValidate.isNotEmpty(areaStyle)) {
            writer.append(" class=\"");
            writer.append(areaStyle);
            writer.append("\"");
        }
        writer.append(">");

        //appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFormatFieldRowWidgetCellClose(java.io.Writer, java.util.Map, org.ofbiz.widget.model.ModelFormField, int)
     */
    public void renderFormatFieldRowWidgetCellClose(Appendable writer, Map<String, Object> context, ModelFormField modelFormField, int positions, int positionSpan, Integer nextPositionInRow) throws IOException {
        writer.append("</td>");
        appendWhitespace(writer);
    }

    public void renderFormatEmptySpace(Appendable writer, Map<String, Object> context, ModelForm modelForm) throws IOException {
        writer.append("&nbsp;");
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderTextFindField(java.io.Writer, java.util.Map, org.ofbiz.widget.model.ModelFormField.TextFindField)
     */
    public void renderTextFindField(Appendable writer, Map<String, Object> context, TextFindField textFindField) throws IOException {

        ModelFormField modelFormField = textFindField.getModelFormField();

        String defaultOption = textFindField.getDefaultOption();
        Locale locale = (Locale)context.get("locale");
        if (!textFindField.getHideOptions()) {
            String opEquals = UtilProperties.getMessage("conditional", "equals", locale);
            String opBeginsWith = UtilProperties.getMessage("conditional", "begins_with", locale);
            String opContains = UtilProperties.getMessage("conditional", "contains", locale);
            String opIsEmpty = UtilProperties.getMessage("conditional", "is_empty", locale);
            String opNotEqual = UtilProperties.getMessage("conditional", "not_equal", locale);
            writer.append(" <select name=\"");
            writer.append(modelFormField.getParameterName(context));
            writer.append("_op\" class=\"selectBox\">");
            writer.append("<option value=\"equals\"").append(("equals".equals(defaultOption)? " selected": "")).append(">").append(opEquals).append("</option>");
            writer.append("<option value=\"like\"").append(("like".equals(defaultOption)? " selected": "")).append(">").append(opBeginsWith).append("</option>");
            writer.append("<option value=\"contains\"").append(("contains".equals(defaultOption)? " selected": "")).append(">").append(opContains).append("</option>");
            writer.append("<option value=\"empty\"").append(("empty".equals(defaultOption)? " selected": "")).append(">").append(opIsEmpty).append("</option>");
            writer.append("<option value=\"notEqual\"").append(("notEqual".equals(defaultOption)? " selected": "")).append(">").append(opNotEqual).append("</option>");
            writer.append("</select>");
        } else {
            writer.append(" <input type=\"hidden\" name=\"");
            writer.append(modelFormField.getParameterName(context));
            writer.append("_op\" value=\"").append(defaultOption).append("\"/>");
        }

        writer.append("<input type=\"text\"");

        appendClassNames(writer, context, modelFormField);

        writer.append(" name=\"");
        writer.append(modelFormField.getParameterName(context));
        writer.append('"');

        String value = modelFormField.getEntry(context, textFindField.getDefaultValue(context));
        if (UtilValidate.isNotEmpty(value)) {
            writer.append(" value=\"");
            writer.append(value);
            writer.append('"');
        }

        writer.append(" size=\"");
        writer.append(Integer.toString(textFindField.getSize()));
        writer.append('"');

        Integer maxlength = textFindField.getMaxlength();
        if (maxlength != null) {
            writer.append(" maxlength=\"");
            writer.append(maxlength.toString());
            writer.append('"');
        }

        if (!textFindField.getClientAutocompleteField()) {
            writer.append(" autocomplete=\"off\"");
        }

        writer.append("/>");

        if (UtilValidate.isNotEmpty(modelFormField.getTitleStyle())) {
            writer.append(" <span class=\"");
            writer.append(modelFormField.getTitleStyle());
            writer.append("\">");
        }

        String ignoreCase = UtilProperties.getMessage("conditional", "ignore_case", locale);
        boolean ignCase = textFindField.getIgnoreCase();

        if (!textFindField.getHideIgnoreCase()) {
            writer.append(" <input type=\"checkbox\" name=\"");
            writer.append(modelFormField.getParameterName(context));
            writer.append("_ic\" value=\"Y\"").append((ignCase ? " checked=\"checked\"" : "")).append("/>");
            writer.append(ignoreCase);
        } else {
            writer.append("<input type=\"hidden\" name=\"");
            writer.append(modelFormField.getParameterName(context));
            writer.append("_ic\" value=\"").append((ignCase ? "Y" : "")).append("\"/>");
        }

        if (UtilValidate.isNotEmpty(modelFormField.getTitleStyle())) {
            writer.append("</span>");
        }

        this.appendTooltip(writer, context, modelFormField);

        //appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderRangeFindField(java.io.Writer, java.util.Map, org.ofbiz.widget.model.ModelFormField.RangeFindField)
     */
    public void renderRangeFindField(Appendable writer, Map<String, Object> context, RangeFindField rangeFindField) throws IOException {

        ModelFormField modelFormField = rangeFindField.getModelFormField();
        Locale locale = (Locale)context.get("locale");
        String opEquals = UtilProperties.getMessage("conditional", "equals", locale);
        String opGreaterThan = UtilProperties.getMessage("conditional", "greater_than", locale);
        String opGreaterThanEquals = UtilProperties.getMessage("conditional", "greater_than_equals", locale);
        String opLessThan = UtilProperties.getMessage("conditional", "less_than", locale);
        String opLessThanEquals = UtilProperties.getMessage("conditional", "less_than_equals", locale);
        //String opIsEmpty = UtilProperties.getMessage("conditional", "is_empty", locale);

        writer.append("<input type=\"text\"");

        appendClassNames(writer, context, modelFormField);

        writer.append(" name=\"");
        writer.append(modelFormField.getParameterName(context));
        writer.append("_fld0_value\"");

        String value = modelFormField.getEntry(context, rangeFindField.getDefaultValue(context));
        if (UtilValidate.isNotEmpty(value)) {
            writer.append(" value=\"");
            writer.append(value);
            writer.append('"');
        }

        writer.append(" size=\"");
        writer.append(Integer.toString(rangeFindField.getSize()));
        writer.append('"');

        Integer maxlength = rangeFindField.getMaxlength();
        if (maxlength != null) {
            writer.append(" maxlength=\"");
            writer.append(maxlength.toString());
            writer.append('"');
        }

        if (!rangeFindField.getClientAutocompleteField()) {
            writer.append(" autocomplete=\"off\"");
        }

        writer.append("/>");

        if (UtilValidate.isNotEmpty(modelFormField.getTitleStyle())) {
            writer.append(" <span class=\"");
            writer.append(modelFormField.getTitleStyle());
            writer.append("\">");
        }

        String defaultOptionFrom = rangeFindField.getDefaultOptionFrom();
        writer.append(" <select name=\"");
        writer.append(modelFormField.getParameterName(context));
        writer.append("_fld0_op\" class=\"selectBox\">");
        writer.append("<option value=\"equals\"").append(("equals".equals(defaultOptionFrom)? " selected": "")).append(">").append(opEquals).append("</option>");
        writer.append("<option value=\"greaterThan\"").append(("greaterThan".equals(defaultOptionFrom)? " selected": "")).append(">").append(opGreaterThan).append("</option>");
        writer.append("<option value=\"greaterThanEqualTo\"").append(("greaterThanEqualTo".equals(defaultOptionFrom)? " selected": "")).append(">").append(opGreaterThanEquals).append("</option>");
        writer.append("</select>");

        writer.append("</span>");

        writer.append(" <br/> ");

        writer.append("<input type=\"text\"");

        appendClassNames(writer, context, modelFormField);

        writer.append(" name=\"");
        writer.append(modelFormField.getParameterName(context));
        writer.append("_fld1_value\"");

        value = modelFormField.getEntry(context);
        if (UtilValidate.isNotEmpty(value)) {
            writer.append(" value=\"");
            writer.append(value);
            writer.append('"');
        }

        writer.append(" size=\"");
        writer.append(Integer.toString(rangeFindField.getSize()));
        writer.append('"');

        if (maxlength != null) {
            writer.append(" maxlength=\"");
            writer.append(maxlength.toString());
            writer.append('"');
        }

        if (!rangeFindField.getClientAutocompleteField()) {
            writer.append(" autocomplete=\"off\"");
        }

        writer.append("/>");

        String defaultOptionThru = rangeFindField.getDefaultOptionThru();
        writer.append(" <select name=\"");
        writer.append(modelFormField.getParameterName(context));
        writer.append("_fld1_op\" class=\"selectBox\">");
        writer.append("<option value=\"lessThan\"").append(("lessThan".equals(defaultOptionThru)? " selected": "")).append(">").append(opLessThan).append("</option>");
        writer.append("<option value=\"lessThanEqualTo\"").append(("lessThanEqualTo".equals(defaultOptionThru)? " selected": "")).append(">").append(opLessThanEquals).append("</option>");
        writer.append("</select>");

        if (UtilValidate.isNotEmpty(modelFormField.getTitleStyle())) {
            writer.append("</span>");
        }

        this.appendTooltip(writer, context, modelFormField);

        //appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderDateFindField(java.io.Writer, java.util.Map, org.ofbiz.widget.model.ModelFormField.DateFindField)
     */
    public void renderDateFindField(Appendable writer, Map<String, Object> context, DateFindField dateFindField) throws IOException {
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

        Map<String, String> uiLabelMap = UtilGenerics.checkMap(context.get("uiLabelMap"));
        if (uiLabelMap == null) {
            Debug.logWarning("Could not find uiLabelMap in context", module);
        }
        String localizedInputTitle = "", localizedIconTitle = "";

        writer.append("<input type=\"text\"");

        appendClassNames(writer, context, modelFormField);

        writer.append(" name=\"");
        writer.append(modelFormField.getParameterName(context));
        writer.append("_fld0_value\"");

        // the default values for a timestamp
        int size = 25;
        int maxlength = 30;

        if ("date".equals(dateFindField.getType())) {
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
        writer.append(" title=\"");
        writer.append(localizedInputTitle);
        writer.append('"');

        String value = modelFormField.getEntry(context, dateFindField.getDefaultValue(context));
        if (UtilValidate.isNotEmpty(value)) {
            if (value.length() > maxlength) {
                value = value.substring(0, maxlength);
            }
            writer.append(" value=\"");
            writer.append(value);
            writer.append('"');
        }

        writer.append(" size=\"");
        writer.append(Integer.toString(size));
        writer.append('"');

        writer.append(" maxlength=\"");
        writer.append(Integer.toString(maxlength));
        writer.append('"');

        writer.append("/>");

        // search for a localized label for the icon
        if (uiLabelMap != null) {
            localizedIconTitle = uiLabelMap.get("CommonViewCalendar");
        }
        ModelForm modelForm = modelFormField.getModelForm();
        // add calendar pop-up button and seed data IF this is not a "time" type date-find
        if (!"time".equals(dateFindField.getType())) {
            if ("date".equals(dateFindField.getType())) {
                writer.append("<a href=\"javascript:call_cal_notime(document.");
            } else {
                writer.append("<a href=\"javascript:call_cal(document.");
            }
            writer.append(FormRenderer.getCurrentFormName(modelForm, context));
            writer.append('.');
            writer.append(modelFormField.getParameterName(context));
            writer.append("_fld0_value,'");
            writer.append(UtilHttp.encodeBlanks(modelFormField.getEntry(context, dateFindField.getDefaultDateTimeString(context))));
            writer.append("');\">");

            writer.append("<img src=\"");
            this.appendContentUrl(writer, "/images/cal.gif");
            writer.append("\" width=\"16\" height=\"16\" border=\"0\" alt=\"");
            writer.append(localizedIconTitle);
            writer.append("\" title=\"");
            writer.append(localizedIconTitle);
            writer.append("\"/></a>");
        }

        if (UtilValidate.isNotEmpty(modelFormField.getTitleStyle())) {
            writer.append(" <span class=\"");
            writer.append(modelFormField.getTitleStyle());
            writer.append("\">");
        }

        String defaultOptionFrom = dateFindField.getDefaultOptionFrom();
        writer.append(" <select name=\"");
        writer.append(modelFormField.getParameterName(context));
        writer.append("_fld0_op\" class=\"selectBox\">");
        writer.append("<option value=\"equals\"").append(("equals".equals(defaultOptionFrom)? " selected": "")).append(">").append(opEquals).append("</option>");
        writer.append("<option value=\"sameDay\"").append(("sameDay".equals(defaultOptionFrom)? " selected": "")).append(">").append(opSameDay).append("</option>");
        writer.append("<option value=\"greaterThanFromDayStart\"").append(("greaterThanFromDayStart".equals(defaultOptionFrom)? " selected": "")).append(">").append(opGreaterThanFromDayStart).append("</option>");
        writer.append("<option value=\"greaterThan\"").append(("greaterThan".equals(defaultOptionFrom)? " selected": "")).append(">").append(opGreaterThan).append("</option>");
        writer.append("</select>");

        if (UtilValidate.isNotEmpty(modelFormField.getTitleStyle())) {
            writer.append(" </span>");
        }

        writer.append(" <br/> ");

        writer.append("<input type=\"text\"");

        appendClassNames(writer, context, modelFormField);

        writer.append(" name=\"");
        writer.append(modelFormField.getParameterName(context));
        writer.append("_fld1_value\"");

        writer.append(" title=\"");
        writer.append(localizedInputTitle);
        writer.append('"');

        value = modelFormField.getEntry(context);
        if (UtilValidate.isNotEmpty(value)) {
            if (value.length() > maxlength) {
                value = value.substring(0, maxlength);
            }
            writer.append(" value=\"");
            writer.append(value);
            writer.append('"');
        }

        writer.append(" size=\"");
        writer.append(Integer.toString(size));
        writer.append('"');

        writer.append(" maxlength=\"");
        writer.append(Integer.toString(maxlength));
        writer.append('"');

        writer.append("/>");

        // add calendar pop-up button and seed data IF this is not a "time" type date-find
        if (!"time".equals(dateFindField.getType())) {
            if ("date".equals(dateFindField.getType())) {
                writer.append("<a href=\"javascript:call_cal_notime(document.");
            } else {
                writer.append("<a href=\"javascript:call_cal(document.");
            }
            writer.append(FormRenderer.getCurrentFormName(modelForm, context));
            writer.append('.');
            writer.append(modelFormField.getParameterName(context));
            writer.append("_fld1_value,'");
            writer.append(UtilHttp.encodeBlanks(modelFormField.getEntry(context, dateFindField.getDefaultDateTimeString(context))));
            writer.append("');\">");

            writer.append("<img src=\"");
            this.appendContentUrl(writer, "/images/cal.gif");
            writer.append("\" width=\"16\" height=\"16\" border=\"0\" alt=\"");
            writer.append(localizedIconTitle);
            writer.append("\" title=\"");
            writer.append(localizedIconTitle);
            writer.append("\"/></a>");
        }

        if (UtilValidate.isNotEmpty(modelFormField.getTitleStyle())) {
            writer.append(" <span class=\"");
            writer.append(modelFormField.getTitleStyle());
            writer.append("\">");
        }

        String defaultOptionThru = dateFindField.getDefaultOptionThru();
        writer.append(" <select name=\"");
        writer.append(modelFormField.getParameterName(context));
        writer.append("_fld1_op\" class=\"selectBox\">");
        writer.append("<option value=\"lessThan\"").append(("lessThan".equals(defaultOptionThru)? " selected": "")).append(">").append(opLessThan).append("</option>");
        writer.append("<option value=\"upToDay\"").append(("upToDay".equals(defaultOptionThru)? " selected": "")).append(">").append(opUpToDay).append("</option>");
        writer.append("<option value=\"upThruDay\"").append(("upThruDay".equals(defaultOptionThru)? " selected": "")).append(">").append(opUpThruDay).append("</option>");
        writer.append("<option value=\"empty\"").append(("empty".equals(defaultOptionThru)? " selected": "")).append(">").append(opIsEmpty).append("</option>");
        writer.append("</select>");

        if (UtilValidate.isNotEmpty(modelFormField.getTitleStyle())) {
            writer.append("</span>");
        }

        this.appendTooltip(writer, context, modelFormField);

        //appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderLookupField(java.io.Writer, java.util.Map, org.ofbiz.widget.model.ModelFormField.LookupField)
     */
    public void renderLookupField(Appendable writer, Map<String, Object> context, LookupField lookupField) throws IOException {
        ModelFormField modelFormField = lookupField.getModelFormField();

        writer.append("<input type=\"text\"");

        appendClassNames(writer, context, modelFormField);

        writer.append(" name=\"");
        writer.append(modelFormField.getParameterName(context));
        writer.append('"');

        String value = modelFormField.getEntry(context, lookupField.getDefaultValue(context));
        if (UtilValidate.isNotEmpty(value)) {
            writer.append(" value=\"");
            writer.append(value);
            writer.append('"');
        }

        writer.append(" size=\"");
        writer.append(Integer.toString(lookupField.getSize()));
        writer.append('"');

        Integer maxlength = lookupField.getMaxlength();
        if (maxlength != null) {
            writer.append(" maxlength=\"");
            writer.append(maxlength.toString());
            writer.append('"');
        }

        String idName = modelFormField.getCurrentContainerId(context);
        if (UtilValidate.isNotEmpty(idName)) {
            writer.append(" id=\"");
            writer.append(idName);
            writer.append('"');
        }

        List<ModelForm.UpdateArea> updateAreas = modelFormField.getOnChangeUpdateAreas();
        boolean ajaxEnabled = updateAreas != null && this.javaScriptEnabled;
        if (!lookupField.getClientAutocompleteField() || ajaxEnabled) {
            writer.append(" autocomplete=\"off\"");
        }

        writer.append("/>");
        ModelForm modelForm = modelFormField.getModelForm();
        // add lookup pop-up button
        String descriptionFieldName = lookupField.getDescriptionFieldName();
        if (UtilValidate.isNotEmpty(descriptionFieldName)) {
            writer.append("<a href=\"javascript:call_fieldlookup3(document.");
            writer.append(FormRenderer.getCurrentFormName(modelForm, context));
            writer.append('.');
            writer.append(modelFormField.getParameterName(context));
            writer.append(",'");
            writer.append(descriptionFieldName);
            writer.append(",'");
        } else {
            writer.append("<a href=\"javascript:call_fieldlookup2(document.");
            writer.append(FormRenderer.getCurrentFormName(modelForm, context));
            writer.append('.');
            writer.append(modelFormField.getParameterName(context));
            writer.append(",'");
        }
        writer.append(appendExternalLoginKey(lookupField.getFormName(context)));
        writer.append("'");
        List<String> targetParameterList = lookupField.getTargetParameterList();
        for (String targetParameter: targetParameterList) {
            // named like: document.${formName}.${targetParameter}.value
            writer.append(", document.");
            writer.append(FormRenderer.getCurrentFormName(modelForm, context));
            writer.append(".");
            writer.append(targetParameter);
            writer.append(".value");
        }
        writer.append(");\">");
        writer.append("<img src=\"");
        this.appendContentUrl(writer, "/images/fieldlookup.gif");
        writer.append("\" width=\"15\" height=\"14\" border=\"0\" alt=\"Lookup\"/></a>");

        this.addAsterisks(writer, context, modelFormField);

        this.makeHyperlinkString(writer, lookupField.getSubHyperlink(), context);
        this.appendTooltip(writer, context, modelFormField);

        if (ajaxEnabled) {
            appendWhitespace(writer);
            writer.append("<script language=\"JavaScript\" type=\"text/javascript\">");
            appendWhitespace(writer);
            writer.append("ajaxAutoCompleter('").append(createAjaxParamsFromUpdateAreas(updateAreas, null, context)).append("');");
            appendWhitespace(writer);
            writer.append("</script>");
        }
        appendWhitespace(writer);

        //appendWhitespace(writer);
    }

    protected String appendExternalLoginKey(String target) {
        String result = target;
        String sessionId = ";jsessionid=" + request.getSession().getId();
        int questionIndex = target.indexOf("?");
        if (questionIndex == -1) {
            result += sessionId;
        } else {
            result = result.replace("?", sessionId + "?");
        }
        return result;
    }

    private int getActualPageSize(Map<String, Object> context) {
        Integer value = (Integer) context.get("actualPageSize");
        return value != null ? value.intValue() : (getHighIndex(context) - getLowIndex(context));
    }

    private int getHighIndex(Map<String, Object> context) {
        Integer value = (Integer) context.get("highIndex");
        return value != null ? value.intValue() : 0;
    }

    private int getListSize(Map<String, Object> context) {
        Integer value = (Integer) context.get("listSize");
        return value != null ? value.intValue() : 0;
    }

    private int getLowIndex(Map<String, Object> context) {
        Integer value = (Integer) context.get("lowIndex");
        return value != null ? value.intValue() : 0;
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
        int listSize = getListSize(context);

        int lowIndex = getLowIndex(context);
        int highIndex = getHighIndex(context);
        int actualPageSize = getActualPageSize(context);

        // if this is all there seems to be (if listSize < 0, then size is unknown)
        if (actualPageSize >= listSize && listSize >= 0) return;

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
        if (viewIndexParam.equals("viewIndex" + "_" + paginatorNumber)) viewIndexParam = "VIEW_INDEX" + "_" + paginatorNumber;
        if (viewSizeParam.equals("viewSize" + "_" + paginatorNumber)) viewSizeParam = "VIEW_SIZE" + "_" + paginatorNumber;

        String str = (String) context.get("_QBESTRING_");

        // strip legacy viewIndex/viewSize params from the query string
        String queryString = UtilHttp.stripViewParamsFromQueryString(str, "" + paginatorNumber);

        // strip parametrized index/size params from the query string
        HashSet<String> paramNames = new HashSet<String>();
        paramNames.add(viewIndexParam);
        paramNames.add(viewSizeParam);
        queryString = UtilHttp.stripNamedParamsFromQueryString(queryString, paramNames);

        String anchor = "";
        String paginateAnchor = modelForm.getPaginateTargetAnchor();
        if (paginateAnchor != null) anchor = "#" + paginateAnchor;

        // Create separate url path String and request parameters String,
        // add viewIndex/viewSize parameters to request parameter String
        String urlPath = UtilHttp.removeQueryStringFromTarget(targetService);
        StringBuilder prepLinkBuffer = new StringBuilder();
        String prepLinkQueryString = UtilHttp.getQueryStringFromTarget(targetService);
        if (prepLinkQueryString != null) {
            prepLinkBuffer.append(prepLinkQueryString);
        }
        if (prepLinkBuffer.indexOf("?") < 0) {
            prepLinkBuffer.append("?");
        } else if (prepLinkBuffer.indexOf("?", prepLinkBuffer.length() - 1) > 0) {
            prepLinkBuffer.append("&amp;");
        }
        if (!UtilValidate.isEmpty(queryString) && !queryString.equals("null")) {
            prepLinkBuffer.append(queryString).append("&amp;");
        }
        prepLinkBuffer.append(viewSizeParam).append("=").append(viewSize).append("&amp;").append(viewIndexParam).append("=");
        String prepLinkText = prepLinkBuffer.toString();
        if (ajaxEnabled) {
            // Prepare params for prototype.js
            prepLinkText = prepLinkText.replace("?", "");
            prepLinkText = prepLinkText.replace("&amp;", "&");
        }

        writer.append("<div class=\"").append(modelForm.getPaginateStyle()).append("\">");
        appendWhitespace(writer);
        writer.append(" <ul>");
        appendWhitespace(writer);

        String linkText;

        // First button
        writer.append("  <li class=\"").append(modelForm.getPaginateFirstStyle());
        if (viewIndex > 0) {
            writer.append("\"><a href=\"");
            if (ajaxEnabled) {
                writer.append("javascript:ajaxUpdateAreas('").append(createAjaxParamsFromUpdateAreas(updateAreas, prepLinkText + 0 + anchor, context)).append("')");
            } else {
                linkText = prepLinkText + 0 + anchor;
                appendOfbizUrl(writer, urlPath + linkText);
            }
            writer.append("\">").append(modelForm.getPaginateFirstLabel(context)).append("</a>");
        } else {
            // disabled button
            writer.append("-disabled\">").append(modelForm.getPaginateFirstLabel(context));
        }
        writer.append("</li>");
        appendWhitespace(writer);

        // Previous button
        writer.append("  <li class=\"").append(modelForm.getPaginatePreviousStyle());
        if (viewIndex > 0) {
            writer.append("\"><a href=\"");
            if (ajaxEnabled) {
                writer.append("javascript:ajaxUpdateAreas('").append(createAjaxParamsFromUpdateAreas(updateAreas, prepLinkText + (viewIndex - 1) + anchor, context)).append("')");
            } else {
                linkText = prepLinkText + (viewIndex - 1) + anchor;
                appendOfbizUrl(writer, urlPath + linkText);
            }
            writer.append("\">").append(modelForm.getPaginatePreviousLabel(context)).append("</a>");
        } else {
            // disabled button
            writer.append("-disabled\">").append(modelForm.getPaginatePreviousLabel(context));
        }
        writer.append("</li>");
        appendWhitespace(writer);

        // Page select dropdown
        if (listSize > 0 && this.javaScriptEnabled) {
            writer.append("  <li>").append(pageLabel).append(" <select name=\"page\" size=\"1\" onchange=\"");
            if (ajaxEnabled) {
                writer.append("javascript:ajaxUpdateAreas('").append(createAjaxParamsFromUpdateAreas(updateAreas, prepLinkText + "' + this.value", context)).append(")");
            } else {
                linkText = prepLinkText;
                if (linkText.startsWith("/")) {
                    linkText = linkText.substring(1);
                }
                writer.append("location.href = '");
                appendOfbizUrl(writer, urlPath + linkText);
                writer.append("' + this.value;");
            }
            writer.append("\">");
            // actual value
            int page = 0;
            for (int i = 0; i < listSize;) {
                if (page == viewIndex) {
                    writer.append("<option selected value=\"");
                } else {
                    writer.append("<option value=\"");
                }
                writer.append(Integer.toString(page));
                writer.append("\">");
                writer.append(Integer.toString(1 + page));
                writer.append("</option>");
                // increment page and calculate next index
                page++;
                i = page * viewSize;
            }
            writer.append("</select></li>");
        }

        //  show row count
        writer.append("<li>");
        writer.append(commonDisplaying);
        writer.append("</li>");
        appendWhitespace(writer);

        // Next button
        writer.append("  <li class=\"").append(modelForm.getPaginateNextStyle());
        if (highIndex < listSize) {
            writer.append("\"><a href=\"");
            if (ajaxEnabled) {
                writer.append("javascript:ajaxUpdateAreas('").append(createAjaxParamsFromUpdateAreas(updateAreas, prepLinkText + (viewIndex + 1) + anchor, context)).append("')");
            } else {
                linkText = prepLinkText + (viewIndex + 1) + anchor;
                appendOfbizUrl(writer, urlPath + linkText);
            }
            writer.append("\">").append(modelForm.getPaginateNextLabel(context)).append("</a>");
        } else {
            // disabled button
            writer.append("-disabled\">").append(modelForm.getPaginateNextLabel(context));
        }
        writer.append("</li>");
        appendWhitespace(writer);

        // Last button
        writer.append("  <li class=\"").append(modelForm.getPaginateLastStyle());
        if (highIndex < listSize) {
            int lastIndex = UtilMisc.getViewLastIndex(listSize, viewSize);
            writer.append("\"><a href=\"");
            if (ajaxEnabled) {
                writer.append("javascript:ajaxUpdateAreas('").append(createAjaxParamsFromUpdateAreas(updateAreas, prepLinkText + lastIndex + anchor, context)).append("')");
            } else {
                linkText = prepLinkText + lastIndex + anchor;
                appendOfbizUrl(writer, urlPath + linkText);
            }
            writer.append("\">").append(modelForm.getPaginateLastLabel(context)).append("</a>");
        } else {
            // disabled button
            writer.append("-disabled\">").append(modelForm.getPaginateLastLabel(context));
        }
        writer.append("</li>");
        appendWhitespace(writer);

        writer.append(" </ul>");
        appendWhitespace(writer);
        writer.append("</div>");
        appendWhitespace(writer);
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
        StringBuilder prepLinkBuffer = new StringBuilder();
        String prepLinkQueryString = UtilHttp.getQueryStringFromTarget(targetService);
        if (prepLinkQueryString != null) {
            prepLinkBuffer.append(prepLinkQueryString);
        }
        if (prepLinkBuffer.indexOf("?") < 0) {
            prepLinkBuffer.append("?");
        } else if (prepLinkBuffer.indexOf("?", prepLinkBuffer.length() - 1) > 0) {
            prepLinkBuffer.append("&amp;");
        }
        if (!UtilValidate.isEmpty(queryString) && !queryString.equals("null")) {
            prepLinkBuffer.append(queryString).append("&amp;");
        }
        prepLinkBuffer.append("sortField").append("=").append(newSortField);
        String prepLinkText = prepLinkBuffer.toString();
        if (ajaxEnabled) {
            prepLinkText = prepLinkText.replace("?", "");
            prepLinkText = prepLinkText.replace("&amp;", "&");
        }

        writer.append("<a");
        if (UtilValidate.isNotEmpty(sortFieldStyle)) {
            writer.append(" class=\"");
            writer.append(sortFieldStyle);
            writer.append("\"");
        }

        writer.append(" href=\"");
        if (ajaxEnabled) {
            writer.append("javascript:ajaxUpdateAreas('").append(createAjaxParamsFromUpdateAreas(updateAreas, prepLinkText, context)).append("')");
        } else {
            appendOfbizUrl(writer, urlPath + prepLinkText);
        }
        writer.append("\">").append(titleText).append("</a>");
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderFileField(java.io.Writer, java.util.Map, org.ofbiz.widget.model.ModelFormField.FileField)
     */
    public void renderFileField(Appendable writer, Map<String, Object> context, FileField textField) throws IOException {
        ModelFormField modelFormField = textField.getModelFormField();

        writer.append("<input type=\"file\"");

        appendClassNames(writer, context, modelFormField);

        writer.append(" name=\"");
        writer.append(modelFormField.getParameterName(context));
        writer.append('"');

        String value = modelFormField.getEntry(context, textField.getDefaultValue(context));
        if (UtilValidate.isNotEmpty(value)) {
            writer.append(" value=\"");
            writer.append(value);
            writer.append('"');
        }

        writer.append(" size=\"");
        writer.append(Integer.toString(textField.getSize()));
        writer.append('"');

        Integer maxlength = textField.getMaxlength();
        if (maxlength != null) {
            writer.append(" maxlength=\"");
            writer.append(maxlength.toString());
            writer.append('"');
        }

        if (!textField.getClientAutocompleteField()) {
            writer.append(" autocomplete=\"off\"");
        }

        writer.append("/>");

        this.makeHyperlinkString(writer, textField.getSubHyperlink(), context);

        this.appendTooltip(writer, context, modelFormField);

        //appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderPasswordField(java.io.Writer, java.util.Map, org.ofbiz.widget.model.ModelFormField.PasswordField)
     */
    public void renderPasswordField(Appendable writer, Map<String, Object> context, PasswordField passwordField) throws IOException {
        ModelFormField modelFormField = passwordField.getModelFormField();

        writer.append("<input type=\"password\"");

        appendClassNames(writer, context, modelFormField);

        writer.append(" name=\"");
        writer.append(modelFormField.getParameterName(context));
        writer.append('"');

        String value = modelFormField.getEntry(context, passwordField.getDefaultValue(context));
        if (UtilValidate.isNotEmpty(value)) {
            writer.append(" value=\"");
            writer.append(value);
            writer.append('"');
        }

        writer.append(" size=\"");
        writer.append(Integer.toString(passwordField.getSize()));
        writer.append('"');

        Integer maxlength = passwordField.getMaxlength();
        if (maxlength != null) {
            writer.append(" maxlength=\"");
            writer.append(maxlength.toString());
            writer.append('"');
        }

        String idName = modelFormField.getCurrentContainerId(context);
        if (UtilValidate.isNotEmpty(idName)) {
            writer.append(" id=\"");
            writer.append(idName);
            writer.append('"');
        }

        if (!passwordField.getClientAutocompleteField()) {
            writer.append(" autocomplete=\"off\"");
        }

        writer.append("/>");

        this.addAsterisks(writer, context, modelFormField);

        this.makeHyperlinkString(writer, passwordField.getSubHyperlink(), context);

        this.appendTooltip(writer, context, modelFormField);

        //appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.form.FormStringRenderer#renderImageField(java.io.Writer, java.util.Map, org.ofbiz.widget.model.ModelFormField.ImageField)
     */
    public void renderImageField(Appendable writer, Map<String, Object> context, ImageField imageField) throws IOException {
        ModelFormField modelFormField = imageField.getModelFormField();

        writer.append("<img ");

        String value = modelFormField.getEntry(context, imageField.getValue(context));
        if (UtilValidate.isNotEmpty(value)) {
            writer.append(" src=\"");
            appendContentUrl(writer, value);
            writer.append('"');
        }
        
        value = modelFormField.getEntry(context, imageField.getStyle(context));
        if (UtilValidate.isNotEmpty(value)) {
            writer.append(" class=\"");
            appendContentUrl(writer, value);
            writer.append('"');
        }

        String event = modelFormField.getEvent();
        String action = modelFormField.getAction(context);
        if (UtilValidate.isNotEmpty(event) && UtilValidate.isNotEmpty(action)) {
            writer.append(" ");
            writer.append(event);
            writer.append("=\"");
            writer.append(action);
            writer.append('"');
        }

        writer.append("/>");

        this.makeHyperlinkString(writer, imageField.getSubHyperlink(), context);

        this.appendTooltip(writer, context, modelFormField);

        //appendWhitespace(writer);
    }

    public void renderFieldGroupOpen(Appendable writer, Map<String, Object> context, ModelForm.FieldGroup fieldGroup) throws IOException {
        String style = fieldGroup.getStyle();
        String id = fieldGroup.getId();
        FlexibleStringExpander titleNotExpanded = FlexibleStringExpander.getInstance(fieldGroup.getTitle());
        String title = titleNotExpanded.expandString(context);
        Boolean collapsed = fieldGroup.initiallyCollapsed();
        String collapsibleAreaId = fieldGroup.getId() + "_body";

        if (UtilValidate.isNotEmpty(style) || UtilValidate.isNotEmpty(id) || UtilValidate.isNotEmpty(title)) {

            writer.append("<div class=\"fieldgroup");
            if (UtilValidate.isNotEmpty(style)) {
                writer.append(" ");
                writer.append(style);
            }
            writer.append("\"");
            if (UtilValidate.isNotEmpty(id)) {
                writer.append(" id=\"");
                writer.append(id);
                writer.append("\"");
            }
            writer.append(">");

            writer.append("<div class=\"fieldgroup-title-bar\"><table><tr><td class=\"collapse\">");

            if (fieldGroup.collapsible()) {
                String expandToolTip = null;
                String collapseToolTip = null;
                Map<String, Object> uiLabelMap = UtilGenerics.checkMap(context.get("uiLabelMap"));
                //Map<String, Object> paramMap = UtilGenerics.checkMap(context.get("requestParameters"));
                if (uiLabelMap != null) {
                    expandToolTip = (String) uiLabelMap.get("CommonExpand");
                    collapseToolTip = (String) uiLabelMap.get("CommonCollapse");
                }

                writer.append("<ul><li class=\"");
                if (collapsed) {
                    writer.append("collapsed\"><a ");
                    writer.append("onclick=\"javascript:toggleCollapsiblePanel(this, '").append(collapsibleAreaId).append("', '").append(expandToolTip).append("', '").append(collapseToolTip).append("');\"");
                } else {
                    writer.append("expanded\"><a ");
                    writer.append("onclick=\"javascript:toggleCollapsiblePanel(this, '").append(collapsibleAreaId).append("', '").append(expandToolTip).append("', '").append(collapseToolTip).append("');\"");
                }
                writer.append(">&nbsp&nbsp&nbsp</a></li></ul>");

                appendWhitespace(writer);
            }
            writer.append("</td><td>");

            if (UtilValidate.isNotEmpty(title)) {
                writer.append("<div class=\"title\">");
                writer.append(title);
                writer.append("</div>");
            }

            writer.append("</td></tr></table></div>");

            writer.append("<div id=\"").append(collapsibleAreaId).append("\" class=\"fieldgroup-body\"");
            if (fieldGroup.collapsible() && collapsed) {
                writer.append(" style=\"display: none;\"");
            }
            writer.append(">");
        }
    }

    public void renderFieldGroupClose(Appendable writer, Map<String, Object> context, ModelForm.FieldGroup fieldGroup) throws IOException {
        String style = fieldGroup.getStyle();
        String id = fieldGroup.getId();
        String title = fieldGroup.getTitle();
        if (UtilValidate.isNotEmpty(style) || UtilValidate.isNotEmpty(id) || UtilValidate.isNotEmpty(title)) {
            writer.append("</div>");
            writer.append("</div>");
        }
    }

    // TODO: Remove embedded styling
    public void renderBanner(Appendable writer, Map<String, Object> context, ModelForm.Banner banner) throws IOException {
        writer.append(" <table width=\"100%\">  <tr>");
        String style = banner.getStyle(context);
        String leftStyle = banner.getLeftTextStyle(context);
        if (UtilValidate.isEmpty(leftStyle)) leftStyle = style;
        String rightStyle = banner.getRightTextStyle(context);
        if (UtilValidate.isEmpty(rightStyle)) rightStyle = style;

        String leftText = banner.getLeftText(context);
        if (UtilValidate.isNotEmpty(leftText)) {
            writer.append("   <td align=\"left\">");
            if (UtilValidate.isNotEmpty(leftStyle)) {
               writer.append("<div");
               writer.append(" class=\"");
               writer.append(leftStyle);
               writer.append("\"");
               writer.append(">");
            }
            writer.append(leftText);
            if (UtilValidate.isNotEmpty(leftStyle)) {
                writer.append("</div>");
            }
            writer.append("</td>");
        }

        String text = banner.getText(context);
        if (UtilValidate.isNotEmpty(text)) {
            writer.append("   <td align=\"center\">");
            if (UtilValidate.isNotEmpty(style)) {
               writer.append("<div");
               writer.append(" class=\"");
               writer.append(style);
               writer.append("\"");
               writer.append(">");
            }
            writer.append(text);
            if (UtilValidate.isNotEmpty(style)) {
                writer.append("</div>");
            }
            writer.append("</td>");
        }

        String rightText = banner.getRightText(context);
        if (UtilValidate.isNotEmpty(rightText)) {
            writer.append("   <td align=\"right\">");
            if (UtilValidate.isNotEmpty(rightStyle)) {
               writer.append("<div");
               writer.append(" class=\"");
               writer.append(rightStyle);
               writer.append("\"");
               writer.append(">");
            }
            writer.append(rightText);
            if (UtilValidate.isNotEmpty(rightStyle)) {
                writer.append("</div>");
            }
            writer.append("</td>");
        }
        writer.append("</tr> </table>");
    }

    /**
     * Renders a link for the column header fields when there is a header-link="" specified in the <field > tag, using
     * style from header-link-style="".  Also renders a selectAll checkbox in multi forms.
     * @param writer
     * @param context
     * @param modelFormField
     * @param titleText
     */
    public void renderHyperlinkTitle(Appendable writer, Map<String, Object> context, ModelFormField modelFormField, String titleText) throws IOException {
        if (UtilValidate.isNotEmpty(modelFormField.getHeaderLink())) {
            StringBuilder targetBuffer = new StringBuilder();
            FlexibleStringExpander target = FlexibleStringExpander.getInstance(modelFormField.getHeaderLink());
            String fullTarget = target.expandString(context);
            targetBuffer.append(fullTarget);
            String targetType = CommonWidgetModels.Link.DEFAULT_URL_MODE;
            if (UtilValidate.isNotEmpty(targetBuffer.toString()) && targetBuffer.toString().toLowerCase().startsWith("javascript:")) {
                targetType="plain";
            }
            WidgetWorker.makeHyperlinkString(writer, modelFormField.getHeaderLinkStyle(), targetType, targetBuffer.toString(), null, titleText, null, modelFormField, this.request, this.response, null, null);
        } else if (modelFormField.isSortField()) {
            renderSortField (writer, context, modelFormField, titleText);
        } else if (modelFormField.isRowSubmit()) {
            if (UtilValidate.isNotEmpty(titleText)) writer.append(titleText).append("<br/>");
            writer.append("<input type=\"checkbox\" name=\"selectAll\" value=\"Y\" onclick=\"javascript:toggleAll(this, '");
            writer.append(modelFormField.getModelForm().getName());
            writer.append("');\"/>");
        } else {
             writer.append(titleText);
        }
    }

    public void renderContainerFindField(Appendable writer, Map<String, Object> context, ContainerField containerField) throws IOException {
        writer.append("<div ");
        String id = containerField.getModelFormField().getIdName();
        if (UtilValidate.isNotEmpty(id)) {
            writer.append("id=\"");
            writer.append(id);
            writer.append("\" ");
        }
        String className = containerField.getModelFormField().getWidgetStyle();
        if (UtilValidate.isNotEmpty(className)) {
            writer.append("class=\"");
            writer.append(className);
            writer.append("\" ");
        }
        writer.append("/>");
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
        StringBuilder ajaxUrl = new StringBuilder();
        boolean firstLoop = true;
        for (ModelForm.UpdateArea updateArea : updateAreas) {
            if (firstLoop) {
                firstLoop = false;
            } else {
                ajaxUrl.append(",");
            }
            String targetUrl = updateArea.getAreaTarget(context);
            String ajaxParams = getAjaxParamsFromTarget(targetUrl);
            if (UtilValidate.isNotEmpty(extraParams)) {
                if (ajaxParams.length() > 0 && !extraParams.startsWith("&")) {
                    ajaxParams += "&";
                }
                ajaxParams += extraParams;
            }
            ajaxUrl.append(updateArea.getAreaId()).append(",");
            try {
                appendOfbizUrl(ajaxUrl, UtilHttp.removeQueryStringFromTarget(targetUrl));
            } catch (IOException e) {
                throw UtilMisc.initCause(new InternalError(e.getMessage()), e);
            }
            ajaxUrl.append(",").append(ajaxParams);
        }
        return ajaxUrl.toString();
    }
}
