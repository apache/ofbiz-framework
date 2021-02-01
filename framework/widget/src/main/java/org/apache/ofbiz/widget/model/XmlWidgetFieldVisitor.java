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
package org.apache.ofbiz.widget.model;

import java.util.Collection;

import org.apache.ofbiz.widget.model.CommonWidgetModels.Link;
import org.apache.ofbiz.widget.model.ModelFormField.AutoComplete;
import org.apache.ofbiz.widget.model.ModelFormField.CheckField;
import org.apache.ofbiz.widget.model.ModelFormField.ContainerField;
import org.apache.ofbiz.widget.model.ModelFormField.DateFindField;
import org.apache.ofbiz.widget.model.ModelFormField.DateTimeField;
import org.apache.ofbiz.widget.model.ModelFormField.DisplayEntityField;
import org.apache.ofbiz.widget.model.ModelFormField.DisplayField;
import org.apache.ofbiz.widget.model.ModelFormField.DropDownField;
import org.apache.ofbiz.widget.model.ModelFormField.FieldInfoWithOptions;
import org.apache.ofbiz.widget.model.ModelFormField.FileField;
import org.apache.ofbiz.widget.model.ModelFormField.FormField;
import org.apache.ofbiz.widget.model.ModelFormField.GridField;
import org.apache.ofbiz.widget.model.ModelFormField.HiddenField;
import org.apache.ofbiz.widget.model.ModelFormField.HyperlinkField;
import org.apache.ofbiz.widget.model.ModelFormField.IgnoredField;
import org.apache.ofbiz.widget.model.ModelFormField.ImageField;
import org.apache.ofbiz.widget.model.ModelFormField.InPlaceEditor;
import org.apache.ofbiz.widget.model.ModelFormField.LookupField;
import org.apache.ofbiz.widget.model.ModelFormField.MenuField;
import org.apache.ofbiz.widget.model.ModelFormField.PasswordField;
import org.apache.ofbiz.widget.model.ModelFormField.RadioField;
import org.apache.ofbiz.widget.model.ModelFormField.RangeFindField;
import org.apache.ofbiz.widget.model.ModelFormField.ResetField;
import org.apache.ofbiz.widget.model.ModelFormField.ScreenField;
import org.apache.ofbiz.widget.model.ModelFormField.SubHyperlink;
import org.apache.ofbiz.widget.model.ModelFormField.SubmitField;
import org.apache.ofbiz.widget.model.ModelFormField.TextField;
import org.apache.ofbiz.widget.model.ModelFormField.TextFindField;
import org.apache.ofbiz.widget.model.ModelFormField.TextareaField;

/**
 * An object that generates XML from widget models.
 * The generated XML is unformatted - if you want to
 * "pretty print" the XML, then use a transformer.
 *
 */
public class XmlWidgetFieldVisitor extends XmlAbstractWidgetVisitor implements ModelFieldVisitor {

    public XmlWidgetFieldVisitor(Appendable writer) {
        super(writer);
    }
    private final Appendable writer = getWriter();

    @Override
    public void visit(CheckField checkField) throws Exception {
        visitModelField(checkField.getModelFormField());
        writer.append("<check");
        visitAttribute("all-checked", checkField.getAllChecked());
        visitFieldInfoWithOptions(checkField);
        writer.append("</check></field>");
    }

    @Override
    public void visit(ContainerField containerField) throws Exception {
        visitModelField(containerField.getModelFormField());
        writer.append("<container/></field>");
    }

    @Override
    public void visit(DateFindField dateTimeField) throws Exception {
        visitModelField(dateTimeField.getModelFormField());
        writer.append("<date-find");
        visitDateTimeFieldAttrs(dateTimeField);
        visitAttribute("default-option-from", dateTimeField.getDefaultOptionFrom());
        visitAttribute("default-option-thru", dateTimeField.getDefaultOptionThru());
        writer.append("/></field>");
    }

    @Override
    public void visit(DateTimeField dateTimeField) throws Exception {
        visitModelField(dateTimeField.getModelFormField());
        writer.append("<date-time");
        visitDateTimeFieldAttrs(dateTimeField);
        writer.append("/></field>");
    }

