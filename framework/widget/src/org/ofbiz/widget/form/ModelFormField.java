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
package org.ofbiz.widget.form;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.BshUtil;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.collections.MapStack;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.finder.EntityFinderUtil;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelField;
import org.ofbiz.entity.model.ModelReader;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ModelParam;
import org.ofbiz.service.ModelService;
import org.ofbiz.widget.WidgetWorker;
import org.ofbiz.widget.form.ModelForm.UpdateArea;
import org.w3c.dom.Element;

import bsh.EvalError;
import bsh.Interpreter;

/**
 * Widget Library - Form model class
 */
public class ModelFormField {

    public static final String module = ModelFormField.class.getName();

    protected ModelForm modelForm;

    protected String name;
    protected FlexibleMapAccessor<Map<String, ? extends Object>> mapAcsr;
    protected String entityName;
    protected String serviceName;
    protected FlexibleMapAccessor<Object> entryAcsr;
    protected String parameterName;
    protected String fieldName;
    protected String attributeName;
    protected FlexibleStringExpander title;
    protected FlexibleStringExpander tooltip;
    protected String titleAreaStyle;
    protected String widgetAreaStyle;
    protected String titleStyle;
    protected String widgetStyle;
    protected String tooltipStyle;
    protected String requiredFieldStyle;
    protected String sortFieldStyle;
    protected String sortFieldAscStyle;
    protected String sortFieldDescStyle;
    protected Integer position = null;
    protected String redWhen;
    protected FlexibleStringExpander useWhen;
    protected boolean encodeOutput = true;
    protected String event;
    protected FlexibleStringExpander action;

    protected FieldInfo fieldInfo = null;
    protected String idName;
    protected boolean separateColumn = false;
    protected Boolean requiredField = null;
    protected Boolean sortField = null;
    protected String headerLink;
    protected String headerLinkStyle;

    /** On Change Event areas to be updated. */
    protected List<UpdateArea> onChangeUpdateAreas;
    /** On Click Event areas to be updated. */
    protected List<UpdateArea> onClickUpdateAreas;

    // ===== CONSTRUCTORS =====
    /** Default Constructor */
    public ModelFormField(ModelForm modelForm) {
        this.modelForm = modelForm;
    }

    /** XML Constructor */
    public ModelFormField(Element fieldElement, ModelForm modelForm) {
        this.modelForm = modelForm;
        this.name = fieldElement.getAttribute("name");
        this.setMapName(fieldElement.getAttribute("map-name"));
        this.entityName = fieldElement.getAttribute("entity-name");
        this.serviceName = fieldElement.getAttribute("service-name");
        this.setEntryName(UtilXml.checkEmpty(fieldElement.getAttribute("entry-name"), this.name));
        this.parameterName = UtilXml.checkEmpty(fieldElement.getAttribute("parameter-name"), this.name);
        this.fieldName = UtilXml.checkEmpty(fieldElement.getAttribute("field-name"), this.name);
        this.attributeName = UtilXml.checkEmpty(fieldElement.getAttribute("attribute-name"), this.name);
        this.setTitle(fieldElement.hasAttribute("title")?fieldElement.getAttribute("title"):null);
        this.setTooltip(fieldElement.getAttribute("tooltip"));
        this.titleAreaStyle = fieldElement.getAttribute("title-area-style");
        this.widgetAreaStyle = fieldElement.getAttribute("widget-area-style");
        this.titleStyle = fieldElement.getAttribute("title-style");
        this.widgetStyle = fieldElement.getAttribute("widget-style");
        this.tooltipStyle = fieldElement.getAttribute("tooltip-style");
        this.requiredFieldStyle = fieldElement.getAttribute("required-field-style");
        this.sortFieldStyle = fieldElement.getAttribute("sort-field-style");
        this.sortFieldAscStyle = fieldElement.getAttribute("sort-field-asc-style");
        this.sortFieldDescStyle = fieldElement.getAttribute("sort-field-desc-style");
        this.redWhen = fieldElement.getAttribute("red-when");
        this.setUseWhen(fieldElement.getAttribute("use-when"));
        this.encodeOutput = !"false".equals(fieldElement.getAttribute("encode-output"));
        this.event = fieldElement.getAttribute("event");
        this.setAction(fieldElement.hasAttribute("action")? fieldElement.getAttribute("action"): null);
        this.idName = fieldElement.getAttribute("id-name");
        this.separateColumn = "true".equals(fieldElement.getAttribute("separate-column"));
        this.requiredField = fieldElement.hasAttribute("required-field") ? "true".equals(fieldElement.getAttribute("required-field")) : null;
        this.sortField = fieldElement.hasAttribute("sort-field") ? "true".equals(fieldElement.getAttribute("sort-field")) : null;
        this.headerLink = fieldElement.getAttribute("header-link");
        this.headerLinkStyle = fieldElement.getAttribute("header-link-style");


        String positionStr = fieldElement.getAttribute("position");
        try {
            if (UtilValidate.isNotEmpty(positionStr)) {
                position = Integer.valueOf(positionStr);
            }
        } catch (Exception e) {
            Debug.logError(
                e,
                "Could not convert position attribute of the field element to an integer: [" + positionStr + "], using the default of the form renderer",
                module);
        }

        // get sub-element and set fieldInfo
        List<? extends Element> subElements = UtilXml.childElementList(fieldElement);
        for (Element subElement : subElements) {
            String subElementName = subElement.getTagName();
            if (Debug.verboseOn())
                Debug.logVerbose("Processing field " + this.name + " with type info tag " + subElementName, module);

            if (UtilValidate.isEmpty(subElementName)) {
                this.fieldInfo = null;
                this.induceFieldInfo(null); //no defaultFieldType specified here, will default to edit
            } else if ("display".equals(subElementName)) {
                this.fieldInfo = new DisplayField(subElement, this);
            } else if ("display-entity".equals(subElementName)) {
                this.fieldInfo = new DisplayEntityField(subElement, this);
            } else if ("hyperlink".equals(subElementName)) {
                this.fieldInfo = new HyperlinkField(subElement, this);
            } else if ("text".equals(subElementName)) {
                this.fieldInfo = new TextField(subElement, this);
            } else if ("textarea".equals(subElementName)) {
                this.fieldInfo = new TextareaField(subElement, this);
            } else if ("date-time".equals(subElementName)) {
                this.fieldInfo = new DateTimeField(subElement, this);
            } else if ("drop-down".equals(subElementName)) {
                this.fieldInfo = new DropDownField(subElement, this);
            } else if ("check".equals(subElementName)) {
                this.fieldInfo = new CheckField(subElement, this);
            } else if ("radio".equals(subElementName)) {
                this.fieldInfo = new RadioField(subElement, this);
            } else if ("submit".equals(subElementName)) {
                this.fieldInfo = new SubmitField(subElement, this);
            } else if ("reset".equals(subElementName)) {
                this.fieldInfo = new ResetField(subElement, this);
            } else if ("hidden".equals(subElementName)) {
                this.fieldInfo = new HiddenField(subElement, this);
            } else if ("ignored".equals(subElementName)) {
                this.fieldInfo = new IgnoredField(subElement, this);
            } else if ("text-find".equals(subElementName)) {
                this.fieldInfo = new TextFindField(subElement, this);
            } else if ("date-find".equals(subElementName)) {
                this.fieldInfo = new DateFindField(subElement, this);
            } else if ("range-find".equals(subElementName)) {
                this.fieldInfo = new RangeFindField(subElement, this);
            } else if ("lookup".equals(subElementName)) {
                this.fieldInfo = new LookupField(subElement, this);
            } else if ("file".equals(subElementName)) {
                this.fieldInfo = new FileField(subElement, this);
            } else if ("password".equals(subElementName)) {
                this.fieldInfo = new PasswordField(subElement, this);
            } else if ("image".equals(subElementName)) {
                this.fieldInfo = new ImageField(subElement, this);
            } else if ("container".equals(subElementName)) {
                this.fieldInfo = new ContainerField(subElement, this);
            } else if ("on-field-event-update-area".equals(subElementName)) {
                addOnEventUpdateArea(new UpdateArea(subElement));
            } else {
                throw new IllegalArgumentException("The field sub-element with name " + subElementName + " is not supported");
            }
        }
    }

    public void addOnEventUpdateArea(UpdateArea updateArea) {
        // Event types are sorted as a convenience for the rendering classes
        Debug.logInfo(this.modelForm.getName() + ":" + this.name + " adding UpdateArea type " + updateArea.getEventType(), module);
        if ("change".equals(updateArea.getEventType())) {
            addOnChangeUpdateArea(updateArea);
        } else if ("click".equals(updateArea.getEventType())) {
            addOnClickUpdateArea(updateArea);
        }
    }

    protected void addOnChangeUpdateArea(UpdateArea updateArea) {
        if (onChangeUpdateAreas == null) {
            onChangeUpdateAreas = FastList.newInstance();
        }
        onChangeUpdateAreas.add(updateArea);
        Debug.logInfo(this.modelForm.getName() + ":" + this.name + " onChangeUpdateAreas size = " + onChangeUpdateAreas.size(), module);
    }

    protected void addOnClickUpdateArea(UpdateArea updateArea) {
        if (onClickUpdateAreas == null) {
            onClickUpdateAreas = FastList.newInstance();
        }
        onClickUpdateAreas.add(updateArea);
    }

    public void mergeOverrideModelFormField(ModelFormField overrideFormField) {
        if (overrideFormField == null)
            return;
        // incorporate updates for values that are not empty in the overrideFormField
        if (UtilValidate.isNotEmpty(overrideFormField.name))
            this.name = overrideFormField.name;
        if (overrideFormField.mapAcsr != null && !overrideFormField.mapAcsr.isEmpty()) {
            //Debug.logInfo("overriding mapAcsr, old=" + (this.mapAcsr==null?"null":this.mapAcsr.getOriginalName()) + ", new=" + overrideFormField.mapAcsr.getOriginalName(), module);
            this.mapAcsr = overrideFormField.mapAcsr;
        }
        if (UtilValidate.isNotEmpty(overrideFormField.entityName))
            this.entityName = overrideFormField.entityName;
        if (UtilValidate.isNotEmpty(overrideFormField.serviceName))
            this.serviceName = overrideFormField.serviceName;
        if (overrideFormField.entryAcsr != null && !overrideFormField.entryAcsr.isEmpty())
            this.entryAcsr = overrideFormField.entryAcsr;
        if (UtilValidate.isNotEmpty(overrideFormField.parameterName))
            this.parameterName = overrideFormField.parameterName;
        if (UtilValidate.isNotEmpty(overrideFormField.fieldName))
            this.fieldName = overrideFormField.fieldName;
        if (UtilValidate.isNotEmpty(overrideFormField.attributeName))
            this.attributeName = overrideFormField.attributeName;
        if (overrideFormField.title != null && !overrideFormField.title.isEmpty()) // title="" can be used to override the original value
            this.title = overrideFormField.title;
        if (overrideFormField.tooltip != null && !overrideFormField.tooltip.isEmpty())
            this.tooltip = overrideFormField.tooltip;
        if (overrideFormField.requiredField != null)
            this.requiredField = overrideFormField.requiredField;
        if (overrideFormField.sortField != null)
            this.sortField = overrideFormField.sortField;
        if (UtilValidate.isNotEmpty(overrideFormField.titleAreaStyle))
            this.titleAreaStyle = overrideFormField.titleAreaStyle;
        if (UtilValidate.isNotEmpty(overrideFormField.widgetAreaStyle))
            this.widgetAreaStyle = overrideFormField.widgetAreaStyle;
        if (UtilValidate.isNotEmpty(overrideFormField.titleStyle))
            this.titleStyle = overrideFormField.titleStyle;
        if (UtilValidate.isNotEmpty(overrideFormField.widgetStyle))
            this.widgetStyle = overrideFormField.widgetStyle;
        if (overrideFormField.position != null)
            this.position = overrideFormField.position;
        if (UtilValidate.isNotEmpty(overrideFormField.redWhen))
            this.redWhen = overrideFormField.redWhen;
        if (UtilValidate.isNotEmpty(overrideFormField.event))
            this.event = overrideFormField.event;
        if (overrideFormField.action != null && !overrideFormField.action.isEmpty())
            this.action = overrideFormField.action;
        if (overrideFormField.useWhen != null && !overrideFormField.useWhen.isEmpty())
            this.useWhen = overrideFormField.useWhen;
        if (overrideFormField.fieldInfo != null) {
            this.setFieldInfo(overrideFormField.fieldInfo);
        }
        if (overrideFormField.fieldInfo != null) {
            this.setHeaderLink(overrideFormField.headerLink);
        }
        if (UtilValidate.isNotEmpty(overrideFormField.idName)) {
            this.idName = overrideFormField.idName;
        }
        if (overrideFormField.onChangeUpdateAreas != null) {
            this.onChangeUpdateAreas = overrideFormField.onChangeUpdateAreas;
        }
        if (overrideFormField.onClickUpdateAreas != null) {
            this.onClickUpdateAreas = overrideFormField.onClickUpdateAreas;
        }
        this.encodeOutput = overrideFormField.encodeOutput;
    }

    public boolean induceFieldInfo(String defaultFieldType) {
        if (this.induceFieldInfoFromEntityField(defaultFieldType)) {
            return true;
        }
        if (this.induceFieldInfoFromServiceParam(defaultFieldType)) {
            return true;
        }
        return false;
    }

    public boolean induceFieldInfoFromServiceParam(String defaultFieldType) {
        if (UtilValidate.isEmpty(this.getServiceName()) || UtilValidate.isEmpty(this.getAttributeName())) {
            return false;
        }
        DispatchContext dispatchContext = this.getModelForm().dispatchContext;
        try {
            ModelService modelService = dispatchContext.getModelService(this.getServiceName());
            if (modelService != null) {
                ModelParam modelParam = modelService.getParam(this.getAttributeName());
                if (modelParam != null) {
                    if (UtilValidate.isNotEmpty(modelParam.entityName) && UtilValidate.isNotEmpty(modelParam.fieldName)) {
                        this.entityName = modelParam.entityName;
                        this.fieldName = modelParam.fieldName;
                        if (this.induceFieldInfoFromEntityField(defaultFieldType)) {
                            return true;
                        }
                    }

                    this.induceFieldInfoFromServiceParam(modelService, modelParam, defaultFieldType);
                    return true;
                }
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, "error getting service parameter definition for auto-field with serviceName: " + this.getServiceName() + ", and attributeName: " + this.getAttributeName(), module);
        }
        return false;
    }

