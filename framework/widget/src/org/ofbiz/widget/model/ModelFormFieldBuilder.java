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
package org.ofbiz.widget.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelField;
import org.ofbiz.entity.model.ModelReader;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ModelParam;
import org.ofbiz.service.ModelService;
import org.ofbiz.widget.model.ModelForm.UpdateArea;
import org.ofbiz.widget.model.ModelFormField.CheckField;
import org.ofbiz.widget.model.ModelFormField.ContainerField;
import org.ofbiz.widget.model.ModelFormField.DateFindField;
import org.ofbiz.widget.model.ModelFormField.DateTimeField;
import org.ofbiz.widget.model.ModelFormField.DisplayEntityField;
import org.ofbiz.widget.model.ModelFormField.DisplayField;
import org.ofbiz.widget.model.ModelFormField.DropDownField;
import org.ofbiz.widget.model.ModelFormField.FileField;
import org.ofbiz.widget.model.ModelFormField.FormField;
import org.ofbiz.widget.model.ModelFormField.GridField;
import org.ofbiz.widget.model.ModelFormField.HiddenField;
import org.ofbiz.widget.model.ModelFormField.HyperlinkField;
import org.ofbiz.widget.model.ModelFormField.IgnoredField;
import org.ofbiz.widget.model.ModelFormField.ImageField;
import org.ofbiz.widget.model.ModelFormField.LookupField;
import org.ofbiz.widget.model.ModelFormField.MenuField;
import org.ofbiz.widget.model.ModelFormField.OptionSource;
import org.ofbiz.widget.model.ModelFormField.PasswordField;
import org.ofbiz.widget.model.ModelFormField.RadioField;
import org.ofbiz.widget.model.ModelFormField.RangeFindField;
import org.ofbiz.widget.model.ModelFormField.ResetField;
import org.ofbiz.widget.model.ModelFormField.ScreenField;
import org.ofbiz.widget.model.ModelFormField.SubmitField;
import org.ofbiz.widget.model.ModelFormField.TextField;
import org.ofbiz.widget.model.ModelFormField.TextFindField;
import org.ofbiz.widget.model.ModelFormField.TextareaField;
import org.w3c.dom.Element;

/**
 * A <code>ModelFormField</code> builder.
 */
public class ModelFormFieldBuilder {

    public static final String module = ModelFormFieldBuilder.class.getName();

    private FlexibleStringExpander action = FlexibleStringExpander.getInstance("");;
    private String attributeName = "";
    private boolean encodeOutput = true;
    private String entityName = "";
    private FlexibleMapAccessor<Object> entryAcsr = null;
    private String event = "";
    private FieldInfo fieldInfo = null;
    private String fieldName = "";
    private String fieldType = null;
    private String headerLink = "";
    private String headerLinkStyle = "";
    private String idName = "";
    private FlexibleMapAccessor<Map<String, ? extends Object>> mapAcsr = null;
    private ModelForm modelForm = null;
    private String name = "";
    private List<UpdateArea> onChangeUpdateAreas = new ArrayList<UpdateArea>();
    private List<UpdateArea> onClickUpdateAreas = new ArrayList<UpdateArea>();
    private String parameterName = "";
    private Integer position = null;
    private String redWhen = "";
    private Boolean requiredField = null;
    private String requiredFieldStyle = "";
    private boolean separateColumn = false;
    private String serviceName = "";
    private Boolean sortField = null;
    private String sortFieldAscStyle = "";
    private String sortFieldDescStyle = "";
    private String sortFieldHelpText = "";
    private String sortFieldStyle = "";
    private FlexibleStringExpander title = FlexibleStringExpander.getInstance("");;
    private String titleAreaStyle = "";
    private String titleStyle = "";
    private FlexibleStringExpander tooltip = FlexibleStringExpander.getInstance("");;
    private String tooltipStyle = "";
    private FlexibleStringExpander useWhen = FlexibleStringExpander.getInstance("");;
    private FlexibleStringExpander ignoreWhen = FlexibleStringExpander.getInstance("");
    private String widgetAreaStyle = "";
    private String widgetStyle = "";
    private String parentFormName = "";
    private String tabindex = "";

    public ModelFormFieldBuilder() {
    }

