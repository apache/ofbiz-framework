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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;

import javolution.util.FastList;

import bsh.EvalError;
import bsh.Interpreter;

import org.ofbiz.base.util.BshUtil;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelField;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelParam;
import org.ofbiz.service.ModelService;

import org.w3c.dom.Element;

/**
 * Widget Library - Form model class
 */
public class ModelForm {

    public static final String module = ModelForm.class.getName();
    public static final String DEFAULT_FORM_RESULT_LIST_NAME = "defaultFormResultList";

    protected GenericDelegator delegator;
    protected LocalDispatcher dispatcher;

    protected String name;
    protected String type;
    protected FlexibleStringExpander target;
    protected String targetType;
    protected String containerId;
    protected String containerStyle;
    protected String focusFieldName;
    protected String title;
    protected String tooltip;
    protected String listName;
    protected String listEntryName;
    protected FlexibleMapAccessor defaultMapName;
    protected String defaultEntityName;
    protected String defaultServiceName;
    protected String formTitleAreaStyle;
    protected String formWidgetAreaStyle;
    protected String defaultTitleAreaStyle;
    protected String defaultWidgetAreaStyle;
    protected String defaultTitleStyle;
    protected String defaultWidgetStyle;
    protected String defaultTooltipStyle;
    protected String itemIndexSeparator;
    protected FlexibleStringExpander paginateTarget;
    protected FlexibleStringExpander paginateIndexField;
    protected FlexibleStringExpander paginateSizeField;
    protected FlexibleStringExpander overrideListSize;
    protected FlexibleStringExpander paginatePreviousLabel;
    protected FlexibleStringExpander paginateNextLabel;
    protected String paginateTargetAnchor;
    protected String paginatePreviousStyle;
    protected String paginateNextStyle;
    protected boolean separateColumns = false;
    protected boolean paginate = true;
    protected boolean useRowSubmit = false;
    protected FlexibleStringExpander targetWindowExdr;
    protected String defaultRequiredFieldStyle;
    protected String oddRowStyle;
    protected String evenRowStyle;
    protected String defaultTableStyle;
    protected String headerRowStyle;
    protected boolean skipStart = false;
    protected boolean skipEnd = false;
    protected boolean hideHeader = false;
    protected boolean overridenListSize = false;

    protected List altTargets = new LinkedList();
    protected List autoFieldsServices = new LinkedList();
    protected List autoFieldsEntities = new LinkedList();
    protected List sortOrderFields = new LinkedList();

    /** This List will contain one copy of each field for each field name in the order
     * they were encountered in the service, entity, or form definition; field definitions
     * with constraints will also be in this list but may appear multiple times for the same
     * field name.
     *
     * When rendering the form the order in this list should be following and it should not be
     * necessary to use the Map. The Map is used when loading the form definition to keep the
     * list clean and implement the override features for field definitions.
     */
    protected List fieldList = new LinkedList();

    /** This Map is keyed with the field name and has a ModelFormField for the value; fields
     * with conditions will not be put in this Map so field definition overrides for fields
     * with conditions is not possible.
     */
    protected Map fieldMap = new HashMap();

    /** This is a list of FieldGroups in the order they were created.
     * Can also include Banner objects.
     */
    protected List fieldGroupList = new ArrayList();
    
    /** This Map is keyed with the field name and has a FieldGroup for the value.
     * Can also include Banner objects.
     */
    protected Map fieldGroupMap = new HashMap();
    
    /** This field group will be the "catch-all" group for fields that are not
     *  included in an explicit field-group.
     */
    protected FieldGroup defaultFieldGroup;
    
    /** Default hyperlink target. */
    public static String DEFAULT_TARGET_TYPE = "intra-app";

    /** Pagination settings and defaults. */
    public static int DEFAULT_PAGE_SIZE = 100;
    protected int defaultViewSize = DEFAULT_PAGE_SIZE;
    public static String DEFAULT_PAG_INDEX_FIELD = "viewIndex";
    public static String DEFAULT_PAG_SIZE_FIELD = "viewSize";
    public static String DEFAULT_PAG_PREV_LABEL = "Previous";
    public static String DEFAULT_PAG_NEXT_LABEL = "Next";
    public static String DEFAULT_PAG_PREV_STYLE = "buttontext";
    public static String DEFAULT_PAG_NEXT_STYLE = "buttontext";
    
    protected List actions;
    protected List rowActions;
    protected FlexibleStringExpander rowCountExdr;
    protected ModelFormField multiSubmitField;
    protected int rowCount = 0;

    // ===== CONSTRUCTORS =====
    /** Default Constructor */
    public ModelForm() {}

    /** XML Constructor */
    public ModelForm(Element formElement, GenericDelegator delegator, LocalDispatcher dispatcher) {
        this.delegator = delegator;
        this.dispatcher = dispatcher;
        initForm(formElement);
    }
    
    public ModelForm(Element formElement) {
        initForm(formElement);
    }
    