    public boolean induceFieldInfoFromServiceParam(ModelService modelService, ModelParam modelParam, String defaultFieldType) {
        if (modelService == null || modelParam == null) {
            return false;
        }

        this.serviceName = modelService.name;
        this.attributeName = modelParam.name;

        if ("find".equals(defaultFieldType)) {
            if (modelParam.type.indexOf("Double") != -1
                || modelParam.type.indexOf("Float") != -1
                || modelParam.type.indexOf("Long") != -1
                || modelParam.type.indexOf("Integer") != -1) {
                ModelFormField.RangeFindField textField = new ModelFormField.RangeFindField(ModelFormField.FieldInfo.SOURCE_AUTO_SERVICE, this);
                textField.setSize(6);
                this.setFieldInfo(textField);
            } else if (modelParam.type.indexOf("Timestamp") != -1) {
                ModelFormField.DateFindField dateTimeField = new ModelFormField.DateFindField(ModelFormField.FieldInfo.SOURCE_AUTO_SERVICE, this);
                dateTimeField.setType("timestamp");
                this.setFieldInfo(dateTimeField);
            } else if (modelParam.type.indexOf("Date") != -1) {
                ModelFormField.DateFindField dateTimeField = new ModelFormField.DateFindField(ModelFormField.FieldInfo.SOURCE_AUTO_SERVICE, this);
                dateTimeField.setType("date");
                this.setFieldInfo(dateTimeField);
            } else if (modelParam.type.indexOf("Time") != -1) {
                ModelFormField.DateFindField dateTimeField = new ModelFormField.DateFindField(ModelFormField.FieldInfo.SOURCE_AUTO_SERVICE, this);
                dateTimeField.setType("time");
                this.setFieldInfo(dateTimeField);
            } else {
                ModelFormField.TextFindField textField = new ModelFormField.TextFindField(ModelFormField.FieldInfo.SOURCE_AUTO_SERVICE, this);
                this.setFieldInfo(textField);
            }
        } else if ("display".equals(defaultFieldType)) {
            ModelFormField.DisplayField displayField = new ModelFormField.DisplayField(ModelFormField.FieldInfo.SOURCE_AUTO_SERVICE, this);
            this.setFieldInfo(displayField);
        } else {
            // default to "edit"
            if (modelParam.type.indexOf("Double") != -1
                || modelParam.type.indexOf("Float") != -1
                || modelParam.type.indexOf("Long") != -1
                || modelParam.type.indexOf("Integer") != -1) {
                ModelFormField.TextField textField = new ModelFormField.TextField(ModelFormField.FieldInfo.SOURCE_AUTO_SERVICE, this);
                textField.setSize(6);
                this.setFieldInfo(textField);
            } else if (modelParam.type.indexOf("Timestamp") != -1) {
                ModelFormField.DateTimeField dateTimeField = new ModelFormField.DateTimeField(ModelFormField.FieldInfo.SOURCE_AUTO_SERVICE, this);
                dateTimeField.setType("timestamp");
                this.setFieldInfo(dateTimeField);
            } else if (modelParam.type.indexOf("Date") != -1) {
                ModelFormField.DateTimeField dateTimeField = new ModelFormField.DateTimeField(ModelFormField.FieldInfo.SOURCE_AUTO_SERVICE, this);
                dateTimeField.setType("date");
                this.setFieldInfo(dateTimeField);
            } else if (modelParam.type.indexOf("Time") != -1) {
                ModelFormField.DateTimeField dateTimeField = new ModelFormField.DateTimeField(ModelFormField.FieldInfo.SOURCE_AUTO_SERVICE, this);
                dateTimeField.setType("time");
                this.setFieldInfo(dateTimeField);
            } else {
                ModelFormField.TextField textField = new ModelFormField.TextField(ModelFormField.FieldInfo.SOURCE_AUTO_SERVICE, this);
                this.setFieldInfo(textField);
            }
        }

        return true;
    }

    public boolean induceFieldInfoFromEntityField(String defaultFieldType) {
        if (UtilValidate.isEmpty(this.getEntityName()) || UtilValidate.isEmpty(this.getFieldName())) {
            return false;
        }
        ModelReader entityModelReader = this.getModelForm().entityModelReader;
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

    public boolean induceFieldInfoFromEntityField(ModelEntity modelEntity, ModelField modelField, String defaultFieldType) {
        if (modelEntity == null || modelField == null) {
            return false;
        }

        this.entityName = modelEntity.getEntityName();
        this.fieldName = modelField.getName();

        if ("find".equals(defaultFieldType)) {
            if ("id".equals(modelField.getType()) || "id-ne".equals(modelField.getType())) {
                ModelFormField.TextFindField textField = new ModelFormField.TextFindField(ModelFormField.FieldInfo.SOURCE_AUTO_ENTITY, this);
                textField.setSize(20);
                textField.setMaxlength(Integer.valueOf(20));
                this.setFieldInfo(textField);
            } else if ("id-long".equals(modelField.getType()) || "id-long-ne".equals(modelField.getType())) {
                ModelFormField.TextFindField textField = new ModelFormField.TextFindField(ModelFormField.FieldInfo.SOURCE_AUTO_ENTITY, this);
                textField.setSize(40);
                textField.setMaxlength(Integer.valueOf(60));
                this.setFieldInfo(textField);
            } else if ("id-vlong".equals(modelField.getType()) || "id-vlong-ne".equals(modelField.getType())) {
                ModelFormField.TextFindField textField = new ModelFormField.TextFindField(ModelFormField.FieldInfo.SOURCE_AUTO_ENTITY, this);
                textField.setSize(60);
                textField.setMaxlength(Integer.valueOf(250));
                this.setFieldInfo(textField);
            } else if ("very-short".equals(modelField.getType())) {
                ModelFormField.TextField textField = new ModelFormField.TextField(ModelFormField.FieldInfo.SOURCE_AUTO_ENTITY, this);
                textField.setSize(6);
                textField.setMaxlength(Integer.valueOf(10));
                this.setFieldInfo(textField);
            } else if ("name".equals(modelField.getType()) || "short-varchar".equals(modelField.getType())) {
                ModelFormField.TextFindField textField = new ModelFormField.TextFindField(ModelFormField.FieldInfo.SOURCE_AUTO_ENTITY, this);
                textField.setSize(40);
                textField.setMaxlength(Integer.valueOf(60));
                this.setFieldInfo(textField);
            } else if (
                "value".equals(modelField.getType())
                    || "comment".equals(modelField.getType())
                    || "description".equals(modelField.getType())
                    || "long-varchar".equals(modelField.getType())
                    || "url".equals(modelField.getType())
                    || "email".equals(modelField.getType())) {
                ModelFormField.TextFindField textField = new ModelFormField.TextFindField(ModelFormField.FieldInfo.SOURCE_AUTO_ENTITY, this);
                textField.setSize(60);
                textField.setMaxlength(Integer.valueOf(250));
                this.setFieldInfo(textField);
            } else if (
                "floating-point".equals(modelField.getType()) || "currency-amount".equals(modelField.getType()) || "numeric".equals(modelField.getType())) {
                ModelFormField.RangeFindField textField = new ModelFormField.RangeFindField(ModelFormField.FieldInfo.SOURCE_AUTO_ENTITY, this);
                textField.setSize(6);
                this.setFieldInfo(textField);
            } else if ("date-time".equals(modelField.getType()) || "date".equals(modelField.getType()) || "time".equals(modelField.getType())) {
                ModelFormField.DateFindField dateTimeField = new ModelFormField.DateFindField(ModelFormField.FieldInfo.SOURCE_AUTO_ENTITY, this);
                if ("date-time".equals(modelField.getType())) {
                    dateTimeField.setType("timestamp");
                } else if ("date".equals(modelField.getType())) {
                    dateTimeField.setType("date");
                } else if ("time".equals(modelField.getType())) {
                    dateTimeField.setType("time");
                }
                this.setFieldInfo(dateTimeField);
            } else {
                ModelFormField.TextFindField textField = new ModelFormField.TextFindField(ModelFormField.FieldInfo.SOURCE_AUTO_ENTITY, this);
                this.setFieldInfo(textField);
            }
        } else if ("display".equals(defaultFieldType)) {
            ModelFormField.DisplayField displayField = new ModelFormField.DisplayField(ModelFormField.FieldInfo.SOURCE_AUTO_SERVICE, this);
            this.setFieldInfo(displayField);
        } else if ("hidden".equals(defaultFieldType)) {
            ModelFormField.HiddenField hiddenField = new ModelFormField.HiddenField(ModelFormField.FieldInfo.SOURCE_AUTO_SERVICE, this);
            this.setFieldInfo(hiddenField);
        } else {
            if ("id".equals(modelField.getType()) || "id-ne".equals(modelField.getType())) {
                ModelFormField.TextField textField = new ModelFormField.TextField(ModelFormField.FieldInfo.SOURCE_AUTO_ENTITY, this);
                textField.setSize(20);
                textField.setMaxlength(Integer.valueOf(20));
                this.setFieldInfo(textField);
            } else if ("id-long".equals(modelField.getType()) || "id-long-ne".equals(modelField.getType())) {
                ModelFormField.TextField textField = new ModelFormField.TextField(ModelFormField.FieldInfo.SOURCE_AUTO_ENTITY, this);
                textField.setSize(40);
                textField.setMaxlength(Integer.valueOf(60));
                this.setFieldInfo(textField);
            } else if ("id-vlong".equals(modelField.getType()) || "id-vlong-ne".equals(modelField.getType())) {
                ModelFormField.TextField textField = new ModelFormField.TextField(ModelFormField.FieldInfo.SOURCE_AUTO_ENTITY, this);
                textField.setSize(60);
                textField.setMaxlength(Integer.valueOf(250));
                this.setFieldInfo(textField);
            } else if ("indicator".equals(modelField.getType())) {
                ModelFormField.DropDownField dropDownField = new ModelFormField.DropDownField(ModelFormField.FieldInfo.SOURCE_AUTO_ENTITY, this);
                dropDownField.setAllowEmpty(false);
                dropDownField.addOptionSource(new ModelFormField.SingleOption("Y", null, dropDownField));
                dropDownField.addOptionSource(new ModelFormField.SingleOption("N", null, dropDownField));
                this.setFieldInfo(dropDownField);
                //ModelFormField.TextField textField = new ModelFormField.TextField(ModelFormField.FieldInfo.SOURCE_AUTO_ENTITY, this);
                //textField.setSize(1);
                //textField.setMaxlength(Integer.valueOf(1));
                //this.setFieldInfo(textField);
            } else if ("very-short".equals(modelField.getType())) {
                ModelFormField.TextField textField = new ModelFormField.TextField(ModelFormField.FieldInfo.SOURCE_AUTO_ENTITY, this);
                textField.setSize(6);
                textField.setMaxlength(Integer.valueOf(10));
                this.setFieldInfo(textField);
            } else if ("very-long".equals(modelField.getType())) {
                ModelFormField.TextareaField textareaField = new ModelFormField.TextareaField(ModelFormField.FieldInfo.SOURCE_AUTO_ENTITY, this);
                textareaField.setCols(60);
                textareaField.setRows(2);
                this.setFieldInfo(textareaField);
            } else if ("name".equals(modelField.getType()) || "short-varchar".equals(modelField.getType())) {
                ModelFormField.TextField textField = new ModelFormField.TextField(ModelFormField.FieldInfo.SOURCE_AUTO_ENTITY, this);
                textField.setSize(40);
                textField.setMaxlength(Integer.valueOf(60));
                this.setFieldInfo(textField);
            } else if (
                "value".equals(modelField.getType())
                    || "comment".equals(modelField.getType())
                    || "description".equals(modelField.getType())
                    || "long-varchar".equals(modelField.getType())
                    || "url".equals(modelField.getType())
                    || "email".equals(modelField.getType())) {
                ModelFormField.TextField textField = new ModelFormField.TextField(ModelFormField.FieldInfo.SOURCE_AUTO_ENTITY, this);
                textField.setSize(60);
                textField.setMaxlength(Integer.valueOf(250));
                this.setFieldInfo(textField);
            } else if (
                "floating-point".equals(modelField.getType()) || "currency-amount".equals(modelField.getType()) || "numeric".equals(modelField.getType())) {
                ModelFormField.TextField textField = new ModelFormField.TextField(ModelFormField.FieldInfo.SOURCE_AUTO_ENTITY, this);
                textField.setSize(6);
                this.setFieldInfo(textField);
            } else if ("date-time".equals(modelField.getType()) || "date".equals(modelField.getType()) || "time".equals(modelField.getType())) {
                ModelFormField.DateTimeField dateTimeField = new ModelFormField.DateTimeField(ModelFormField.FieldInfo.SOURCE_AUTO_ENTITY, this);
                if ("date-time".equals(modelField.getType())) {
                    dateTimeField.setType("timestamp");
                } else if ("date".equals(modelField.getType())) {
                    dateTimeField.setType("date");
                } else if ("time".equals(modelField.getType())) {
                    dateTimeField.setType("time");
                }
                this.setFieldInfo(dateTimeField);
            } else {
                ModelFormField.TextField textField = new ModelFormField.TextField(ModelFormField.FieldInfo.SOURCE_AUTO_ENTITY, this);
                this.setFieldInfo(textField);
            }
        }

        return true;
    }

    public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer) throws IOException {
        this.fieldInfo.renderFieldString(writer, context, formStringRenderer);
    }

    public List<UpdateArea> getOnChangeUpdateAreas() {
        return onChangeUpdateAreas;
    }

    public List<UpdateArea> getOnClickUpdateAreas() {
        return onClickUpdateAreas;
    }

    public FieldInfo getFieldInfo() {
        return fieldInfo;
    }

    public ModelForm getModelForm() {
        return modelForm;
    }

    /**
     * @param fieldInfo
     */
    public void setFieldInfo(FieldInfo fieldInfo) {
        if (fieldInfo == null)
            return;

        // field info is a little different, check source for priority
        if (this.fieldInfo == null || (fieldInfo.getFieldSource() <= this.fieldInfo.getFieldSource())) {
            this.fieldInfo = fieldInfo;
            this.fieldInfo.modelFormField = this;
        }
    }

    /**
     * Gets the name of the Service Attribute (aka Parameter) that corresponds
     * with this field. This can be used to get additional information about the field.
     * Use the getServiceName() method to get the Entity name that the field is in.
     *
     * @return
     */
    public String getAttributeName() {
        if (UtilValidate.isNotEmpty(this.attributeName)) {
            return this.attributeName;
        } else {
            return this.name;
        }
    }

    public String getEntityName() {
        if (UtilValidate.isNotEmpty(this.entityName)) {
            return this.entityName;
        } else {
            return this.modelForm.getDefaultEntityName();
        }
    }

    public String getEntryName() {
        if (this.entryAcsr != null && !this.entryAcsr.isEmpty()) {
            return this.entryAcsr.getOriginalName();
        } else {
            return this.name;
        }
    }

    /**
     * Gets the entry from the context that corresponds to this field; if this
     * form is being rendered in an error condition (ie isError in the context
     * is true) then the value will be retreived from the parameters Map in
     * the context.
     *
     * @param context
     * @param encoder
     * @return
     */
    public String getEntry(Map<String, ? extends Object> context) {
        return this.getEntry(context, "");
    }