    /** XML Constructor */
    public ModelFormFieldBuilder(Element fieldElement, ModelForm modelForm, ModelReader entityModelReader,
            DispatchContext dispatchContext) {
        String name = fieldElement.getAttribute("name");
        this.action = FlexibleStringExpander.getInstance(fieldElement.getAttribute("action"));
        this.attributeName = UtilXml.checkEmpty(fieldElement.getAttribute("attribute-name"), name);
        this.encodeOutput = !"false".equals(fieldElement.getAttribute("encode-output"));
        this.entityName = fieldElement.getAttribute("entity-name");
        this.entryAcsr = FlexibleMapAccessor.getInstance(UtilXml.checkEmpty(fieldElement.getAttribute("entry-name"), name));
        this.event = fieldElement.getAttribute("event");
        this.fieldName = UtilXml.checkEmpty(fieldElement.getAttribute("field-name"), name);
        this.headerLink = fieldElement.getAttribute("header-link");
        this.headerLinkStyle = fieldElement.getAttribute("header-link-style");
        this.idName = fieldElement.getAttribute("id-name");
        this.mapAcsr = FlexibleMapAccessor.getInstance(fieldElement.getAttribute("map-name"));
        this.modelForm = modelForm;
        this.name = name;
        this.parameterName = UtilXml.checkEmpty(fieldElement.getAttribute("parameter-name"), name);
        String positionAtttr = fieldElement.getAttribute("position");
        Integer position = null;
        if (!positionAtttr.isEmpty()) {
            position = Integer.parseInt(positionAtttr);
        }
        this.position = position;
        this.redWhen = fieldElement.getAttribute("red-when");
        String requiredField = fieldElement.getAttribute("required-field");
        this.requiredField = requiredField.isEmpty() ? null : "true".equals(requiredField);
        this.requiredFieldStyle = fieldElement.getAttribute("required-field-style");
        this.separateColumn = "true".equals(fieldElement.getAttribute("separate-column"));
        this.serviceName = fieldElement.getAttribute("service-name");
        String sortField = fieldElement.getAttribute("sort-field");
        this.sortField = sortField.isEmpty() ? null : "true".equals(sortField);
        this.sortFieldAscStyle = fieldElement.getAttribute("sort-field-asc-style");
        this.sortFieldDescStyle = fieldElement.getAttribute("sort-field-desc-style");
        this.sortFieldHelpText = fieldElement.getAttribute("sort-field-help-text");
        this.sortFieldStyle = fieldElement.getAttribute("sort-field-style");
        this.title = FlexibleStringExpander.getInstance(fieldElement.getAttribute("title"));
        this.titleAreaStyle = fieldElement.getAttribute("title-area-style");
        this.titleStyle = fieldElement.getAttribute("title-style");
        this.tooltip = FlexibleStringExpander.getInstance(fieldElement.getAttribute("tooltip"));
        this.tooltipStyle = fieldElement.getAttribute("tooltip-style");
        this.useWhen = FlexibleStringExpander.getInstance(fieldElement.getAttribute("use-when"));
        this.ignoreWhen = FlexibleStringExpander.getInstance(fieldElement.getAttribute("ignore-when"));
        this.widgetAreaStyle = fieldElement.getAttribute("widget-area-style");
        this.widgetStyle = fieldElement.getAttribute("widget-style");
        this.parentFormName = fieldElement.getAttribute("form-name");
        this.tabindex = fieldElement.getAttribute("tabindex");
        Element childElement = null;
        List<? extends Element> subElements = UtilXml.childElementList(fieldElement);
        for (Element subElement : subElements) {
            String subElementName = subElement.getTagName();
            if ("on-field-event-update-area".equals(subElementName)) {
                UpdateArea updateArea = new UpdateArea(subElement);
                if ("change".equals(updateArea.getEventType()))
                    onChangeUpdateAreas.add(updateArea);
                else if ("click".equals(updateArea.getEventType()))
                    onClickUpdateAreas.add(updateArea);
            } else {
                if (this.fieldType != null) {
                    throw new IllegalArgumentException("Multiple field types found: " + this.fieldType + ", " + subElementName);
                }
                this.fieldType = subElementName;
                childElement = subElement;
            }
        }
        if (UtilValidate.isEmpty(this.fieldType)) {
            this.induceFieldInfo(modelForm, null, entityModelReader, dispatchContext);
        } else if ("display".equals(this.fieldType))
            this.fieldInfo = new DisplayField(childElement, null);
        else if ("display-entity".equals(this.fieldType))
            this.fieldInfo = new DisplayEntityField(childElement, null);
        else if ("hyperlink".equals(this.fieldType))
            this.fieldInfo = new HyperlinkField(childElement, null);
        else if ("text".equals(this.fieldType))
            this.fieldInfo = new TextField(childElement, null);
        else if ("textarea".equals(this.fieldType))
            this.fieldInfo = new TextareaField(childElement, null);
        else if ("date-time".equals(this.fieldType))
            this.fieldInfo = new DateTimeField(childElement, null);
        else if ("drop-down".equals(this.fieldType))
            this.fieldInfo = new DropDownField(childElement, null);
        else if ("check".equals(this.fieldType))
            this.fieldInfo = new CheckField(childElement, null);
        else if ("radio".equals(this.fieldType))
            this.fieldInfo = new RadioField(childElement, null);
        else if ("submit".equals(this.fieldType))
            this.fieldInfo = new SubmitField(childElement, null);
        else if ("reset".equals(this.fieldType))
            this.fieldInfo = new ResetField(childElement, null);
        else if ("hidden".equals(this.fieldType))
            this.fieldInfo = new HiddenField(childElement, null);
        else if ("ignored".equals(this.fieldType))
            this.fieldInfo = new IgnoredField(childElement, null);
        else if ("text-find".equals(this.fieldType))
            this.fieldInfo = new TextFindField(childElement, null);
        else if ("date-find".equals(this.fieldType))
            this.fieldInfo = new DateFindField(childElement, null);
        else if ("range-find".equals(this.fieldType))
            this.fieldInfo = new RangeFindField(childElement, null);
        else if ("lookup".equals(this.fieldType))
            this.fieldInfo = new LookupField(childElement, null);
        else if ("include-menu".equals(this.fieldType))
            this.fieldInfo = new MenuField(childElement, null);
        else if ("include-form".equals(this.fieldType))
            this.fieldInfo = new FormField(childElement, null);
        else if ("include-grid".equals(this.fieldType))
            this.fieldInfo = new GridField(childElement, null);
        else if ("include-screen".equals(this.fieldType))
            this.fieldInfo = new ScreenField(childElement, null);
        else if ("file".equals(this.fieldType))
            this.fieldInfo = new FileField(childElement, null);
        else if ("password".equals(this.fieldType))
            this.fieldInfo = new PasswordField(childElement, null);
        else if ("image".equals(this.fieldType))
            this.fieldInfo = new ImageField(childElement, null);
        else if ("container".equals(this.fieldType))
            this.fieldInfo = new ContainerField(childElement, null);
        else
            throw new IllegalArgumentException("The field sub-element with name " + this.fieldType + " is not supported");
    }