    public void initForm(Element formElement) {

        // check if there is a parent form to inherit from
        String parentResource = formElement.getAttribute("extends-resource");
        String parentForm = formElement.getAttribute("extends");
        if (parentForm.length() > 0) {
            ModelForm parent = null;
            // check if we have a resource name (part of the string before the ?)
            if (parentResource.length() > 0) {
                try {
                    parent = FormFactory.getFormFromLocation(parentResource, parentForm, delegator, dispatcher);
                } catch (Exception e) {
                    Debug.logError(e, "Failed to load parent form definition '" + parentForm + "' at resource '" + parentResource + "'", module);
                }
            } else if (!parentForm.equals(formElement.getAttribute("name"))) {
                // try to find a form definition in the same file
                Element rootElement = formElement.getOwnerDocument().getDocumentElement();
                List formElements = UtilXml.childElementList(rootElement, "form");
                //Uncomment below to add support for abstract forms
                //formElements.addAll(UtilXml.childElementList(rootElement, "abstract-form"));
                Iterator formElementIter = formElements.iterator();
                while (formElementIter.hasNext()) {
                    Element formElementEntry = (Element) formElementIter.next();
                    if (formElementEntry.getAttribute("name").equals(parentForm)) {
                        parent = new ModelForm(formElementEntry, delegator, dispatcher);
                        break;
                    }
                }
                if (parent == null) {
                    Debug.logError("Failed to find parent form definition '" + parentForm + "' in same document.", module);
                }
            } else {
                Debug.logError("Recursive form definition found for '" + formElement.getAttribute("name") + ".'", module);
            }

            if (parent != null) {
                this.type = parent.type;
                this.target = parent.target;
                this.containerId = parent.containerId;
                this.containerStyle = parent.containerStyle;
                this.focusFieldName = parent.focusFieldName;
                this.title = parent.title;
                this.tooltip = parent.tooltip;
                this.listName = parent.listName;
                this.listEntryName = parent.listEntryName;
                this.tooltip = parent.tooltip;
                this.defaultEntityName = parent.defaultEntityName;
                this.defaultServiceName = parent.defaultServiceName;
                this.formTitleAreaStyle = parent.formTitleAreaStyle;
                this.formWidgetAreaStyle = parent.formWidgetAreaStyle;
                this.defaultTitleAreaStyle = parent.defaultTitleAreaStyle;
                this.defaultWidgetAreaStyle = parent.defaultWidgetAreaStyle;
                this.oddRowStyle = parent.oddRowStyle;
                this.evenRowStyle = parent.evenRowStyle;
                this.defaultTableStyle = parent.defaultTableStyle;
                this.headerRowStyle = parent.headerRowStyle;
                this.defaultTitleStyle = parent.defaultTitleStyle;
                this.defaultWidgetStyle = parent.defaultWidgetStyle;
                this.defaultTooltipStyle = parent.defaultTooltipStyle;
                this.itemIndexSeparator = parent.itemIndexSeparator;
                //this.fieldList = parent.fieldList;
                //this.fieldMap = parent.fieldMap;
                this.separateColumns = parent.separateColumns;
                this.targetType = parent.targetType;
                this.defaultMapName = parent.defaultMapName;
                this.targetWindowExdr = parent.targetWindowExdr;
                this.hideHeader = parent.hideHeader;
                
                
                // Create this fieldList/Map from clones of parent's
                Iterator fieldListIter = parent.fieldList.iterator();
                while (fieldListIter.hasNext()) {
                    ModelFormField parentChildField = (ModelFormField)fieldListIter.next();
                    ModelFormField childField = new ModelFormField(this);
                    childField.mergeOverrideModelFormField(parentChildField);
                    this.fieldList.add(childField);
                    this.fieldMap.put(childField.getName(), childField);
                }
            }
        }

        this.name = formElement.getAttribute("name");
        if (this.type == null || formElement.hasAttribute("type")) {
            this.type = formElement.getAttribute("type");
        }
        if (this.target == null || formElement.hasAttribute("target")) {
            setTarget( formElement.getAttribute("target") );
        }
        if (this.targetWindowExdr == null || formElement.hasAttribute("target-window")) {
            setTargetWindow(formElement.getAttribute("target-window"));
        }
        if (this.containerId == null || formElement.hasAttribute("id")) {
            this.containerId = formElement.getAttribute("id");
        }
        if (this.containerStyle == null || formElement.hasAttribute("style")) {
            this.containerStyle = formElement.getAttribute("style");
        }
        if (this.focusFieldName == null || formElement.hasAttribute("focus-field-name")) {
            this.focusFieldName = formElement.getAttribute("focus-field-name");
        }
        if (this.title == null || formElement.hasAttribute("title")) {
            this.title = formElement.getAttribute("title");
        }
        if (this.tooltip == null || formElement.hasAttribute("tooltip")) {
            this.tooltip = formElement.getAttribute("tooltip");
        }
        if (this.listName == null || formElement.hasAttribute("list-name")) {
            this.listName = formElement.getAttribute("list-name");
        }
        // if no list-name then look in the list-iterator-name; this is deprecated but we'll look at it anyway
        if (UtilValidate.isEmpty(this.listName) && formElement.hasAttribute("list-iterator-name")) {
            this.listName = formElement.getAttribute("list-iterator-name");
        }
        if (this.listEntryName == null || formElement.hasAttribute("list-entry-name")) {
            this.listEntryName = formElement.getAttribute("list-entry-name");
        }
        if (this.defaultMapName == null || formElement.hasAttribute("default-map-name")) {
            this.setDefaultMapName(formElement.getAttribute("default-map-name"));
        }
        if (this.defaultServiceName == null || formElement.hasAttribute("default-service-name")) {
            this.defaultServiceName = formElement.getAttribute("default-service-name");
        }
        if (this.defaultEntityName == null || formElement.hasAttribute("default-entity-name")) {
            this.defaultEntityName = formElement.getAttribute("default-entity-name");
        }

        if (this.formTitleAreaStyle == null || formElement.hasAttribute("form-title-area-style")) {
            this.formTitleAreaStyle = formElement.getAttribute("form-title-area-style");
        }
        if (this.formWidgetAreaStyle == null || formElement.hasAttribute("form-widget-area-style")) {
            this.formWidgetAreaStyle = formElement.getAttribute("form-widget-area-style");
        }

        if (this.defaultTitleAreaStyle == null || formElement.hasAttribute("default-title-area-style")) {
            this.defaultTitleAreaStyle = formElement.getAttribute("default-title-area-style");
        }
        if (this.defaultWidgetAreaStyle == null || formElement.hasAttribute("default-widget-area-style")) {
            this.defaultWidgetAreaStyle = formElement.getAttribute("default-widget-area-style");
        }
        if (this.oddRowStyle == null || formElement.hasAttribute("odd-row-style")) {
            this.oddRowStyle = formElement.getAttribute("odd-row-style");
        }
        if (this.evenRowStyle == null || formElement.hasAttribute("even-row-style")) {
            this.evenRowStyle = formElement.getAttribute("even-row-style");
        }
        if (this.defaultTableStyle == null || formElement.hasAttribute("default-table-style")) {
            this.defaultTableStyle = formElement.getAttribute("default-table-style");
        }
        if (this.headerRowStyle == null || formElement.hasAttribute("header-row-style")) {
            this.headerRowStyle = formElement.getAttribute("header-row-style");
        }
        if (this.defaultTitleStyle == null || formElement.hasAttribute("header-row-style")) {
            this.defaultTitleStyle = formElement.getAttribute("default-title-style");
        }
        if (this.defaultWidgetStyle == null || formElement.hasAttribute("default-widget-style")) {
            this.defaultWidgetStyle = formElement.getAttribute("default-widget-style");
        }
        if (this.defaultTooltipStyle == null || formElement.hasAttribute("default-tooltip-style")) {
            this.defaultTooltipStyle = formElement.getAttribute("default-tooltip-style");
        }
        if (this.itemIndexSeparator == null || formElement.hasAttribute("item-index-separator")) {
            this.itemIndexSeparator = formElement.getAttribute("item-index-separator");
        }
        if (this.targetType == null || formElement.hasAttribute("target-type")) {
            this.targetType = formElement.getAttribute("target-type");
        }
        if (this.defaultRequiredFieldStyle == null || formElement.hasAttribute("default-required-field-style")) {
            this.defaultRequiredFieldStyle = formElement.getAttribute("default-required-field-style");
        }

        // pagination settings
        if (this.paginateTarget == null || formElement.hasAttribute("paginate-target")) {
            setPaginateTarget(formElement.getAttribute("paginate-target"));
        }
        if (this.paginateTargetAnchor == null || formElement.hasAttribute("paginate-target-anchor")) {
            this.paginateTargetAnchor = formElement.getAttribute("paginate-target-anchor");
        }
        if (this.paginateIndexField == null || formElement.hasAttribute("paginate-index-field")) {
            setPaginateIndexField(formElement.getAttribute("paginate-index-field"));
        }
        if (this.paginateSizeField == null || formElement.hasAttribute("paginate-size-field")) {
            setPaginateSizeField(formElement.getAttribute("paginate-size-field"));
        }
        if (this.overrideListSize == null || formElement.hasAttribute("override-list-size")) {
            this.overrideListSize = new FlexibleStringExpander(formElement.getAttribute("override-list-size"));
        }
        if (this.paginatePreviousLabel == null || formElement.hasAttribute("paginate-previous-label")) {
            this.paginatePreviousLabel = new FlexibleStringExpander(formElement.getAttribute("paginate-previous-label"));
        }
        if (this.paginateNextLabel == null || formElement.hasAttribute("paginate-next-label")) {
            this.paginateNextLabel = new FlexibleStringExpander(formElement.getAttribute("paginate-next-label"));
        }
        if (this.paginatePreviousStyle == null || formElement.hasAttribute("paginate-previous-style")) {
            setPaginatePreviousStyle(formElement.getAttribute("paginate-previous-style"));
        }
        if (this.paginateNextStyle == null || formElement.hasAttribute("paginate-next-style")) {
            setPaginateNextStyle(formElement.getAttribute("paginate-next-style"));
        }
        
        this.paginate = "true".equals(formElement.getAttribute("paginate"));
        this.skipStart = "true".equals(formElement.getAttribute("skip-start"));
        this.skipEnd = "true".equals(formElement.getAttribute("skip-end"));
        this.hideHeader = "true".equals(formElement.getAttribute("hide-header"));
        if (formElement.hasAttribute("separate-columns")) {
            String sepColumns = formElement.getAttribute("separate-columns");
            if (sepColumns != null && sepColumns.equalsIgnoreCase("true"))
                separateColumns = true;
        }
        if (formElement.hasAttribute("use-row-submit")) {
            String rowSubmit = formElement.getAttribute("use-row-submit");
            if (rowSubmit != null && rowSubmit.equalsIgnoreCase("true"))
                useRowSubmit = true;
        }
        if (formElement.hasAttribute("view-size")) {
            setDefaultViewSize(formElement.getAttribute("view-size"));
        }
        if (this.rowCountExdr == null || formElement.hasAttribute("row-count")) {
            this.rowCountExdr = new FlexibleStringExpander(formElement.getAttribute("row-count"));
        }

        // alt-target
        List altTargetElements = UtilXml.childElementList(formElement, "alt-target");
        Iterator altTargetElementIter = altTargetElements.iterator();
        while (altTargetElementIter.hasNext()) {
            Element altTargetElement = (Element) altTargetElementIter.next();
            AltTarget altTarget = new AltTarget(altTargetElement);
            this.addAltTarget(altTarget);
        }
            
        // auto-fields-service
        List autoFieldsServiceElements = UtilXml.childElementList(formElement, "auto-fields-service");
        Iterator autoFieldsServiceElementIter = autoFieldsServiceElements.iterator();
        while (autoFieldsServiceElementIter.hasNext()) {
            Element autoFieldsServiceElement = (Element) autoFieldsServiceElementIter.next();
            AutoFieldsService autoFieldsService = new AutoFieldsService(autoFieldsServiceElement);
            this.addAutoFieldsFromService(autoFieldsService, dispatcher);
        }

        // auto-fields-entity
        List autoFieldsEntityElements = UtilXml.childElementList(formElement, "auto-fields-entity");
        Iterator autoFieldsEntityElementIter = autoFieldsEntityElements.iterator();
        while (autoFieldsEntityElementIter.hasNext()) {
            Element autoFieldsEntityElement = (Element) autoFieldsEntityElementIter.next();
            AutoFieldsEntity autoFieldsEntity = new AutoFieldsEntity(autoFieldsEntityElement);
            this.addAutoFieldsFromEntity(autoFieldsEntity, delegator);
        }

        // read in add field defs, add/override one by one using the fieldList and fieldMap
        List fieldElements = UtilXml.childElementList(formElement, "field");
        Iterator fieldElementIter = fieldElements.iterator();
        String thisType = this.getType();
        while (fieldElementIter.hasNext()) {
            Element fieldElement = (Element) fieldElementIter.next();
            ModelFormField modelFormField = new ModelFormField(fieldElement, this);
            ModelFormField.FieldInfo fieldInfo = modelFormField.getFieldInfo();
            if (thisType.equals("multi") && fieldInfo instanceof ModelFormField.SubmitField) {
               multiSubmitField = modelFormField; 
            } else {
                modelFormField = this.addUpdateField(modelFormField);
            }
            //Debug.logInfo("Added field " + modelFormField.getName() + " from def, mapName=" + modelFormField.getMapName(), module);
        }

        // Create the default field group
        defaultFieldGroup = new FieldGroup(null, this);
        // get the sort-order
        Element sortOrderElement = UtilXml.firstChildElement(formElement, "sort-order");
        if (sortOrderElement != null) {
            FieldGroup lastFieldGroup = new FieldGroup(null, this);
            this.fieldGroupList.add(lastFieldGroup);
            // read in sort-field
            List sortFieldElements = UtilXml.childElementList(sortOrderElement);
            Iterator sortFieldElementIter = sortFieldElements.iterator();
            while (sortFieldElementIter.hasNext()) {
                Element sortFieldElement = (Element) sortFieldElementIter.next();
                String tagName = sortFieldElement.getTagName();
                if (tagName.equals("sort-field")) {
                    String fieldName = sortFieldElement.getAttribute("name");
                    this.sortOrderFields.add(fieldName );
                    this.fieldGroupMap.put(fieldName, lastFieldGroup);
                } else if (tagName.equals("banner")) {
                    Banner thisBanner = new Banner(sortFieldElement, this);
                    this.fieldGroupList.add(thisBanner);
                    
                    lastFieldGroup = new FieldGroup(null, this);
                    this.fieldGroupList.add(lastFieldGroup);
                } else if (tagName.equals("field-group")) {
                    FieldGroup thisFieldGroup = new FieldGroup(sortFieldElement, this);
                    this.fieldGroupList.add(thisFieldGroup);
                    
                    lastFieldGroup = new FieldGroup(null, this);
                    this.fieldGroupList.add(lastFieldGroup);
                }
            }
        }

        // reorder fields according to sort order
        if (sortOrderFields.size() > 0) {
            List sortedFields = new ArrayList(this.fieldList.size());
            Iterator sortOrderFieldIter = this.sortOrderFields.iterator();
            while (sortOrderFieldIter.hasNext()) {
                String fieldName = (String) sortOrderFieldIter.next();
                if (UtilValidate.isEmpty(fieldName)) {
                    continue;
                }

                // get all fields with the given name from the existing list and put them in the sorted list
                Iterator fieldIter = this.fieldList.iterator();
                while (fieldIter.hasNext()) {
                    ModelFormField modelFormField = (ModelFormField) fieldIter.next();
                    if (fieldName.equals(modelFormField.getName())) {
                        // matched the name; remove from the original last and add to the sorted list
                        fieldIter.remove();
                        sortedFields.add(modelFormField);
                    }
                }
            }
            // now add all of the rest of the fields from fieldList, ie those that were not explicitly listed in the sort order
            sortedFields.addAll(this.fieldList);
            // sortedFields all done, set fieldList
            this.fieldList = sortedFields;
        }

        // read all actions under the "actions" element
        Element actionsElement = UtilXml.firstChildElement(formElement, "actions");
        if (actionsElement != null) {
            this.actions = ModelFormAction.readSubActions(this, actionsElement);
        }

        // read all actions under the "row-actions" element
        Element rowActionsElement = UtilXml.firstChildElement(formElement, "row-actions");
        if (rowActionsElement != null) {
            this.rowActions = ModelFormAction.readSubActions(this, rowActionsElement);
        }
    }