    public String getEntry(Map<String, ? extends Object> context , String defaultValue) {
        Boolean isError = (Boolean) context.get("isError");
        Boolean useRequestParameters = (Boolean) context.get("useRequestParameters");

        Locale locale = (Locale) context.get("locale");
        if (locale == null) locale = Locale.getDefault();
        TimeZone timeZone = (TimeZone) context.get("timeZone");
        if (timeZone == null) timeZone = TimeZone.getDefault();

        String returnValue;

        // if useRequestParameters is TRUE then parameters will always be used, if FALSE then parameters will never be used
        // if isError is TRUE and useRequestParameters is not FALSE (ie is null or TRUE) then parameters will be used
        if ((Boolean.TRUE.equals(isError) && !Boolean.FALSE.equals(useRequestParameters)) || (Boolean.TRUE.equals(useRequestParameters))) {
            //Debug.logInfo("Getting entry, isError true so getting from parameters for field " + this.getName() + " of form " + this.modelForm.getName(), module);
            Map<String, Object> parameters = UtilGenerics.checkMap(context.get("parameters"), String.class, Object.class);
            String parameterName = this.getParameterName(context);
            if (parameters != null && parameters.get(parameterName) != null) {
                Object parameterValue = parameters.get(parameterName);
                if (parameterValue instanceof String) {
                    returnValue = (String) parameterValue;
                } else {
                    // we might want to do something else here in the future, but for now this is probably best
                    Debug.logWarning("Found a non-String parameter value for field [" + this.getModelForm().getName() + "." + this.getFieldName() + "]", module);
                    returnValue = defaultValue;
                }
            } else {
                returnValue = defaultValue;
            }
        } else {
            //Debug.logInfo("Getting entry, isError false so getting from Map in context for field " + this.getName() + " of form " + this.modelForm.getName(), module);
            Map<String, ? extends Object> dataMap = this.getMap(context);
            boolean dataMapIsContext = false;
            if (dataMap == null) {
                //Debug.logInfo("Getting entry, no Map found with name " + this.getMapName() + ", using context for field " + this.getName() + " of form " + this.modelForm.getName(), module);
                dataMap = context;
                dataMapIsContext = true;
            }
            Object retVal = null;
            if (this.entryAcsr != null && !this.entryAcsr.isEmpty()) {
                //Debug.logInfo("Getting entry, using entryAcsr for field " + this.getName() + " of form " + this.modelForm.getName(), module);
                if (dataMap instanceof GenericEntity) {
                    GenericEntity genEnt = (GenericEntity) dataMap;
                    if (genEnt.getModelEntity().isField(this.entryAcsr.getOriginalName())) {
                        retVal = genEnt.get(this.entryAcsr.getOriginalName(), locale);
                    } else {
                        //TODO: this may never come up, but if necessary use the FlexibleStringExander to eval the name first: String evaled = this.entryAcsr
                    }
                } else {
                    retVal = this.entryAcsr.get(dataMap, locale);
                }
            } else {
                //Debug.logInfo("Getting entry, no entryAcsr so using field name " + this.name + " for field " + this.getName() + " of form " + this.modelForm.getName(), module);
                // if no entry name was specified, use the field's name
                retVal = dataMap.get(this.name);
            }

            // this is a special case to fill in fields during a create by default from parameters passed in
            if (dataMapIsContext && retVal == null && !Boolean.FALSE.equals(useRequestParameters)) {
                Map<String, ? extends Object> parameters = UtilGenerics.checkMap(context.get("parameters"));
                if (parameters != null) {
                    if (this.entryAcsr != null && !this.entryAcsr.isEmpty()) {
                        retVal = this.entryAcsr.get(parameters);
                    } else {
                        retVal = parameters.get(this.name);
                    }
                }
            }

            if (retVal != null) {
                // format string based on the user's locale and time zone
                if (retVal instanceof Double || retVal instanceof Float || retVal instanceof BigDecimal) {
                    NumberFormat nf = NumberFormat.getInstance(locale);
                    nf.setMaximumFractionDigits(10);
                    return nf.format(retVal);
                } else if (retVal instanceof java.sql.Date) {
                    DateFormat df = UtilDateTime.toDateFormat(UtilDateTime.DATE_FORMAT, timeZone, null);
                    return df.format((java.util.Date) retVal);
                } else if (retVal instanceof java.sql.Time) {
                    DateFormat df = UtilDateTime.toTimeFormat(UtilDateTime.TIME_FORMAT, timeZone, null);
                    return df.format((java.util.Date) retVal);
                } else if (retVal instanceof java.sql.Timestamp) {
                    DateFormat df = UtilDateTime.toDateTimeFormat(UtilDateTime.DATE_TIME_FORMAT, timeZone, null);
                    return df.format((java.util.Date) retVal);
                } else if (retVal instanceof java.util.Date) {
                    DateFormat df = UtilDateTime.toDateTimeFormat("EEE MMM dd hh:mm:ss z yyyy", timeZone, null);
                    return df.format((java.util.Date) retVal);
                } else {
                    returnValue = retVal.toString();
                }
            } else {
                returnValue = defaultValue;
            }
        }

        if (this.getEncodeOutput() && returnValue != null) {
            StringUtil.SimpleEncoder simpleEncoder = (StringUtil.SimpleEncoder) context.get("simpleEncoder");
            if (simpleEncoder != null) {
                returnValue = simpleEncoder.encode(returnValue);
            }
        }
        return returnValue;
    }

    public Map<String, ? extends Object> getMap(Map<String, ? extends Object> context) {
        if (this.mapAcsr == null || this.mapAcsr.isEmpty()) {
            //Debug.logInfo("Getting Map from default of the form because of no mapAcsr for field " + this.getName(), module);
            return this.modelForm.getDefaultMap(context);
        } else {
            //Debug.logInfo("Getting Map from mapAcsr for field " + this.getName(), module);
            return mapAcsr.get(context);
        }
    }

    /**
     * Gets the name of the Entity Field that corresponds
     * with this field. This can be used to get additional information about the field.
     * Use the getEntityName() method to get the Entity name that the field is in.
     *
     * @return
     */
    public String getFieldName() {
        if (UtilValidate.isNotEmpty(this.fieldName)) {
            return this.fieldName;
        } else {
            return this.name;
        }
    }

    /** Get the name of the Map in the form context that contains the entry,
     * available from the getEntryName() method. This entry is used to
     * pre-populate the field widget when not in an error condition. In an
     * error condition the parameter name is used to get the value from the
     * parameters Map.
     *
     * @return
     */
    public String getMapName() {
        if (this.mapAcsr != null && !this.mapAcsr.isEmpty()) {
            return this.mapAcsr.getOriginalName();
        } else {
            return this.modelForm.getDefaultMapName();
        }
    }

    public String getName() {
        return name;
    }

    /**
     * Get the name to use for the parameter for this field in the form interpreter.
     * For HTML forms this is the request parameter name.
     *
     * @return
     */
    public String getParameterName(Map<String, ? extends Object> context) {
        String baseName;
        if (UtilValidate.isNotEmpty(this.parameterName)) {
            baseName = this.parameterName;
        } else {
            baseName = this.name;
        }

        Integer itemIndex = (Integer) context.get("itemIndex");
        if (itemIndex != null && "multi".equals(this.modelForm.getType())) {
            return baseName + this.modelForm.getItemIndexSeparator() + itemIndex.intValue();
        } else {
            return baseName;
        }
    }

    public int getPosition() {
        if (this.position == null) {
            return 1;
        } else {
            return position.intValue();
        }
    }

    public String getRedWhen() {
        return redWhen;
    }


    public String getEvent() {
        return event;
    }

