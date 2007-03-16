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

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import javolution.util.FastList;

import org.ofbiz.base.util.BshUtil;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.collections.MapStack;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.finder.EntityFinderUtil;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelField;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelParam;
import org.ofbiz.service.ModelService;
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
    protected FlexibleMapAccessor mapAcsr;
    protected String entityName;
    protected String serviceName;
    protected FlexibleMapAccessor entryAcsr;
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
    protected Integer position = null;
    protected String redWhen;
    protected String event;
    protected FlexibleStringExpander action;
    protected FlexibleStringExpander useWhen;

    protected FieldInfo fieldInfo = null;
    protected String idName;
    protected boolean separateColumn = false;
    protected boolean requiredField = false;
    protected String headerLink;
    protected String headerLinkStyle;

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
        this.redWhen = fieldElement.getAttribute("red-when");
        this.event = fieldElement.getAttribute("event");
        this.setAction(fieldElement.hasAttribute("action")? fieldElement.getAttribute("action"): null);
        this.setUseWhen(fieldElement.getAttribute("use-when"));
        this.idName = fieldElement.getAttribute("id-name");
        String sepColumns = fieldElement.getAttribute("separate-column");
        if (sepColumns != null && sepColumns.equalsIgnoreCase("true"))
            separateColumn = true;
        this.requiredField = "true".equals(fieldElement.getAttribute("required-field"));
        this.headerLink = fieldElement.getAttribute("header-link");
        this.headerLinkStyle = fieldElement.getAttribute("header-link-style");


        String positionStr = fieldElement.getAttribute("position");
        try {
            if (positionStr != null && positionStr.length() > 0) {
                position = Integer.valueOf(positionStr);
            }
        } catch (Exception e) {
            Debug.logError(
                e,
                "Could not convert position attribute of the field element to an integer: [" + positionStr + "], using the default of the form renderer",
                module);
        }

        // get sub-element and set fieldInfo
        Element subElement = UtilXml.firstChildElement(fieldElement);
        if (subElement != null) {
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
            } else {
                throw new IllegalArgumentException("The field sub-element with name " + subElementName + " is not supported");
            }
        }
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
        if (overrideFormField.title != null && !overrideFormField.title.isEmpty())
            this.title = overrideFormField.title;
        if (overrideFormField.tooltip != null && !overrideFormField.tooltip.isEmpty())
            this.tooltip = overrideFormField.tooltip;

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
        if (UtilValidate.isNotEmpty(overrideFormField.idName))
            this.idName = overrideFormField.idName;
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
        LocalDispatcher dispatcher = this.getModelForm().getDispacher();
        try {
            ModelService modelService = dispatcher.getDispatchContext().getModelService(this.getServiceName());
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
        GenericDelegator delegator = this.getModelForm().getDelegator();
        ModelEntity modelEntity = delegator.getModelEntity(this.getEntityName());
        if (modelEntity != null) {
            ModelField modelField = modelEntity.getField(this.getFieldName());
            if (modelField != null) {
                // okay, populate using the entity field info...
                this.induceFieldInfoFromEntityField(modelEntity, modelField, defaultFieldType);
                return true;
            }
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
                textField.setMaxlength(new Integer(20));
                this.setFieldInfo(textField);
            } else if ("id-long".equals(modelField.getType()) || "id-long-ne".equals(modelField.getType())) {
                ModelFormField.TextFindField textField = new ModelFormField.TextFindField(ModelFormField.FieldInfo.SOURCE_AUTO_ENTITY, this);
                textField.setSize(40);
                textField.setMaxlength(new Integer(60));
                this.setFieldInfo(textField);
            } else if ("id-vlong".equals(modelField.getType()) || "id-vlong-ne".equals(modelField.getType())) {
                ModelFormField.TextFindField textField = new ModelFormField.TextFindField(ModelFormField.FieldInfo.SOURCE_AUTO_ENTITY, this);
                textField.setSize(60);
                textField.setMaxlength(new Integer(250));
                this.setFieldInfo(textField);
            } else if ("very-short".equals(modelField.getType())) {
                ModelFormField.TextField textField = new ModelFormField.TextField(ModelFormField.FieldInfo.SOURCE_AUTO_ENTITY, this);
                textField.setSize(6);
                textField.setMaxlength(new Integer(10));
                this.setFieldInfo(textField);
            } else if ("name".equals(modelField.getType()) || "short-varchar".equals(modelField.getType())) {
                ModelFormField.TextFindField textField = new ModelFormField.TextFindField(ModelFormField.FieldInfo.SOURCE_AUTO_ENTITY, this);
                textField.setSize(40);
                textField.setMaxlength(new Integer(60));
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
                textField.setMaxlength(new Integer(250));
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
                textField.setMaxlength(new Integer(20));
                this.setFieldInfo(textField);
            } else if ("id-long".equals(modelField.getType()) || "id-long-ne".equals(modelField.getType())) {
                ModelFormField.TextField textField = new ModelFormField.TextField(ModelFormField.FieldInfo.SOURCE_AUTO_ENTITY, this);
                textField.setSize(40);
                textField.setMaxlength(new Integer(60));
                this.setFieldInfo(textField);
            } else if ("id-vlong".equals(modelField.getType()) || "id-vlong-ne".equals(modelField.getType())) {
                ModelFormField.TextField textField = new ModelFormField.TextField(ModelFormField.FieldInfo.SOURCE_AUTO_ENTITY, this);
                textField.setSize(60);
                textField.setMaxlength(new Integer(250));
                this.setFieldInfo(textField);
            } else if ("indicator".equals(modelField.getType())) {
                ModelFormField.DropDownField dropDownField = new ModelFormField.DropDownField(ModelFormField.FieldInfo.SOURCE_AUTO_ENTITY, this);
                dropDownField.setAllowEmpty(false);
                dropDownField.addOptionSource(new ModelFormField.SingleOption("Y", null, dropDownField));
                dropDownField.addOptionSource(new ModelFormField.SingleOption("N", null, dropDownField));
                this.setFieldInfo(dropDownField);
                //ModelFormField.TextField textField = new ModelFormField.TextField(ModelFormField.FieldInfo.SOURCE_AUTO_ENTITY, this);
                //textField.setSize(1);
                //textField.setMaxlength(new Integer(1));
                //this.setFieldInfo(textField);
            } else if ("very-short".equals(modelField.getType())) {
                ModelFormField.TextField textField = new ModelFormField.TextField(ModelFormField.FieldInfo.SOURCE_AUTO_ENTITY, this);
                textField.setSize(6);
                textField.setMaxlength(new Integer(10));
                this.setFieldInfo(textField);
            } else if ("very-long".equals(modelField.getType())) {
                ModelFormField.TextareaField textareaField = new ModelFormField.TextareaField(ModelFormField.FieldInfo.SOURCE_AUTO_ENTITY, this);
                textareaField.setCols(60);
                textareaField.setRows(2);
                this.setFieldInfo(textareaField);
            } else if ("name".equals(modelField.getType()) || "short-varchar".equals(modelField.getType())) {
                ModelFormField.TextField textField = new ModelFormField.TextField(ModelFormField.FieldInfo.SOURCE_AUTO_ENTITY, this);
                textField.setSize(40);
                textField.setMaxlength(new Integer(60));
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
                textField.setMaxlength(new Integer(250));
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

    public void renderFieldString(StringBuffer buffer, Map context, FormStringRenderer formStringRenderer) {
        this.fieldInfo.renderFieldString(buffer, context, formStringRenderer);
    }

    /**
     * @return
     */
    public FieldInfo getFieldInfo() {
        return fieldInfo;
    }

    /**
     * @return
     */
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

    /**
     * @return
     */
    public String getEntityName() {
        if (UtilValidate.isNotEmpty(this.entityName)) {
            return this.entityName;
        } else {
            return this.modelForm.getDefaultEntityName();
        }
    }

    /**
     * @return
     */
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
     * @return
     */
    public String getEntry(Map context) {
        return this.getEntry(context, "");
    }

    public String getEntry(Map context, String defaultValue) {
        Boolean isError = (Boolean) context.get("isError");
        Boolean useRequestParameters = (Boolean) context.get("useRequestParameters");
        
        Locale locale = (Locale) context.get("locale");
        if (locale == null) locale = Locale.getDefault();
        
        // if useRequestParameters is TRUE then parameters will always be used, if FALSE then parameters will never be used
        // if isError is TRUE and useRequestParameters is not FALSE (ie is null or TRUE) then parameters will be used
        if ((Boolean.TRUE.equals(isError) && !Boolean.FALSE.equals(useRequestParameters)) || (Boolean.TRUE.equals(useRequestParameters))) {
            //Debug.logInfo("Getting entry, isError true so getting from parameters for field " + this.getName() + " of form " + this.modelForm.getName(), module);
            Map parameters = (Map) context.get("parameters");
            if (parameters != null && parameters.get(this.getParameterName(context)) != null) {
                return (String) parameters.get(this.getParameterName(context));
            } else {
                return defaultValue;
            }
        } else {
            //Debug.logInfo("Getting entry, isError false so getting from Map in context for field " + this.getName() + " of form " + this.modelForm.getName(), module);
            Map dataMap = this.getMap(context);
            boolean dataMapIsContext = false;
            if (dataMap == null) {
                //Debug.logInfo("Getting entry, no Map found with name " + this.getMapName() + ", using context for field " + this.getName() + " of form " + this.modelForm.getName(), module);
                dataMap = context;
                dataMapIsContext = true;
            }
            Object retVal = null;
            if (this.entryAcsr != null && !this.entryAcsr.isEmpty()) {
                //Debug.logInfo("Getting entry, using entryAcsr for field " + this.getName() + " of form " + this.modelForm.getName(), module);
                retVal = this.entryAcsr.get(dataMap);
            } else {
                //Debug.logInfo("Getting entry, no entryAcsr so using field name " + this.name + " for field " + this.getName() + " of form " + this.modelForm.getName(), module);
                // if no entry name was specified, use the field's name
                retVal = dataMap.get(this.name);
            }
            
            // this is a special case to fill in fields during a create by default from parameters passed in
            if (dataMapIsContext && retVal == null && !Boolean.FALSE.equals(useRequestParameters)) {
                Map parameters = (Map) context.get("parameters");
                if (parameters != null) {
                    if (this.entryAcsr != null && !this.entryAcsr.isEmpty()) {
                        retVal = this.entryAcsr.get(parameters);
                    } else {
                        retVal = parameters.get(this.name);
                    }
                }
            }

            if (retVal != null) {
                // format number based on the user's locale
                if (retVal instanceof Double || retVal instanceof Float || retVal instanceof BigDecimal) {
                    NumberFormat nf = NumberFormat.getInstance(locale);
                    nf.setMaximumFractionDigits(10);
                    return nf.format(retVal);
                } else {
                    return retVal.toString();
                }
            } else {
                return defaultValue;
            }
        }
    }

    public Map getMap(Map context) {
        if (this.mapAcsr == null || this.mapAcsr.isEmpty()) {
            //Debug.logInfo("Getting Map from default of the form because of no mapAcsr for field " + this.getName(), module);
            return this.modelForm.getDefaultMap(context);
        } else {
            //Debug.logInfo("Getting Map from mapAcsr for field " + this.getName(), module);
            return (Map) mapAcsr.get(context);
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

    /**
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Get the name to use for the parameter for this field in the form interpreter.
     * For HTML forms this is the request parameter name.
     *
     * @return
     */
    public String getParameterName(Map context) {
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

    /**
     * @return
     */
    public int getPosition() {
        if (this.position == null) {
            return 1;
        } else {
            return position.intValue();
        }
    }

    /**
     * @return
     */
    public String getRedWhen() {
        return redWhen;
    }


    /**
     * @return
     */
    public String getEvent() {
        return event;
    }

    /**
     * @return
     */
    public String getAction(Map context) {
        if (this.action != null && this.action.getOriginal() != null) {
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
    public boolean shouldBeRed(Map context) {
        // red-when ( never | before-now | after-now | by-name ) "by-name"

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
        String value = this.getEntry(context);
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

    /**
     * @return
     */
    public String getServiceName() {
        if (UtilValidate.isNotEmpty(this.serviceName)) {
            return this.serviceName;
        } else {
            return this.modelForm.getDefaultServiceName();
        }
    }

    /**
     * @return
     */
    public String getTitle(Map context) {
        if (this.title != null && this.title.getOriginal() != null) {
            return title.expandString(context);
        } else {
            // create a title from the name of this field; expecting a Java method/field style name, ie productName or productCategoryId
            if (this.name == null || this.name.length() == 0) {
                // this should never happen, ie name is required
                return "";
            }
            
            // search for a localized label for the field's name
            Map uiLabelMap = (Map) context.get("uiLabelMap");
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
            StringBuffer autoTitleBuffer = new StringBuffer();

            // always use upper case first letter...
            autoTitleBuffer.append(Character.toUpperCase(this.name.charAt(0)));

            // just put spaces before the upper case letters
            for (int i = 1; i < this.name.length(); i++) {
                char curChar = this.name.charAt(i);
                if (Character.isUpperCase(curChar)) {
                    autoTitleBuffer.append(' ');
                }
                autoTitleBuffer.append(curChar);
            }

            return autoTitleBuffer.toString();
        }
    }

    /**
     * @return
     */
    public String getTitleAreaStyle() {
        if (UtilValidate.isNotEmpty(this.titleAreaStyle)) {
            return this.titleAreaStyle;
        } else {
            return this.modelForm.getDefaultTitleAreaStyle();
        }
    }

    /**
     * @return
     */
    public String getTitleStyle() {
        if (UtilValidate.isNotEmpty(this.titleStyle)) {
            return this.titleStyle;
        } else {
            return this.modelForm.getDefaultTitleStyle();
        }
    }

    /**
     * @return
     */
    public String getRequiredFieldStyle() {
        if (UtilValidate.isNotEmpty(this.requiredFieldStyle)) {
            return this.requiredFieldStyle;
        } else {
            return this.modelForm.getDefaultRequiredFieldStyle();
        }
    }

    /**
     * @return
     */
    public String getTooltip(Map context) {
        if (tooltip != null && !tooltip.isEmpty()) {
            return tooltip.expandString(context);
        } else {
            return "";
        }
    }

    /**
     * @return
     */
    public String getUseWhen(Map context) {
        if (useWhen != null && !useWhen.isEmpty()) {
            return useWhen.expandString(context);
        } else {
            return "";
        }
    }

    /**
     * @return
     */
    public String getIdName() {
        return idName;
    }
    
    /**
     * @return
     */
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

    public boolean shouldUse(Map context) {
        String useWhenStr = this.getUseWhen(context);
        if (UtilValidate.isEmpty(useWhenStr)) {
            return true;
        } else {
            try {
                Interpreter bsh = this.modelForm.getBshInterpreter(context);
                Object retVal = bsh.eval(useWhenStr);
                boolean condTrue = false;
                // retVal should be a Boolean, if not something weird is up...
                if (retVal instanceof Boolean) {
                    Boolean boolVal = (Boolean) retVal;
                    condTrue = boolVal.booleanValue();
                } else {
                    throw new IllegalArgumentException("Return value from use-when condition eval was not a Boolean: "
                            + retVal.getClass().getName() + " [" + retVal + "] on the field " + this.name + " of form " + this.modelForm.name);
                }

                return condTrue;
            } catch (EvalError e) {
                String errMsg = "Error evaluating BeanShell use-when condition [" + useWhenStr + "] on the field "
                        + this.name + " of form " + this.modelForm.name + ": " + e.toString();
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

    /**
     * @return
     */
    public String getWidgetAreaStyle() {
        if (UtilValidate.isNotEmpty(this.widgetAreaStyle)) {
            return this.widgetAreaStyle;
        } else {
            return this.modelForm.getDefaultWidgetAreaStyle();
        }
    }

    /**
     * @return
     */
    public String getWidgetStyle() {
        if (UtilValidate.isNotEmpty(this.widgetStyle)) {
            return this.widgetStyle;
        } else {
            return this.modelForm.getDefaultWidgetStyle();
        }
    }

    /**
     * @return
     */
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
        entryAcsr = new FlexibleMapAccessor(string);
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
        this.mapAcsr = new FlexibleMapAccessor(string);
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
        position = new Integer(i);
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
        this.action = new FlexibleStringExpander(string);
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
        this.title = new FlexibleStringExpander(string);
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
        this.tooltip = new FlexibleStringExpander(string);
    }

    /**
     * @param string
     */
    public void setUseWhen(String string) {
        this.useWhen = new FlexibleStringExpander(string);
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

    /**
     * @return
     */
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
    
    
    /**
     * @return
     */
    public boolean getRequiredField() {
        return this.requiredField;
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

        public static Map fieldTypeByName = new HashMap();

        static {
            fieldTypeByName.put("display", new Integer(1));
            fieldTypeByName.put("hyperlink", new Integer(2));
            fieldTypeByName.put("text", new Integer(3));
            fieldTypeByName.put("textarea", new Integer(4));
            fieldTypeByName.put("date-time", new Integer(5));
            fieldTypeByName.put("drop-down", new Integer(6));
            fieldTypeByName.put("check", new Integer(7));
            fieldTypeByName.put("radio", new Integer(8));
            fieldTypeByName.put("submit", new Integer(9));
            fieldTypeByName.put("reset", new Integer(10));
            fieldTypeByName.put("hidden", new Integer(11));
            fieldTypeByName.put("ignored", new Integer(12));
            fieldTypeByName.put("text-find", new Integer(13));
            fieldTypeByName.put("date-find", new Integer(14));
            fieldTypeByName.put("range-find", new Integer(15));
            fieldTypeByName.put("lookup", new Integer(16));
            fieldTypeByName.put("file", new Integer(17));
            fieldTypeByName.put("password", new Integer(18));
            fieldTypeByName.put("image", new Integer(19));
            fieldTypeByName.put("display-entity", new Integer(20));
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

        /**
         * @return
         */
        public ModelFormField getModelFormField() {
            return modelFormField;
        }

        /**
         * @return
         */
        public int getFieldType() {
            return fieldType;
        }

        /**
         * @return
         */
        public int getFieldSource() {
            return this.fieldSource;
        }

        public static int findFieldTypeFromName(String name) {
            Integer fieldTypeInt = (Integer) FieldInfo.fieldTypeByName.get(name);
            if (fieldTypeInt != null) {
                return fieldTypeInt.intValue();
            } else {
                throw new IllegalArgumentException("Could not get fieldType for field type name " + name);
            }
        }

        public abstract void renderFieldString(StringBuffer buffer, Map context, FormStringRenderer formStringRenderer);
    }

    public static abstract class FieldInfoWithOptions extends FieldInfo {
        protected FieldInfoWithOptions() {
            super();
        }

        protected FlexibleStringExpander noCurrentSelectedKey = null;
        protected List optionSources = new LinkedList();

        public FieldInfoWithOptions(int fieldSource, int fieldType, ModelFormField modelFormField) {
            super(fieldSource, fieldType, modelFormField);
        }

        public FieldInfoWithOptions(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);

            noCurrentSelectedKey = new FlexibleStringExpander(element.getAttribute("no-current-selected-key"));

            // read all option and entity-options sub-elements, maintaining order
            List childElements = UtilXml.childElementList(element);
            if (childElements.size() > 0) {
                Iterator childElementIter = childElements.iterator();
                while (childElementIter.hasNext()) {
                    Element childElement = (Element) childElementIter.next();
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

        public List getAllOptionValues(Map context, GenericDelegator delegator) {
            List optionValues = new LinkedList();
            Iterator optionSourceIter = this.optionSources.iterator();
            while (optionSourceIter.hasNext()) {
                OptionSource optionSource = (OptionSource) optionSourceIter.next();
                optionSource.addOptionValues(optionValues, context, delegator);
            }
            return optionValues;
        }

        public static String getDescriptionForOptionKey(String key, List allOptionValues) {
            if (UtilValidate.isEmpty(key)) {
                return "";
            }

            if (UtilValidate.isEmpty(allOptionValues)) {
                return key;
            }

            Iterator optionValueIter = allOptionValues.iterator();
            while (optionValueIter.hasNext()) {
                OptionValue optionValue = (OptionValue) optionValueIter.next();
                if (key.equals(optionValue.getKey())) {
                    return optionValue.getDescription();
                }
            }

            // if we get here we didn't find a match, just return the key
            return key;
        }

        public String getNoCurrentSelectedKey(Map context) {
            if (this.noCurrentSelectedKey == null) {
                return null;
            }
            return this.noCurrentSelectedKey.expandString(context);
        }

        public void setNoCurrentSelectedKey(String string) {            
            this.noCurrentSelectedKey = new FlexibleStringExpander(string);
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

        public abstract void addOptionValues(List optionValues, Map context, GenericDelegator delegator);
    }

    public static class SingleOption extends OptionSource {
        protected FlexibleStringExpander key;
        protected FlexibleStringExpander description;

        public SingleOption(String key, String description, FieldInfo fieldInfo) {
            this.key = new FlexibleStringExpander(key);
            this.description = new FlexibleStringExpander(UtilXml.checkEmpty(description, key));
            this.fieldInfo = fieldInfo;
        }

        public SingleOption(Element optionElement, FieldInfo fieldInfo) {
            this.key = new FlexibleStringExpander(optionElement.getAttribute("key"));
            this.description = new FlexibleStringExpander(UtilXml.checkEmpty(optionElement.getAttribute("description"), optionElement.getAttribute("key")));
            this.fieldInfo = fieldInfo;
        }

        public void addOptionValues(List optionValues, Map context, GenericDelegator delegator) {
            optionValues.add(new OptionValue(key.expandString(context), description.expandString(context)));
        }
    }

    public static class ListOptions extends OptionSource {
        protected FlexibleMapAccessor listAcsr;
        protected String listEntryName;
        protected FlexibleMapAccessor keyAcsr;
        protected FlexibleStringExpander description;

        public ListOptions(String listName, String listEntryName, String keyName, String description, FieldInfo fieldInfo) {
            this.listAcsr = new FlexibleMapAccessor(listName);
            this.listEntryName = listEntryName;
            this.keyAcsr = new FlexibleMapAccessor(keyName);
            this.description = new FlexibleStringExpander(description);
            this.fieldInfo = fieldInfo;
        }

        public ListOptions(Element optionElement, FieldInfo fieldInfo) {
            this.listEntryName = optionElement.getAttribute("list-entry-name");
            this.listAcsr = new FlexibleMapAccessor(optionElement.getAttribute("list-name"));
            this.keyAcsr = new FlexibleMapAccessor(optionElement.getAttribute("key-name"));
            this.listAcsr = new FlexibleMapAccessor(optionElement.getAttribute("list-name"));
            this.listEntryName = optionElement.getAttribute("list-entry-name");
            this.description = new FlexibleStringExpander(optionElement.getAttribute("description"));
            this.fieldInfo = fieldInfo;
        }

        public void addOptionValues(List optionValues, Map context, GenericDelegator delegator) {
            List dataList = (List) this.listAcsr.get(context);
            if (dataList != null && dataList.size() != 0) {
                Iterator dataIter = dataList.iterator();
                while (dataIter.hasNext()) {
                    Object data = dataIter.next();
                    Map localContext = new HashMap(context);
                    if (UtilValidate.isNotEmpty(this.listEntryName)) {
                        localContext.put(this.listEntryName, data);
                    } else {
                        localContext.putAll((Map) data);
                    }
                    optionValues.add(new OptionValue((String) keyAcsr.get(localContext), description.expandString(localContext)));
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

        protected List constraintList = null;
        protected List orderByList = null;

        public EntityOptions(FieldInfo fieldInfo) {
            this.fieldInfo = fieldInfo;
        }

        public EntityOptions(Element entityOptionsElement, FieldInfo fieldInfo) {
            this.entityName = entityOptionsElement.getAttribute("entity-name");
            this.keyFieldName = entityOptionsElement.getAttribute("key-field-name");
            this.description = new FlexibleStringExpander(entityOptionsElement.getAttribute("description"));
            this.cache = !"false".equals(entityOptionsElement.getAttribute("cache"));
            this.filterByDate = entityOptionsElement.getAttribute("filter-by-date");

            List constraintElements = UtilXml.childElementList(entityOptionsElement, "entity-constraint");
            if (constraintElements != null && constraintElements.size() > 0) {
                this.constraintList = new LinkedList();
                Iterator constraintElementIter = constraintElements.iterator();
                while (constraintElementIter.hasNext()) {
                    Element constraintElement = (Element) constraintElementIter.next();
                    constraintList.add(new EntityFinderUtil.ConditionExpr(constraintElement));
                }
            }

            List orderByElements = UtilXml.childElementList(entityOptionsElement, "entity-order-by");
            if (orderByElements != null && orderByElements.size() > 0) {
                this.orderByList = new LinkedList();
                Iterator orderByElementIter = orderByElements.iterator();
                while (orderByElementIter.hasNext()) {
                    Element orderByElement = (Element) orderByElementIter.next();
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

        public void addOptionValues(List optionValues, Map context, GenericDelegator delegator) {
            // first expand any conditions that need expanding based on the current context
            EntityCondition findCondition = null;
            if (this.constraintList != null && this.constraintList.size() > 0) {
                List expandedConditionList = new LinkedList();
                Iterator constraintIter = constraintList.iterator();
                while (constraintIter.hasNext()) {
                    EntityFinderUtil.Condition condition = (EntityFinderUtil.Condition) constraintIter.next();
                    expandedConditionList.add(condition.createCondition(context, this.entityName, delegator));
                }
                findCondition = new EntityConditionList(expandedConditionList, EntityOperator.AND);
            }
            
            try {
                Locale locale = UtilMisc.ensureLocale(context.get("locale"));
                
                List values = null;
                if (this.cache) {
                    values = delegator.findByConditionCache(this.entityName, findCondition, null, this.orderByList);
                } else {
                    values = delegator.findByCondition(this.entityName, findCondition, null, this.orderByList);
                }

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

                Iterator valueIter = values.iterator();
                while (valueIter.hasNext()) {
                    GenericValue value = (GenericValue) valueIter.next();
                    // add key and description with string expansion, ie expanding ${} stuff, passing locale explicitly to expand value string because it won't be found in the Entity
                    MapStack localContext = null;
                    if (context instanceof MapStack) {
                        localContext = ((MapStack) context).standAloneStack();
                    } else {
                        localContext = MapStack.create(context);
                    }
                    localContext.push(value);

                    // expand with the new localContext, which is locale aware
                    String optionDesc = this.description.expandString(localContext, locale);
                    
                    Object keyFieldObject = value.get(this.getKeyFieldName());
                    if (keyFieldObject == null) {
                        throw new IllegalArgumentException("The value found for key-name [" + this.getKeyFieldName() + "], may not be a valid key field name.");
                    }
                    String keyFieldValue = keyFieldObject.toString();
                    optionValues.add(new OptionValue(keyFieldValue, optionDesc));
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error getting entity options in form", module);
            }
        }
    }

    public static class DisplayField extends FieldInfo {
        protected boolean alsoHidden = true;
        protected FlexibleStringExpander description;
        protected String type;  // matches type of field, currently text or currency
        protected FlexibleStringExpander currency;

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
            this.setCurrency(element.getAttribute("currency"));
            this.setDescription(element.getAttribute("description"));
            this.alsoHidden = !"false".equals(element.getAttribute("also-hidden"));
        }

        public void renderFieldString(StringBuffer buffer, Map context, FormStringRenderer formStringRenderer) {
            formStringRenderer.renderDisplayField(buffer, context, this);
        }

        /**
         * @return
         */
        public boolean getAlsoHidden() {
            return alsoHidden;
        }

        /**
         * @return
         */
        public String getDescription(Map context) {
            String retVal = null;
            if (this.description != null && !this.description.isEmpty()) {
                retVal = this.description.expandString(context);
            } else {
                retVal = modelFormField.getEntry(context);
            }
            if (retVal == null || retVal.length() == 0) {
                retVal = "";
            } else if ("currency".equals(type)) { 
                Locale locale = (Locale) context.get("locale");
                if (locale == null) locale = Locale.getDefault();
                String isoCode = null;
                if (this.currency != null && !this.currency.isEmpty()) {
                    isoCode = this.currency.expandString(context);
                }
                try {
                    Double parsedRetVal = (Double) ObjectType.simpleTypeConvert(retVal, "Double", null, locale, false);
                    retVal = UtilFormatOut.formatCurrency(parsedRetVal, isoCode, locale);
                } catch (GeneralException e) {
                    String errMsg = "Error formatting currency value [" + retVal + "]: " + e.toString();
                    Debug.logError(e, errMsg, module);
                    throw new IllegalArgumentException(errMsg);
                }
            } 
            return retVal;
        }

        /**
         * @param b
         */
        public void setAlsoHidden(boolean b) {
            alsoHidden = b;
        }

        /**
         * @param string
         */
        public void setDescription(String string) {
            description = new FlexibleStringExpander(string);
        }
        
        /**
         * @param string
         */
        public void setCurrency(String string) {
            currency = new FlexibleStringExpander(string);
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
                this.subHyperlink = new SubHyperlink(subHyperlinkElement);
            }
        }

        /**
         * @return
         */
        public String getDescription(Map context) {
            Locale locale = UtilMisc.ensureLocale(context.get("locale"));
            
            // rather than using the context to expand the string, lookup the given entity and use it to expand the string
            GenericValue value = null;
            String fieldKey = this.keyFieldName;
            if (UtilValidate.isEmpty(fieldKey)) {
                fieldKey = this.modelFormField.fieldName;
            }
            GenericDelegator delegator = this.modelFormField.modelForm.getDelegator();
            String fieldValue = modelFormField.getEntry(context);
            try {
                if (this.cache) {
                    value = delegator.findByPrimaryKeyCache(this.entityName, UtilMisc.toMap(fieldKey, fieldValue));
                } else {
                    value = delegator.findByPrimaryKey(this.entityName, UtilMisc.toMap(fieldKey, fieldValue));
                }
            } catch (GenericEntityException e) {
                String errMsg = "Error getting value from the database for display of field [" + this.modelFormField.getName() + "] on form [" + this.modelFormField.modelForm.getName() + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new IllegalArgumentException(errMsg);
            }
            
            String retVal = null;
            if (value != null) {
                retVal = this.description.expandString(value, locale);
            }
            // try to get the entry for the field if description doesn't expand to anything
            if (retVal == null || retVal.length() == 0) {
                retVal = fieldValue;
            }
            if (retVal == null || retVal.length() == 0) {
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
        protected String targetType;
        protected FlexibleStringExpander target;
        protected FlexibleStringExpander description;
        protected FlexibleStringExpander targetWindowExdr;

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
            this.setTarget(element.getAttribute("target"));
            this.alsoHidden = !"false".equals(element.getAttribute("also-hidden"));
            this.targetType = element.getAttribute("target-type");
            this.targetWindowExdr = new FlexibleStringExpander(element.getAttribute("target-window"));
        }

        public void renderFieldString(StringBuffer buffer, Map context, FormStringRenderer formStringRenderer) {
            formStringRenderer.renderHyperlinkField(buffer, context, this);
        }

        /**
         * @return
         */
        public boolean getAlsoHidden() {
            return this.alsoHidden;
        }

        /**
         * @return
         */
        public String getTargetType() {
            if (UtilValidate.isNotEmpty(this.targetType)) {
                return this.targetType;
            } else {
                return HyperlinkField.DEFAULT_TARGET_TYPE;
            }
        }

        public String getTargetWindow(Map context) {
            String targetWindow = this.targetWindowExdr.expandString(context);
            return targetWindow;
        }
        
        /**
         * @return
         */
        public String getDescription(Map context) {
            return this.description.expandString(context);
        }

        /**
         * @return
         */
        public String getTarget(Map context) {
            return this.target.expandString(context);
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
            this.description = new FlexibleStringExpander(string);
        }

        /**
         * @param string
         */
        public void setTarget(String string) {
            this.target = new FlexibleStringExpander(string);
        }
    }

    public static class SubHyperlink {
        protected FlexibleStringExpander useWhen;
        protected String linkStyle;
        protected String targetType;
        protected FlexibleStringExpander target;
        protected FlexibleStringExpander description;
        protected FlexibleStringExpander targetWindowExdr;

        public SubHyperlink(Element element) {
            this.setDescription(element.getAttribute("description"));
            this.setTarget(element.getAttribute("target"));
            this.setUseWhen(element.getAttribute("use-when"));
            this.linkStyle = element.getAttribute("link-style");
            this.targetType = element.getAttribute("target-type");
            this.targetWindowExdr = new FlexibleStringExpander(element.getAttribute("target-window"));
        }

        /**
         * @return
         */
        public String getLinkStyle() {
            return this.linkStyle;
        }

        /**
         * @return
         */
        public String getTargetType() {
            if (UtilValidate.isNotEmpty(this.targetType)) {
                return this.targetType;
            } else {
                return HyperlinkField.DEFAULT_TARGET_TYPE;
            }
        }

        /**
         * @return
         */
        public String getDescription(Map context) {
            if (this.description != null) {
                return this.description.expandString(context);
            } else {
                return "";
            }
        }

        public String getTargetWindow(Map context) {
            String targetWindow = this.targetWindowExdr.expandString(context);
            return targetWindow;
        }
        
        /**
         * @return
         */
        public String getTarget(Map context) {
            if (this.target != null) {
                return this.target.expandString(context);
            } else {
                return "";
            }
        }

        /**
         * @return
         */
        public String getUseWhen(Map context) {
            if (this.useWhen != null) {
                return this.useWhen.expandString(context);
            } else {
                return "";
            }
        }

        public boolean shouldUse(Map context) {
            boolean shouldUse = true;
            String useWhen = this.getUseWhen(context);
            if (UtilValidate.isNotEmpty(useWhen)) {
                try {
                    Interpreter bsh = (Interpreter) context.get("bshInterpreter");
                    if (bsh == null) {
                        bsh = BshUtil.makeInterpreter(context);
                        context.put("bshInterpreter", bsh);
                    }

                    Object retVal = bsh.eval(useWhen);

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
            this.description = new FlexibleStringExpander(string);
        }

        /**
         * @param string
         */
        public void setTarget(String string) {
            this.target = new FlexibleStringExpander(string);
        }

        /**
         * @param string
         */
        public void setUseWhen(String string) {
            this.useWhen = new FlexibleStringExpander(string);
        }
    }

    public static class TextField extends FieldInfo {
        protected int size = 25;
        protected Integer maxlength;
        protected FlexibleStringExpander defaultValue;
        protected SubHyperlink subHyperlink;
        protected boolean disabled;

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
                if (sizeStr != null && sizeStr.length() > 0) {
                    Debug.logError("Could not parse the size value of the text element: [" + sizeStr + "], setting to the default of " + size, module);
                }
            }

            String maxlengthStr = element.getAttribute("maxlength");
            try {
                maxlength = Integer.valueOf(maxlengthStr);
            } catch (Exception e) {
                maxlength = null;
                if (maxlengthStr != null && maxlengthStr.length() > 0) {
                    Debug.logError("Could not parse the max-length value of the text element: [" + maxlengthStr + "], setting to null; default of no maxlength will be used", module);
                }
            }
            
            this.disabled = "true".equals(element.getAttribute("disabled"));

            Element subHyperlinkElement = UtilXml.firstChildElement(element, "sub-hyperlink");
            if (subHyperlinkElement != null) {
                this.subHyperlink = new SubHyperlink(subHyperlinkElement);
            }
        }

        public void renderFieldString(StringBuffer buffer, Map context, FormStringRenderer formStringRenderer) {
            formStringRenderer.renderTextField(buffer, context, this);
        }

        /**
         * @return
         */
        public Integer getMaxlength() {
            return maxlength;
        }

        /**
         * @return
         */
        public int getSize() {
            return size;
        }
        
        /**
         * @return
         */
        public boolean getDisabled() {
            return this.disabled;
        }
        
        public void setDisabled(boolean b) {
            this.disabled = b;   
        }


        /**
         * @return
         */
        public String getDefaultValue(Map context) {
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
            this.defaultValue = new FlexibleStringExpander(str);
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
            visualEditorButtons = new FlexibleStringExpander(element.getAttribute("visual-editor-buttons"));

            String colsStr = element.getAttribute("cols");
            try {
                cols = Integer.parseInt(colsStr);
            } catch (Exception e) {
                if (colsStr != null && colsStr.length() > 0) {
                    Debug.logError("Could not parse the size value of the text element: [" + colsStr + "], setting to default of " + cols, module);
                }
            }

            String rowsStr = element.getAttribute("rows");
            try {
                rows = Integer.parseInt(rowsStr);
            } catch (Exception e) {
                if (rowsStr != null && rowsStr.length() > 0) {
                    Debug.logError("Could not parse the size value of the text element: [" + rowsStr + "], setting to default of " + rows, module);
                }
            }
        }

        public void renderFieldString(StringBuffer buffer, Map context, FormStringRenderer formStringRenderer) {
            formStringRenderer.renderTextareaField(buffer, context, this);
        }

        /**
         * @return
         */
        public int getCols() {
            return cols;
        }

        /**
         * @return
         */
        public int getRows() {
            return rows;
        }

        /**
         * @return
         */
        public String getDefaultValue(Map context) {
            if (this.defaultValue != null) {
                return this.defaultValue.expandString(context);
            } else {
                return "";
            }
        }

        /**
         * @return
         */
        public boolean getVisualEditorEnable() {
            return this.visualEditorEnable;
        }

        /**
         * @return
         */
        public String getVisualEditorButtons(Map context) {
            return this.visualEditorButtons.expandString(context);
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
            this.defaultValue = new FlexibleStringExpander(str);
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
            this.visualEditorButtons = new FlexibleStringExpander(eb);
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

        public void renderFieldString(StringBuffer buffer, Map context, FormStringRenderer formStringRenderer) {
            formStringRenderer.renderDateTimeField(buffer, context, this);
        }

        /**
         * @return
         */
        public String getType() {
            return type;
        }

        /**
         * @return
         */
        public String getDefaultValue(Map context) {
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
            this.defaultValue = new FlexibleStringExpander(str);
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
        public String getDefaultDateTimeString(Map context) {
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
            this.currentDescription = new FlexibleStringExpander(element.getAttribute("current-description"));

            // set the default size
            if (size == null) {
                size = "1";
            }

            String sizeStr = element.getAttribute("other-field-size");
            try {
                this.otherFieldSize = Integer.parseInt(sizeStr);
            } catch (Exception e) {
                if (sizeStr != null && sizeStr.length() > 0) {
                    Debug.logError("Could not parse the size value of the text element: [" + sizeStr + "], setting to the default of " + this.otherFieldSize, module);
                }
            }

            Element subHyperlinkElement = UtilXml.firstChildElement(element, "sub-hyperlink");
            if (subHyperlinkElement != null) {
                this.subHyperlink = new SubHyperlink(subHyperlinkElement);
            }
        }

        public void renderFieldString(StringBuffer buffer, Map context, FormStringRenderer formStringRenderer) {
            formStringRenderer.renderDropDownField(buffer, context, this);
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

        public String getCurrentDescription(Map context) {
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
            this.currentDescription = new FlexibleStringExpander(string);
        }

        public SubHyperlink getSubHyperlink() {
            return this.subHyperlink;
        }
        public void setSubHyperlink(SubHyperlink newSubHyperlink) {
            this.subHyperlink = newSubHyperlink;
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
        public String getParameterNameOther(Map context) {
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

        public void renderFieldString(StringBuffer buffer, Map context, FormStringRenderer formStringRenderer) {
            formStringRenderer.renderRadioField(buffer, context, this);
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
            allChecked = new FlexibleStringExpander(element.getAttribute("all-checked"));
        }

        public void renderFieldString(StringBuffer buffer, Map context, FormStringRenderer formStringRenderer) {
            formStringRenderer.renderCheckField(buffer, context, this);
        }
        
        public Boolean isAllChecked(Map context) {
            String allCheckedStr = this.allChecked.expandString(context);
            if (UtilValidate.isNotEmpty(allCheckedStr)) {
                return new Boolean("true".equals(allCheckedStr));
            } else {
                return null;
            }
        }
    }

    public static class SubmitField extends FieldInfo {
        protected String buttonType;
        protected String imageLocation;

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
        }

        public void renderFieldString(StringBuffer buffer, Map context, FormStringRenderer formStringRenderer) {
            formStringRenderer.renderSubmitField(buffer, context, this);
        }

        /**
         * @return
         */
        public String getButtonType() {
            return buttonType;
        }

        /**
         * @return
         */
        public String getImageLocation() {
            return imageLocation;
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

        public void renderFieldString(StringBuffer buffer, Map context, FormStringRenderer formStringRenderer) {
            formStringRenderer.renderResetField(buffer, context, this);
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

        public void renderFieldString(StringBuffer buffer, Map context, FormStringRenderer formStringRenderer) {
            formStringRenderer.renderHiddenField(buffer, context, this);
        }

        public String getValue(Map context) {
            if (this.value != null && !this.value.isEmpty()) {
                return this.value.expandString(context);
            } else {
                return modelFormField.getEntry(context);
            }
        }

        public void setValue(String string) {
            this.value = new FlexibleStringExpander(string);
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

        public void renderFieldString(StringBuffer buffer, Map context, FormStringRenderer formStringRenderer) {
            formStringRenderer.renderIgnoredField(buffer, context, this);
        }
    }

    public static class TextFindField extends TextField {
        protected boolean ignoreCase = true;
        protected String defaultOption = "like";

        public TextFindField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
            this.ignoreCase = "true".equals(element.getAttribute("ignore-case"));
            this.defaultOption = element.getAttribute("default-option");
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

        public void renderFieldString(StringBuffer buffer, Map context, FormStringRenderer formStringRenderer) {
            formStringRenderer.renderTextFindField(buffer, context, this);
        }
    }

    public static class DateFindField extends DateTimeField {
        public DateFindField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
        }

        public DateFindField(int fieldSource, ModelFormField modelFormField) {
            super(fieldSource, modelFormField);
        }

        public void renderFieldString(StringBuffer buffer, Map context, FormStringRenderer formStringRenderer) {
            formStringRenderer.renderDateFindField(buffer, context, this);
        }
    }

    public static class RangeFindField extends TextField {
        public RangeFindField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
        }

        public RangeFindField(int fieldSource, ModelFormField modelFormField) {
            super(fieldSource, modelFormField);
        }

        public void renderFieldString(StringBuffer buffer, Map context, FormStringRenderer formStringRenderer) {
            formStringRenderer.renderRangeFindField(buffer, context, this);
        }
    }

    public static class LookupField extends TextField {
        protected FlexibleStringExpander formName;
        protected String descriptionFieldName;
        protected String targetParameter;
        
        public LookupField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
            this.formName = new FlexibleStringExpander(element.getAttribute("target-form-name"));
            this.descriptionFieldName = element.getAttribute("description-field-name");
            this.targetParameter = element.getAttribute("target-parameter");
        }

        public LookupField(int fieldSource, ModelFormField modelFormField) {
            super(fieldSource, modelFormField);
        }

        public void renderFieldString(StringBuffer buffer, Map context, FormStringRenderer formStringRenderer) {
            formStringRenderer.renderLookupField(buffer, context, this);
        }

        public String getFormName(Map context) {
            return this.formName.expandString(context);
        }

        public List getTargetParameterList() {
            List paramList = FastList.newInstance();
            if (UtilValidate.isNotEmpty(this.targetParameter)) {
                StringTokenizer stk = new StringTokenizer(this.targetParameter, ", ");
                while (stk.hasMoreTokens()) {
                    paramList.add(stk.nextToken());
                }
            }
            return paramList;
        }

        public void setFormName(String str) {
            this.formName = new FlexibleStringExpander(str);
        }
        
        public String getDescriptionFieldName() {
            return this.descriptionFieldName;
        }

        public void setDescriptionFieldName(String str) {
            this.descriptionFieldName = str;
        }
    }

    public static class FileField extends TextField {

        public FileField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
        }

        public FileField(int fieldSource, ModelFormField modelFormField) {
            super(fieldSource, modelFormField);
        }

        public void renderFieldString(StringBuffer buffer, Map context, FormStringRenderer formStringRenderer) {
            formStringRenderer.renderFileField(buffer, context, this);
        }
    }

    public static class PasswordField extends TextField {

        public PasswordField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
        }

        public PasswordField(int fieldSource, ModelFormField modelFormField) {
            super(fieldSource, modelFormField);
        }

        public void renderFieldString(StringBuffer buffer, Map context, FormStringRenderer formStringRenderer) {
            formStringRenderer.renderPasswordField(buffer, context, this);
        }
    }

    public static class ImageField extends FieldInfo {
        protected int border = 0;
        protected Integer width;
        protected Integer height;
        protected FlexibleStringExpander defaultValue;
        protected FlexibleStringExpander value;
        protected SubHyperlink subHyperlink;

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

            String borderStr = element.getAttribute("border");
            try {
                border = Integer.parseInt(borderStr);
            } catch (Exception e) {
                if (borderStr != null && borderStr.length() > 0) {
                    Debug.logError("Could not parse the border value of the text element: [" + borderStr + "], setting to the default of " + border, module);
                }
            }

            String widthStr = element.getAttribute("width");
            try {
                width = Integer.valueOf(widthStr);
            } catch (Exception e) {
                width = null;
                if (widthStr != null && widthStr.length() > 0) {
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
                if (heightStr != null && heightStr.length() > 0) {
                    Debug.logError(
                        "Could not parse the size value of the text element: [" + heightStr + "], setting to null; default of no height will be used",
                        module);
                }
            }

            Element subHyperlinkElement = UtilXml.firstChildElement(element, "sub-hyperlink");
            if (subHyperlinkElement != null) {
                this.subHyperlink = new SubHyperlink(subHyperlinkElement);
            }
        }

        public void renderFieldString(StringBuffer buffer, Map context, FormStringRenderer formStringRenderer) {
            formStringRenderer.renderImageField(buffer, context, this);
        }


        /**
         * @param str
         */
        public void setDefaultValue(String str) {
            this.defaultValue = new FlexibleStringExpander(str);
        }

        public SubHyperlink getSubHyperlink() {
            return this.subHyperlink;
        }
        public void setSubHyperlink(SubHyperlink newSubHyperlink) {
            this.subHyperlink = newSubHyperlink;
        }
        /**
         * @return
         */
        public Integer getWidth() {
            return width;
        }
        /**
         * @return
         */
        public Integer getHeight() {
            return height;
        }

        /**
         * @return
         */
        public int getBorder() {
            return border;
        }

        /**
         * @return
         */
        public String getDefaultValue(Map context) {
            if (this.defaultValue != null) {
                return this.defaultValue.expandString(context);
            } else {
                return "";
            }
        }

        public String getValue(Map context) {
            if (this.value != null && !this.value.isEmpty()) {
                return this.value.expandString(context);
            } else {
                return modelFormField.getEntry(context);
            }
        }

        public void setValue(String string) {
            this.value = new FlexibleStringExpander(string);
        }

    }
}