    @Override
    public void visit(DisplayEntityField displayField) throws Exception {
        visitModelField(displayField.getModelFormField());
        writer.append("<display-entity");
        visitDisplayFieldAttrs(displayField);
        visitAttribute("cache", displayField.getCache());
        visitAttribute("entity-name", displayField.getEntityName());
        visitAttribute("key-field-name", displayField.getKeyFieldName());
        writer.append(">");
        visitInPlaceEditor(displayField.getInPlaceEditor());
        visitSubHyperlink(displayField.getSubHyperlink());
        writer.append("</display-entity></field>");
    }

    @Override
    public void visit(DisplayField displayField) throws Exception {
        visitModelField(displayField.getModelFormField());
        writer.append("<display");
        visitDisplayFieldAttrs(displayField);
        writer.append(">");
        visitInPlaceEditor(displayField.getInPlaceEditor());
        writer.append("</display></field>");
    }

    @Override
    public void visit(DropDownField dropDownField) throws Exception {
        visitModelField(dropDownField.getModelFormField());
        writer.append("<drop-down");
        visitAttribute("allow-empty", dropDownField.getAllowEmpty());
        visitAttribute("allow-multiple", dropDownField.getAllowMultiple());
        visitAttribute("current", dropDownField.getCurrent());
        visitAttribute("current-description", dropDownField.getCurrentDescription());
        visitAttribute("other-field-size", dropDownField.getOtherFieldSize());
        visitAttribute("size", dropDownField.getSize());
        visitAttribute("text-size", dropDownField.getTextSize());
        visitFieldInfoWithOptions(dropDownField);
        visitAutoComplete(dropDownField.getAutoComplete());
        visitSubHyperlink(dropDownField.getSubHyperlink());
        writer.append("</drop-down></field>");
    }

    @Override
    public void visit(FileField textField) throws Exception {
        visitModelField(textField.getModelFormField());
        writer.append("<file");
        visitTextFieldAttrs(textField);
        writer.append(">");
        visitSubHyperlink(textField.getSubHyperlink());
        writer.append("</file></field>");
    }

    @Override
    public void visit(HiddenField hiddenField) throws Exception {
        visitModelField(hiddenField.getModelFormField());
        writer.append("<hidden");
        visitAttribute("value", hiddenField.getValue());
        writer.append("/></field>");
    }

    @Override
    public void visit(HyperlinkField hyperlinkField) throws Exception {
        visitModelField(hyperlinkField.getModelFormField());
        writer.append("<hyperlink");
        visitAttribute("also-hidden", hyperlinkField.getAlsoHidden());
        visitAttribute("confirmation-message", hyperlinkField.getConfirmationMsgExdr());
        visitAttribute("description", hyperlinkField.getDescription());
        visitAttribute("request-confirmation", hyperlinkField.getRequestConfirmation());
        Link link = hyperlinkField.getLink();
        visitLinkAttributes(link);
        if (link.getImage() != null || link.getAutoEntityParameters() != null || link.getAutoServiceParameters() != null) {
            writer.append(">");
            visitImage(link.getImage());
            visitAutoEntityParameters(link.getAutoEntityParameters());
            visitAutoServiceParameters(link.getAutoServiceParameters());
            writer.append("</hyperlink>");
        } else {
            writer.append("/>");
        }
        writer.append("</field>");
    }

    @Override
    public void visit(FormField formField) throws Exception {
        visitModelField(formField.getModelFormField());
        writer.append("<include-form");
        visitAttribute("name", formField.getFormName());
        visitAttribute("location", formField.getFormLocation());
        writer.append("/>");
    }

    @Override
    public void visit(GridField gridField) throws Exception {
        visitModelField(gridField.getModelFormField());
        writer.append("<include-grid");
        visitAttribute("name", gridField.getGridName());
        visitAttribute("location", gridField.getGridLocation());
        writer.append("/>");
    }

    @Override
    public void visit(MenuField menuField) throws Exception {
        visitModelField(menuField.getModelFormField());
        writer.append("<include-menu");
        visitAttribute("name", menuField.getMenuName());
        visitAttribute("location", menuField.getMenuLocation());
        writer.append("/>");
    }

    @Override
    public void visit(ScreenField screenField) throws Exception {
        visitModelField(screenField.getModelFormField());
        writer.append("<include-screen");
        visitAttribute("name", screenField.getScreenName());
        visitAttribute("location", screenField.getScreenLocation());
        writer.append("/>");
    }