    public String getAction(Map<String, ? extends Object> context) {
        if (this.action != null && !this.action.isEmpty()) {
            return action.expandString(context);
        } else {
            return null;
        }
    }

/**
     * the widget/interaction part will be red if the date value is
     *  before-now (for ex. thruDate), after-now (for ex. fromDate), or by-name (if the
     *  field's name or entry-name or fromDate or thruDate the corresponding
     *  action will be done); only applicable when the field is a timestamp
     *
     * @param context
     * @return
     */
    public boolean shouldBeRed(Map<String, Object> context) {
        // red-when (never | before-now | after-now | by-name) "by-name"

        String redCondition = this.redWhen;

        if ("never".equals(redCondition)) {
            return false;
        }

        // for performance resaons we check this first, most fields will be eliminated here and the valueOfs will not be necessary
        if (UtilValidate.isEmpty(redCondition) || "by-name".equals(redCondition)) {
            if ("fromDate".equals(this.name) || (this.entryAcsr != null && "fromDate".equals(this.entryAcsr.getOriginalName()))) {
                redCondition = "after-now";
            } else if ("thruDate".equals(this.name) || (this.entryAcsr != null && "thruDate".equals(this.entryAcsr.getOriginalName()))) {
                redCondition = "before-now";
            } else {
                return false;
            }
        }

        boolean isBeforeNow = false;
        if ("before-now".equals(redCondition)) {
            isBeforeNow = true;
        } else if ("after-now".equals(redCondition)) {
            isBeforeNow = false;
        } else {
            return false;
        }

        java.sql.Date dateVal = null;
        java.sql.Time timeVal = null;
        java.sql.Timestamp timestampVal = null;

        //now before going on, check to see if the current entry is a valid date and/or time and get the value
        String value = this.getEntry(context, null);
        try {
            timestampVal = java.sql.Timestamp.valueOf(value);
        } catch (Exception e) {
            // okay, not a timestamp...
        }

        if (timestampVal == null) {
            try {
                dateVal = java.sql.Date.valueOf(value);
            } catch (Exception e) {
                // okay, not a date...
            }
        }

        if (timestampVal == null && dateVal == null) {
            try {
                timeVal = java.sql.Time.valueOf(value);
            } catch (Exception e) {
                // okay, not a time...
            }
        }

        if (timestampVal == null && dateVal == null && timeVal == null) {
            return false;
        }

        long nowMillis = System.currentTimeMillis();
        if (timestampVal != null) {
            java.sql.Timestamp nowStamp = new java.sql.Timestamp(nowMillis);
            if (!timestampVal.equals(nowStamp)) {
                if (isBeforeNow) {
                    if (timestampVal.before(nowStamp)) {
                        return true;
                    }
                } else {
                    if (timestampVal.after(nowStamp)) {
                        return true;
                    }
                }
            }
        } else if (dateVal != null) {
            java.sql.Date nowDate = new java.sql.Date(nowMillis);
            if (!dateVal.equals(nowDate)) {
                if (isBeforeNow) {
                    if (dateVal.before(nowDate)) {
                        return true;
                    }
                } else {
                    if (dateVal.after(nowDate)) {
                        return true;
                    }
                }
            }
        } else if (timeVal != null) {
            java.sql.Time nowTime = new java.sql.Time(nowMillis);
            if (!timeVal.equals(nowTime)) {
                if (isBeforeNow) {
                    if (timeVal.before(nowTime)) {
                        return true;
                    }
                } else {
                    if (timeVal.after(nowTime)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public String getServiceName() {
        if (UtilValidate.isNotEmpty(this.serviceName)) {
            return this.serviceName;
        } else {
            return this.modelForm.getDefaultServiceName();
        }
    }

    public String getTitle(Map<String, Object> context) {
        if (this.title != null && !this.title.isEmpty()) {
            return title.expandString(context);
        } else {
            // create a title from the name of this field; expecting a Java method/field style name, ie productName or productCategoryId
            if (UtilValidate.isEmpty(this.name)) {
                // this should never happen, ie name is required
                return "";
            }

            // search for a localized label for the field's name
            Map<String, String> uiLabelMap = UtilGenerics.checkMap(context.get("uiLabelMap"));
            if (uiLabelMap != null) {
                String titleFieldName = "FormFieldTitle_" + this.name;
                String localizedName = (String) uiLabelMap.get(titleFieldName);
                if (!localizedName.equals(titleFieldName)) {
                    return localizedName;
                }
            } else {
                Debug.logWarning("Could not find uiLabelMap in context while rendering form " + this.modelForm.getName(), module);
            }

            // create a title from the name of this field; expecting a Java method/field style name, ie productName or productCategoryId
            StringBuilder autoTitlewriter = new StringBuilder();

            // always use upper case first letter...
            autoTitlewriter.append(Character.toUpperCase(this.name.charAt(0)));

            // just put spaces before the upper case letters
            for (int i = 1; i < this.name.length(); i++) {
                char curChar = this.name.charAt(i);
                if (Character.isUpperCase(curChar)) {
                    autoTitlewriter.append(' ');
                }
                autoTitlewriter.append(curChar);
            }

            return autoTitlewriter.toString();
        }
    }

    public String getTitleAreaStyle() {
        if (UtilValidate.isNotEmpty(this.titleAreaStyle)) {
            return this.titleAreaStyle;
        } else {
            return this.modelForm.getDefaultTitleAreaStyle();
        }
    }

    public String getTitleStyle() {
        if (UtilValidate.isNotEmpty(this.titleStyle)) {
            return this.titleStyle;
        } else {
            return this.modelForm.getDefaultTitleStyle();
        }
    }

    public String getRequiredFieldStyle() {
        if (UtilValidate.isNotEmpty(this.requiredFieldStyle)) {
            return this.requiredFieldStyle;
        } else {
            return this.modelForm.getDefaultRequiredFieldStyle();
        }
    }

    public String getSortFieldStyle() {
        if (UtilValidate.isNotEmpty(this.sortFieldStyle)) {
            return this.sortFieldStyle;
        }
        return this.modelForm.getDefaultSortFieldStyle();
    }

    public String getSortFieldStyleAsc() {
        if (UtilValidate.isNotEmpty(this.sortFieldAscStyle)) {
            return this.sortFieldAscStyle;
        }
        return this.modelForm.getDefaultSortFieldAscStyle();
    }

    public String getSortFieldStyleDesc() {
        if (UtilValidate.isNotEmpty(this.sortFieldDescStyle)) {
            return this.sortFieldDescStyle;
        }
        return this.modelForm.getDefaultSortFieldDescStyle();
    }

    public String getTooltip(Map<String, Object> context) {
        if (tooltip != null && !tooltip.isEmpty()) {
            return tooltip.expandString(context);
        } else {
            return "";
        }
    }

    public String getUseWhen(Map<String, Object> context) {
        if (this.useWhen != null && !this.useWhen.isEmpty()) {
            return this.useWhen.expandString(context);
        } else {
            return "";
        }
    }

    public boolean getEncodeOutput() {
        return this.encodeOutput;
    }

    public String getIdName() {
        if (UtilValidate.isNotEmpty(idName)) {
            return idName;
        } else {
            return this.modelForm.getName() + "_" + this.getFieldName();
        }
    }

    public String getHeaderLink() {
        return headerLink;
    }

    public String getHeaderLinkStyle() {
        return headerLinkStyle;
    }


    /**
     * @param string
     */
    public void setIdName(String string) {
        idName = string;
    }


    public boolean isUseWhenEmpty() {
        if (this.useWhen == null) {
            return true;
        }

        return this.useWhen.isEmpty();
    }

    public boolean shouldUse(Map<String, Object> context) {
        String useWhenStr = this.getUseWhen(context);
        if (UtilValidate.isEmpty(useWhenStr)) {
            return true;
        } else {
            try {
                Interpreter bsh = this.modelForm.getBshInterpreter(context);
                Object retVal = bsh.eval(StringUtil.convertOperatorSubstitutions(useWhenStr));
                boolean condTrue = false;
                // retVal should be a Boolean, if not something weird is up...
                if (retVal instanceof Boolean) {
                    Boolean boolVal = (Boolean) retVal;
                    condTrue = boolVal.booleanValue();
                } else {
                    throw new IllegalArgumentException("Return value from use-when condition eval was not a Boolean: "
                            + retVal.getClass().getName() + " [" + retVal + "] on the field " + this.name + " of form " + this.modelForm.getName());
                }

                return condTrue;
            } catch (EvalError e) {
                String errMsg = "Error evaluating BeanShell use-when condition [" + useWhenStr + "] on the field "
                        + this.name + " of form " + this.modelForm.getName() + ": " + e.toString();
                Debug.logError(e, errMsg, module);
                //Debug.logError("For use-when eval error context is: " + context, module);
                throw new IllegalArgumentException(errMsg);
            }
        }
    }

    /**
     * Checks if field is a row submit field.
     */
    public boolean isRowSubmit() {
        if (!"multi".equals(getModelForm().getType())) return false;
        if (getFieldInfo().getFieldType() != ModelFormField.FieldInfo.CHECK) return false;
        if (!CheckField.ROW_SUBMIT_FIELD_NAME.equals(getName())) return false;
        return true;
    }

    public String getWidgetAreaStyle() {
        if (UtilValidate.isNotEmpty(this.widgetAreaStyle)) {
            return this.widgetAreaStyle;
        } else {
            return this.modelForm.getDefaultWidgetAreaStyle();
        }
    }

    public String getWidgetStyle() {
        if (UtilValidate.isNotEmpty(this.widgetStyle)) {
            return this.widgetStyle;
        } else {
            return this.modelForm.getDefaultWidgetStyle();
        }
    }

    public String getTooltipStyle() {
        if (UtilValidate.isNotEmpty(this.tooltipStyle)) {
            return this.tooltipStyle;
        } else {
            return this.modelForm.getDefaultTooltipStyle();
        }
    }

    /**
     * @param string
     */
    public void setAttributeName(String string) {
        attributeName = string;
    }

    /**
     * @param string
     */
    public void setEntityName(String string) {
        entityName = string;
    }

    /**
     * @param string
     */
    public void setEntryName(String string) {
        entryAcsr = FlexibleMapAccessor.getInstance(string);
    }

    /**
     * @param string
     */
    public void setFieldName(String string) {
        fieldName = string;
    }

    /**
     * @param string
     */
    public void setMapName(String string) {
        this.mapAcsr = FlexibleMapAccessor.getInstance(string);
    }

    /**
     * @param string
     */
    public void setName(String string) {
        name = string;
    }

    /**
     * @param string
     */
    public void setParameterName(String string) {
        parameterName = string;
    }

    /**
     * @param i
     */
    public void setPosition(int i) {
        position = Integer.valueOf(i);
    }

    /**
     * @param string
     */
    public void setRedWhen(String string) {
        redWhen = string;
    }


    /**
     * @param string
     */
    public void setEvent(String string) {
        event = string;
    }

    /**
     * @param string
     */
    public void setAction(String string) {
        this.action = FlexibleStringExpander.getInstance(string);
    }

    /**
     * @param string
     */
    public void setServiceName(String string) {
        serviceName = string;
    }

    /**
     * @param string
     */
    public void setTitle(String string) {
        this.title = FlexibleStringExpander.getInstance(string);
    }

    /**
     * @param string
     */
    public void setTitleAreaStyle(String string) {
        this.titleAreaStyle = string;
    }

    /**
     * @param string
     */
    public void setTitleStyle(String string) {
        this.titleStyle = string;
    }

    /**
     * @param string
     */
    public void setTooltip(String string) {
        this.tooltip = FlexibleStringExpander.getInstance(string);
    }

    /**
     * @param string
     */
    public void setUseWhen(String string) {
        this.useWhen = FlexibleStringExpander.getInstance(string);
    }

    public void setEncodeOutput(boolean encodeOutput) {
        this.encodeOutput = encodeOutput;
    }

    /**
     * @param string
     */
    public void setWidgetAreaStyle(String string) {
        this.widgetAreaStyle = string;
    }

    /**
     * @param string
     */
    public void setWidgetStyle(String string) {
        this.widgetStyle = string;
    }

    /**
     * @param string
     */
    public void setTooltipStyle(String string) {
        this.tooltipStyle = string;
    }

    public boolean getSeparateColumn() {
        return this.separateColumn;
    }

    /**
     * @param string
     */
    public void setHeaderLink(String string) {
        this.headerLink = string;
    }

    /**
     * @param string
     */
    public void setHeaderLinkStyle(String string) {
        this.headerLinkStyle = string;
    }


    public boolean getRequiredField() {
        return this.requiredField != null ? this.requiredField : false;
    }

    /**
     * @param boolean
     */
    public void setRequiredField(boolean required) {
        this.requiredField = required;
    }

    public boolean isSortField() {
        return this.sortField != null && this.sortField.booleanValue();
    }

    /**
     * @param boolean
     */
    public void setSortField(boolean sort) {
        this.sortField = Boolean.valueOf(sort);
    }

    /**
     * @param ModelForm
     */
    public void setModelForm(ModelForm modelForm) {
        this.modelForm = modelForm;
    }


    public static abstract class FieldInfo {

        public static final int DISPLAY = 1;
        public static final int HYPERLINK = 2;
        public static final int TEXT = 3;
        public static final int TEXTAREA = 4;
        public static final int DATE_TIME = 5;
        public static final int DROP_DOWN = 6;
        public static final int CHECK = 7;
        public static final int RADIO = 8;
        public static final int SUBMIT = 9;
        public static final int RESET = 10;
        public static final int HIDDEN = 11;
        public static final int IGNORED = 12;
        public static final int TEXTQBE = 13;
        public static final int DATEQBE = 14;
        public static final int RANGEQBE = 15;
        public static final int LOOKUP = 16;
        public static final int FILE = 17;
        public static final int PASSWORD = 18;
        public static final int IMAGE = 19;
        public static final int DISPLAY_ENTITY = 20;

        // the numbering here represents the priority of the source;
        //when setting a new fieldInfo on a modelFormField it will only set
        //the new one if the fieldSource is less than or equal to the existing
        //fieldSource, which should always be passed as one of the following...
        public static final int SOURCE_EXPLICIT = 1;
        public static final int SOURCE_AUTO_ENTITY = 2;
        public static final int SOURCE_AUTO_SERVICE = 3;

        public static Map<String, Integer> fieldTypeByName = new HashMap<String, Integer>();

        static {
            fieldTypeByName.put("display", Integer.valueOf(1));
            fieldTypeByName.put("hyperlink", Integer.valueOf(2));
            fieldTypeByName.put("text", Integer.valueOf(3));
            fieldTypeByName.put("textarea", Integer.valueOf(4));
            fieldTypeByName.put("date-time", Integer.valueOf(5));
            fieldTypeByName.put("drop-down", Integer.valueOf(6));
            fieldTypeByName.put("check", Integer.valueOf(7));
            fieldTypeByName.put("radio", Integer.valueOf(8));
            fieldTypeByName.put("submit", Integer.valueOf(9));
            fieldTypeByName.put("reset", Integer.valueOf(10));
            fieldTypeByName.put("hidden", Integer.valueOf(11));
            fieldTypeByName.put("ignored", Integer.valueOf(12));
            fieldTypeByName.put("text-find", Integer.valueOf(13));
            fieldTypeByName.put("date-find", Integer.valueOf(14));
            fieldTypeByName.put("range-find", Integer.valueOf(15));
            fieldTypeByName.put("lookup", Integer.valueOf(16));
            fieldTypeByName.put("file", Integer.valueOf(17));
            fieldTypeByName.put("password", Integer.valueOf(18));
            fieldTypeByName.put("image", Integer.valueOf(19));
            fieldTypeByName.put("display-entity", Integer.valueOf(20));
            fieldTypeByName.put("container", Integer.valueOf(21));
        }

        protected int fieldType;
        protected int fieldSource;
        protected ModelFormField modelFormField;

        /** Don't allow the Default Constructor */
        protected FieldInfo() {}

        /** Value Constructor */
        public FieldInfo(int fieldSource, int fieldType, ModelFormField modelFormField) {
            this.fieldType = fieldType;
            this.fieldSource = fieldSource;
            this.modelFormField = modelFormField;
        }

        /** XML Constructor */
        public FieldInfo(Element element, ModelFormField modelFormField) {
            this.fieldSource = FieldInfo.SOURCE_EXPLICIT;
            this.fieldType = findFieldTypeFromName(element.getTagName());
            this.modelFormField = modelFormField;
        }

        public ModelFormField getModelFormField() {
            return modelFormField;
        }

        public int getFieldType() {
            return fieldType;
        }

        public int getFieldSource() {
            return this.fieldSource;
        }

        public static int findFieldTypeFromName(String name) {
            Integer fieldTypeInt = FieldInfo.fieldTypeByName.get(name);
            if (fieldTypeInt != null) {
                return fieldTypeInt.intValue();
            } else {
                throw new IllegalArgumentException("Could not get fieldType for field type name " + name);
            }
        }

        public abstract void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer) throws IOException;
    }

    public static abstract class FieldInfoWithOptions extends FieldInfo {
        protected FieldInfoWithOptions() {
            super();
        }

        protected FlexibleStringExpander noCurrentSelectedKey = null;
        protected List<OptionSource> optionSources = new LinkedList<OptionSource>();

        public FieldInfoWithOptions(int fieldSource, int fieldType, ModelFormField modelFormField) {
            super(fieldSource, fieldType, modelFormField);
        }

        public FieldInfoWithOptions(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);

            noCurrentSelectedKey = FlexibleStringExpander.getInstance(element.getAttribute("no-current-selected-key"));

            // read all option and entity-options sub-elements, maintaining order
            List<? extends Element> childElements = UtilXml.childElementList(element);
            if (childElements.size() > 0) {
                for (Element childElement: childElements) {
                    if ("option".equals(childElement.getTagName())) {
                        this.addOptionSource(new SingleOption(childElement, this));
                    } else if ("list-options".equals(childElement.getTagName())) {
                        this.addOptionSource(new ListOptions(childElement, this));
                    } else if ("entity-options".equals(childElement.getTagName())) {
                        this.addOptionSource(new EntityOptions(childElement, this));
                    }
                }
            } else {
                // this must be added or the multi-form select box options would not show up
                this.addOptionSource(new SingleOption("Y", " ", this));
            }
        }

        public List<OptionValue> getAllOptionValues(Map<String, Object> context, Delegator delegator) {
            List<OptionValue> optionValues = new LinkedList<OptionValue>();
            for (OptionSource optionSource: this.optionSources) {
                optionSource.addOptionValues(optionValues, context, delegator);
            }
            return optionValues;
        }

        public static String getDescriptionForOptionKey(String key, List<OptionValue> allOptionValues) {
            if (UtilValidate.isEmpty(key)) {
                return "";
            }

            if (UtilValidate.isEmpty(allOptionValues)) {
                return key;
            }

            for (OptionValue optionValue: allOptionValues) {
                if (key.equals(optionValue.getKey())) {
                    return optionValue.getDescription();
                }
            }

            // if we get here we didn't find a match, just return the key
            return key;
        }

        public String getNoCurrentSelectedKey(Map<String, Object> context) {
            if (this.noCurrentSelectedKey == null) {
                return null;
            }
            return this.noCurrentSelectedKey.expandString(context);
        }

        public void setNoCurrentSelectedKey(String string) {
            this.noCurrentSelectedKey = FlexibleStringExpander.getInstance(string);
        }

        public void addOptionSource(OptionSource optionSource) {
            this.optionSources.add(optionSource);
        }
    }

    public static class OptionValue {
        protected String key;
        protected String description;

        public OptionValue(String key, String description) {
            this.key = key;
            this.description = description;
        }

        public String getKey() {
            return key;
        }

        public String getDescription() {
            return description;
        }
    }

    public static abstract class OptionSource {
        protected FieldInfo fieldInfo;

        public abstract void addOptionValues(List<OptionValue> optionValues, Map<String, Object> context, Delegator delegator);
    }

    public static class SingleOption extends OptionSource {
        protected FlexibleStringExpander key;
        protected FlexibleStringExpander description;

        public SingleOption(String key, String description, FieldInfo fieldInfo) {
            this.key = FlexibleStringExpander.getInstance(key);
            this.description = FlexibleStringExpander.getInstance(UtilXml.checkEmpty(description, key));
            this.fieldInfo = fieldInfo;
        }

        public SingleOption(Element optionElement, FieldInfo fieldInfo) {
            this.key = FlexibleStringExpander.getInstance(optionElement.getAttribute("key"));
            this.description = FlexibleStringExpander.getInstance(UtilXml.checkEmpty(optionElement.getAttribute("description"), optionElement.getAttribute("key")));
            this.fieldInfo = fieldInfo;
        }

        @Override
        public void addOptionValues(List<OptionValue> optionValues, Map<String, Object> context, Delegator delegator) {
            optionValues.add(new OptionValue(key.expandString(context), description.expandString(context)));
        }
    }

    public static class ListOptions extends OptionSource {
        protected FlexibleMapAccessor<List<? extends Object>> listAcsr;
        protected String listEntryName;
        protected FlexibleMapAccessor<Object> keyAcsr;
        protected FlexibleStringExpander description;

        public ListOptions(String listName, String listEntryName, String keyName, String description, FieldInfo fieldInfo) {
            this.listAcsr = FlexibleMapAccessor.getInstance(listName);
            this.listEntryName = listEntryName;
            this.keyAcsr = FlexibleMapAccessor.getInstance(keyName);
            this.description = FlexibleStringExpander.getInstance(description);
            this.fieldInfo = fieldInfo;
        }

        public ListOptions(Element optionElement, FieldInfo fieldInfo) {
            this.listEntryName = optionElement.getAttribute("list-entry-name");
            this.keyAcsr = FlexibleMapAccessor.getInstance(optionElement.getAttribute("key-name"));
            this.listAcsr = FlexibleMapAccessor.getInstance(optionElement.getAttribute("list-name"));
            this.listEntryName = optionElement.getAttribute("list-entry-name");
            this.description = FlexibleStringExpander.getInstance(optionElement.getAttribute("description"));
            this.fieldInfo = fieldInfo;
        }

        @Override
        public void addOptionValues(List<OptionValue> optionValues, Map<String, Object> context, Delegator delegator) {
            List<? extends Object> dataList = UtilGenerics.checkList(this.listAcsr.get(context));
            if (dataList != null && dataList.size() != 0) {
                for (Object data: dataList) {
                    Map<String, Object> localContext = FastMap.newInstance();
                    localContext.putAll(context);
                    if (UtilValidate.isNotEmpty(this.listEntryName)) {
                        localContext.put(this.listEntryName, data);
                    } else {
                        Map<String, Object> dataMap = UtilGenerics.checkMap(data);
                        localContext.putAll(dataMap);
                    }
                    Object keyObj = keyAcsr.get(localContext);
                    String key = null;
                    if (keyObj instanceof String) {
                        key = (String) keyObj;
                    } else {
                        try {
                            key = (String) ObjectType.simpleTypeConvert(keyObj, "String", null, null);
                        } catch (GeneralException e) {
                            String errMsg = "Could not convert field value for the field: [" + this.keyAcsr.toString() + "] to String for the value [" + keyObj + "]: " + e.toString();
                            Debug.logError(e, errMsg, module);
                        }
                    }
                    optionValues.add(new OptionValue(key, description.expandString(localContext)));
                }
            }
        }
    }

    public static class EntityOptions extends OptionSource {
        protected String entityName;
        protected String keyFieldName;
        protected FlexibleStringExpander description;
        protected boolean cache = true;
        protected String filterByDate;

        protected List<EntityFinderUtil.ConditionExpr> constraintList = null;
        protected List<String> orderByList = null;

        public EntityOptions(FieldInfo fieldInfo) {
            this.fieldInfo = fieldInfo;
        }

        public EntityOptions(Element entityOptionsElement, FieldInfo fieldInfo) {
            this.entityName = entityOptionsElement.getAttribute("entity-name");
            this.keyFieldName = entityOptionsElement.getAttribute("key-field-name");
            this.description = FlexibleStringExpander.getInstance(entityOptionsElement.getAttribute("description"));
            this.cache = !"false".equals(entityOptionsElement.getAttribute("cache"));
            this.filterByDate = entityOptionsElement.getAttribute("filter-by-date");

            List<? extends Element> constraintElements = UtilXml.childElementList(entityOptionsElement, "entity-constraint");
            if (UtilValidate.isNotEmpty(constraintElements)) {
                this.constraintList = new LinkedList<EntityFinderUtil.ConditionExpr>();
                for (Element constraintElement: constraintElements) {
                    constraintList.add(new EntityFinderUtil.ConditionExpr(constraintElement));
                }
            }

            List<? extends Element> orderByElements = UtilXml.childElementList(entityOptionsElement, "entity-order-by");
            if (UtilValidate.isNotEmpty(orderByElements)) {
                this.orderByList = new LinkedList<String>();
                for (Element orderByElement: orderByElements) {
                    orderByList.add(orderByElement.getAttribute("field-name"));
                }
            }

            this.fieldInfo = fieldInfo;
        }

        public String getKeyFieldName() {
            if (UtilValidate.isNotEmpty(this.keyFieldName)) {
                return this.keyFieldName;
            } else {
                // get the modelFormField fieldName
                return this.fieldInfo.getModelFormField().getFieldName();
            }
        }

        @Override
        public void addOptionValues(List<OptionValue> optionValues, Map<String, Object> context, Delegator delegator) {
            // first expand any conditions that need expanding based on the current context
            EntityCondition findCondition = null;
            if (UtilValidate.isNotEmpty(this.constraintList)) {
                List<EntityCondition> expandedConditionList = new LinkedList<EntityCondition>();
                for (EntityFinderUtil.Condition condition: constraintList) {
                    ModelEntity modelEntity = delegator.getModelEntity(this.entityName);
                    expandedConditionList.add(condition.createCondition(context, modelEntity, delegator.getModelFieldTypeReader(modelEntity)));
                }
                findCondition = EntityCondition.makeCondition(expandedConditionList);
            }

            try {
                Locale locale = UtilMisc.ensureLocale(context.get("locale"));

                List<GenericValue> values = null;
                values = delegator.findList(this.entityName, findCondition, null, this.orderByList, null, this.cache);

                // filter-by-date if requested
                if ("true".equals(this.filterByDate)) {
                    values = EntityUtil.filterByDate(values, true);
                } else if (!"false".equals(this.filterByDate)) {
                    // not explicitly true or false, check to see if has fromDate and thruDate, if so do the filter
                    ModelEntity modelEntity = delegator.getModelEntity(this.entityName);
                    if (modelEntity != null && modelEntity.isField("fromDate") && modelEntity.isField("thruDate")) {
                        values = EntityUtil.filterByDate(values, true);
                    }
                }

                for (GenericValue value: values) {
                    // add key and description with string expansion, ie expanding ${} stuff, passing locale explicitly to expand value string because it won't be found in the Entity
                    MapStack<String> localContext = MapStack.create(context);
                    localContext.push(value);

                    // expand with the new localContext, which is locale aware
                    String optionDesc = this.description.expandString(localContext, locale);

                    Object keyFieldObject = value.get(this.getKeyFieldName());
                    if (keyFieldObject == null) {
                        throw new IllegalArgumentException("The entity-options identifier (from key-name attribute, or default to the field name) [" + this.getKeyFieldName() + "], may not be a valid key field name for the entity [" + this.entityName + "].");
                    }
                    String keyFieldValue = keyFieldObject.toString();
                    optionValues.add(new OptionValue(keyFieldValue, optionDesc));
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error getting entity options in form", module);
            }
        }
    }

    public static class InPlaceEditor {
        protected FlexibleStringExpander url;
        protected String cancelControl;
        protected String cancelText;
        protected String clickToEditText;
        protected String fieldPostCreation;
        protected String formClassName;
        protected String highlightColor;
        protected String highlightEndColor;
        protected String hoverClassName;
        protected String htmlResponse;
        protected String loadingClassName;
        protected String loadingText;
        protected String okControl;
        protected String okText;
        protected String paramName;
        protected String savingClassName;
        protected String savingText;
        protected String submitOnBlur;
        protected String textBeforeControls;
        protected String textAfterControls;
        protected String textBetweenControls;
        protected String updateAfterRequestCall;
        protected String rows;
        protected String cols;
        protected Map<FlexibleMapAccessor<Object>, Object> fieldMap;

        public InPlaceEditor (Element element) {
            this.setUrl(element.getAttribute("url"));
            this.cancelControl = element.getAttribute("cancel-control");
            this.cancelText = element.getAttribute("cancel-text");
            this.clickToEditText = element.getAttribute("click-to-edit-text");
            this.fieldPostCreation = element.getAttribute("field-post-creation");
            this.formClassName = element.getAttribute("form-class-name");
            this.highlightColor = element.getAttribute("highlight-color");
            this.highlightEndColor = element.getAttribute("highlight-end-color");
            this.hoverClassName = element.getAttribute("hover-class-name");
            this.htmlResponse = element.getAttribute("html-response");
            this.loadingClassName = element.getAttribute("loading-class-name");
            this.loadingText = element.getAttribute("loading-text");
            this.okControl = element.getAttribute("ok-control");
            this.okText = element.getAttribute("ok-text");
            this.paramName = element.getAttribute("param-name");
            this.savingClassName = element.getAttribute("saving-class-name");
            this.savingText = element.getAttribute("saving-text");
            this.submitOnBlur = element.getAttribute("submit-on-blur");
            this.textBeforeControls = element.getAttribute("text-before-controls");
            this.textAfterControls = element.getAttribute("text-after-controls");
            this.textBetweenControls = element.getAttribute("text-between-controls");
            this.updateAfterRequestCall = element.getAttribute("update-after-request-call");

            Element simpleElement = UtilXml.firstChildElement(element, "simple-editor");
            this.rows = simpleElement.getAttribute("rows");
            this.cols = simpleElement.getAttribute("cols");

            this.fieldMap = EntityFinderUtil.makeFieldMap(element);
        }

        public String getUrl(Map<String, Object> context) {
            if (this.url != null) {
                return this.url.expandString(context);
            } else {
                return "";
            }
        }

        public String getCancelControl() {
            return this.cancelControl;
        }

        public String getCancelText() {
            return this.cancelText;
        }

        public String getClickToEditText() {
            return this.clickToEditText;
        }

        public String getFieldPostCreation() {
           return this.fieldPostCreation;
        }

        public String getFormClassName() {
            return this.formClassName;
        }

        public String getHighlightColor() {
            return this.highlightColor;
        }

        public String getHighlightEndColor() {
            return this.highlightEndColor;
        }

        public String getHoverClassName() {
            return this.hoverClassName;
        }

        public String getHtmlResponse() {
            return this.htmlResponse;
        }

        public String getLoadingClassName() {
            return this.loadingClassName;
        }

        public String getLoadingText() {
            return this.loadingText;
        }

        public String getOkControl() {
            return this.okControl;
        }

        public String getOkText() {
            return this.okText;
        }

        public String getParamName() {
            return this.paramName;
        }

        public String getSavingClassName() {
            return this.savingClassName;
        }

        public String getSavingText() {
            return this.savingText;
        }

        public String getSubmitOnBlur() {
            return this.submitOnBlur;
        }

        public String getTextBeforeControls() {
            return this.textBeforeControls;
        }

        public String getTextAfterControls() {
            return this.textAfterControls;
        }

        public String getTextBetweenControls() {
            return this.textBetweenControls;
        }

        public String getUpdateAfterRequestCall() {
            return this.updateAfterRequestCall;
        }

        public String getRows() {
            return this.rows;
        }

        public String getCols() {
            return this.cols;
        }

        public Map<String, Object> getFieldMap(Map<String, Object> context) {
            Map<String, Object> inPlaceEditorContext = new HashMap<String, Object>();
            EntityFinderUtil.expandFieldMapToContext(this.fieldMap, context, inPlaceEditorContext);
            return inPlaceEditorContext;
        }

        public void setUrl(String url) {
            this.url = FlexibleStringExpander.getInstance(url);
        }

        public void setCancelControl(String string) {
            this.cancelControl = string;
        }

        public void setCancelText(String string) {
            this.cancelText = string;
        }

        public void setClickToEditText(String string) {
            this.clickToEditText = string;
        }

        public void setFieldPostCreation(String string) {
            this.fieldPostCreation = string;
        }

        public void setFormClassName(String string) {
            this.formClassName = string;
        }

        public void setHighlightColor(String string) {
            this.highlightColor = string;
        }

        public void setHighlightEndColor(String string) {
            this.highlightEndColor = string;
        }

        public void setHoverClassName(String string) {
            this.hoverClassName = string;
        }

        public void setHtmlResponse(String string) {
            this.htmlResponse = string;
        }

        public void setLoadingClassName(String string) {
            this.loadingClassName = string;
        }

        public void setLoadingText(String string) {
            this.loadingText = string;
        }

        public void setOkControl(String string) {
            this.okControl = string;
        }

        public void setOkText(String string) {
            this.okText = string;
        }

        public void setParamName(String string) {
            this.paramName = string;
        }

        public void setSavingClassName(String string) {
            this.savingClassName = string;
        }

        public void setSavingText(String string) {
            this.savingText = string;
        }

        public void setSubmitOnBlur(String string) {
            this.submitOnBlur = string;
        }

        public void setTextBeforeControls(String string) {
            this.textBeforeControls = string;
        }

        public void setTextAfterControls(String string) {
            this.textAfterControls = string;
        }

        public void setTextBetweenControls(String string) {
            this.textBetweenControls = string;
        }

        public void setUpdateAfterRequestCall(String string) {
            this.updateAfterRequestCall = string;
        }

        public void setRows(String string) {
            this.rows = string;
        }

        public void setCols(String string) {
            this.cols = string;
        }

        public void setFieldMap(Map<FlexibleMapAccessor<Object>, Object> fieldMap) {
            this.fieldMap = fieldMap;
        }
    }

    public static class DisplayField extends FieldInfo {
        protected boolean alsoHidden = true;
        protected FlexibleStringExpander description;
        protected String type;  // matches type of field, currently text or currency
        protected String imageLocation;
        protected FlexibleStringExpander currency;
        protected FlexibleStringExpander date;
        protected InPlaceEditor inPlaceEditor;

        protected DisplayField() {
            super();
        }

        public DisplayField(ModelFormField modelFormField) {
            super(FieldInfo.SOURCE_EXPLICIT, FieldInfo.DISPLAY, modelFormField);
        }

        public DisplayField(int fieldSource, ModelFormField modelFormField) {
            super(fieldSource, FieldInfo.DISPLAY, modelFormField);
        }

        public DisplayField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
            this.type = element.getAttribute("type");
            this.imageLocation = element.getAttribute("image-location");
            this.setCurrency(element.getAttribute("currency"));
            this.setDescription(element.getAttribute("description"));
            this.setDate(element.getAttribute("date"));
            this.alsoHidden = !"false".equals(element.getAttribute("also-hidden"));

            Element inPlaceEditorElement = UtilXml.firstChildElement(element, "in-place-editor");
            if (inPlaceEditorElement != null) {
                this.inPlaceEditor = new InPlaceEditor(inPlaceEditorElement);
            }
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer) throws IOException {
            formStringRenderer.renderDisplayField(writer, context, this);
        }

        public boolean getAlsoHidden() {
            return alsoHidden;
        }
        public String getType(){
            return this.type;
        }

        public String getImageLocation(){
            return this.imageLocation;
        }

        public String getDescription(Map<String, Object> context) {
            String retVal = null;
            if (this.description != null && !this.description.isEmpty()) {
                retVal = this.description.expandString(context);
                if (retVal != null) {
                    StringUtil.SimpleEncoder simpleEncoder = (StringUtil.SimpleEncoder) context.get("simpleEncoder");
                    if (simpleEncoder != null) {
                        retVal = simpleEncoder.encode(retVal);
                    }
                }
            } else {
                retVal = this.modelFormField.getEntry(context);
            }
            if (UtilValidate.isEmpty(retVal)) {
                retVal = "";
            } else if ("currency".equals(type)) {
                retVal = retVal.replaceAll("&nbsp;", " "); // FIXME : encoding currency is a problem for some locale, we should not have any &nbsp; in retVal other case may arise in future...
                Locale locale = (Locale) context.get("locale");
                if (locale == null) locale = Locale.getDefault();
                String isoCode = null;
                if (this.currency != null && !this.currency.isEmpty()) {
                    isoCode = this.currency.expandString(context);
                }

                try {
                    BigDecimal parsedRetVal = (BigDecimal) ObjectType.simpleTypeConvert(retVal, "BigDecimal", null, null, locale, true);
                    retVal = UtilFormatOut.formatCurrency(parsedRetVal, isoCode, locale, 10); // we set the max to 10 digits as an hack to not round numbers in the ui
                } catch (GeneralException e) {
                    String errMsg = "Error formatting currency value [" + retVal + "]: " + e.toString();
                    Debug.logError(e, errMsg, module);
                    throw new IllegalArgumentException(errMsg);
                }
            } else if ("date".equals(this.type) && retVal.length() > 10) {
                retVal = retVal.substring(0,10);
            } else if ("date-time".equals(this.type) && retVal.length() > 16) {
                retVal = retVal.substring(0,16);
            }
            return retVal;
        }

        public InPlaceEditor getInPlaceEditor() {
            return this.inPlaceEditor;
        }

        /**
         * @param b
         */
        public void setAlsoHidden(boolean b) {
            alsoHidden = b;
        }

        /**
         * @param Description
         */
        public void setDescription(String string) {
            description = FlexibleStringExpander.getInstance(string);
        }

        /**
         * @param string
         */
        public void setCurrency(String string) {
            currency = FlexibleStringExpander.getInstance(string);
        }
        /**
         * @param date
         */
        public void setDate(String string) {
            date = FlexibleStringExpander.getInstance(string);
        }

        public void setInPlaceEditor(InPlaceEditor newInPlaceEditor) {
            this.inPlaceEditor = newInPlaceEditor;
        }
    }

    public static class DisplayEntityField extends DisplayField {
        protected String entityName;
        protected String keyFieldName;
        protected boolean cache = true;
        protected SubHyperlink subHyperlink;

        protected DisplayEntityField() {
            super();
        }

        public DisplayEntityField(ModelFormField modelFormField) {
            super(modelFormField);
            this.fieldType = FieldInfo.DISPLAY_ENTITY;
        }

        public DisplayEntityField(int fieldSource, ModelFormField modelFormField) {
            super(fieldSource, modelFormField);
            this.fieldType = FieldInfo.DISPLAY_ENTITY;
        }

        public DisplayEntityField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);

            this.entityName = element.getAttribute("entity-name");
            this.keyFieldName = element.getAttribute("key-field-name");
            this.cache = !"false".equals(element.getAttribute("cache"));

            if (this.description == null || this.description.isEmpty()) {
                this.setDescription("${description}");
            }

            Element subHyperlinkElement = UtilXml.firstChildElement(element, "sub-hyperlink");
            if (subHyperlinkElement != null) {
                this.subHyperlink = new SubHyperlink(subHyperlinkElement, this.getModelFormField());
            }
        }

        @Override
        public String getDescription(Map<String, Object> context) {
            Locale locale = UtilMisc.ensureLocale(context.get("locale"));

            // rather than using the context to expand the string, lookup the given entity and use it to expand the string
            GenericValue value = null;
            String fieldKey = this.keyFieldName;
            if (UtilValidate.isEmpty(fieldKey)) {
                fieldKey = this.modelFormField.fieldName;
            }
            Delegator delegator = this.modelFormField.modelForm.getDelegator(context);
            String fieldValue = modelFormField.getEntry(context);
            try {
                value = delegator.findOne(this.entityName, this.cache, fieldKey, fieldValue);
            } catch (GenericEntityException e) {
                String errMsg = "Error getting value from the database for display of field [" + this.modelFormField.getName() + "] on form [" + this.modelFormField.modelForm.getName() + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new IllegalArgumentException(errMsg);
            }

            String retVal = null;
            if (value != null) {
                // expanding ${} stuff, passing locale explicitly to expand value string because it won't be found in the Entity
                MapStack<String> localContext = MapStack.create(context);
                localContext.push(value);

                // expand with the new localContext, which is locale aware
                retVal = this.description.expandString(localContext, locale);
            }
            // try to get the entry for the field if description doesn't expand to anything
            if (UtilValidate.isEmpty(retVal)) {
                retVal = fieldValue;
            }
            if (UtilValidate.isEmpty(retVal)) {
                retVal = "";
            }
            return retVal;
        }

        public SubHyperlink getSubHyperlink() {
            return this.subHyperlink;
        }
        public void setSubHyperlink(SubHyperlink newSubHyperlink) {
            this.subHyperlink = newSubHyperlink;
        }
    }

    public static class HyperlinkField extends FieldInfo {
        public static String DEFAULT_TARGET_TYPE = "intra-app";

        protected boolean alsoHidden = true;
        protected String linkType;
        protected String targetType;
        protected String image;
        protected FlexibleStringExpander target;
        protected FlexibleStringExpander description;
        protected FlexibleStringExpander alternate;
        protected FlexibleStringExpander targetWindowExdr;
        protected List<WidgetWorker.Parameter> parameterList = FastList.newInstance();

        protected boolean requestConfirmation = false;
        protected FlexibleStringExpander confirmationMsgExdr;
        protected HyperlinkField() {
            super();
        }

        public HyperlinkField(ModelFormField modelFormField) {
            super(FieldInfo.SOURCE_EXPLICIT, FieldInfo.HYPERLINK, modelFormField);
        }

        public HyperlinkField(int fieldSource, ModelFormField modelFormField) {
            super(fieldSource, FieldInfo.HYPERLINK, modelFormField);
        }

        public HyperlinkField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);

            this.setDescription(element.getAttribute("description"));
            this.setAlternate(element.getAttribute("alternate"));
            this.setTarget(element.getAttribute("target"));
            this.alsoHidden = !"false".equals(element.getAttribute("also-hidden"));
            this.linkType = element.getAttribute("link-type");
            this.targetType = element.getAttribute("target-type");
            this.targetWindowExdr = FlexibleStringExpander.getInstance(element.getAttribute("target-window"));
            this.image = element.getAttribute("image-location");
            this.setRequestConfirmation("true".equals(element.getAttribute("request-confirmation")));
            this.setConfirmationMsg(element.getAttribute("confirmation-message"));
            List<? extends Element> parameterElementList = UtilXml.childElementList(element, "parameter");
            for (Element parameterElement: parameterElementList) {
                this.parameterList.add(new WidgetWorker.Parameter(parameterElement));
            }
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer) throws IOException {
            formStringRenderer.renderHyperlinkField(writer, context, this);
        }