    /**
     * add/override modelFormField using the fieldList and fieldMap
     *
     * @return The same ModelFormField, or if merged with an existing field, the existing field.
     */
    public ModelFormField addUpdateField(ModelFormField modelFormField) {
        if (!modelFormField.isUseWhenEmpty()) {
            // is a conditional field, add to the List but don't worry about the Map
            //for adding to list, see if there is another field with that name in the list and if so, put it before that one
            boolean inserted = false;
            for (int i = 0; i < this.fieldList.size(); i++) {
                ModelFormField curField = (ModelFormField) this.fieldList.get(i);
                if (curField.getName() != null && curField.getName().equals(modelFormField.getName())) {
                    this.fieldList.add(i, modelFormField);
                    inserted = true;
                    break;
                }
            }
            if (!inserted) {
                this.fieldList.add(modelFormField);
            }
            return modelFormField;
        } else {

            // not a conditional field, see if a named field exists in Map
            ModelFormField existingField = (ModelFormField) this.fieldMap.get(modelFormField.getName());
            if (existingField != null) {
                // does exist, update the field by doing a merge/override
                existingField.mergeOverrideModelFormField(modelFormField);
                return existingField;
            } else {
                // does not exist, add to List and Map
                this.fieldList.add(modelFormField);
                this.fieldMap.put(modelFormField.getName(), modelFormField);
                return modelFormField;
            }
        }
    }

    public void addAltTarget(AltTarget altTarget) {
        altTargets.add(altTarget);
    }

    public void addAutoFieldsFromService(AutoFieldsService autoFieldsService, LocalDispatcher dispatcher) {
        autoFieldsServices.add(autoFieldsService);

        // read service def and auto-create fields
        ModelService modelService = null;
        try {
            modelService = dispatcher.getDispatchContext().getModelService(autoFieldsService.serviceName);
        } catch (GenericServiceException e) {
            String errmsg = "Error finding Service with name " + autoFieldsService.serviceName + " for auto-fields-service in a form widget";
            Debug.logError(e, errmsg, module);
            throw new IllegalArgumentException(errmsg);
        }

        List modelParams = modelService.getInModelParamList();
        Iterator modelParamIter = modelParams.iterator();
        while (modelParamIter.hasNext()) {
            ModelParam modelParam = (ModelParam) modelParamIter.next();
            // skip auto params that the service engine populates...
            if ("userLogin".equals(modelParam.name) || "locale".equals(modelParam.name)) {
                continue;
            }
            if (modelParam.formDisplay) {
                if (UtilValidate.isNotEmpty(modelParam.entityName) && UtilValidate.isNotEmpty(modelParam.fieldName)) {
                    ModelEntity modelEntity = delegator.getModelEntity(modelParam.entityName);
                    if (modelEntity != null) {
                        ModelField modelField = modelEntity.getField(modelParam.fieldName);
                        if (modelField != null) {
                            // okay, populate using the entity field info...
                            ModelFormField modelFormField = this.addFieldFromEntityField(modelEntity, modelField, autoFieldsService.defaultFieldType);
                            if (UtilValidate.isNotEmpty(autoFieldsService.mapName)) {
                                modelFormField.setMapName(autoFieldsService.mapName);
                            }

                            // continue to skip creating based on service param
                            continue;
                        }
                    }
                }

                ModelFormField modelFormField = this.addFieldFromServiceParam(modelService, modelParam, autoFieldsService.defaultFieldType);
                if (UtilValidate.isNotEmpty(autoFieldsService.mapName)) {
                    modelFormField.setMapName(autoFieldsService.mapName);
                }
            }
        }
    }

    public ModelFormField addFieldFromServiceParam(ModelService modelService, ModelParam modelParam, String defaultFieldType) {
        // create field def from service param def
        ModelFormField newFormField = new ModelFormField(this);
        newFormField.setName(modelParam.name);
        newFormField.setServiceName(modelService.name);
        newFormField.setAttributeName(modelParam.name);
        newFormField.setTitle(modelParam.formLabel);
        newFormField.induceFieldInfoFromServiceParam(modelService, modelParam, defaultFieldType);
        return this.addUpdateField(newFormField);
    }

    public void addAutoFieldsFromEntity(AutoFieldsEntity autoFieldsEntity, GenericDelegator delegator) {
        autoFieldsEntities.add(autoFieldsEntity);
        // read entity def and auto-create fields
        ModelEntity modelEntity = delegator.getModelEntity(autoFieldsEntity.entityName);
        if (modelEntity == null) {
            throw new IllegalArgumentException("Error finding Entity with name " + autoFieldsEntity.entityName + " for auto-fields-entity in a form widget");
        }

        Iterator modelFieldIter = modelEntity.getFieldsIterator();
        while (modelFieldIter.hasNext()) {
            ModelField modelField = (ModelField) modelFieldIter.next();
            if (modelField.getIsAutoCreatedInternal()) {
                // don't ever auto-add these, should only be added if explicitly referenced
                continue;
            }
            ModelFormField modelFormField = this.addFieldFromEntityField(modelEntity, modelField, autoFieldsEntity.defaultFieldType);
            if (UtilValidate.isNotEmpty(autoFieldsEntity.mapName)) {
                modelFormField.setMapName(autoFieldsEntity.mapName);
            }
        }
    }

    public ModelFormField addFieldFromEntityField(ModelEntity modelEntity, ModelField modelField, String defaultFieldType) {
        // create field def from entity field def
        ModelFormField newFormField = new ModelFormField(this);
        newFormField.setName(modelField.getName());
        newFormField.setEntityName(modelEntity.getEntityName());
        newFormField.setFieldName(modelField.getName());
        newFormField.induceFieldInfoFromEntityField(modelEntity, modelField, defaultFieldType);
        return this.addUpdateField(newFormField);
    }

    /**
     * Renders this form to a String, i.e. in a text format, as defined with the
     * FormStringRenderer implementation.
     *
     * @param buffer The StringBuffer that the form text will be written to
     * @param context Map containing the form context; the following are
     *   reserved words in this context: parameters (Map), isError (Boolean),
     *   itemIndex (Integer, for lists only, otherwise null), bshInterpreter,
     *   formName (String, optional alternate name for form, defaults to the
     *   value of the name attribute)
     * @param formStringRenderer An implementation of the FormStringRenderer
     *   interface that is responsible for the actual text generation for
     *   different form elements; implementing your own makes it possible to
     *   use the same form definitions for many types of form UIs
     */
    public void renderFormString(StringBuffer buffer, Map context, FormStringRenderer formStringRenderer) {
        ModelFormAction.runSubActions(this.actions, context);
        
        // if this is a list form, don't useRequestParameters
        if ("list".equals(this.type) || "multi".equals(this.type)) {
            context.put("useRequestParameters", Boolean.FALSE);
        }

        // find the highest position number to get the max positions used
        int positions = 1;
        Iterator fieldIter = this.fieldList.iterator();
        while (fieldIter.hasNext()) {
            ModelFormField modelFormField = (ModelFormField) fieldIter.next();
            int curPos = modelFormField.getPosition();
            if (curPos > positions) {
                positions = curPos;
            }
            ModelFormField.FieldInfo currentFieldInfo = modelFormField.getFieldInfo();
            if (currentFieldInfo != null) {
                ModelFormField fieldInfoFormField = currentFieldInfo.getModelFormField();
                if (fieldInfoFormField != null) {
                    fieldInfoFormField.setModelForm(this);
                }
            } else {
                throw new IllegalArgumentException("Error rendering form, a field has no FieldInfo, ie no sub-element for the type of field for field named: " + modelFormField.getName());
            }
       }

        if ("single".equals(this.type)) {
            this.renderSingleFormString(buffer, context, formStringRenderer, positions);
        } else if ("list".equals(this.type)) {
            this.renderListFormString(buffer, context, formStringRenderer, positions);
        } else if ("multi".equals(this.type)) {
            this.renderMultiFormString(buffer, context, formStringRenderer, positions);
        } else if ("upload".equals(this.type)) {
            this.renderSingleFormString(buffer, context, formStringRenderer, positions);
        } else {
            throw new IllegalArgumentException("The type " + this.getType() + " is not supported for form with name " + this.getName());
        }
    }

