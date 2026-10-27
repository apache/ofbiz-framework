/*
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

    @Override
    public void visit(CheckField checkField) throws Exception {
        visitModelField(checkField.getModelFormField());
        getWriter().append("<check");
        visitAttribute("all-checked", checkField.getAllChecked());
        visitFieldInfoWithOptions(checkField);
        getWriter().append("</check></field>");
    }

    @Override
    public void visit(ContainerField containerField) throws Exception {
        visitModelField(containerField.getModelFormField());
        getWriter().append("<container/></field>");
    }

    @Override
    public void visit(DateFindField dateTimeField) throws Exception {
        visitModelField(dateTimeField.getModelFormField());
        getWriter().append("<date-find");
        visitDateTimeFieldAttrs(dateTimeField);
        visitAttribute("default-option-from", dateTimeField.getDefaultOptionFrom());
        visitAttribute("default-option-thru", dateTimeField.getDefaultOptionThru());
        getWriter().append("/></field>");
    }

    @Override
    public void visit(DateTimeField dateTimeField) throws Exception {
        visitModelField(dateTimeField.getModelFormField());
        getWriter().append("<date-time");
        visitDateTimeFieldAttrs(dateTimeField);
        getWriter().append("/></field>");
    }

    @Override
    public void visit(ModelFormField.DateRangePickerField dateRangePickerField) throws Exception {
        visitModelField(dateRangePickerField.getModelFormField());
        getWriter().append("<date-range-picker");
        visitDateRangePickerFieldAttrs(dateRangePickerField);
        getWriter().append("/></field>");
    }

    @Override
    public void visit(DisplayEntityField displayField) throws Exception {
        visitModelField(displayField.getModelFormField());
        getWriter().append("<display-entity");
        visitDisplayFieldAttrs(displayField);
        visitAttribute("cache", displayField.getCache());
        visitAttribute("entity-name", displayField.getEntityName());
        visitAttribute("key-field-name", displayField.getKeyFieldName());
        getWriter().append(">");
        visitInPlaceEditor(displayField.getInPlaceEditor());
        visitSubHyperlink(displayField.getSubHyperlink());
        getWriter().append("</display-entity></field>");
    }

    @Override
    public void visit(DisplayField displayField) throws Exception {
        visitModelField(displayField.getModelFormField());
        getWriter().append("<display");
        visitDisplayFieldAttrs(displayField);
        getWriter().append(">");
        visitInPlaceEditor(displayField.getInPlaceEditor());
        getWriter().append("</display></field>");
    }

    @Override
    public void visit(DropDownField dropDownField) throws Exception {
        visitModelField(dropDownField.getModelFormField());
        getWriter().append("<drop-down");
        visitAttribute("allow-empty", dropDownField.getAllowEmpty());
        visitAttribute("allow-multiple", dropDownField.getAllowMultiple());
        visitAttribute("current", dropDownField.getCurrent());
        visitAttribute("current-description", dropDownField.getCurrentDescription());
        visitAttribute("other-field-size", dropDownField.getOtherFieldSize());
        visitAttribute("size", dropDownField.getSize());

        var textSizeOptional = dropDownField.getTextSize();
        if (textSizeOptional.isPresent()) {
            visitAttribute("text-size", textSizeOptional.get());
        }

        visitFieldInfoWithOptions(dropDownField);
        visitAutoComplete(dropDownField.getAutoComplete());
        visitSubHyperlink(dropDownField.getSubHyperlink());
        getWriter().append("</drop-down></field>");
    }

    @Override
    public void visit(FileField textField) throws Exception {
        visitModelField(textField.getModelFormField());
        getWriter().append("<file");
        visitTextFieldAttrs(textField);
        getWriter().append(">");
        visitSubHyperlink(textField.getSubHyperlink());
        getWriter().append("</file></field>");
    }

    @Override
    public void visit(HiddenField hiddenField) throws Exception {
        visitModelField(hiddenField.getModelFormField());
        getWriter().append("<hidden");
        visitAttribute("value", hiddenField.getValue());
        getWriter().append("/></field>");
    }

    @Override
    public void visit(HyperlinkField hyperlinkField) throws Exception {
        visitModelField(hyperlinkField.getModelFormField());
        getWriter().append("<hyperlink");
        visitAttribute("also-hidden", hyperlinkField.getAlsoHidden());
        visitAttribute("confirmation-message", hyperlinkField.getConfirmationMsgExdr());
        visitAttribute("description", hyperlinkField.getDescription());
        visitAttribute("request-confirmation", hyperlinkField.getRequestConfirmation());
        Link link = hyperlinkField.getLink();
        visitLinkAttributes(link);
        if (link.getImage() != null || link.getAutoEntityParameters() != null || link.getAutoServiceParameters() != null) {
            getWriter().append(">");
            visitImage(link.getImage());
            visitAutoEntityParameters(link.getAutoEntityParameters());
            visitAutoServiceParameters(link.getAutoServiceParameters());
            getWriter().append("</hyperlink>");
        } else {
            getWriter().append("/>");
        }
        getWriter().append("</field>");
    }

    @Override
    public void visit(FormField formField) throws Exception {
        visitModelField(formField.getModelFormField());
        getWriter().append("<include-form");
        visitAttribute("name", formField.getFormName());
        visitAttribute("location", formField.getFormLocation());
        getWriter().append("/>");
    }

    @Override
    public void visit(GridField gridField) throws Exception {
        visitModelField(gridField.getModelFormField());
        getWriter().append("<include-grid");
        visitAttribute("name", gridField.getGridName());
        visitAttribute("location", gridField.getGridLocation());
        getWriter().append("/>");
    }

    @Override
    public void visit(MenuField menuField) throws Exception {
        visitModelField(menuField.getModelFormField());
        getWriter().append("<include-menu");
        visitAttribute("name", menuField.getMenuName());
        visitAttribute("location", menuField.getMenuLocation());
        getWriter().append("/>");
    }

    @Override
    public void visit(ScreenField screenField) throws Exception {
        visitModelField(screenField.getModelFormField());
        getWriter().append("<include-screen");
        visitAttribute("name", screenField.getScreenName());
        visitAttribute("location", screenField.getScreenLocation());
        getWriter().append("/>");
    }

    @Override
    public void visit(IgnoredField ignoredField) throws Exception {
        visitModelField(ignoredField.getModelFormField());
        getWriter().append("<ignored/></field>");
    }

    @Override
    public void visit(ImageField imageField) throws Exception {
        visitModelField(imageField.getModelFormField());
        getWriter().append("<image");
        visitAttribute("alternate", imageField.getAlternate());
        visitAttribute("default-value", imageField.getDefaultValue());
        visitAttribute("description", imageField.getDescription());
        visitAttribute("style", imageField.getStyle());
        visitAttribute("value", imageField.getValue());
        getWriter().append(">");
        visitSubHyperlink(imageField.getSubHyperlink());
        getWriter().append("</image></field>");
    }

    @Override
    public void visit(LookupField textField) throws Exception {
        visitModelField(textField.getModelFormField());
        getWriter().append("<lookup");
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
        getWriter().append(">");
        visitSubHyperlink(textField.getSubHyperlink());
        getWriter().append("</lookup></field>");
    }

    @Override
    public void visit(PasswordField textField) throws Exception {
        visitModelField(textField.getModelFormField());
        getWriter().append("<password");
        visitTextFieldAttrs(textField);
        getWriter().append(">");
        visitSubHyperlink(textField.getSubHyperlink());
        getWriter().append("</password></field>");
    }

    @Override
    public void visit(RadioField radioField) throws Exception {
        visitModelField(radioField.getModelFormField());
        getWriter().append("<radio");
        visitFieldInfoWithOptions(radioField);
        getWriter().append("</radio></field>");
    }

    @Override
    public void visit(RangeFindField textField) throws Exception {
        visitModelField(textField.getModelFormField());
        getWriter().append("<range-find");
        visitTextFieldAttrs(textField);
        visitAttribute("default-option-from", textField.getDefaultOptionFrom());
        visitAttribute("default-option-thru", textField.getDefaultOptionThru());
        getWriter().append(">");
        visitSubHyperlink(textField.getSubHyperlink());
        getWriter().append("</range-find></field>");
    }

    @Override
    public void visit(ResetField resetField) throws Exception {
        visitModelField(resetField.getModelFormField());
        getWriter().append("<reset/></field>");
    }

    @Override
    public void visit(SubmitField submitField) throws Exception {
        visitModelField(submitField.getModelFormField());
        getWriter().append("<submit");
        visitAttribute("background-submit-refresh-target", submitField.getBackgroundSubmitRefreshTargetExdr());
        visitAttribute("button-type", submitField.getButtonType());
        visitAttribute("confirmation-message", submitField.getConfirmationMsgExdr());
        visitAttribute("image-location", submitField.getImageLocation());
        visitAttribute("request-confirmation", submitField.getRequestConfirmation());
        getWriter().append("/></field>");
    }

    @Override
    public void visit(TextareaField textareaField) throws Exception {
        visitModelField(textareaField.getModelFormField());
        getWriter().append("<textarea");
        visitAttribute("cols", textareaField.getCols());
        visitAttribute("default-value", textareaField.getDefaultValue());
        visitAttribute("read-only", textareaField.isReadOnly());
        visitAttribute("rows", textareaField.getRows());
        visitAttribute("visual-editor-buttons", textareaField.getVisualEditorButtons());
        visitAttribute("visual-editor-enable", textareaField.getVisualEditorEnable());
        visitAttribute("placeholder", textareaField.getPlaceholder());
        getWriter().append("/></field>");
    }

    @Override
    public void visit(TextField textField) throws Exception {
        visitModelField(textField.getModelFormField());
        getWriter().append("<text");
        visitTextFieldAttrs(textField);
        getWriter().append(">");
        visitSubHyperlink(textField.getSubHyperlink());
        getWriter().append("</text></field>");
    }

    @Override
    public void visit(TextFindField textField) throws Exception {
        visitModelField(textField.getModelFormField());
        getWriter().append("<text-find");
        visitTextFieldAttrs(textField);
        visitAttribute("default-option", textField.getDefaultOption());
        visitAttribute("hide-options", textField.getHideOptions());
        visitAttribute("ignore-case", textField.getIgnoreCase());
        getWriter().append(">");
        visitSubHyperlink(textField.getSubHyperlink());
        getWriter().append("</text-find></field>");
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
        visitAttribute("type", field.isDateType() ? "date" : field.isTimeType() ? "time" : "timestamp");
        visitAttribute("input-method", field.getInputMethod());
        visitAttribute("isTwelveHour", field.isTwelveHour());
        visitAttribute("mask", field.useMask() ? "Y" : "N");
        visitAttribute("step", field.getStep());
    }

    private void visitDateRangePickerFieldAttrs(ModelFormField.DateRangePickerField field) throws Exception {
        visitAttribute("always-show-calendars", field.getAlwaysShowCalendars());
        visitAttribute("apply-button-classes", field.getApplyButtonClasses());
        visitAttribute("apply-label", field.getApplyLabel());
        visitAttribute("auto-apply", field.getAutoApply());
        visitAttribute("button-classes", field.getButtonClasses());
        visitAttribute("cancel-button-classes", field.getCancelButtonClasses());
        visitAttribute("cancel-label", field.getCancelLabel());
        visitAttribute("clear-title", field.getClearTitle());
        visitAttribute("default-value", field.getDefaultValue());
        visitAttribute("drops", field.getDrops());
        visitAttribute("linked-calendars", field.getLinkedCalendars());
        visitAttribute("max-year", field.getMaxYear());
        visitAttribute("min-year", field.getMinYear());
        visitAttribute("opens", field.getOpens());
        visitAttribute("show-dropdowns", field.getShowDropdowns());
        visitAttribute("show-iso-week-numbers", field.getShowIsoWeekNumbers());
        visitAttribute("show-ranges", field.getShowRanges());
        visitAttribute("show-week-numbers", field.getShowWeekNumbers());
        visitAttribute("single-date-picker", field.getSingleDatePicker());
        visitAttribute("time-picker", field.getTimePicker());
        visitAttribute("time-picker-24-hour", field.getTimePicker24Hour());
        visitAttribute("time-picker-increment", field.getTimePickerIncrement());
        visitAttribute("time-picker-seconds", field.getTimePickerSeconds());
    }

    private void visitFieldInfoWithOptions(FieldInfoWithOptions fieldInfo) throws Exception {
        visitAttribute("no-current-selected-key", fieldInfo.getNoCurrentSelectedKey());
        getWriter().append(">");
        // TODO: Options
    }

    private void visitModelField(ModelFormField modelField) throws Exception {
        if (modelField != null) {
            getWriter().append("<field");
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
            getWriter().append(">");
            visitUpdateAreas(modelField.getOnChangeUpdateAreas());
            visitUpdateAreas(modelField.getOnClickUpdateAreas());
        }
    }

    private void visitSubHyperlink(SubHyperlink hyperlink) throws Exception {
        if (hyperlink != null) {
            getWriter().append("<sub-hyperlink");
            Link link = hyperlink.getLink();
            visitLinkAttributes(link);
            visitAttribute("description", hyperlink.getDescription());
            visitAttribute("use-when", hyperlink.getUseWhen());
            if (link.getImage() != null || link.getAutoEntityParameters() != null || link.getAutoServiceParameters() != null) {
                getWriter().append(">");
                visitImage(link.getImage());
                visitAutoEntityParameters(link.getAutoEntityParameters());
                visitAutoServiceParameters(link.getAutoServiceParameters());
                getWriter().append("</sub-hyperlink>");
            } else {
                getWriter().append("/>");
            }
        }
    }

    private void visitAutoComplete(AutoComplete autoComplete) throws Exception {
        if (autoComplete != null) {
            getWriter().append("<auto-complete");
            visitAttribute("auto-select", autoComplete.getAutoSelect());
            visitAttribute("frequency", autoComplete.getFrequency());
            visitAttribute("min-chars", autoComplete.getMinChars());
            visitAttribute("choices", autoComplete.getChoices());
            visitAttribute("partial-search", autoComplete.getPartialSearch());
            visitAttribute("partial-chars", autoComplete.getPartialChars());
            visitAttribute("ignore-case", autoComplete.getIgnoreCase());
            visitAttribute("full-search", autoComplete.getFullSearch());
            getWriter().append("/>");
        }
    }

    private void visitInPlaceEditor(InPlaceEditor editor) throws Exception {
        if (editor != null) {
            getWriter().append("<in-place-editor");
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
                getWriter().append(">");
                getWriter().append("<simple-editor");
                visitAttribute("cols", editor.getCols());
                visitAttribute("rows", editor.getRows());
                getWriter().append("/></in-place-editor>");
            } else {
                getWriter().append("/>");
            }
        }
    }

    private void visitUpdateAreas(Collection<ModelForm.UpdateArea> updateAreas) throws Exception {
        for (ModelForm.UpdateArea updateArea : updateAreas) {
            getWriter().append("<on-field-event-update-area");
            visitAttribute("event-type", updateArea.getEventType());
            visitAttribute("area-id", updateArea.getAreaId());
            visitAttribute("area-target", updateArea.getAreaTarget());
            getWriter().append(">");
            visitAutoEntityParameters(updateArea.getAutoEntityParameters());
            visitAutoServiceParameters(updateArea.getAutoServiceParameters());
            visitParameters(updateArea.getParameterList());
            getWriter().append("</on-field-event-update-area>");
        }
    }
}