        public boolean getAlsoHidden() {
            return this.alsoHidden;
        }

        public boolean getRequestConfirmation() {
            return this.requestConfirmation;
        }

        public String getConfirmation(Map<String, Object> context) {
            String message = getConfirmationMsg(context);
            if (UtilValidate.isNotEmpty(message)) {
                return message;
            }
            else if (getRequestConfirmation()) {
                String defaultMessage = UtilProperties.getPropertyValue("general", "default.confirmation.message", "${uiLabelMap.CommonConfirm}");
                setConfirmationMsg(defaultMessage);
                return getConfirmationMsg(context);
            }
            return "";
        }

        public String getConfirmationMsg(Map<String, Object> context) {
            return this.confirmationMsgExdr.expandString(context);
        }

        public String getLinkType() {
            return this.linkType;
        }

        public String getTargetType() {
            if (UtilValidate.isNotEmpty(this.targetType)) {
                return this.targetType;
            } else {
                return HyperlinkField.DEFAULT_TARGET_TYPE;
            }
        }

        public String getTargetWindow(Map<String, Object> context) {
            String targetWindow = this.targetWindowExdr.expandString(context);
            return targetWindow;
        }

        public String getDescription(Map<String, Object> context) {
            return this.description.expandString(context);
        }

        public String getAlternate(Map<String, Object> context) {
            return this.alternate.expandString(context);
        }