    @Override
    public void visit(IgnoredField ignoredField) throws Exception {
        visitModelField(ignoredField.getModelFormField());
        writer.append("<ignored/></field>");
    }

    @Override
    public void visit(ImageField imageField) throws Exception {
        visitModelField(imageField.getModelFormField());
        writer.append("<image");
        visitAttribute("alternate", imageField.getAlternate());
        visitAttribute("default-value", imageField.getDefaultValue());
        visitAttribute("description", imageField.getDescription());
        visitAttribute("style", imageField.getStyle());
        visitAttribute("value", imageField.getValue());
        writer.append(">");
        visitSubHyperlink(imageField.getSubHyperlink());
        writer.append("</image></field>");
    }

    @Override
    public void visit(LookupField textField) throws Exception {
        visitModelField(textField.getModelFormField());
        writer.append("<lookup");
        visitTextFieldAttrs(textField);
        visitAttribute("description-field-name", textField.getDescriptionFieldName());
        visitAttribute("fade-background", textField.getFadeBackground());
        visitAttribute("target-form-name", textField.getFormName());
        visitAttribute("initially-collapsed", textField.getInitiallyCollapsed());
        visitAttribute("height", textField.getLookupHeight());
        visitAttribute("position", textField.getLookupPosition());
        visitAttribute("presentation", textField.getLookupPresentation());
        visitAttribute("width", textField.getLookupWidth());
        visitAttribute("show-description", textField.getShowDescription());
        visitAttribute("target-parameter", textField.getTargetParameter());
        writer.append(">");
        visitSubHyperlink(textField.getSubHyperlink());
        writer.append("</lookup></field>");
    }

    @Override
    public void visit(PasswordField textField) throws Exception {
        visitModelField(textField.getModelFormField());
        writer.append("<password");
        visitTextFieldAttrs(textField);
        writer.append(">");
        visitSubHyperlink(textField.getSubHyperlink());
        writer.append("</password></field>");
    }

    @Override
    public void visit(RadioField radioField) throws Exception {
        visitModelField(radioField.getModelFormField());
        writer.append("<radio");
        visitFieldInfoWithOptions(radioField);
        writer.append("</radio></field>");
    }

    @Override
    public void visit(RangeFindField textField) throws Exception {
        visitModelField(textField.getModelFormField());
        writer.append("<range-find");
        visitTextFieldAttrs(textField);
        visitAttribute("default-option-from", textField.getDefaultOptionFrom());
        visitAttribute("default-option-thru", textField.getDefaultOptionThru());
        writer.append(">");
        visitSubHyperlink(textField.getSubHyperlink());
        writer.append("</range-find></field>");
    }

    @Override
    public void visit(ResetField resetField) throws Exception {
        visitModelField(resetField.getModelFormField());
        writer.append("<reset/></field>");
    }

    @Override
    public void visit(SubmitField submitField) throws Exception {
        visitModelField(submitField.getModelFormField());
        writer.append("<submit");
        visitAttribute("background-submit-refresh-target", submitField.getBackgroundSubmitRefreshTargetExdr());
        visitAttribute("button-type", submitField.getButtonType());
        visitAttribute("confirmation-message", submitField.getConfirmationMsgExdr());
        visitAttribute("image-location", submitField.getImageLocation());
        visitAttribute("request-confirmation", submitField.getRequestConfirmation());
        writer.append("/></field>");
    }

    @Override
    public void visit(TextareaField textareaField) throws Exception {
        visitModelField(textareaField.getModelFormField());
        writer.append("<textarea");
        visitAttribute("cols", textareaField.getCols());
        visitAttribute("default-value", textareaField.getDefaultValue());
        visitAttribute("read-only", textareaField.isReadOnly());
        visitAttribute("rows", textareaField.getRows());
        visitAttribute("visual-editor-buttons", textareaField.getVisualEditorButtons());
        visitAttribute("visual-editor-enable", textareaField.getVisualEditorEnable());
        writer.append("/></field>");
    }

    @Override
    public void visit(TextField textField) throws Exception {
        visitModelField(textField.getModelFormField());
        writer.append("<text");
        visitTextFieldAttrs(textField);
        writer.append(">");
        visitSubHyperlink(textField.getSubHyperlink());
        writer.append("</text></field>");
    }