    public ModelFormFieldBuilder(ModelFormField modelFormField) {
        this.action = modelFormField.getAction();
        this.attributeName = modelFormField.getAttributeName();
        this.encodeOutput = modelFormField.getEncodeOutput();
        this.entityName = modelFormField.getEntityName();
        this.entryAcsr = modelFormField.getEntryAcsr();
        this.event = modelFormField.getEvent();
        this.fieldInfo = modelFormField.getFieldInfo();
        this.fieldName = modelFormField.getFieldName();
        this.headerLink = modelFormField.getHeaderLink();
        this.headerLinkStyle = modelFormField.getHeaderLinkStyle();
        this.idName = modelFormField.getIdName();
        this.mapAcsr = modelFormField.getMapAcsr();
        this.modelForm = modelFormField.getModelForm();
        this.name = modelFormField.getName();
        this.onChangeUpdateAreas.addAll(modelFormField.getOnChangeUpdateAreas());
        this.onClickUpdateAreas.addAll(modelFormField.getOnClickUpdateAreas());
        this.parameterName = modelFormField.getParameterName();
        this.position = modelFormField.getPosition();
        this.redWhen = modelFormField.getRedWhen();
        this.requiredField = modelFormField.getRequiredField();
        this.requiredFieldStyle = modelFormField.getRequiredFieldStyle();
        this.separateColumn = modelFormField.getSeparateColumn();
        this.serviceName = modelFormField.getServiceName();
        this.sortField = modelFormField.getSortField();
        this.sortFieldAscStyle = modelFormField.getSortFieldAscStyle();
        this.sortFieldDescStyle = modelFormField.getSortFieldDescStyle();
        this.sortFieldHelpText = modelFormField.getSortFieldHelpText();
        this.sortFieldStyle = modelFormField.getSortFieldStyle();
        this.title = modelFormField.getTitle();
        this.titleAreaStyle = modelFormField.getTitleAreaStyle();
        this.titleStyle = modelFormField.getTitleStyle();
        this.tooltip = modelFormField.getTooltip();
        this.tooltipStyle = modelFormField.getTooltipStyle();
        this.useWhen = modelFormField.getUseWhen();
        this.widgetAreaStyle = modelFormField.getWidgetAreaStyle();
        this.widgetStyle = modelFormField.getWidgetStyle();
        this.parentFormName = modelFormField.getParentFormName();
        this.tabindex = modelFormField.getTabindex();
    }

    public ModelFormFieldBuilder(ModelFormFieldBuilder builder) {
        this.action = builder.getAction();
        this.attributeName = builder.getAttributeName();
        this.encodeOutput = builder.getEncodeOutput();
        this.entityName = builder.getEntityName();
        this.entryAcsr = builder.getEntryAcsr();
        this.event = builder.getEvent();
        this.fieldInfo = builder.getFieldInfo();
        this.fieldName = builder.getFieldName();
        this.headerLink = builder.getHeaderLink();
        this.headerLinkStyle = builder.getHeaderLinkStyle();
        this.idName = builder.getIdName();
        this.mapAcsr = builder.getMapAcsr();
        this.modelForm = builder.getModelForm();
        this.name = builder.getName();
        this.onChangeUpdateAreas.addAll(builder.getOnChangeUpdateAreas());
        this.onClickUpdateAreas.addAll(builder.getOnClickUpdateAreas());
        this.parameterName = builder.getParameterName();
        this.position = builder.getPosition();
        this.redWhen = builder.getRedWhen();
        this.requiredField = builder.getRequiredField();
        this.requiredFieldStyle = builder.getRequiredFieldStyle();
        this.separateColumn = builder.getSeparateColumn();
        this.serviceName = builder.getServiceName();
        this.sortField = builder.getSortField();
        this.sortFieldAscStyle = builder.getSortFieldAscStyle();
        this.sortFieldDescStyle = builder.getSortFieldDescStyle();
        this.sortFieldHelpText = builder.getSortFieldHelpText();
        this.sortFieldStyle = builder.getSortFieldStyle();
        this.title = builder.getTitle();
        this.titleAreaStyle = builder.getTitleAreaStyle();
        this.titleStyle = builder.getTitleStyle();
        this.tooltip = builder.getTooltip();
        this.tooltipStyle = builder.getTooltipStyle();
        this.useWhen = builder.getUseWhen();
        this.widgetAreaStyle = builder.getWidgetAreaStyle();
        this.widgetStyle = builder.getWidgetStyle();
        this.parentFormName = builder.getParentFormName();
        this.tabindex = builder.getTabindex();
    }