    public void renderSingleFormString(StringBuffer buffer, Map context, FormStringRenderer formStringRenderer, int positions) {
        List tempFieldList = FastList.newInstance();
        tempFieldList.addAll(this.fieldList);
        
        // Check to see if there is a field, same name and same use-when (could come from extended form)
        for (int j = 0; j < tempFieldList.size(); j++) {
            ModelFormField modelFormField = (ModelFormField) tempFieldList.get(j);
            if (!modelFormField.isUseWhenEmpty()) {
                boolean shouldUse1 = modelFormField.shouldUse(context);
                for (int i = j+1; i < tempFieldList.size(); i++) {
                    ModelFormField curField = (ModelFormField) tempFieldList.get(i);
                    if (curField.getName() != null && curField.getName().equals(modelFormField.getName())) {
                        boolean shouldUse2 = curField.shouldUse(context);
                        if (shouldUse1 == shouldUse2) {
                            tempFieldList.remove(i--);
                        }
                    } else {
                        continue;
                    }
                }
            }
        }

        Set alreadyRendered = new TreeSet();
        FieldGroup lastFieldGroup = null;
        // render form open
        if (!skipStart) formStringRenderer.renderFormOpen(buffer, context, this);

        // render all hidden & ignored fields
        this.renderHiddenIgnoredFields(buffer, context, formStringRenderer, alreadyRendered);

        // render formatting wrapper open
        // This should be covered by fieldGroup.renderStartString
        //formStringRenderer.renderFormatSingleWrapperOpen(buffer, context, this);

        // render each field row, except hidden & ignored rows
        Iterator fieldIter = tempFieldList.iterator();
        ModelFormField lastFormField = null;
        ModelFormField currentFormField = null;
        ModelFormField nextFormField = null;
        if (fieldIter.hasNext()) {
            currentFormField = (ModelFormField) fieldIter.next();
        }
        if (fieldIter.hasNext()) {
            nextFormField = (ModelFormField) fieldIter.next();
        }
        
        FieldGroup currentFieldGroup = null;
        String currentFieldGroupName = null;
        String lastFieldGroupName = null;
        if (currentFormField != null) {
            currentFieldGroup = (FieldGroup)fieldGroupMap.get(currentFormField.getFieldName()); 
            if (currentFieldGroup == null) {
                currentFieldGroup = defaultFieldGroup;
            }
            if (currentFieldGroup != null) {
                currentFieldGroupName = currentFieldGroup.getId();
            } 
        }
        
            
        boolean isFirstPass = true;
        boolean haveRenderedOpenFieldRow = false;
        while (currentFormField != null) {
            // do the check/get next stuff at the beginning so we can still use the continue stuff easily
            // don't do it on the first pass though...
            if (isFirstPass) {
                isFirstPass = false;
                List inbetweenList = getInbetweenList(lastFieldGroup, currentFieldGroup);
                Iterator iter = inbetweenList.iterator();
                while (iter.hasNext()) {
                    Object obj = iter.next(); 
                    if (obj instanceof ModelForm.Banner) {
                        ((ModelForm.Banner) obj).renderString(buffer, context, formStringRenderer);   
                    } else {
                        // no need to open and close an empty table, so skip that call
                        formStringRenderer.renderFieldGroupOpen(buffer, context, (FieldGroup) obj);
                        formStringRenderer.renderFieldGroupClose(buffer, context, (FieldGroup) obj);
                    }
                }
                if (currentFieldGroup != null && (lastFieldGroup == null || !lastFieldGroupName.equals(currentFieldGroupName))) {
                    currentFieldGroup.renderStartString(buffer, context, formStringRenderer);
                    lastFieldGroup = currentFieldGroup;
                }
            } else {
                if (fieldIter.hasNext()) {
                    // at least two loops left
                    lastFormField = currentFormField;
                    currentFormField = nextFormField;
                    nextFormField = (ModelFormField) fieldIter.next();
                } else if (nextFormField != null) {
                    // okay, just one loop left
                    lastFormField = currentFormField;
                    currentFormField = nextFormField;
                    nextFormField = null;
                } else {
                    // at the end...
                    lastFormField = currentFormField;
                    currentFormField = null;
                    // nextFormField is already null
                    break;
                }
                currentFieldGroup = null;
                if (currentFormField != null) {
                    currentFieldGroup = (FieldGroup) fieldGroupMap.get(currentFormField.getName()); 
                }
                if (currentFieldGroup == null) {
                    currentFieldGroup = defaultFieldGroup;
                }
                currentFieldGroupName = currentFieldGroup.getId();
                
                if (lastFieldGroup != null ) {
                    lastFieldGroupName = lastFieldGroup.getId();
                    if (!lastFieldGroupName.equals(currentFieldGroupName)) {
                        lastFieldGroup.renderEndString(buffer, context, formStringRenderer);
                        
                        List inbetweenList = getInbetweenList(lastFieldGroup, currentFieldGroup);
                        Iterator iter = inbetweenList.iterator();
                        while (iter.hasNext()) {
                            Object obj = iter.next(); 
                            if (obj instanceof ModelForm.Banner) {
                                ((ModelForm.Banner) obj).renderString(buffer, context, formStringRenderer);   
                            } else {
                                // no need to open and close an empty table, so skip that call
                                formStringRenderer.renderFieldGroupOpen(buffer, context, (FieldGroup) obj);
                                formStringRenderer.renderFieldGroupClose(buffer, context, (FieldGroup) obj);
                            }
                        }
                    }
                }
                
                if (currentFieldGroup != null && (lastFieldGroup == null || !lastFieldGroupName.equals(currentFieldGroupName))) {
                        currentFieldGroup.renderStartString(buffer, context, formStringRenderer);
                        lastFieldGroup = currentFieldGroup;
                }
            }

            ModelFormField.FieldInfo fieldInfo = currentFormField.getFieldInfo();
            if (fieldInfo.getFieldType() == ModelFormField.FieldInfo.HIDDEN || fieldInfo.getFieldType() == ModelFormField.FieldInfo.IGNORED) {
                continue;
            }
            if (alreadyRendered.contains(currentFormField.getName())) {
                continue;
            }
            //Debug.logInfo("In single form evaluating use-when for field " + currentFormField.getName() + ": " + currentFormField.getUseWhen(), module);
            if (!currentFormField.shouldUse(context)) {
                continue;
            }
            alreadyRendered.add(currentFormField.getName());

            boolean stayingOnRow = false;
            if (lastFormField != null) {
                if (lastFormField.getPosition() >= currentFormField.getPosition()) {
                    // moving to next row
                    stayingOnRow = false;
                } else {
                    // staying on same row
                    stayingOnRow = true;
                }
            }

            int positionSpan = 1;
            Integer nextPositionInRow = null;
            if (nextFormField != null) {
                if (nextFormField.getPosition() > currentFormField.getPosition()) {
                    positionSpan = nextFormField.getPosition() - currentFormField.getPosition() - 1;
                    nextPositionInRow = new Integer(nextFormField.getPosition());
                } else {
                    positionSpan = positions - currentFormField.getPosition();
                    if (!stayingOnRow && nextFormField.getPosition() > 1) {
                        // TODO: here is a weird case where it is setup such
                        //that the first position(s) in the row are skipped
                        // not sure what to do about this right now...
                    }
                }
            }

            if (stayingOnRow) {
                // no spacer cell, might add later though...
                //formStringRenderer.renderFormatFieldRowSpacerCell(buffer, context, currentFormField);
            } else {
                if (haveRenderedOpenFieldRow) {
                    // render row formatting close
                    formStringRenderer.renderFormatFieldRowClose(buffer, context, this);
                    haveRenderedOpenFieldRow = false;
                }

                // render row formatting open
                formStringRenderer.renderFormatFieldRowOpen(buffer, context, this);
                haveRenderedOpenFieldRow = true;
            }

            // render title formatting open
            formStringRenderer.renderFormatFieldRowTitleCellOpen(buffer, context, currentFormField);

            // render title (unless this is a submit or a reset field)
            if (fieldInfo.getFieldType() != ModelFormField.FieldInfo.SUBMIT && fieldInfo.getFieldType() != ModelFormField.FieldInfo.RESET) {
                formStringRenderer.renderFieldTitle(buffer, context, currentFormField);
            } else {
                formStringRenderer.renderFormatEmptySpace(buffer, context, this);
            }

            // render title formatting close
            formStringRenderer.renderFormatFieldRowTitleCellClose(buffer, context, currentFormField);

            // render separator
            formStringRenderer.renderFormatFieldRowSpacerCell(buffer, context, currentFormField);

            // render widget formatting open
            formStringRenderer.renderFormatFieldRowWidgetCellOpen(buffer, context, currentFormField, positions, positionSpan, nextPositionInRow);

            // render widget
            currentFormField.renderFieldString(buffer, context, formStringRenderer);

            // render widget formatting close
            formStringRenderer.renderFormatFieldRowWidgetCellClose(buffer, context, currentFormField, positions, positionSpan, nextPositionInRow);

        }
        // always render row formatting close after the end
        formStringRenderer.renderFormatFieldRowClose(buffer, context, this);

        if (lastFieldGroup != null) {
            lastFieldGroup.renderEndString(buffer, context, formStringRenderer);
        }
        // render formatting wrapper close
        // should be handled by renderEndString
        //formStringRenderer.renderFormatSingleWrapperClose(buffer, context, this);

        // render form close
        if (!skipEnd) formStringRenderer.renderFormClose(buffer, context, this);
    }

    public void renderListFormString(StringBuffer buffer, Map context, FormStringRenderer formStringRenderer, int positions) {
        // render list/tabular type forms

        // render formatting wrapper open
        formStringRenderer.renderFormatListWrapperOpen(buffer, context, this);

        // ===== render header row =====
        if (!getHideHeader()) {
            this.renderHeaderRow(buffer, context, formStringRenderer);
        }

        // ===== render the item rows =====
        this.renderItemRows(buffer, context, formStringRenderer, true);

        // render formatting wrapper close
        formStringRenderer.renderFormatListWrapperClose(buffer, context, this);
    }