    @Override
    public void visit(TextFindField textField) throws Exception {
        visitModelField(textField.getModelFormField());
        writer.append("<text-find");
        visitTextFieldAttrs(textField);
        visitAttribute("default-option", textField.getDefaultOption());
        visitAttribute("hide-options", textField.getHideOptions());
        visitAttribute("ignore-case", textField.getIgnoreCase());
        writer.append(">");
        visitSubHyperlink(textField.getSubHyperlink());
        writer.append("</text-find></field>");
    }

    private void visitTextFieldAttrs(TextField field) throws Exception {
        visitAttribute("client-autocomplete-field", field.getClientAutocompleteField());
        visitAttribute("default-value", field.getDefaultValue());
        visitAttribute("mask", field.getMask());
        visitAttribute("maxlength", field.getMaxlength());
        visitAttribute("placeholder", field.getPlaceholder());
        visitAttribute("read-only", field.getReadonly());
        visitAttribute("size", field.getSize());
    }

    private void visitDisplayFieldAttrs(DisplayField field) throws Exception {
        visitAttribute("also-hidden", field.getAlsoHidden());
        visitAttribute("currency", field.getCurrency());
        visitAttribute("date", field.getDate());
        visitAttribute("default-value", field.getDefaultValue());
        visitAttribute("description", field.getDescription());
        visitAttribute("image-location", field.getImageLocation());
        visitAttribute("size", field.getSize());
        visitAttribute("type", field.getType());
    }

    private void visitDateTimeFieldAttrs(DateTimeField field) throws Exception {
        visitAttribute("default-value", field.getDefaultValue());
        visitAttribute("type", field.getType());
        visitAttribute("input-method", field.getInputMethod());
        visitAttribute("clock", field.getClock());
        visitAttribute("mask", field.getMask());
        visitAttribute("step", field.getStep());
    }

    private void visitFieldInfoWithOptions(FieldInfoWithOptions fieldInfo) throws Exception {
        visitAttribute("no-current-selected-key", fieldInfo.getNoCurrentSelectedKey());
        writer.append(">");
        // TODO: Options
    }

    private void visitModelField(ModelFormField modelField) throws Exception {
        if (modelField != null) {
            writer.append("<field");
            visitAttribute("name", modelField.getName());
            visitAttribute("action", modelField.getAction());
            visitAttribute("attribute-name", modelField.getAttributeName());
            visitAttribute("encode-output", modelField.getEncodeOutput());
            visitAttribute("entity-name", modelField.getEntityName());
            visitAttribute("entry-name", modelField.getEntryName());
            visitAttribute("event", modelField.getEvent());
            visitAttribute("field-name", modelField.getFieldName());
            visitAttribute("header-link", modelField.getHeaderLink());
            visitAttribute("header-link-style", modelField.getHeaderLinkStyle());
            visitAttribute("id-name", modelField.getIdName());
            visitAttribute("map-name", modelField.getMapName());
            visitAttribute("parameter-name", modelField.getParameterName());
            visitAttribute("position", modelField.getPosition());
            visitAttribute("red-when", modelField.getRedWhen());
            visitAttribute("required-field", modelField.getRequiredField());
            visitAttribute("required-field-style", modelField.getRequiredFieldStyle());
            visitAttribute("separate-column", modelField.getSeparateColumn());
            visitAttribute("service-name", modelField.getServiceName());
            visitAttribute("sort-field", modelField.getSortField());
            visitAttribute("sort-field-asc-style", modelField.getSortFieldAscStyle());
            visitAttribute("sort-field-desc-style", modelField.getSortFieldDescStyle());
            visitAttribute("sort-field-help-text", modelField.getSortFieldHelpText());
            visitAttribute("sort-field-style", modelField.getSortFieldStyle());
            visitAttribute("title", modelField.getTitle());
            visitAttribute("title-area-style", modelField.getTitleAreaStyle());
            visitAttribute("title-style", modelField.getTitleStyle());
            visitAttribute("tooltip", modelField.getTooltip());
            visitAttribute("tooltip-style", modelField.getTooltipStyle());
            visitAttribute("use-when", modelField.getUseWhen());
            visitAttribute("widget-area-style", modelField.getWidgetAreaStyle());
            visitAttribute("widget-style", modelField.getWidgetStyle());
            writer.append(">");
            visitUpdateAreas(modelField.getOnChangeUpdateAreas());
            visitUpdateAreas(modelField.getOnClickUpdateAreas());
        }
    }