    public ModelFormFieldBuilder addOnChangeUpdateArea(UpdateArea onChangeUpdateArea) {
        this.onChangeUpdateAreas.add(onChangeUpdateArea);
        return this;
    }

    public ModelFormFieldBuilder addOnClickUpdateArea(UpdateArea onClickUpdateArea) {
        this.onClickUpdateAreas.add(onClickUpdateArea);
        return this;
    }

    public ModelFormField build() {
        return ModelFormField.from(this);
    }

    public FlexibleStringExpander getAction() {
        return action;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public boolean getEncodeOutput() {
        return encodeOutput;
    }

    public String getEntityName() {
        return entityName;
    }

    public FlexibleMapAccessor<Object> getEntryAcsr() {
        return entryAcsr;
    }

    public String getEvent() {
        return event;
    }

    public FieldInfo getFieldInfo() {
        return fieldInfo;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getFieldType() {
        return fieldType;
    }

    public String getHeaderLink() {
        return headerLink;
    }

    public String getHeaderLinkStyle() {
        return headerLinkStyle;
    }

    public String getIdName() {
        return idName;
    }

    public FlexibleMapAccessor<Map<String, ? extends Object>> getMapAcsr() {
        return mapAcsr;
    }

    public ModelForm getModelForm() {
        return modelForm;
    }

    public String getName() {
        return name;
    }

    public List<UpdateArea> getOnChangeUpdateAreas() {
        return onChangeUpdateAreas;
    }

    public List<UpdateArea> getOnClickUpdateAreas() {
        return onClickUpdateAreas;
    }

    public String getParameterName() {
        return parameterName;
    }

    public Integer getPosition() {
        return position;
    }

    public String getRedWhen() {
        return redWhen;
    }

    public Boolean getRequiredField() {
        return requiredField;
    }

    public String getRequiredFieldStyle() {
        return requiredFieldStyle;
    }

    public boolean getSeparateColumn() {
        return separateColumn;
    }

    public String getServiceName() {
        return serviceName;
    }

    public Boolean getSortField() {
        return sortField;
    }

    public String getSortFieldAscStyle() {
        return sortFieldAscStyle;
    }

    public String getSortFieldDescStyle() {
        return sortFieldDescStyle;
    }

    public String getSortFieldHelpText() {
        return sortFieldHelpText;
    }

    public String getSortFieldStyle() {
        return sortFieldStyle;
    }

    public FlexibleStringExpander getTitle() {
        return title;
    }

    public String getTitleAreaStyle() {
        return titleAreaStyle;
    }

    public String getTitleStyle() {
        return titleStyle;
    }

    public FlexibleStringExpander getTooltip() {
        return tooltip;
    }

    public String getTooltipStyle() {
        return tooltipStyle;
    }

    public FlexibleStringExpander getUseWhen() {
        return useWhen;
    }

    public FlexibleStringExpander getIgnoreWhen() {
        return ignoreWhen;
    }

    public String getWidgetAreaStyle() {
        return widgetAreaStyle;
    }

    public String getWidgetStyle() {
        return widgetStyle;
    }

    public String getParentFormName() {
        return parentFormName;
    }

    public String getTabindex() {
        return tabindex;
    }

    private boolean induceFieldInfo(ModelForm modelForm, String defaultFieldType, ModelReader entityModelReader, DispatchContext dispatchContext) {
        if (induceFieldInfoFromEntityField(defaultFieldType, entityModelReader))
            return true;
        if (induceFieldInfoFromServiceParam(defaultFieldType, entityModelReader, dispatchContext))
            return true;
        return false;
    }

    public boolean induceFieldInfoFromEntityField(ModelEntity modelEntity, ModelField modelField, String defaultFieldType) {
        if (modelEntity == null || modelField == null)
            return false;
        this.entityName = modelEntity.getEntityName();
        this.fieldName = modelField.getName();
        if ("find".equals(defaultFieldType)) {
            if ("id".equals(modelField.getType()) || "id-ne".equals(modelField.getType())) {
                ModelFormField.TextFindField textField = new ModelFormField.TextFindField(FieldInfo.SOURCE_AUTO_ENTITY, 20,
                        Integer.valueOf(20), null);
                this.setFieldInfo(textField);
            } else if ("id-long".equals(modelField.getType()) || "id-long-ne".equals(modelField.getType())) {
                ModelFormField.TextFindField textField = new ModelFormField.TextFindField(FieldInfo.SOURCE_AUTO_ENTITY, 40,
                        Integer.valueOf(60), null);
                this.setFieldInfo(textField);
            } else if ("id-vlong".equals(modelField.getType()) || "id-vlong-ne".equals(modelField.getType())) {
                ModelFormField.TextFindField textField = new ModelFormField.TextFindField(FieldInfo.SOURCE_AUTO_ENTITY, 60,
                        Integer.valueOf(250), null);
                this.setFieldInfo(textField);
            } else if ("very-short".equals(modelField.getType())) {
                ModelFormField.TextField textField = new ModelFormField.TextField(FieldInfo.SOURCE_AUTO_ENTITY, 6,
                        Integer.valueOf(10), null);
                this.setFieldInfo(textField);
            } else if ("name".equals(modelField.getType()) || "short-varchar".equals(modelField.getType())) {
                ModelFormField.TextFindField textField = new ModelFormField.TextFindField(FieldInfo.SOURCE_AUTO_ENTITY, 40,
                        Integer.valueOf(60), null);
                this.setFieldInfo(textField);
            } else if ("value".equals(modelField.getType()) || "comment".equals(modelField.getType())
                    || "description".equals(modelField.getType()) || "long-varchar".equals(modelField.getType())
                    || "url".equals(modelField.getType()) || "email".equals(modelField.getType())) {
                ModelFormField.TextFindField textField = new ModelFormField.TextFindField(FieldInfo.SOURCE_AUTO_ENTITY, 60,
                        Integer.valueOf(250), null);
                this.setFieldInfo(textField);
            } else if ("floating-point".equals(modelField.getType()) || "currency-amount".equals(modelField.getType())
                    || "numeric".equals(modelField.getType())) {
                ModelFormField.RangeFindField textField = new ModelFormField.RangeFindField(FieldInfo.SOURCE_AUTO_ENTITY, 6, null);
                this.setFieldInfo(textField);
            } else if ("date-time".equals(modelField.getType()) || "date".equals(modelField.getType())
                    || "time".equals(modelField.getType())) {
                String type = modelField.getType();
                if ("date-time".equals(modelField.getType())) {
                    type = "timestamp";
                }
                ModelFormField.DateFindField dateTimeField = new ModelFormField.DateFindField(FieldInfo.SOURCE_AUTO_ENTITY, type);
                this.setFieldInfo(dateTimeField);
            } else {
                ModelFormField.TextFindField textField = new ModelFormField.TextFindField(FieldInfo.SOURCE_AUTO_ENTITY, null);
                this.setFieldInfo(textField);
            }
        } else if ("display".equals(defaultFieldType)) {
            ModelFormField.DisplayField displayField = new ModelFormField.DisplayField(FieldInfo.SOURCE_AUTO_SERVICE, null);
            this.setFieldInfo(displayField);
        } else if ("hidden".equals(defaultFieldType)) {
            ModelFormField.HiddenField hiddenField = new ModelFormField.HiddenField(FieldInfo.SOURCE_AUTO_SERVICE, null);
            this.setFieldInfo(hiddenField);
        } else {
            if ("id".equals(modelField.getType()) || "id-ne".equals(modelField.getType())) {
                ModelFormField.TextField textField = new ModelFormField.TextField(FieldInfo.SOURCE_AUTO_ENTITY, 20,
                        Integer.valueOf(20), null);
                this.setFieldInfo(textField);
            } else if ("id-long".equals(modelField.getType()) || "id-long-ne".equals(modelField.getType())) {
                ModelFormField.TextField textField = new ModelFormField.TextField(FieldInfo.SOURCE_AUTO_ENTITY, 40,
                        Integer.valueOf(60), null);
                this.setFieldInfo(textField);
            } else if ("id-vlong".equals(modelField.getType()) || "id-vlong-ne".equals(modelField.getType())) {
                ModelFormField.TextField textField = new ModelFormField.TextField(FieldInfo.SOURCE_AUTO_ENTITY, 60,
                        Integer.valueOf(250), null);
                this.setFieldInfo(textField);
            } else if ("indicator".equals(modelField.getType())) {
                ArrayList<OptionSource> optionSources = new ArrayList<OptionSource>();
                optionSources.add(new ModelFormField.SingleOption("Y", null, null));
                optionSources.add(new ModelFormField.SingleOption("N", null, null));
                ModelFormField.DropDownField dropDownField = new ModelFormField.DropDownField(FieldInfo.SOURCE_AUTO_ENTITY,
                        optionSources);
                this.setFieldInfo(dropDownField);
            } else if ("very-short".equals(modelField.getType())) {
                ModelFormField.TextField textField = new ModelFormField.TextField(FieldInfo.SOURCE_AUTO_ENTITY, 6,
                        Integer.valueOf(10), null);
                this.setFieldInfo(textField);
            } else if ("very-long".equals(modelField.getType())) {
                ModelFormField.TextareaField textareaField = new ModelFormField.TextareaField(FieldInfo.SOURCE_AUTO_ENTITY, null);
                this.setFieldInfo(textareaField);
            } else if ("name".equals(modelField.getType()) || "short-varchar".equals(modelField.getType())) {
                ModelFormField.TextField textField = new ModelFormField.TextField(FieldInfo.SOURCE_AUTO_ENTITY, 40,
                        Integer.valueOf(60), null);
                this.setFieldInfo(textField);
            } else if ("value".equals(modelField.getType()) || "comment".equals(modelField.getType())
                    || "description".equals(modelField.getType()) || "long-varchar".equals(modelField.getType())
                    || "url".equals(modelField.getType()) || "email".equals(modelField.getType())) {
                ModelFormField.TextField textField = new ModelFormField.TextField(FieldInfo.SOURCE_AUTO_ENTITY, 60,
                        Integer.valueOf(250), null);
                this.setFieldInfo(textField);
            } else if ("floating-point".equals(modelField.getType()) || "currency-amount".equals(modelField.getType())
                    || "numeric".equals(modelField.getType())) {
                ModelFormField.TextField textField = new ModelFormField.TextField(FieldInfo.SOURCE_AUTO_ENTITY, 6, null, null);
                this.setFieldInfo(textField);
            } else if ("date-time".equals(modelField.getType()) || "date".equals(modelField.getType())
                    || "time".equals(modelField.getType())) {
                String type = modelField.getType();
                if ("date-time".equals(modelField.getType())) {
                    type = "timestamp";
                }
                ModelFormField.DateTimeField dateTimeField = new ModelFormField.DateTimeField(FieldInfo.SOURCE_AUTO_ENTITY, type);
                this.setFieldInfo(dateTimeField);
            } else {
                ModelFormField.TextField textField = new ModelFormField.TextField(FieldInfo.SOURCE_AUTO_ENTITY, null);
                this.setFieldInfo(textField);
            }
        }
        return true;
    }

    private boolean induceFieldInfoFromEntityField(String defaultFieldType, ModelReader entityModelReader) {
        if (UtilValidate.isEmpty(this.getEntityName()) || UtilValidate.isEmpty(this.getFieldName()))
            return false;
        try {
            ModelEntity modelEntity = entityModelReader.getModelEntity(this.getEntityName());
            if (modelEntity != null) {
                ModelField modelField = modelEntity.getField(this.getFieldName());
                if (modelField != null) {
                    // okay, populate using the entity field info...
                    this.induceFieldInfoFromEntityField(modelEntity, modelField, defaultFieldType);
                    return true;
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return false;
    }

    public boolean induceFieldInfoFromServiceParam(ModelService modelService, ModelParam modelParam, String defaultFieldType) {
        if (modelService == null || modelParam == null)
            return false;
        this.serviceName = modelService.name;
        this.attributeName = modelParam.name;
        if ("find".equals(defaultFieldType)) {
            if (modelParam.type.indexOf("Double") != -1 || modelParam.type.indexOf("Float") != -1
                    || modelParam.type.indexOf("Long") != -1 || modelParam.type.indexOf("Integer") != -1) {
                ModelFormField.RangeFindField textField = new ModelFormField.RangeFindField(FieldInfo.SOURCE_AUTO_SERVICE, 6,
                        null);
                this.setFieldInfo(textField);
            } else if (modelParam.type.indexOf("Timestamp") != -1) {
                ModelFormField.DateFindField dateTimeField = new ModelFormField.DateFindField(FieldInfo.SOURCE_AUTO_SERVICE,
                        "timestamp");
                this.setFieldInfo(dateTimeField);
            } else if (modelParam.type.indexOf("Date") != -1) {
                ModelFormField.DateFindField dateTimeField = new ModelFormField.DateFindField(FieldInfo.SOURCE_AUTO_SERVICE,
                        "date");
                this.setFieldInfo(dateTimeField);
            } else if (modelParam.type.indexOf("Time") != -1) {
                ModelFormField.DateFindField dateTimeField = new ModelFormField.DateFindField(FieldInfo.SOURCE_AUTO_SERVICE,
                        "time");
                this.setFieldInfo(dateTimeField);
            } else {
                ModelFormField.TextFindField textField = new ModelFormField.TextFindField(FieldInfo.SOURCE_AUTO_SERVICE, null);
                this.setFieldInfo(textField);
            }
        } else if ("display".equals(defaultFieldType)) {
            ModelFormField.DisplayField displayField = new ModelFormField.DisplayField(FieldInfo.SOURCE_AUTO_SERVICE, null);
            this.setFieldInfo(displayField);
        } else {
            // default to "edit"
            if (modelParam.type.indexOf("Double") != -1 || modelParam.type.indexOf("Float") != -1
                    || modelParam.type.indexOf("Long") != -1 || modelParam.type.indexOf("Integer") != -1) {
                ModelFormField.TextField textField = new ModelFormField.TextField(FieldInfo.SOURCE_AUTO_SERVICE, 6, null, null);
                this.setFieldInfo(textField);
            } else if (modelParam.type.indexOf("Timestamp") != -1) {
                ModelFormField.DateTimeField dateTimeField = new ModelFormField.DateTimeField(FieldInfo.SOURCE_AUTO_SERVICE,
                        "timestamp");
                this.setFieldInfo(dateTimeField);
            } else if (modelParam.type.indexOf("Date") != -1) {
                ModelFormField.DateTimeField dateTimeField = new ModelFormField.DateTimeField(FieldInfo.SOURCE_AUTO_SERVICE,
                        "date");
                this.setFieldInfo(dateTimeField);
            } else if (modelParam.type.indexOf("Time") != -1) {
                ModelFormField.DateTimeField dateTimeField = new ModelFormField.DateTimeField(FieldInfo.SOURCE_AUTO_SERVICE,
                        "time");
                this.setFieldInfo(dateTimeField);
            } else {
                ModelFormField.TextField textField = new ModelFormField.TextField(FieldInfo.SOURCE_AUTO_SERVICE, null);
                this.setFieldInfo(textField);
            }
        }
        return true;
    }

    private boolean induceFieldInfoFromServiceParam(String defaultFieldType, ModelReader entityModelReader,
            DispatchContext dispatchContext) {
        if (UtilValidate.isEmpty(this.getServiceName()) || UtilValidate.isEmpty(this.getAttributeName()))
            return false;
        try {
            ModelService modelService = dispatchContext.getModelService(this.getServiceName());
            if (modelService != null) {
                ModelParam modelParam = modelService.getParam(this.getAttributeName());
                if (modelParam != null) {
                    if (UtilValidate.isNotEmpty(modelParam.entityName) && UtilValidate.isNotEmpty(modelParam.fieldName)) {
                        this.entityName = modelParam.entityName;
                        this.fieldName = modelParam.fieldName;
                        if (this.induceFieldInfoFromEntityField(defaultFieldType, entityModelReader)) {
                            return true;
                        }
                    }

                    this.induceFieldInfoFromServiceParam(modelService, modelParam, defaultFieldType);
                    return true;
                }
            }
        } catch (GenericServiceException e) {
            Debug.logError(e,
                    "error getting service parameter definition for auto-field with serviceName: " + this.getServiceName()
                            + ", and attributeName: " + this.getAttributeName(), module);
        }
        return false;
    }

    public void mergeOverrideModelFormField(ModelFormFieldBuilder builder) {
        if (builder == null)
            return;
        if (UtilValidate.isNotEmpty(builder.getName()))
            this.name = builder.getName();
        if (UtilValidate.isNotEmpty(builder.getMapAcsr()))
            this.mapAcsr = builder.getMapAcsr();
        if (UtilValidate.isNotEmpty(builder.getEntityName()))
            this.entityName = builder.getEntityName();
        if (UtilValidate.isNotEmpty(builder.getServiceName()))
            this.serviceName = builder.getServiceName();
        if (UtilValidate.isNotEmpty(builder.getEntryAcsr()))
            this.entryAcsr = builder.getEntryAcsr();
        if (UtilValidate.isNotEmpty(builder.getParameterName()))
            this.parameterName = builder.getParameterName();
        if (UtilValidate.isNotEmpty(builder.getFieldName()))
            this.fieldName = builder.getFieldName();
        if (!builder.getAttributeName().isEmpty())
            this.attributeName = builder.getAttributeName();
        if (UtilValidate.isNotEmpty(builder.getTitle()))
            this.title = builder.getTitle();
        if (UtilValidate.isNotEmpty(builder.getTooltip()))
            this.tooltip = builder.getTooltip();
        if (builder.getSortField() != null)
            this.sortField = builder.getSortField();
        if (UtilValidate.isNotEmpty(builder.getSortFieldHelpText()))
            this.sortFieldHelpText = builder.getSortFieldHelpText();
        if (UtilValidate.isNotEmpty(builder.getTitleAreaStyle()))
            this.titleAreaStyle = builder.getTitleAreaStyle();
        if (UtilValidate.isNotEmpty(builder.getWidgetAreaStyle()))
            this.widgetAreaStyle = builder.getWidgetAreaStyle();
        if (UtilValidate.isNotEmpty(builder.getTitleStyle()))
            this.titleStyle = builder.getTitleStyle();
        if (UtilValidate.isNotEmpty(builder.getWidgetStyle()))
            this.widgetStyle = builder.getWidgetStyle();
        if (UtilValidate.isNotEmpty(builder.getRedWhen()))
            this.redWhen = builder.getRedWhen();
        if (UtilValidate.isNotEmpty(builder.getEvent()))
            this.event = builder.getEvent();
        if (!builder.getAction().isEmpty())
            this.action = builder.getAction();
        if (UtilValidate.isNotEmpty(builder.getUseWhen()))
            this.useWhen = builder.getUseWhen();
        if (UtilValidate.isNotEmpty(builder.getIgnoreWhen()))
            this.ignoreWhen = builder.getIgnoreWhen();
        if (builder.getFieldInfo() != null)
            this.setFieldInfo(builder.getFieldInfo());
        if (UtilValidate.isNotEmpty(builder.getHeaderLink()))
            this.headerLink = builder.getHeaderLink();
        if (UtilValidate.isNotEmpty(builder.getHeaderLinkStyle()))
            this.headerLinkStyle = builder.getHeaderLinkStyle();
        if (UtilValidate.isNotEmpty(builder.getIdName()))
            this.idName = builder.getIdName();
        if (UtilValidate.isNotEmpty(builder.getOnChangeUpdateAreas()))
            this.onChangeUpdateAreas.addAll(builder.getOnChangeUpdateAreas());
        if (UtilValidate.isNotEmpty(builder.getOnClickUpdateAreas()))
            this.onClickUpdateAreas.addAll(builder.getOnClickUpdateAreas());
        if (UtilValidate.isNotEmpty(builder.getParentFormName()))
            this.parentFormName = builder.getParentFormName();
        if (UtilValidate.isNotEmpty(builder.getTabindex()))
            this.tabindex = builder.getTabindex();
        this.encodeOutput = builder.getEncodeOutput();
        this.position = builder.getPosition();
        this.requiredField = builder.getRequiredField();
        this.separateColumn = builder.getSeparateColumn();
    }

    public ModelFormFieldBuilder setAction(String action) {
        this.action = FlexibleStringExpander.getInstance(action);
        return this;
    }

    public ModelFormFieldBuilder setAttributeName(String attributeName) {
        this.attributeName = attributeName;
        return this;
    }

    public ModelFormFieldBuilder setEncodeOutput(boolean encodeOutput) {
        this.encodeOutput = encodeOutput;
        return this;
    }

    public ModelFormFieldBuilder setEntityName(String entityName) {
        this.entityName = entityName;
        return this;
    }

    public ModelFormFieldBuilder setEntryName(String entryName) {
        this.entryAcsr = FlexibleMapAccessor.getInstance(entryName);
        return this;
    }

    public ModelFormFieldBuilder setEvent(String event) {
        this.event = event;
        return this;
    }

    public ModelFormFieldBuilder setFieldInfo(FieldInfo fieldInfo) {
        if (fieldInfo != null && (this.fieldInfo == null || (fieldInfo.getFieldSource() <= this.fieldInfo.getFieldSource()))) {
            this.fieldInfo = fieldInfo;
        }
        return this;
    }

    public ModelFormFieldBuilder setFieldName(String fieldName) {
        this.fieldName = fieldName;
        return this;
    }

    public ModelFormFieldBuilder setFieldType(String fieldType) {
        this.fieldType = fieldType;
        return this;
    }

    public ModelFormFieldBuilder setHeaderLink(String headerLink) {
        this.headerLink = headerLink;
        return this;
    }

    public ModelFormFieldBuilder setHeaderLinkStyle(String headerLinkStyle) {
        this.headerLinkStyle = headerLinkStyle;
        return this;
    }

    public ModelFormFieldBuilder setIdName(String idName) {
        this.idName = idName;
        return this;
    }

    public ModelFormFieldBuilder setMapName(String mapName) {
        this.mapAcsr = FlexibleMapAccessor.getInstance(mapName);
        return this;
    }

    public ModelFormFieldBuilder setModelForm(ModelForm modelForm) {
        this.modelForm = modelForm;
        return this;
    }

    public ModelFormFieldBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public ModelFormFieldBuilder setParameterName(String parameterName) {
        this.parameterName = parameterName;
        return this;
    }

    public ModelFormFieldBuilder setPosition(Integer position) {
        this.position = position;
        return this;
    }

    public ModelFormFieldBuilder setRedWhen(String redWhen) {
        this.redWhen = redWhen;
        return this;
    }

    public ModelFormFieldBuilder setRequiredField(Boolean requiredField) {
        this.requiredField = requiredField;
        return this;
    }

    public ModelFormFieldBuilder setRequiredFieldStyle(String requiredFieldStyle) {
        this.requiredFieldStyle = requiredFieldStyle;
        return this;
    }

    public ModelFormFieldBuilder setSeparateColumn(boolean separateColumn) {
        this.separateColumn = separateColumn;
        return this;
    }

    public ModelFormFieldBuilder setServiceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    public ModelFormFieldBuilder setSortField(Boolean sortField) {
        this.sortField = sortField;
        return this;
    }

    public ModelFormFieldBuilder setSortFieldAscStyle(String sortFieldAscStyle) {
        this.sortFieldAscStyle = sortFieldAscStyle;
        return this;
    }

    public ModelFormFieldBuilder setSortFieldDescStyle(String sortFieldDescStyle) {
        this.sortFieldDescStyle = sortFieldDescStyle;
        return this;
    }

    public ModelFormFieldBuilder setSortFieldHelpText(String sortFieldHelpText) {
        this.sortFieldHelpText = sortFieldHelpText;
        return this;
    }

    public ModelFormFieldBuilder setSortFieldStyle(String sortFieldStyle) {
        this.sortFieldStyle = sortFieldStyle;
        return this;
    }

    public ModelFormFieldBuilder setTitle(String title) {
        this.title = FlexibleStringExpander.getInstance(title);
        return this;
    }

    public ModelFormFieldBuilder setTitleAreaStyle(String titleAreaStyle) {
        this.titleAreaStyle = titleAreaStyle;
        return this;
    }

    public ModelFormFieldBuilder setTitleStyle(String titleStyle) {
        this.titleStyle = titleStyle;
        return this;
    }

    public ModelFormFieldBuilder setTooltip(String tooltip) {
        this.tooltip = FlexibleStringExpander.getInstance(tooltip);
        return this;
    }

    public ModelFormFieldBuilder setTooltipStyle(String tooltipStyle) {
        this.tooltipStyle = tooltipStyle;
        return this;
    }

    public ModelFormFieldBuilder setUseWhen(String useWhen) {
        this.useWhen = FlexibleStringExpander.getInstance(useWhen);
        return this;
    }

    public ModelFormFieldBuilder setWidgetAreaStyle(String widgetAreaStyle) {
        this.widgetAreaStyle = widgetAreaStyle;
        return this;
    }

    public ModelFormFieldBuilder setWidgetStyle(String widgetStyle) {
        this.widgetStyle = widgetStyle;
        return this;
    }
    public ModelFormFieldBuilder setParentFormName(String parentFormName) {
        this.parentFormName = parentFormName;
        return this;
    }
    public ModelFormFieldBuilder setTabindex(String tabindex) {
        this.tabindex = tabindex;
        return this;
    }
}