    public void renderMultiFormString(StringBuffer buffer, Map context, FormStringRenderer formStringRenderer, int positions) {
        formStringRenderer.renderFormOpen(buffer, context, this);

        // render formatting wrapper open
        formStringRenderer.renderFormatListWrapperOpen(buffer, context, this);

        // ===== render header row =====
        this.renderHeaderRow(buffer, context, formStringRenderer);

        // ===== render the item rows =====
        this.renderItemRows(buffer, context, formStringRenderer, false);

        formStringRenderer.renderFormatListWrapperClose(buffer, context, this);
        
        formStringRenderer.renderMultiFormClose(buffer, context, this);
    }

    public void renderHeaderRow(StringBuffer buffer, Map context, FormStringRenderer formStringRenderer) {
        formStringRenderer.renderFormatHeaderRowOpen(buffer, context, this);

        // render title for each field, except hidden & ignored, etc

        // start by rendering all display and hyperlink fields, until we
        //get to a field that should go into the form cell, then render
        //the form cell with all non-display and non-hyperlink fields, then
        //do a start after the first form input field and
        //render all display and hyperlink fields after the form

        // do the first part of display and hyperlink fields
        Iterator displayHyperlinkFieldIter = this.fieldList.iterator();
        ModelFormField previousModelFormField = null;
        while (displayHyperlinkFieldIter.hasNext()) {
            ModelFormField modelFormField = (ModelFormField) displayHyperlinkFieldIter.next();
            ModelFormField.FieldInfo fieldInfo = modelFormField.getFieldInfo();

            // don't do any header for hidden or ignored fields
            if (fieldInfo.getFieldType() == ModelFormField.FieldInfo.HIDDEN || fieldInfo.getFieldType() == ModelFormField.FieldInfo.IGNORED) {
                continue;
            }

            //Modification Nicolas to support Two or more field with the same name and they are used with condition
            if (previousModelFormField != null && previousModelFormField.getTitle(context).equals(modelFormField.getTitle(context)) && 
                    !(previousModelFormField.isUseWhenEmpty() && modelFormField.isUseWhenEmpty())) {
                continue;
            }

            if (fieldInfo.getFieldType() != ModelFormField.FieldInfo.DISPLAY && fieldInfo.getFieldType() != ModelFormField.FieldInfo.DISPLAY_ENTITY && fieldInfo.getFieldType() != ModelFormField.FieldInfo.HYPERLINK) {
                // okay, now do the form cell
                break;
            }

            // DON'T check this for the header row, doesn't really make sense, should always show the header: if (!modelFormField.shouldUse(context)) { continue; }

            formStringRenderer.renderFormatHeaderRowCellOpen(buffer, context, this, modelFormField);

            formStringRenderer.renderFieldTitle(buffer, context, modelFormField);

            formStringRenderer.renderFormatHeaderRowCellClose(buffer, context, this, modelFormField);
       
            //Modification Nicolas
            previousModelFormField = modelFormField;
        }

        List headerFormFields = new LinkedList();
        Iterator formFieldIter = this.fieldList.iterator();
        //boolean isFirstFormHeader = true;
        while (formFieldIter.hasNext()) {
            ModelFormField modelFormField = (ModelFormField) formFieldIter.next();
            ModelFormField.FieldInfo fieldInfo = modelFormField.getFieldInfo();

            // don't do any header for hidden or ignored fields
            if (fieldInfo.getFieldType() == ModelFormField.FieldInfo.HIDDEN || fieldInfo.getFieldType() == ModelFormField.FieldInfo.IGNORED) {
                continue;
            }

            // skip all of the display/hyperlink fields
            if (fieldInfo.getFieldType() == ModelFormField.FieldInfo.DISPLAY || fieldInfo.getFieldType() == ModelFormField.FieldInfo.DISPLAY_ENTITY || fieldInfo.getFieldType() == ModelFormField.FieldInfo.HYPERLINK) {
                continue;
            }

            if (!modelFormField.shouldUse(context)) {
                continue;
            }

            headerFormFields.add(modelFormField);
        }

        // render the "form" cell
        formStringRenderer.renderFormatHeaderRowFormCellOpen(buffer, context, this);

        Iterator headerFormFieldIter = headerFormFields.iterator();
        while (headerFormFieldIter.hasNext()) {
            ModelFormField modelFormField = (ModelFormField) headerFormFieldIter.next();
            //ModelFormField.FieldInfo fieldInfo = modelFormField.getFieldInfo();

            if (separateColumns || modelFormField.getSeparateColumn()) 
                formStringRenderer.renderFormatItemRowCellOpen(buffer, context, this, modelFormField);

            // render title (unless this is a submit or a reset field)
            formStringRenderer.renderFieldTitle(buffer, context, modelFormField);

            if (separateColumns || modelFormField.getSeparateColumn()) 
                formStringRenderer.renderFormatItemRowCellClose(buffer, context, this, modelFormField);

            if (headerFormFieldIter.hasNext()) {
                // TODO: determine somehow if this is the last one... how?
               if (!separateColumns && !modelFormField.getSeparateColumn()) 
                    formStringRenderer.renderFormatHeaderRowFormCellTitleSeparator(buffer, context, this, modelFormField, false);
            }
        }

        formStringRenderer.renderFormatHeaderRowFormCellClose(buffer, context, this);

        // render the rest of the display/hyperlink fields
        while (displayHyperlinkFieldIter.hasNext()) {
            ModelFormField modelFormField = (ModelFormField) displayHyperlinkFieldIter.next();
            ModelFormField.FieldInfo fieldInfo = modelFormField.getFieldInfo();

            // don't do any header for hidden or ignored fields
            if (fieldInfo.getFieldType() == ModelFormField.FieldInfo.HIDDEN || fieldInfo.getFieldType() == ModelFormField.FieldInfo.IGNORED) {
                continue;
            }

            // skip all non-display and non-hyperlink fields
            if (fieldInfo.getFieldType() != ModelFormField.FieldInfo.DISPLAY && fieldInfo.getFieldType() != ModelFormField.FieldInfo.DISPLAY_ENTITY && fieldInfo.getFieldType() != ModelFormField.FieldInfo.HYPERLINK) {
                continue;
            }

            if (!modelFormField.shouldUse(context)) {
                continue;
            }

            formStringRenderer.renderFormatHeaderRowCellOpen(buffer, context, this, modelFormField);

            formStringRenderer.renderFieldTitle(buffer, context, modelFormField);

            formStringRenderer.renderFormatHeaderRowCellClose(buffer, context, this, modelFormField);
        }

        formStringRenderer.renderFormatHeaderRowClose(buffer, context, this);
    }

    protected Object safeNext(Iterator iterator) {
        try {
            return iterator.next();
        } catch (NoSuchElementException e) {
            return null;
        }
    }
    