    private void visitSubHyperlink(SubHyperlink hyperlink) throws Exception {
        if (hyperlink != null) {
            writer.append("<sub-hyperlink");
            Link link = hyperlink.getLink();
            visitLinkAttributes(link);
            visitAttribute("description", hyperlink.getDescription());
            visitAttribute("use-when", hyperlink.getUseWhen());
            if (link.getImage() != null || link.getAutoEntityParameters() != null || link.getAutoServiceParameters() != null) {
                writer.append(">");
                visitImage(link.getImage());
                visitAutoEntityParameters(link.getAutoEntityParameters());
                visitAutoServiceParameters(link.getAutoServiceParameters());
                writer.append("</sub-hyperlink>");
            } else {
                writer.append("/>");
            }
        }
    }

    private void visitAutoComplete(AutoComplete autoComplete) throws Exception {
        if (autoComplete != null) {
            writer.append("<auto-complete");
            visitAttribute("auto-select", autoComplete.getAutoSelect());
            visitAttribute("frequency", autoComplete.getFrequency());
            visitAttribute("min-chars", autoComplete.getMinChars());
            visitAttribute("choices", autoComplete.getChoices());
            visitAttribute("partial-search", autoComplete.getPartialSearch());
            visitAttribute("partial-chars", autoComplete.getPartialChars());
            visitAttribute("ignore-case", autoComplete.getIgnoreCase());
            visitAttribute("full-search", autoComplete.getFullSearch());
            writer.append("/>");
        }
    }

    private void visitInPlaceEditor(InPlaceEditor editor) throws Exception {
        if (editor != null) {
            writer.append("<in-place-editor");
            visitAttribute("cancel-control", editor.getCancelControl());
            visitAttribute("cancel-text", editor.getCancelText());
            visitAttribute("click-to-edit-text", editor.getClickToEditText());
            visitAttribute("field-post-creation", editor.getFieldPostCreation());
            visitAttribute("form-class-name", editor.getFormClassName());
            visitAttribute("highlight-color", editor.getHighlightColor());
            visitAttribute("highlight-end-color", editor.getHighlightEndColor());
            visitAttribute("hover-class-name", editor.getHoverClassName());
            visitAttribute("html-response", editor.getHtmlResponse());
            visitAttribute("loading-class-name", editor.getLoadingClassName());
            visitAttribute("loading-text", editor.getLoadingText());
            visitAttribute("ok-control", editor.getOkControl());
            visitAttribute("ok-text", editor.getOkText());
            visitAttribute("param-name", editor.getParamName());
            visitAttribute("saving-class-name", editor.getSavingClassName());
            visitAttribute("saving-text", editor.getSavingText());
            visitAttribute("submit-on-blur", editor.getSubmitOnBlur());
            visitAttribute("text-before-controls", editor.getTextBeforeControls());
            visitAttribute("text-after-controls", editor.getTextAfterControls());
            visitAttribute("text-between-controls", editor.getTextBetweenControls());
            visitAttribute("update-after-request-call", editor.getUpdateAfterRequestCall());
            visitAttribute("url", editor.getUrl());
            if (!editor.getCols().isEmpty() || !editor.getRows().isEmpty()) {
                writer.append(">");
                writer.append("<simple-editor");
                visitAttribute("cols", editor.getCols());
                visitAttribute("rows", editor.getRows());
                writer.append("/></in-place-editor>");
            } else {
                writer.append("/>");
            }
        }
    }

    private void visitUpdateAreas(Collection<ModelForm.UpdateArea> updateAreas) throws Exception {
        for (ModelForm.UpdateArea updateArea : updateAreas) {
            writer.append("<on-field-event-update-area");
            visitAttribute("event-type", updateArea.getEventType());
            visitAttribute("area-id", updateArea.getAreaId());
            visitAttribute("area-target", updateArea.getAreaTarget());
            writer.append(">");
            visitAutoEntityParameters(updateArea.getAutoEntityParameters());
            visitAutoServiceParameters(updateArea.getAutoServiceParameters());
            visitParameters(updateArea.getParameterList());
            writer.append("</on-field-event-update-area>");
        }
    }
}