        public String getTarget(Map<String, Object> context) {
            return this.target.expandString(context);
        }

        public List<WidgetWorker.Parameter> getParameterList() {
            return this.parameterList;
        }

        public String getImage() {
            return this.image;
        }

        /**
         * @param b
         */
        public void setAlsoHidden(boolean b) {
            this.alsoHidden = b;
        }

        /**
         * @param string
         */
        public void setTargetType(String string) {
            this.targetType = string;
        }

        /**
         * @param string
         */
        public void setDescription(String string) {
            this.description = FlexibleStringExpander.getInstance(string);
        }

        /**
         * @param string
         */
        public void setAlternate(String string) {
            this.alternate = FlexibleStringExpander.getInstance(string);
        }

        /**
         * @param string
         */
        public void setTarget(String string) {
            this.target = FlexibleStringExpander.getInstance(string);
        }

        public void setRequestConfirmation(boolean val) {
            this.requestConfirmation = val;
        }

        public void setConfirmationMsg(String val) {
            this.confirmationMsgExdr = FlexibleStringExpander.getInstance(val);
        }
    }

    public static class SubHyperlink {
        protected FlexibleStringExpander useWhen;
        protected String linkType;
        protected String linkStyle;
        protected String targetType;
        protected FlexibleStringExpander target;
        protected FlexibleStringExpander description;
        protected FlexibleStringExpander targetWindowExdr;
        protected List<WidgetWorker.Parameter> parameterList = FastList.newInstance();
        protected boolean requestConfirmation = false;
        protected FlexibleStringExpander confirmationMsgExdr;
        protected ModelFormField modelFormField;

        public SubHyperlink(Element element, ModelFormField modelFormField) {
            this.setDescription(element.getAttribute("description"));
            this.setTarget(element.getAttribute("target"));
            this.setUseWhen(element.getAttribute("use-when"));
            this.linkType = element.getAttribute("link-type");
            this.linkStyle = element.getAttribute("link-style");
            this.targetType = element.getAttribute("target-type");
            this.targetWindowExdr = FlexibleStringExpander.getInstance(element.getAttribute("target-window"));
            List<? extends Element> parameterElementList = UtilXml.childElementList(element, "parameter");
            for (Element parameterElement: parameterElementList) {
                this.parameterList.add(new WidgetWorker.Parameter(parameterElement));
            }
            setRequestConfirmation("true".equals(element.getAttribute("request-confirmation")));
            setConfirmationMsg(element.getAttribute("confirmation-message"));

            this.modelFormField = modelFormField;
        }

        public String getLinkStyle() {
            return this.linkStyle;
        }

        public String getTargetType() {
            if (UtilValidate.isNotEmpty(this.targetType)) {
                return this.targetType;
            } else {
                return HyperlinkField.DEFAULT_TARGET_TYPE;
            }
        }

        public String getDescription(Map<String, Object> context) {
            if (this.description != null) {
                return this.description.expandString(context);
            } else {
                return "";
            }
        }

        public String getTargetWindow(Map<String, Object> context) {
            String targetWindow = this.targetWindowExdr.expandString(context);
            return targetWindow;
        }

        public String getTarget(Map<String, Object> context) {
            if (this.target != null) {
                return this.target.expandString(context);
            } else {
                return "";
            }
        }

        public String getLinkType() {
            return this.linkType;
        }

        public List<WidgetWorker.Parameter> getParameterList() {
            return this.parameterList;
        }

        public String getUseWhen(Map<String, Object> context) {
            if (this.useWhen != null) {
                return this.useWhen.expandString(context);
            } else {
                return "";
            }
        }

        public boolean getRequestConfirmation() {
            return this.requestConfirmation;
        }

        public String getConfirmationMsg(Map<String, Object> context) {
            return this.confirmationMsgExdr.expandString(context);
        }

        public String getConfirmation(Map<String, Object> context) {
            String message = getConfirmationMsg(context);
            if (UtilValidate.isNotEmpty(message)) {
                return message;
            }
            else if (getRequestConfirmation()) {
                String defaultMessage = UtilProperties.getPropertyValue("general", "default.confirmation.message", "${uiLabelMap.CommonConfirm}");
                setConfirmationMsg(defaultMessage);
                return getConfirmationMsg(context);
            }
            return "";
        }

        public ModelFormField getModelFormField() {
            return this.modelFormField;
        }

        public boolean shouldUse(Map<String, Object> context) {
            boolean shouldUse = true;
            String useWhen = this.getUseWhen(context);
            if (UtilValidate.isNotEmpty(useWhen)) {
                try {
                    Interpreter bsh = (Interpreter) context.get("bshInterpreter");
                    if (bsh == null) {
                        bsh = BshUtil.makeInterpreter(context);
                        context.put("bshInterpreter", bsh);
                    }

                    Object retVal = bsh.eval(StringUtil.convertOperatorSubstitutions(useWhen));

                    // retVal should be a Boolean, if not something weird is up...
                    if (retVal instanceof Boolean) {
                        Boolean boolVal = (Boolean) retVal;
                        shouldUse = boolVal.booleanValue();
                    } else {
                        throw new IllegalArgumentException(
                            "Return value from target condition eval was not a Boolean: " + retVal.getClass().getName() + " [" + retVal + "]");
                    }
                } catch (EvalError e) {
                    String errmsg = "Error evaluating BeanShell target conditions";
                    Debug.logError(e, errmsg, module);
                    throw new IllegalArgumentException(errmsg);
                }
            }
            return shouldUse;
        }

        /**
         * @param string
         */
        public void setLinkStyle(String string) {
            this.linkStyle = string;
        }

        /**
         * @param string
         */
        public void setTargetType(String string) {
            this.targetType = string;
        }

        /**
         * @param string
         */
        public void setDescription(String string) {
            this.description = FlexibleStringExpander.getInstance(string);
        }

        /**
         * @param string
         */
        public void setTarget(String string) {
            this.target = FlexibleStringExpander.getInstance(string);
        }

        /**
         * @param string
         */
        public void setUseWhen(String string) {
            this.useWhen = FlexibleStringExpander.getInstance(string);
        }

        public void setRequestConfirmation(boolean val) {
            this.requestConfirmation = val;
        }

        public void setConfirmationMsg(String val) {
            this.confirmationMsgExdr = FlexibleStringExpander.getInstance(val);
        }
    }

    public static class AutoComplete {
        protected String autoSelect;
        protected String frequency;
        protected String minChars;
        protected String choices;
        protected String partialSearch;
        protected String partialChars;
        protected String ignoreCase;
        protected String fullSearch;

        public AutoComplete(Element element) {
            this.autoSelect = element.getAttribute("auto-select");
            this.frequency = element.getAttribute("frequency");
            this.minChars = element.getAttribute("min-chars");
            this.choices = element.getAttribute("choices");
            this.partialSearch = element.getAttribute("partial-search");
            this.partialChars = element.getAttribute("partial-chars");
            this.ignoreCase = element.getAttribute("ignore-case");
            this.fullSearch = element.getAttribute("full-search");
        }

        public String getAutoSelect() {
            return this.autoSelect;
        }

        public String getFrequency() {
            return this.frequency;
        }

        public String getMinChars() {
            return this.minChars;
        }

        public String getChoices() {
            return this.choices;
        }

        public String getPartialSearch() {
            return this.partialSearch;
        }

        public String getPartialChars() {
            return this.partialChars;
        }

        public String getIgnoreCase() {
            return this.ignoreCase;
        }

        public String getFullSearch() {
            return this.fullSearch;
        }

        public void setAutoSelect(String string) {
            this.autoSelect = string;
        }

        public void setFrequency(String string) {
            this.frequency = string;
        }

        public void setMinChars(String string) {
            this.minChars = string;
        }

        public void setChoices(String string) {
            this.choices = string;
        }

        public void setPartialSearch(String string) {
            this.partialSearch = string;
        }

        public void setPartialChars(String string) {
            this.partialChars = string;
        }

        public void setIgnoreCase(String string) {
            this.ignoreCase = string;
        }

        public void setFullSearch(String string) {
            this.fullSearch = string;
        }
    }

    public static class TextField extends FieldInfo {
        protected int size = 25;
        protected Integer maxlength;
        protected FlexibleStringExpander defaultValue;
        protected SubHyperlink subHyperlink;
        protected boolean disabled;
        protected boolean clientAutocompleteField;

        protected TextField() {
            super();
        }

        public TextField(ModelFormField modelFormField) {
            super(FieldInfo.SOURCE_EXPLICIT, FieldInfo.TEXT, modelFormField);
        }

        public TextField(int fieldSource, ModelFormField modelFormField) {
            super(fieldSource, FieldInfo.TEXT, modelFormField);
        }

        public TextField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
            this.setDefaultValue(element.getAttribute("default-value"));

            String sizeStr = element.getAttribute("size");
            try {
                size = Integer.parseInt(sizeStr);
            } catch (Exception e) {
                if (UtilValidate.isNotEmpty(sizeStr)) {
                    Debug.logError("Could not parse the size value of the text element: [" + sizeStr + "], setting to the default of " + size, module);
                }
            }

            String maxlengthStr = element.getAttribute("maxlength");
            try {
                maxlength = Integer.valueOf(maxlengthStr);
            } catch (Exception e) {
                maxlength = null;
                if (UtilValidate.isNotEmpty(maxlengthStr)) {
                    Debug.logError("Could not parse the max-length value of the text element: [" + maxlengthStr + "], setting to null; default of no maxlength will be used", module);
                }
            }

            this.disabled = "true".equals(element.getAttribute("disabled"));

            this.clientAutocompleteField = !"false".equals(element.getAttribute("client-autocomplete-field"));

            Element subHyperlinkElement = UtilXml.firstChildElement(element, "sub-hyperlink");
            if (subHyperlinkElement != null) {
                this.subHyperlink = new SubHyperlink(subHyperlinkElement, this.getModelFormField());
            }
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer) throws IOException {
            formStringRenderer.renderTextField(writer, context, this);
        }

        public Integer getMaxlength() {
            return maxlength;
        }

        public int getSize() {
            return size;
        }

        public boolean getDisabled() {
            return this.disabled;
        }

        public void setDisabled(boolean b) {
            this.disabled = b;
        }

        public boolean getClientAutocompleteField() {
            return this.clientAutocompleteField;
        }

        public void setClientAutocompleteField(boolean b) {
            this.clientAutocompleteField = b;
        }

        public String getDefaultValue(Map<String, Object> context) {
            if (this.defaultValue != null) {
                return this.defaultValue.expandString(context);
            } else {
                return "";
            }
        }

        /**
         * @param integer
         */
        public void setMaxlength(Integer integer) {
            maxlength = integer;
        }

        /**
         * @param i
         */
        public void setSize(int i) {
            size = i;
        }

        /**
         * @param str
         */
        public void setDefaultValue(String str) {
            this.defaultValue = FlexibleStringExpander.getInstance(str);
        }

        public SubHyperlink getSubHyperlink() {
            return this.subHyperlink;
        }
        public void setSubHyperlink(SubHyperlink newSubHyperlink) {
            this.subHyperlink = newSubHyperlink;
        }
    }

    public static class TextareaField extends FieldInfo {
        protected int cols = 60;
        protected int rows = 2;
        protected FlexibleStringExpander defaultValue;
        protected boolean visualEditorEnable = false;
        protected boolean readOnly = false;
        protected FlexibleStringExpander visualEditorButtons;

        protected TextareaField() {
            super();
        }

        public TextareaField(ModelFormField modelFormField) {
            super(FieldInfo.SOURCE_EXPLICIT, FieldInfo.TEXTAREA, modelFormField);
        }

        public TextareaField(int fieldSource, ModelFormField modelFormField) {
            super(fieldSource, FieldInfo.TEXTAREA, modelFormField);
        }

        public TextareaField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
            this.setDefaultValue(element.getAttribute("default-value"));

            visualEditorEnable = "true".equals(element.getAttribute("visual-editor-enable"));
            visualEditorButtons = FlexibleStringExpander.getInstance(element.getAttribute("visual-editor-buttons"));
            readOnly = "true".equals(element.getAttribute("read-only"));

            String colsStr = element.getAttribute("cols");
            try {
                cols = Integer.parseInt(colsStr);
            } catch (Exception e) {
                if (UtilValidate.isNotEmpty(colsStr)) {
                    Debug.logError("Could not parse the size value of the text element: [" + colsStr + "], setting to default of " + cols, module);
                }
            }

            String rowsStr = element.getAttribute("rows");
            try {
                rows = Integer.parseInt(rowsStr);
            } catch (Exception e) {
                if (UtilValidate.isNotEmpty(rowsStr)) {
                    Debug.logError("Could not parse the size value of the text element: [" + rowsStr + "], setting to default of " + rows, module);
                }
            }
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer) throws IOException {
            formStringRenderer.renderTextareaField(writer, context, this);
        }

        public int getCols() {
            return cols;
        }

        public int getRows() {
            return rows;
        }

        public String getDefaultValue(Map<String, Object> context) {
            if (this.defaultValue != null) {
                return this.defaultValue.expandString(context);
            } else {
                return "";
            }
        }

        public boolean getVisualEditorEnable() {
            return this.visualEditorEnable;
        }

        public String getVisualEditorButtons(Map<String, Object> context) {
            return this.visualEditorButtons.expandString(context);
        }

        public boolean isReadOnly() {
            return readOnly;
        }

        /**
         * @param r
         */
        public void setReadOnly(boolean r) {
            readOnly = r;
        }

        /**
         * @param i
         */
        public void setCols(int i) {
            cols = i;
        }

        /**
         * @param i
         */
        public void setRows(int i) {
            rows = i;
        }

        /**
         * @param str
         */
        public void setDefaultValue(String str) {
            this.defaultValue = FlexibleStringExpander.getInstance(str);
        }

        /**
         * @param i
         */
        public void setVisualEditorEnable(boolean visualEditorEnable) {
            this.visualEditorEnable = visualEditorEnable;
        }

        /**
         * @param i
         */
        public void setVisualEditorButtons(String eb) {
            this.visualEditorButtons = FlexibleStringExpander.getInstance(eb);
        }
    }

    public static class DateTimeField extends FieldInfo {
        protected String type;
        protected FlexibleStringExpander defaultValue;
        protected String inputMethod;
        protected String clock;

        protected DateTimeField() {
            super();
        }

        public DateTimeField(ModelFormField modelFormField) {
            super(FieldInfo.SOURCE_EXPLICIT, FieldInfo.DATE_TIME, modelFormField);
        }

        public DateTimeField(int fieldSource, ModelFormField modelFormField) {
            super(fieldSource, FieldInfo.DATE_TIME, modelFormField);
        }

        public DateTimeField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
            this.setDefaultValue(element.getAttribute("default-value"));
            type = element.getAttribute("type");
            inputMethod = element.getAttribute("input-method");
            clock = element.getAttribute("clock");
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer) throws IOException {
            formStringRenderer.renderDateTimeField(writer, context, this);
        }

        public String getType() {
            return type;
        }

        public String getDefaultValue(Map<String, Object> context) {
            if (this.defaultValue != null) {
                return this.defaultValue.expandString(context);
            } else {
                return "";
            }
        }

        public String getInputMethod() {
            return this.inputMethod;
        }

        public String getClock() {
            return this.clock;
        }

        /**
         * @param string
         */
        public void setType(String string) {
            type = string;
        }

        /**
         * @param str
         */
        public void setDefaultValue(String str) {
            this.defaultValue = FlexibleStringExpander.getInstance(str);
        }

        public void setInputMethod(String str) {
            this.inputMethod = str;
        }

        public void setClock(String str) {
            this.clock = str;
        }

        /**
         * Returns the default-value if specified, otherwise the current date, time or timestamp
         *
         * @param context Context Map
         * @return Default value string for date-time
         */
        public String getDefaultDateTimeString(Map<String, Object> context) {
            if (this.defaultValue != null && !this.defaultValue.isEmpty()) {
                return this.getDefaultValue(context);
            }

            if ("date".equals(this.type)) {
                return (new java.sql.Date(System.currentTimeMillis())).toString();
            } else if ("time".equals(this.type)) {
                return (new java.sql.Time(System.currentTimeMillis())).toString();
            } else {
                return UtilDateTime.nowTimestamp().toString();
            }
        }
    }

    public static class DropDownField extends FieldInfoWithOptions {
        protected boolean allowEmpty = false;
        protected boolean allowMulti = false;
        protected String current;
        protected String size;
        protected FlexibleStringExpander currentDescription;
        protected SubHyperlink subHyperlink;
        protected int otherFieldSize = 0;
        protected AutoComplete autoComplete;

        protected DropDownField() {
            super();
        }

        public DropDownField(ModelFormField modelFormField) {
            super(FieldInfo.SOURCE_EXPLICIT, FieldInfo.DROP_DOWN, modelFormField);
        }

        public DropDownField(int fieldSource, ModelFormField modelFormField) {
            super(fieldSource, FieldInfo.DROP_DOWN, modelFormField);
        }

        public DropDownField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);

            this.current = element.getAttribute("current");
            this.size = element.getAttribute("size");
            this.allowEmpty = "true".equals(element.getAttribute("allow-empty"));
            this.allowMulti = "true".equals(element.getAttribute("allow-multiple"));
            this.currentDescription = FlexibleStringExpander.getInstance(element.getAttribute("current-description"));

            // set the default size
            if (size == null) {
                size = "1";
            }

            String sizeStr = element.getAttribute("other-field-size");
            try {
                this.otherFieldSize = Integer.parseInt(sizeStr);
            } catch (Exception e) {
                if (UtilValidate.isNotEmpty(sizeStr)) {
                    Debug.logError("Could not parse the size value of the text element: [" + sizeStr + "], setting to the default of " + this.otherFieldSize, module);
                }
            }

            Element subHyperlinkElement = UtilXml.firstChildElement(element, "sub-hyperlink");
            if (subHyperlinkElement != null) {
                this.subHyperlink = new SubHyperlink(subHyperlinkElement, this.getModelFormField());
            }

            Element autoCompleteElement = UtilXml.firstChildElement(element, "auto-complete");
            if (autoCompleteElement != null) {
                this.autoComplete = new AutoComplete(autoCompleteElement);
            }
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer) throws IOException {
            formStringRenderer.renderDropDownField(writer, context, this);
        }

        public boolean isAllowEmpty() {
            return this.allowEmpty;
        }

        public boolean isAllowMultiple() {
            return this.allowMulti;
        }

        public String getCurrent() {
            if (UtilValidate.isEmpty(this.current)) {
                return "first-in-list";
            } else {
                return this.current;
            }
        }

        public String getCurrentDescription(Map<String, Object> context) {
            if (this.currentDescription == null)
                return null;
            else
                return this.currentDescription.expandString(context);
        }

        public void setAllowEmpty(boolean b) {
            this.allowEmpty = b;
        }

        public void setCurrent(String string) {
            this.current = string;
        }

        public void setCurrentDescription(String string) {
            this.currentDescription = FlexibleStringExpander.getInstance(string);
        }

        public SubHyperlink getSubHyperlink() {
            return this.subHyperlink;
        }
        public void setSubHyperlink(SubHyperlink newSubHyperlink) {
            this.subHyperlink = newSubHyperlink;
        }

        public AutoComplete getAutoComplete() {
            return this.autoComplete;
        }

        public void setAutoComplete(AutoComplete newAutoComplete) {
            this.autoComplete = newAutoComplete;
        }

        public int getOtherFieldSize() {
            return this.otherFieldSize;
        }

        public String getSize() {
            return this.size;
        }

        /**
         * Get the name to use for the parameter for this field in the form interpreter.
         * For HTML forms this is the request parameter name.
         *
         * @return
         */
        public String getParameterNameOther(Map<String, Object> context) {
            String baseName;
            if (UtilValidate.isNotEmpty(this.modelFormField.parameterName)) {
                baseName = this.modelFormField.parameterName;
            } else {
                baseName = this.modelFormField.name;
            }

            baseName += "_OTHER";
            Integer itemIndex = (Integer) context.get("itemIndex");
            if (itemIndex != null && "multi".equals(this.modelFormField.modelForm.getType())) {
                return baseName + this.modelFormField.modelForm.getItemIndexSeparator() + itemIndex.intValue();
            } else {
                return baseName;
            }
        }

    }

    public static class RadioField extends FieldInfoWithOptions {
        protected RadioField() {
            super();
        }

        public RadioField(ModelFormField modelFormField) {
            super(FieldInfo.SOURCE_EXPLICIT, FieldInfo.RADIO, modelFormField);
        }

        public RadioField(int fieldSource, ModelFormField modelFormField) {
            super(fieldSource, FieldInfo.RADIO, modelFormField);
        }

        public RadioField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer) throws IOException {
            formStringRenderer.renderRadioField(writer, context, this);
        }
    }

    public static class CheckField extends FieldInfoWithOptions {
        public final static String ROW_SUBMIT_FIELD_NAME = "_rowSubmit";
        protected FlexibleStringExpander allChecked = null;

        protected CheckField() {
            super();
        }

        public CheckField(ModelFormField modelFormField) {
            super(FieldInfo.SOURCE_EXPLICIT, FieldInfo.CHECK, modelFormField);
        }

        public CheckField(int fieldSource, ModelFormField modelFormField) {
            super(fieldSource, FieldInfo.CHECK, modelFormField);
        }

        public CheckField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
            allChecked = FlexibleStringExpander.getInstance(element.getAttribute("all-checked"));
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer) throws IOException {
            formStringRenderer.renderCheckField(writer, context, this);
        }

        public Boolean isAllChecked(Map<String, Object> context) {
            String allCheckedStr = this.allChecked.expandString(context);
            if (UtilValidate.isNotEmpty(allCheckedStr)) {
                return Boolean.valueOf("true".equals(allCheckedStr));
            } else {
                return null;
            }
        }
    }

    public static class SubmitField extends FieldInfo {
        protected String buttonType;
        protected String imageLocation;
        protected FlexibleStringExpander backgroundSubmitRefreshTargetExdr;
        protected boolean requestConfirmation = false;
        protected FlexibleStringExpander confirmationMsgExdr;

        protected SubmitField() {
            super();
        }

        public SubmitField(ModelFormField modelFormField) {
            super(FieldInfo.SOURCE_EXPLICIT, FieldInfo.SUBMIT, modelFormField);
        }

        public SubmitField(int fieldInfo, ModelFormField modelFormField) {
            super(fieldInfo, FieldInfo.SUBMIT, modelFormField);
        }

        public SubmitField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
            this.buttonType = element.getAttribute("button-type");
            this.imageLocation = element.getAttribute("image-location");
            this.backgroundSubmitRefreshTargetExdr = FlexibleStringExpander.getInstance(element.getAttribute("background-submit-refresh-target"));
            setRequestConfirmation("true".equals(element.getAttribute("request-confirmation")));
            setConfirmationMsg(element.getAttribute("confirmation-message"));
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer) throws IOException {
            formStringRenderer.renderSubmitField(writer, context, this);
        }

        public String getButtonType() {
            return buttonType;
        }

        public String getImageLocation() {
            return imageLocation;
        }

        public boolean getRequestConfirmation() {
            return this.requestConfirmation;
        }

        public String getConfirmationMsg(Map<String, Object> context) {
            return this.confirmationMsgExdr.expandString(context);
        }

        public String getConfirmation(Map<String, Object> context) {
            String message = getConfirmationMsg(context);
            if (UtilValidate.isNotEmpty(message)) {
                return message;
            }
            else if (getRequestConfirmation()) {
                String defaultMessage = UtilProperties.getPropertyValue("general", "default.confirmation.message", "${uiLabelMap.CommonConfirm}");
                setConfirmationMsg(defaultMessage);
                return getConfirmationMsg(context);
            }
            return "";
        }

        /**
         * @param string
         */
        public void setButtonType(String string) {
            buttonType = string;
        }

        /**
         * @param string
         */
        public void setImageLocation(String string) {
            imageLocation = string;
        }

        public String getBackgroundSubmitRefreshTarget(Map<String, Object> context) {
            return this.backgroundSubmitRefreshTargetExdr.expandString(context);
        }

        public void setRequestConfirmation(boolean val) {
            this.requestConfirmation = val;
        }

        public void setConfirmationMsg(String val) {
            this.confirmationMsgExdr = FlexibleStringExpander.getInstance(val);
        }

    }

    public static class ResetField extends FieldInfo {
        protected ResetField() {
            super();
        }

        public ResetField(ModelFormField modelFormField) {
            super(FieldInfo.SOURCE_EXPLICIT, FieldInfo.RESET, modelFormField);
        }

        public ResetField(int fieldSource, ModelFormField modelFormField) {
            super(fieldSource, FieldInfo.RESET, modelFormField);
        }

        public ResetField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer) throws IOException {
            formStringRenderer.renderResetField(writer, context, this);
        }
    }

    public static class HiddenField extends FieldInfo {
        protected FlexibleStringExpander value;

        protected HiddenField() {
            super();
        }

        public HiddenField(ModelFormField modelFormField) {
            super(FieldInfo.SOURCE_EXPLICIT, FieldInfo.HIDDEN, modelFormField);
        }

        public HiddenField(int fieldSource, ModelFormField modelFormField) {
            super(fieldSource, FieldInfo.HIDDEN, modelFormField);
        }

        public HiddenField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
            this.setValue(element.getAttribute("value"));
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer) throws IOException {
            formStringRenderer.renderHiddenField(writer, context, this);
        }

        public String getValue(Map<String, Object> context) {
            if (this.value != null && !this.value.isEmpty()) {
                String valueEnc = this.value.expandString(context);
                StringUtil.SimpleEncoder simpleEncoder = (StringUtil.SimpleEncoder) context.get("simpleEncoder");
                if (simpleEncoder != null) {
                    valueEnc = simpleEncoder.encode(valueEnc);
                }
                return valueEnc;
            } else {
                return modelFormField.getEntry(context);
            }
        }

        public void setValue(String string) {
            this.value = FlexibleStringExpander.getInstance(string);
        }
    }

    public static class IgnoredField extends FieldInfo {
        protected IgnoredField() {
            super();
        }

        public IgnoredField(ModelFormField modelFormField) {
            super(FieldInfo.SOURCE_EXPLICIT, FieldInfo.IGNORED, modelFormField);
        }

        public IgnoredField(int fieldSource, ModelFormField modelFormField) {
            super(fieldSource, FieldInfo.IGNORED, modelFormField);
        }

        public IgnoredField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer) throws IOException {
            formStringRenderer.renderIgnoredField(writer, context, this);
        }
    }

    public static class TextFindField extends TextField {
        protected boolean ignoreCase = true;
        protected boolean hideIgnoreCase = false;
        protected String defaultOption = "like";
        protected boolean hideOptions = false;

        public TextFindField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
            this.ignoreCase = "true".equals(element.getAttribute("ignore-case"));
            this.hideIgnoreCase = "true".equals(element.getAttribute("hide-options")) ||
                "ignore-case".equals(element.getAttribute("hide-options")) ? true : false;
            if(element.hasAttribute("default-option")) {
                this.defaultOption = element.getAttribute("default-option");
            } else {
                this.defaultOption = UtilProperties.getPropertyValue("widget", "widget.form.defaultTextFindOption", "like");
            }
            this.hideOptions = "true".equals(element.getAttribute("hide-options")) ||
                "options".equals(element.getAttribute("hide-options")) ? true : false;
        }

        public TextFindField(int fieldSource, ModelFormField modelFormField) {
            super(fieldSource, modelFormField);
        }

        public boolean getIgnoreCase() {
            return this.ignoreCase;
        }

        public String getDefaultOption() {
            return this.defaultOption;
        }

        public boolean getHideIgnoreCase() {
            return this.hideIgnoreCase;
        }

        public boolean getHideOptions() {
            return this.hideOptions;
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer) throws IOException {
            formStringRenderer.renderTextFindField(writer, context, this);
        }
    }

    public static class DateFindField extends DateTimeField {
        protected String defaultOptionFrom = "greaterThanEqualTo";
        protected String defaultOptionThru = "lessThanEqualTo";

        public DateFindField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
            this.defaultOptionFrom = element.getAttribute("default-option-from");
            this.defaultOptionThru = element.getAttribute("default-option-thru");
        }

        public DateFindField(int fieldSource, ModelFormField modelFormField) {
            super(fieldSource, modelFormField);
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer) throws IOException {
            formStringRenderer.renderDateFindField(writer, context, this);
        }

        public String getDefaultOptionFrom() {
            return this.defaultOptionFrom;
        }

        public String getDefaultOptionThru() {
            return this.defaultOptionThru;
        }
    }

    public static class RangeFindField extends TextField {
        protected String defaultOptionFrom = "greaterThanEqualTo";
        protected String defaultOptionThru = "lessThanEqualTo";

        public RangeFindField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
            this.defaultOptionFrom = element.getAttribute("default-option-from");
            this.defaultOptionThru = element.getAttribute("default-option-thru");
        }

        public RangeFindField(int fieldSource, ModelFormField modelFormField) {
            super(fieldSource, modelFormField);
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer) throws IOException {
            formStringRenderer.renderRangeFindField(writer, context, this);
        }

        public String getDefaultOptionFrom() {
            return this.defaultOptionFrom;
        }

        public String getDefaultOptionThru() {
            return this.defaultOptionThru;
        }
    }

    public static class LookupField extends TextField {
        protected FlexibleStringExpander formName;
        protected String descriptionFieldName;
        protected String targetParameter;
        protected SubHyperlink subHyperlink;
        protected String lookupPresentation;
        protected String lookupWidth;
        protected String lookupHeight;
        protected String lookupPosition;
        protected String fadeBackground;

        public LookupField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
            this.formName = FlexibleStringExpander.getInstance(element.getAttribute("target-form-name"));
            this.descriptionFieldName = element.getAttribute("description-field-name");
            this.targetParameter = element.getAttribute("target-parameter");
            this.lookupPresentation = element.getAttribute("presentation");
            this.lookupHeight = element.getAttribute("height");
            this.lookupWidth = element.getAttribute("width");
            this.lookupPosition = element.getAttribute("position");
            this.fadeBackground = element.getAttribute("fade-background");

            Element subHyperlinkElement = UtilXml.firstChildElement(element, "sub-hyperlink");
            if (subHyperlinkElement != null) {
                this.subHyperlink = new SubHyperlink(subHyperlinkElement, this.getModelFormField());
            }
        }

        public LookupField(int fieldSource, ModelFormField modelFormField) {
            super(fieldSource, modelFormField);
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer) throws IOException {
            formStringRenderer.renderLookupField(writer, context, this);
        }

        public String getFormName(Map<String, Object> context) {
            return this.formName.expandString(context);
        }

        public List<String> getTargetParameterList() {
            List<String> paramList = FastList.newInstance();
            if (UtilValidate.isNotEmpty(this.targetParameter)) {
                StringTokenizer stk = new StringTokenizer(this.targetParameter, ", ");
                while (stk.hasMoreTokens()) {
                    paramList.add(stk.nextToken());
                }
            }
            return paramList;
        }

        public void setFormName(String str) {
            this.formName = FlexibleStringExpander.getInstance(str);
        }

        public String getDescriptionFieldName() {
            return this.descriptionFieldName;
        }

        public void setDescriptionFieldName(String str) {
            this.descriptionFieldName = str;
        }

        @Override
        public SubHyperlink getSubHyperlink() {
            return this.subHyperlink;
        }

        public String getLookupPresentation() {
            return this.lookupPresentation;
        }

        public void setLookupPresentation(String str) {
            this.lookupPresentation = str;
        }

        public String getLookupWidth() {
            return this.lookupWidth;
        }

        public void setLookupWidth(String str) {
            this.lookupWidth = str;
        }

        public String getLookupHeight() {
            return this.lookupHeight;
        }

        public void setLookupHeight(String str) {
            this.lookupHeight = str;
        }

        public String getLookupPosition() {
            return this.lookupPosition;
        }

        public void setLookupPosition(String str) {
            this.lookupPosition = str;
        }

        public String getFadeBackground() {
            return this.fadeBackground;
        }

        public void setFadeBackground(String str) {
            this.fadeBackground = str;
        }
    }

    public static class FileField extends TextField {

        public FileField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
        }

        public FileField(int fieldSource, ModelFormField modelFormField) {
            super(fieldSource, modelFormField);
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer) throws IOException {
            formStringRenderer.renderFileField(writer, context, this);
        }
    }

    public static class PasswordField extends TextField {

        public PasswordField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
        }

        public PasswordField(int fieldSource, ModelFormField modelFormField) {
            super(fieldSource, modelFormField);
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer) throws IOException {
            formStringRenderer.renderPasswordField(writer, context, this);
        }
    }

    public static class ImageField extends FieldInfo {
        protected int border = 0;
        protected Integer width;
        protected Integer height;
        protected FlexibleStringExpander defaultValue;
        protected FlexibleStringExpander value;
        protected SubHyperlink subHyperlink;
        protected FlexibleStringExpander description;
        protected FlexibleStringExpander alternate;

        protected ImageField() {
            super();
        }

        public ImageField(ModelFormField modelFormField) {
            super(FieldInfo.SOURCE_EXPLICIT, FieldInfo.IMAGE, modelFormField);
        }

        public ImageField(int fieldSource, ModelFormField modelFormField) {
            super(fieldSource, FieldInfo.IMAGE, modelFormField);
        }

        public ImageField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
            this.setValue(element.getAttribute("value"));
            this.setDescription(element.getAttribute("description"));
            this.setAlternate(element.getAttribute("alternate"));

            String borderStr = element.getAttribute("border");
            try {
                border = Integer.parseInt(borderStr);
            } catch (Exception e) {
                if (UtilValidate.isNotEmpty(borderStr)) {
                    Debug.logError("Could not parse the border value of the text element: [" + borderStr + "], setting to the default of " + border, module);
                }
            }

            String widthStr = element.getAttribute("width");
            try {
                width = Integer.valueOf(widthStr);
            } catch (Exception e) {
                width = null;
                if (UtilValidate.isNotEmpty(widthStr)) {
                    Debug.logError(
                        "Could not parse the size value of the text element: [" + widthStr + "], setting to null; default of no width will be used",
                        module);
                }
            }

            String heightStr = element.getAttribute("height");
            try {
                height = Integer.valueOf(heightStr);
            } catch (Exception e) {
                height = null;
                if (UtilValidate.isNotEmpty(heightStr)) {
                    Debug.logError(
                        "Could not parse the size value of the text element: [" + heightStr + "], setting to null; default of no height will be used",
                        module);
                }
            }

            Element subHyperlinkElement = UtilXml.firstChildElement(element, "sub-hyperlink");
            if (subHyperlinkElement != null) {
                this.subHyperlink = new SubHyperlink(subHyperlinkElement, this.getModelFormField());
            }
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer) throws IOException {
            formStringRenderer.renderImageField(writer, context, this);
        }


        /**
         * @param str
         */
        public void setDefaultValue(String str) {
            this.defaultValue = FlexibleStringExpander.getInstance(str);
        }

        public SubHyperlink getSubHyperlink() {
            return this.subHyperlink;
        }
        public void setSubHyperlink(SubHyperlink newSubHyperlink) {
            this.subHyperlink = newSubHyperlink;
        }
        public Integer getWidth() {
            return width;
        }
        public Integer getHeight() {
            return height;
        }

        public int getBorder() {
            return border;
        }

        public String getDefaultValue(Map<String, Object> context) {
            if (this.defaultValue != null) {
                return this.defaultValue.expandString(context);
            } else {
                return "";
            }
        }

        public String getValue(Map<String, Object> context) {
            if (this.value != null && !this.value.isEmpty()) {
                return this.value.expandString(context);
            } else {
                return modelFormField.getEntry(context);
            }
        }

        public void setValue(String string) {
            this.value = FlexibleStringExpander.getInstance(string);
        }

        public String getDescription(Map<String, Object> context) {
            if (this.description != null && !this.description.isEmpty()) {
                return this.description.expandString(context);
            } else {
                return "";
            }
        }

        public void setDescription(String description) {
            this.description = FlexibleStringExpander.getInstance(description);
        }

        public String getAlternate(Map<String, Object> context) {
            if (this.alternate != null && !this.alternate.isEmpty()) {
                return this.alternate.expandString(context);
            } else {
                return "";
            }
        }

        public void setAlternate(String alternate) {
            this.alternate = FlexibleStringExpander.getInstance(alternate);
        }

    }

    public static class ContainerField extends FieldInfo {
        protected String id;

        public ContainerField() {
            super();
            // TODO Auto-generated constructor stub
        }

        public ContainerField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
            // TODO Auto-generated constructor stub
            this.setId(modelFormField.getIdName());
        }

        public ContainerField(int fieldSource, int fieldType,
                ModelFormField modelFormField) {
            super(fieldSource, fieldType, modelFormField);
            // TODO Auto-generated constructor stub
        }

        public void renderFieldString(Appendable writer,
                Map<String, Object> context,
                FormStringRenderer formStringRenderer) throws IOException {
            // TODO Auto-generated method stub
            formStringRenderer.renderContainerFindField(writer, context, this);
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }
}