    public void renderItemRows(StringBuffer buffer, Map context, FormStringRenderer formStringRenderer, boolean formPerItem) {
        this.rowCount = 0;
        String lookupName = this.getListName();
        if (UtilValidate.isEmpty(lookupName)) {
            Debug.logError("No value for list or iterator name found.", module);
            return;
        }
        Object obj = context.get(lookupName);
        if (obj == null) {
            Debug.logInfo("No object for list or iterator name:" + lookupName + " found.", module);
            return;
        }
        // if list is empty, do not render rows
        Iterator iter = null;
        List items = null;
        if (obj instanceof Iterator) {
            iter = (Iterator) obj;   
            setPaginate(true);
        } else if (obj instanceof List) {
            items = (List) obj;
            iter = items.listIterator();
            setPaginate(true);
        }

        // set low and high index
        getListLimits(context, obj);

        int listSize = ((Integer) context.get("listSize")).intValue();
        //int viewIndex = ((Integer) context.get("viewIndex")).intValue();
        //int viewSize = ((Integer) context.get("viewSize")).intValue();
        int lowIndex = ((Integer) context.get("lowIndex")).intValue();
        int highIndex = ((Integer) context.get("highIndex")).intValue();

        // we're passed a subset of the list, so use (0, viewSize) range
        if (isOverridenListSize()) {
            lowIndex = 0;
            highIndex = ((Integer) context.get("viewSize")).intValue();
        }

        if (iter != null) {
            // render item rows
            int itemIndex = -1;
            Object item = null;
            while ((item = this.safeNext(iter)) != null) {
                itemIndex++;
                if (itemIndex >= highIndex) {
                    break;
                }

                // TODO: this is a bad design, for EntityListIterators we should skip to the lowIndex and go from there, MUCH more efficient...
                if (itemIndex < lowIndex) {
                    continue;
                }
                
                Map localContext = new HashMap(context);
                if (UtilValidate.isNotEmpty(this.getListEntryName())) {
                    localContext.put(this.getListEntryName(), item);
                } else {
                    Map itemMap = (Map) item;
                    localContext.putAll(itemMap);
                }
                
                ModelFormAction.runSubActions(this.rowActions, localContext);

                localContext.put("itemIndex", new Integer(itemIndex - lowIndex));
                this.resetBshInterpreter(localContext);
                this.rowCount++;

                if (Debug.verboseOn()) Debug.logVerbose("In form got another row, context is: " + localContext, module);

                // Check to see if there is a field, same name and same use-when (could come from extended form)
                for (int j = 0; j < this.fieldList.size(); j++) {
                    ModelFormField modelFormField = (ModelFormField) this.fieldList.get(j);
                    if (!modelFormField.isUseWhenEmpty()) {
                        boolean shouldUse1 = modelFormField.shouldUse(localContext);
                        for (int i = j+1; i < this.fieldList.size(); i++) {
                            ModelFormField curField = (ModelFormField) this.fieldList.get(i);
                            if (curField.getName() != null && curField.getName().equals(modelFormField.getName())) {
                                boolean shouldUse2 = curField.shouldUse(localContext);
                                if (shouldUse1 == shouldUse2) {
                                    this.fieldList.remove(i--);
                                }
                            } else {
                                continue;
                            }
                        }
                    }
                }
                
                // render row formatting open
                formStringRenderer.renderFormatItemRowOpen(buffer, localContext, this);

                // do the first part of display and hyperlink fields
                Iterator innerDisplayHyperlinkFieldIter = this.fieldList.iterator();
                while (innerDisplayHyperlinkFieldIter.hasNext()) {
                    ModelFormField modelFormField = (ModelFormField) innerDisplayHyperlinkFieldIter.next();
                    ModelFormField.FieldInfo fieldInfo = modelFormField.getFieldInfo();

                    // don't do any header for hidden or ignored fields
                    if (fieldInfo.getFieldType() == ModelFormField.FieldInfo.HIDDEN || fieldInfo.getFieldType() == ModelFormField.FieldInfo.IGNORED) {
                        continue;
                    }

                    if (fieldInfo.getFieldType() != ModelFormField.FieldInfo.DISPLAY && fieldInfo.getFieldType() != ModelFormField.FieldInfo.DISPLAY_ENTITY && fieldInfo.getFieldType() != ModelFormField.FieldInfo.HYPERLINK) {
                        // okay, now do the form cell
                        break;
                    }

                    if (!modelFormField.shouldUse(localContext)) {
                        continue;
                    }

                    formStringRenderer.renderFormatItemRowCellOpen(buffer, localContext, this, modelFormField);

                    modelFormField.renderFieldString(buffer, localContext, formStringRenderer);

                    formStringRenderer.renderFormatItemRowCellClose(buffer, localContext, this, modelFormField);
                }

                // render the "form" cell
                formStringRenderer.renderFormatItemRowFormCellOpen(buffer, localContext, this);

                if (formPerItem) {
                    formStringRenderer.renderFormOpen(buffer, localContext, this);
                }

                // do all of the hidden fields...
                this.renderHiddenIgnoredFields(buffer, localContext, formStringRenderer, null);

                Iterator innerFormFieldIter = this.fieldList.iterator();
                while (innerFormFieldIter.hasNext()) {
                    ModelFormField modelFormField = (ModelFormField) innerFormFieldIter.next();
                    ModelFormField.FieldInfo fieldInfo = modelFormField.getFieldInfo();

                    // don't do any header for hidden or ignored fields
                    if (fieldInfo.getFieldType() == ModelFormField.FieldInfo.HIDDEN || fieldInfo.getFieldType() == ModelFormField.FieldInfo.IGNORED) {
                        continue;
                    }

                    // skip all of the display/hyperlink fields
                    if (fieldInfo.getFieldType() == ModelFormField.FieldInfo.DISPLAY || fieldInfo.getFieldType() == ModelFormField.FieldInfo.DISPLAY_ENTITY || fieldInfo.getFieldType() == ModelFormField.FieldInfo.HYPERLINK) {
                        continue;
                    }

                    if (!modelFormField.shouldUse(localContext)) {
                        continue;
                    }

                    if (separateColumns || modelFormField.getSeparateColumn()) 
                        formStringRenderer.renderFormatItemRowCellOpen(buffer, localContext, this, modelFormField);
                    // render field widget
                    modelFormField.renderFieldString(buffer, localContext, formStringRenderer);

                    if (separateColumns || modelFormField.getSeparateColumn()) 
                        formStringRenderer.renderFormatItemRowCellClose(buffer, localContext, this, modelFormField);
                }

                if (formPerItem) {
                    formStringRenderer.renderFormClose(buffer, localContext, this);
                }

                formStringRenderer.renderFormatItemRowFormCellClose(buffer, localContext, this);

                // render the rest of the display/hyperlink fields
                while (innerDisplayHyperlinkFieldIter.hasNext()) {
                    ModelFormField modelFormField = (ModelFormField) innerDisplayHyperlinkFieldIter.next();
                    ModelFormField.FieldInfo fieldInfo = modelFormField.getFieldInfo();

                    // don't do any header for hidden or ignored fields
                    if (fieldInfo.getFieldType() == ModelFormField.FieldInfo.HIDDEN || fieldInfo.getFieldType() == ModelFormField.FieldInfo.IGNORED) {
                        continue;
                    }

                    // skip all non-display and non-hyperlink fields
                    if (fieldInfo.getFieldType() != ModelFormField.FieldInfo.DISPLAY && fieldInfo.getFieldType() != ModelFormField.FieldInfo.DISPLAY_ENTITY && fieldInfo.getFieldType() != ModelFormField.FieldInfo.HYPERLINK) {
                        continue;
                    }

                    if (!modelFormField.shouldUse(localContext)) {
                        continue;
                    }

                    formStringRenderer.renderFormatItemRowCellOpen(buffer, localContext, this, modelFormField);

                    modelFormField.renderFieldString(buffer, localContext, formStringRenderer);

                    formStringRenderer.renderFormatItemRowCellClose(buffer, localContext, this, modelFormField);
                }

                // render row formatting close
                formStringRenderer.renderFormatItemRowClose(buffer, localContext, this);
            }

            // reduce the highIndex if number of items falls short
            if ((itemIndex + 1) < highIndex) {
                highIndex = itemIndex + 1;
                // if list size is overridden, use full listSize
                context.put("highIndex", new Integer(isOverridenListSize() ? listSize : highIndex));
            }
            context.put("actualPageSize", new Integer(highIndex - lowIndex));
            
            if (iter instanceof EntityListIterator) {
                try {
                    ((EntityListIterator) iter).close();
                } catch(GenericEntityException e) {
                    Debug.logError(e, "Error closing list form render EntityListIterator: " + e.toString(), module);
                }
            }
        }
    }

    public void renderHiddenIgnoredFields(StringBuffer buffer, Map context, FormStringRenderer formStringRenderer, Set alreadyRendered) {
        Iterator fieldIter = this.fieldList.iterator();
        while (fieldIter.hasNext()) {
            ModelFormField modelFormField = (ModelFormField) fieldIter.next();
            ModelFormField.FieldInfo fieldInfo = modelFormField.getFieldInfo();

            // render hidden/ignored field widget
            switch (fieldInfo.getFieldType()) {
                case ModelFormField.FieldInfo.HIDDEN :
                case ModelFormField.FieldInfo.IGNORED :
                    if (modelFormField.shouldUse(context)) {
                        modelFormField.renderFieldString(buffer, context, formStringRenderer);
                        if (alreadyRendered != null)
                            alreadyRendered.add(modelFormField.getName());
                    }
                    break;

                case ModelFormField.FieldInfo.DISPLAY :
                case ModelFormField.FieldInfo.DISPLAY_ENTITY :
                    ModelFormField.DisplayField displayField = (ModelFormField.DisplayField) fieldInfo;
                    if (displayField.getAlsoHidden() && modelFormField.shouldUse(context)) {
                        formStringRenderer.renderHiddenField(buffer, context, modelFormField, modelFormField.getEntry(context));
                        // don't add to already rendered here, or the display won't ger rendered: if (alreadyRendered != null) alreadyRendered.add(modelFormField.getName());
                    }
                    break;

                case ModelFormField.FieldInfo.HYPERLINK :
                    ModelFormField.HyperlinkField hyperlinkField = (ModelFormField.HyperlinkField) fieldInfo;
                    if (hyperlinkField.getAlsoHidden() && modelFormField.shouldUse(context)) {
                        formStringRenderer.renderHiddenField(buffer, context, modelFormField, modelFormField.getEntry(context));
                        // don't add to already rendered here, or the hyperlink won't ger rendered: if (alreadyRendered != null) alreadyRendered.add(modelFormField.getName());
                    }
                    break;
            }
        }
    }

    public LocalDispatcher getDispacher() {
        return this.dispatcher;
    }

    public GenericDelegator getDelegator() {
        return this.delegator;
    }


    public LocalDispatcher getDispatcher(Map context) {
        LocalDispatcher dispatcher = (LocalDispatcher) context.get("dispatcher");
        return dispatcher;
    }

    public GenericDelegator getDelegator(Map context) {
        GenericDelegator delegator = (GenericDelegator) context.get("delegator");
        return delegator;
    }

    public String getTargetType() {
        return this.targetType;   
    }
    
    /**
     * @return
     */
    public String getDefaultEntityName() {
        return this.defaultEntityName;
    }

    /**
     * @return
     */
    public String getDefaultMapName() {
        return this.defaultMapName.getOriginalName();
    }

    public Map getDefaultMap(Map context) {
        return (Map) this.defaultMapName.get(context);
    }
    
    /**
     * @return
     */
    public String getDefaultRequiredFieldStyle() {
        return this.defaultRequiredFieldStyle;
    }


    /**
     * @return
     */
    public String getDefaultServiceName() {
        return this.defaultServiceName;
    }

    /**
     * @return
     */
    public String getFormTitleAreaStyle() {
        return this.formTitleAreaStyle;
    }

    /**
     * @return
     */
    public String getFormWidgetAreaStyle() {
        return this.formWidgetAreaStyle;
    }

    /**
     * @return
     */
    public String getDefaultTitleAreaStyle() {
        return this.defaultTitleAreaStyle;
    }

    /**
     * @return
     */
    public String getDefaultWidgetAreaStyle() {
        return this.defaultWidgetAreaStyle;
    }

    /**
     * @return
     */
    public String getOddRowStyle() {
        return this.oddRowStyle;
    }
    
    /**
     * @return
     */
    public String getEvenRowStyle() {
        return this.evenRowStyle;
    }

    /**
     * @return
     */
    public String getDefaultTableStyle() {
        return this.defaultTableStyle;
    }
    
    /**
     * @return
     */
    public String getHeaderRowStyle() {
        return this.headerRowStyle;
    }
    
    /**
     * @return
     */
    public String getDefaultTitleStyle() {
        return this.defaultTitleStyle;
    }

    /**
     * @return
     */
    public String getDefaultWidgetStyle() {
        return this.defaultWidgetStyle;
    }

    /**
     * @return
     */
    public String getDefaultTooltipStyle() {
        return this.defaultTooltipStyle;
    }

    /**
     * @return
     */
    public String getItemIndexSeparator() {
        if (UtilValidate.isNotEmpty(this.itemIndexSeparator)) {
            return this.itemIndexSeparator;
        } else {
            return "_o_";
        }
    }

    /**
     * @return
     */
    public String getListEntryName() {
        return this.listEntryName;
    }

    /**
     * @return
     */
    public String getListName() {
        String lstNm =  this.listName;
        if (UtilValidate.isEmpty(lstNm)) {
            lstNm = DEFAULT_FORM_RESULT_LIST_NAME;
        }
        return lstNm;
    }

    /**
     * @return
     */
    public String getName() {
        return this.name;
    }

    public String getCurrentFormName(Map context) {
        Integer itemIndex = (Integer) context.get("itemIndex");
        String formName = (String) context.get("formName");
        if (UtilValidate.isEmpty(formName)) {
            formName = this.getName();
        }

        if (itemIndex != null && "list".equals(this.getType())) {
            return formName + this.getItemIndexSeparator() + itemIndex.intValue();
        } else {
            return formName;
        }
    }

    /** iterate through altTargets list to see if any should be used, if not return original target
     * @return The target for this Form
     */
    public String getTarget(Map context) {
        try {
            // use the same Interpreter (ie with the same context setup) for all evals
            Interpreter bsh = this.getBshInterpreter(context);
            Iterator altTargetIter = this.altTargets.iterator();
            while (altTargetIter.hasNext()) {
                AltTarget altTarget = (AltTarget) altTargetIter.next();
                Object retVal = bsh.eval(altTarget.useWhen);
                boolean condTrue = false;
                // retVal should be a Boolean, if not something weird is up...
                if (retVal instanceof Boolean) {
                    Boolean boolVal = (Boolean) retVal;
                    condTrue = boolVal.booleanValue();
                } else {
                    throw new IllegalArgumentException(
                        "Return value from target condition eval was not a Boolean: " + retVal.getClass().getName() + " [" + retVal + "] of form " + this.name);
                }

                if (condTrue) {
                    return altTarget.target;
                }
            }
        } catch (EvalError e) {
            String errmsg = "Error evaluating BeanShell target conditions on form " + this.name;
            Debug.logError(e, errmsg, module);
            throw new IllegalArgumentException(errmsg);
        }

        return target.expandString(context);
    }

    /**
     * @return
     */
    public String getContainerId() {
        return this.containerId;
    }

    /**
     * @return
     */
    public String getContainerStyle() {
        return this.containerStyle;
    }

    /**
     * @return
     */
    public String getfocusFieldName() {
        return this.focusFieldName;
    }

    /**
     * @return
     */
    public String getTitle() {
        return this.title;
    }

    /**
     * @return
     */
    public String getTooltip() {
        return this.tooltip;
    }

    /**
     * @return
     */
    public String getType() {
        return this.type;
    }

    public void resetBshInterpreter(Map context) {
        context.remove("bshInterpreter");
    }
    
    public Interpreter getBshInterpreter(Map context) throws EvalError {
        Interpreter bsh = (Interpreter) context.get("bshInterpreter");
        if (bsh == null) {
            bsh = BshUtil.makeInterpreter(context);
            context.put("bshInterpreter", bsh);
        }
        return bsh;
    }

    /**
     * @param string
     */
    public void setDefaultEntityName(String string) {
        this.defaultEntityName = string;
    }

    /**
     * @param string
     */
    public void setDefaultMapName(String string) {
        this.defaultMapName = new FlexibleMapAccessor(string);
    }

    /**
     * @param string
     */
    public void setDefaultServiceName(String string) {
        this.defaultServiceName = string;
    }

    /**
     * @param string
     */
    public void setFormTitleAreaStyle(String string) {
        this.formTitleAreaStyle = string;
    }

    /**
     * @param string
     */
    public void setFormWidgetAreaStyle(String string) {
        this.formWidgetAreaStyle = string;
    }

    /**
     * @param string
     */
    public void setDefaultTitleAreaStyle(String string) {
        this.defaultTitleAreaStyle = string;
    }

    /**
     * @param string
     */
    public void setDefaultWidgetAreaStyle(String string) {
        this.defaultWidgetAreaStyle = string;
    }
    
    /**
     * @param string
     */
    public void setOddRowStyle(String string) {
        this.oddRowStyle = string;
    }
    
    /**
     * @param string
     */
    public void setEvenRowStyle(String string) {
        this.evenRowStyle = string;
    }
    
    /**
     * @param string
     */
    public void setDefaultTableStyle(String string) {
        this.defaultTableStyle = string;
    }
    
    /**
     * @param string
     */
    public void setHeaderRowStyle(String string) {
        this.headerRowStyle = string;
    }
    
    /**
     * @param string
     */
    public void setDefaultTitleStyle(String string) {
        this.defaultTitleStyle = string;
    }

    /**
     * @param string
     */
    public void setDefaultWidgetStyle(String string) {
        this.defaultWidgetStyle = string;
    }

    /**
     * @param string
     */
    public void setDefaultTooltipStyle(String string) {
        this.defaultTooltipStyle = string;
    }

    /**
     * @param string
     */
    public void setItemIndexSeparator(String string) {
        this.itemIndexSeparator = string;
    }

    /**
     * @param string
     */
    public void setListEntryName(String string) {
        this.listEntryName = string;
    }

    /**
     * @param string
     */
    public void setListName(String string) {
        this.listName = string;
    }

    /**
     * @param string
     */
    public void setName(String string) {
        this.name = string;
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
    public void setContainerId(String string) {
        this.containerId = string;
    }

    /**
     * @param string
     */
    public void setfocusFieldName(String string) {
        this.focusFieldName = string;
    }

    /**
     * @param string
     */
    public void setTitle(String string) {
        this.title = string;
    }

    /**
     * @param string
     */
    public void setTooltip(String string) {
        this.tooltip = string;
    }

    /**
     * @param string
     */
    public void setType(String string) {
        this.type = string;
    }

    /**
     * @return
     */
    public String getPaginateTarget(Map context) {
        String targ = this.paginateTarget.expandString(context);
        if (UtilValidate.isEmpty(targ)) {
            targ = getTarget(context);
        }
        
        return targ;
    }

    public String getPaginateTargetAnchor() {
        return this.paginateTargetAnchor;
    }
    
    public String getPaginateIndexField(Map context) {
        String field = this.paginateIndexField.expandString(context);
        if (UtilValidate.isEmpty(field)) {
            field = DEFAULT_PAG_INDEX_FIELD;
        }
        return field;
    }

    public int getPaginateIndex(Map context) {
        String field = this.getPaginateIndexField(context);
        
        int viewIndex = 0;
        try {
            Object value = context.get(field);

            if (value == null) {
        	// try parameters.VIEW_INDEX as that is an old OFBiz convention
        	Map parameters = (Map) context.get("parameters");
        	if (parameters != null) {
        	    value = parameters.get("VIEW_INDEX");
        	    
        	    if (value == null) {
        		value = parameters.get(field);
        	    }
        	}
            }

            if (value instanceof Integer) { 
                viewIndex = ((Integer) value).intValue();
            } else if (value instanceof String) { 
                viewIndex = Integer.parseInt((String) value);
            }
        } catch (Exception e) {
            Debug.logWarning(e, "Error getting paginate view index: " + e.toString(), module);
        }
        
        return viewIndex;
    }

    public String getPaginateSizeField(Map context) {
        String field = this.paginateSizeField.expandString(context);
        if (UtilValidate.isEmpty(field)) {
            field = DEFAULT_PAG_SIZE_FIELD;
        }
        return field;
    }

    public int getPaginateSize(Map context) {
        String field = this.getPaginateSizeField(context);
        
        int viewSize = DEFAULT_PAGE_SIZE;
        try {
            Object value = context.get(field);
            
            if (value == null) {
        	// try parameters.VIEW_SIZE as that is an old OFBiz convention
        	Map parameters = (Map) context.get("parameters");
        	if (parameters != null) {
        	    value = parameters.get("VIEW_SIZE");
        	    
        	    if (value == null) {
        		value = parameters.get(field);
        	    }
        	}
            }
            
            if (value instanceof Integer) { 
                viewSize = ((Integer) value).intValue();
            } else if (value instanceof String) { 
                viewSize = Integer.parseInt((String) value);
            }
        } catch (Exception e) {
            Debug.logWarning(e, "Error getting paginate view size: " + e.toString(), module);
        }
        
        return viewSize;
    }

    public String getPaginatePreviousLabel(Map context) {
        String field = this.paginatePreviousLabel.expandString(context);
        if (UtilValidate.isEmpty(field)) {
            field = DEFAULT_PAG_PREV_LABEL;
        }
        return field;
    }

    public String getPaginateNextLabel(Map context) {
        String field = this.paginateNextLabel.expandString(context);
        if (UtilValidate.isEmpty(field)) {
            field = DEFAULT_PAG_NEXT_LABEL;
        }
        return field;
    }

    public String getPaginatePreviousStyle() {
        return this.paginatePreviousStyle;
    }

    public String getPaginateNextStyle() {
        return this.paginateNextStyle;
    }

    public String getTargetWindow(Map context) {
        return this.targetWindowExdr.expandString(context);
    }
            
    public void setTargetWindow( String val ) {
        this.targetWindowExdr = new FlexibleStringExpander(val);
    }

    /**
     * @return
     */
    public boolean getSeparateColumns() {
        return this.separateColumns;
    }

    public boolean getPaginate() {
        return this.paginate;
    }

    public boolean getSkipStart() {
        return this.skipStart;
    }
    
    public boolean getSkipEnd() {
        return this.skipEnd;
    }

    public boolean isOverridenListSize() {
        return this.overridenListSize;
    }

    public void setSkipStart(boolean val) {
        this.skipStart = val;
    }
    
    public void setSkipEnd(boolean val) {
        this.skipEnd = val;
    }
    
    public boolean getHideHeader() {
        return this.hideHeader;
    }
    
    public void setPaginate(boolean val) {
        paginate = val;
    }

    public void setOverridenListSize(boolean overridenListSize) {
        this.overridenListSize = overridenListSize;
    }

    /**
     * @param string
     */
    public void setPaginateTarget(String string) {
        this.paginateTarget = new FlexibleStringExpander(string);
    }

    public void setPaginateIndexField(String string) {
        this.paginateIndexField = new FlexibleStringExpander(string);
    }

    public void setPaginateSizeField(String string) {
        this.paginateSizeField = new FlexibleStringExpander(string);
    }

    public void setPaginatePreviousStyle(String string) {
        this.paginatePreviousStyle = (UtilValidate.isEmpty(string) ? DEFAULT_PAG_PREV_STYLE : string);
    }

    public void setPaginateNextStyle(String string) {
        this.paginateNextStyle = (UtilValidate.isEmpty(string) ? DEFAULT_PAG_NEXT_STYLE : string);
    }

    public void setDefaultViewSize(int val) {
        defaultViewSize = val;
    }

    public void setDefaultViewSize(String val) {
        try {
            Integer sz = new Integer(val);
            defaultViewSize = sz.intValue();
        } catch(NumberFormatException e) {
            defaultViewSize = DEFAULT_PAGE_SIZE;   
        }
    }
    
    public int getListSize(Map context) {
        Integer value = (Integer) context.get("listSize");
        return value != null ? value.intValue() : 0;
    }

    public int getViewIndex(Map context) {
        Integer value = (Integer) context.get("viewIndex");
        return value != null ? value.intValue() : 0;
    }

    public int getViewSize(Map context) {
        Integer value = (Integer) context.get("viewSize");
        return value != null ? value.intValue() : 20;
    }

    public int getLowIndex(Map context) {
        Integer value = (Integer) context.get("lowIndex");
        return value != null ? value.intValue() : 0;
    }

    public int getHighIndex(Map context) {
        Integer value = (Integer) context.get("highIndex");
        return value != null ? value.intValue() : 0;
    }

    public int getActualPageSize(Map context) {
        Integer value = (Integer) context.get("actualPageSize");
        return value != null ? value.intValue() : (getHighIndex(context) - getLowIndex(context));
    }

    public List getFieldList() {
        return fieldList;
    }

    private int getOverrideListSize(Map context) {
        int listSize = 0;
        String size = this.overrideListSize.expandString(context);
        if (!UtilValidate.isEmpty(size)) {
            try {
                listSize = Integer.parseInt(size);
            } catch (NumberFormatException e) {
                Debug.logError(e, "Error getting override list size from value " + size, module);
            }
        }
        return listSize;
    }

    public void getListLimits(Map context, Object entryList) {
        int listSize = 0;
        int viewIndex = 0;
        int viewSize = 0;
        int lowIndex = 0;
        int highIndex = 0;

        listSize = getOverrideListSize(context);
        if (listSize > 0) {
            setOverridenListSize(true);
        } else if (entryList instanceof EntityListIterator) {
            EntityListIterator iter = (EntityListIterator) entryList;   
            try {
                iter.last();
                listSize = iter.currentIndex();
                iter.beforeFirst();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error getting list size", module);
                listSize = 0;
            }
        } else if (entryList instanceof List) {
            List items = (List) entryList;
            listSize = items.size();
        }
        
        if (paginate) {
            viewIndex = this.getPaginateIndex(context);
            viewSize = this.getPaginateSize(context);
            
            lowIndex = viewIndex * viewSize;
            highIndex = (viewIndex + 1) * viewSize;
        } else {
            viewIndex = 0;
            viewSize = DEFAULT_PAGE_SIZE;
            lowIndex = 0;
            highIndex = DEFAULT_PAGE_SIZE;
        }
        
        context.put("listSize", new Integer(listSize));
        context.put("viewIndex", new Integer(viewIndex));
        context.put("viewSize", new Integer(viewSize));
        context.put("lowIndex", new Integer(lowIndex));
        context.put("highIndex", new Integer(highIndex));
    }
    
    public String getPassedRowCount(Map context) {
        return rowCountExdr.expandString(context);
    }
    
    public int getRowCount() {
        return this.rowCount;
    }

    public boolean getUseRowSubmit() {
        return this.useRowSubmit;
    }

    public ModelFormField getMultiSubmitField() {
        return this.multiSubmitField;
    }

    public List getInbetweenList(FieldGroup startFieldGroup, FieldGroup endFieldGroup) {
        ArrayList inbetweenList = new ArrayList();
        boolean firstFound = false;
        String startFieldGroupId = null;
        String endFieldGroupId = null;
        if (endFieldGroup != null) {
            endFieldGroupId = endFieldGroup.getId();   
        }
        if (startFieldGroup == null) {
            firstFound = true;
        } else {
            startFieldGroupId = startFieldGroup.getId();   
        }
        Iterator iter = fieldGroupList.iterator();
        while (iter.hasNext()) {
            Object obj = iter.next();
            if (obj instanceof ModelForm.Banner) {
                if (firstFound) inbetweenList.add(obj);   
            } else {
                FieldGroup fieldGroup = (FieldGroup)obj;
                String fieldGroupId = fieldGroup.getId();
                if (!firstFound) {
                    if (fieldGroupId.equals(startFieldGroupId)) {
                        firstFound = true;
                        continue;
                    }
                }
                if (firstFound) {
                    if (fieldGroupId.equals(endFieldGroupId)) {
                        break;
                    } else {
                        inbetweenList.add(fieldGroup);   
                    }
                }
            }
        }
        return inbetweenList;
    }
    
    public static class AltTarget {
        public String useWhen;
        public String target;
        public AltTarget(Element altTargetElement) {
            this.useWhen = altTargetElement.getAttribute("use-when");
            this.target = altTargetElement.getAttribute("target");
        }
    }

    public static class AutoFieldsService {
        public String serviceName;
        public String mapName;
        public String defaultFieldType;
        public AutoFieldsService(Element element) {
            this.serviceName = element.getAttribute("service-name");
            this.mapName = element.getAttribute("map-name");
            this.defaultFieldType = element.getAttribute("default-field-type");
        }
    }

    public static class AutoFieldsEntity {
        public String entityName;
        public String mapName;
        public String defaultFieldType;
        public AutoFieldsEntity(Element element) {
            this.entityName = element.getAttribute("entity-name");
            this.mapName = element.getAttribute("map-name");
            this.defaultFieldType = element.getAttribute("default-field-type");
        }
    }

    public static class FieldGroup {
        public String id;
        public String style;
        protected ModelForm modelForm;
        protected static int baseSeqNo = 0;
        protected static String baseId = "_G";
        public FieldGroup(Element sortOrderElement, ModelForm modelForm) {
          
            this.modelForm = modelForm;
            if (sortOrderElement != null) {
                this.id = sortOrderElement.getAttribute("id");
                if (UtilValidate.isEmpty(this.id)) {
                    String lastGroupId = baseId + baseSeqNo++ + "_";
                    this.setId(lastGroupId);
                }
                this.style = sortOrderElement.getAttribute("style");
                List sortFieldElements = UtilXml.childElementList(sortOrderElement, "sort-field");
                Iterator sortFieldElementIter = sortFieldElements.iterator();
                while (sortFieldElementIter.hasNext()) {
                    Element sortFieldElement = (Element) sortFieldElementIter.next();
                    modelForm.sortOrderFields.add(sortFieldElement.getAttribute("name"));
                    modelForm.fieldGroupMap.put(sortFieldElement.getAttribute("name"), this);
                }
            } else {
                String lastGroupId = baseId + baseSeqNo++ + "_";
                this.setId(lastGroupId);
            }
        }
        
        public String getId() {
            return this.id;   
        }
        
        public void setId( String id) {
            this.id = id;   
        }
        
        public String getStyle() {
            return this.style;   
        }
        
        public void renderStartString(StringBuffer buffer, Map context, FormStringRenderer formStringRenderer) {
            formStringRenderer.renderFieldGroupOpen(buffer, context, this);
            formStringRenderer.renderFormatSingleWrapperOpen(buffer, context, modelForm);
        }
        
        public void renderEndString(StringBuffer buffer, Map context, FormStringRenderer formStringRenderer) {
            formStringRenderer.renderFormatSingleWrapperClose(buffer, context, modelForm);
            formStringRenderer.renderFieldGroupClose(buffer, context, this);
        }
    }

    public static class Banner {
        protected ModelForm modelForm;
        public FlexibleStringExpander style;
        public FlexibleStringExpander text;
        public FlexibleStringExpander textStyle;
        public FlexibleStringExpander leftText;
        public FlexibleStringExpander leftTextStyle;
        public FlexibleStringExpander rightText;
        public FlexibleStringExpander rightTextStyle;
        
        public Banner(Element sortOrderElement, ModelForm modelForm) {
            this.modelForm = modelForm;
            this.style = new FlexibleStringExpander(sortOrderElement.getAttribute("style"));
            this.text = new FlexibleStringExpander(sortOrderElement.getAttribute("text"));
            this.textStyle = new FlexibleStringExpander(sortOrderElement.getAttribute("text-style"));
            this.leftText = new FlexibleStringExpander(sortOrderElement.getAttribute("left-text"));
            this.leftTextStyle = new FlexibleStringExpander(sortOrderElement.getAttribute("left-text-style"));
            this.rightText = new FlexibleStringExpander(sortOrderElement.getAttribute("right-text"));
            this.rightTextStyle = new FlexibleStringExpander(sortOrderElement.getAttribute("right-text-style"));
        }
        
        public String getStyle(Map context) { return this.style.expandString(context); }
        public String getText(Map context) { return this.text.expandString(context); }
        public String getTextStyle(Map context) { return this.textStyle.expandString(context); }
        public String getLeftText(Map context) { return this.leftText.expandString(context); }
        public String getLeftTextStyle(Map context) { return this.leftTextStyle.expandString(context); }
        public String getRightText(Map context) { return this.rightText.expandString(context); }
        public String getRightTextStyle(Map context) { return this.rightTextStyle.expandString(context); }
        
        public void renderString(StringBuffer buffer, Map context, FormStringRenderer formStringRenderer) {
            formStringRenderer.renderBanner(buffer, context, this);
        }
    }
}
