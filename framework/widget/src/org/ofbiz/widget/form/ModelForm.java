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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;

import org.ofbiz.base.util.BshUtil;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.collections.MapStack;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelField;
import org.ofbiz.entity.model.ModelReader;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelParam;
import org.ofbiz.service.ModelService;
import org.ofbiz.webapp.control.ConfigXMLReader;
import org.ofbiz.widget.ModelWidget;
import org.w3c.dom.Element;

import bsh.EvalError;
import bsh.Interpreter;

/**
 * Widget Library - Form model class
 */
@SuppressWarnings("serial")
public class ModelForm extends ModelWidget {

    public static final String module = ModelForm.class.getName();
    public static final String DEFAULT_FORM_RESULT_LIST_NAME = "defaultFormResultList";

    protected ModelReader entityModelReader;
    protected DispatchContext dispatchContext;

    protected String formLocation;
    protected String parentFormName;
    protected String parentFormLocation;
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
    protected FlexibleMapAccessor<Map<String, ? extends Object>> defaultMapName;
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
    protected FlexibleStringExpander paginateFirstLabel;
    protected FlexibleStringExpander paginatePreviousLabel;
    protected FlexibleStringExpander paginateNextLabel;
    protected FlexibleStringExpander paginateLastLabel;
    protected FlexibleStringExpander paginateViewSizeLabel;
    protected String paginateTargetAnchor;
    protected String paginateStyle;
    protected boolean separateColumns = false;
    protected boolean paginate = true;
    protected boolean useRowSubmit = false;
    protected FlexibleStringExpander targetWindowExdr;
    protected String defaultRequiredFieldStyle;
    protected String defaultSortFieldStyle;
    protected String defaultSortFieldAscStyle;
    protected String defaultSortFieldDescStyle;
    protected String oddRowStyle;
    protected String evenRowStyle;
    protected String defaultTableStyle;
    protected String headerRowStyle;
    protected boolean skipStart = false;
    protected boolean skipEnd = false;
    protected boolean hideHeader = false;
    protected boolean overridenListSize = false;
    protected boolean clientAutocompleteFields = true;

    protected List<AltTarget> altTargets = FastList.newInstance();
    protected List<AutoFieldsService> autoFieldsServices = FastList.newInstance();
    protected List<AutoFieldsEntity> autoFieldsEntities = FastList.newInstance();
    protected List<String> sortOrderFields = FastList.newInstance();
    protected List<AltRowStyle> altRowStyles = FastList.newInstance();

    /** This List will contain one copy of each field for each field name in the order
     * they were encountered in the service, entity, or form definition; field definitions
     * with constraints will also be in this list but may appear multiple times for the same
     * field name.
     *
     * When rendering the form the order in this list should be following and it should not be
     * necessary to use the Map. The Map is used when loading the form definition to keep the
     * list clean and implement the override features for field definitions.
     */
    protected List<ModelFormField> fieldList = FastList.newInstance();

    /** This Map is keyed with the field name and has a ModelFormField for the value.
     */
    protected Map<String, ModelFormField> fieldMap = FastMap.newInstance();
    
    /** Keeps track of conditional fields to help ensure that only one is rendered
     */
    protected Set<String> useWhenFields = FastSet.newInstance();

    /** This is a list of FieldGroups in the order they were created.
     * Can also include Banner objects.
     */
    protected List<FieldGroupBase> fieldGroupList = FastList.newInstance();

    /** This Map is keyed with the field name and has a FieldGroup for the value.
     * Can also include Banner objects.
     */
    protected Map<String, FieldGroupBase> fieldGroupMap = FastMap.newInstance();

    /** This field group will be the "catch-all" group for fields that are not
     *  included in an explicit field-group.
     */
    protected FieldGroup defaultFieldGroup;

    /** Default hyperlink target. */
    public static String DEFAULT_TARGET_TYPE = "intra-app";

    /** Pagination settings and defaults. */
    public static int DEFAULT_PAGE_SIZE = 10;
    protected int defaultViewSize = DEFAULT_PAGE_SIZE;
    public static String DEFAULT_PAG_INDEX_FIELD = "viewIndex";
    public static String DEFAULT_PAG_SIZE_FIELD = "viewSize";
    public static String DEFAULT_PAG_STYLE = "nav-pager";
    public static String DEFAULT_PAG_FIRST_STYLE = "nav-first";
    public static String DEFAULT_PAG_PREV_STYLE = "nav-previous";
    public static String DEFAULT_PAG_NEXT_STYLE = "nav-next";
    public static String DEFAULT_PAG_LAST_STYLE = "nav-last";

    /** Sort field default styles. */
    public static String DEFAULT_SORT_FIELD_STYLE = "sort-order";
    public static String DEFAULT_SORT_FIELD_ASC_STYLE = "sort-order-asc";
    public static String DEFAULT_SORT_FIELD_DESC_STYLE = "sort-order-desc";

    protected List<ModelFormAction> actions;
    protected List<ModelFormAction> rowActions;
    protected FlexibleStringExpander rowCountExdr;
    protected List<ModelFormField> multiSubmitFields = FastList.newInstance();
    protected int rowCount = 0;

    /** On Submit areas to be updated. */
    protected List<UpdateArea> onSubmitUpdateAreas;
    /** On Paginate areas to be updated. */
    protected List<UpdateArea> onPaginateUpdateAreas;

    // ===== CONSTRUCTORS =====
    /** Default Constructor */
    public ModelForm() {}

    /** XML Constructor */
    public ModelForm(Element formElement, ModelReader entityModelReader, DispatchContext dispatchContext) {
        super(formElement);
        this.entityModelReader = entityModelReader;
        this.dispatchContext = dispatchContext;
        initForm(formElement);
    }

    public ModelForm(Element formElement) {
        super(formElement);
        initForm(formElement);
    }

    public void initForm(Element formElement) {

        setDefaultViewSize(UtilProperties.getPropertyValue("widget.properties", "widget.form.defaultViewSize"));
        // check if there is a parent form to inherit from
        String parentResource = formElement.getAttribute("extends-resource");
        String parentForm = formElement.getAttribute("extends");
        if (parentForm.length() > 0) {
            ModelForm parent = null;
            // check if we have a resource name (part of the string before the ?)
            if (parentResource.length() > 0) {
                try {
                    parent = FormFactory.getFormFromLocation(parentResource, parentForm, entityModelReader, dispatchContext);
                    this.parentFormName = parentForm;
                    this.parentFormLocation = parentResource;
                } catch (Exception e) {
                    Debug.logError(e, "Failed to load parent form definition '" + parentForm + "' at resource '" + parentResource + "'", module);
                }
            } else if (!parentForm.equals(formElement.getAttribute("name"))) {
                // try to find a form definition in the same file
                Element rootElement = formElement.getOwnerDocument().getDocumentElement();
                List<? extends Element> formElements = UtilXml.childElementList(rootElement, "form");
                //Uncomment below to add support for abstract forms
                //formElements.addAll(UtilXml.childElementList(rootElement, "abstract-form"));
                for (Element formElementEntry: formElements) {
                    if (formElementEntry.getAttribute("name").equals(parentForm)) {
                        parent = new ModelForm(formElementEntry, entityModelReader, dispatchContext);
                        break;
                    }
                }
                if (parent == null) {
                    Debug.logError("Failed to find parent form definition '" + parentForm + "' in same document.", module);
                } else {
                    this.parentFormName = parentForm;
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
                this.separateColumns = parent.separateColumns;
                this.targetType = parent.targetType;
                this.defaultMapName = parent.defaultMapName;
                this.targetWindowExdr = parent.targetWindowExdr;
                this.hideHeader = parent.hideHeader;
                this.clientAutocompleteFields = parent.clientAutocompleteFields;
                this.paginateTarget = parent.paginateTarget;

                this.altTargets.addAll(parent.altTargets);
                this.actions = parent.actions;
                this.rowActions = parent.rowActions;
                this.defaultViewSize = parent.defaultViewSize;
                this.onSubmitUpdateAreas = parent.onSubmitUpdateAreas;
                this.onPaginateUpdateAreas = parent.onPaginateUpdateAreas;
                this.altRowStyles = parent.altRowStyles;
                
                this.useWhenFields = parent.useWhenFields;

                //these are done below in a special way...
                //this.fieldList = parent.fieldList;
                //this.fieldMap = parent.fieldMap;

                // Create this fieldList/Map from clones of parent's
                for (ModelFormField parentChildField: parent.fieldList) {
                    ModelFormField childField = new ModelFormField(this);
                    childField.mergeOverrideModelFormField(parentChildField);
                    this.fieldList.add(childField);
                    this.fieldMap.put(childField.getName(), childField);
                }
            }
        }

        if (this.type == null || formElement.hasAttribute("type")) {
            this.type = formElement.getAttribute("type");
        }
        if (this.target == null || formElement.hasAttribute("target")) {
            setTarget(formElement.getAttribute("target"));
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
        if (this.defaultSortFieldStyle == null || formElement.hasAttribute("default-sort-field-style")) {
            this.defaultSortFieldStyle = formElement.getAttribute("default-sort-field-style");
        }
        if (this.defaultSortFieldAscStyle == null || formElement.hasAttribute("default-sort-field-asc-style")) {
            this.defaultSortFieldAscStyle = formElement.getAttribute("default-sort-field-asc-style");
        }
        if (this.defaultSortFieldDescStyle == null || formElement.hasAttribute("default-sort-field-desc-style")) {
            this.defaultSortFieldDescStyle = formElement.getAttribute("default-sort-field-desc-style");
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
            this.overrideListSize = FlexibleStringExpander.getInstance(formElement.getAttribute("override-list-size"));
        }
        if (this.paginateFirstLabel == null || formElement.hasAttribute("paginate-first-label")) {
            this.paginateFirstLabel = FlexibleStringExpander.getInstance(formElement.getAttribute("paginate-first-label"));
        }
        if (this.paginatePreviousLabel == null || formElement.hasAttribute("paginate-previous-label")) {
            this.paginatePreviousLabel = FlexibleStringExpander.getInstance(formElement.getAttribute("paginate-previous-label"));
        }
        if (this.paginateNextLabel == null || formElement.hasAttribute("paginate-next-label")) {
            this.paginateNextLabel = FlexibleStringExpander.getInstance(formElement.getAttribute("paginate-next-label"));
        }
        if (this.paginateLastLabel == null || formElement.hasAttribute("paginate-last-label")) {
            this.paginateLastLabel = FlexibleStringExpander.getInstance(formElement.getAttribute("paginate-last-label"));
        }
        if (this.paginateViewSizeLabel == null || formElement.hasAttribute("paginate-viewsize-label")) {
            this.paginateViewSizeLabel = FlexibleStringExpander.getInstance(formElement.getAttribute("paginate-viewsize-label"));
        }
        if (this.paginateStyle == null || formElement.hasAttribute("paginate-style")) {
            setPaginateStyle(formElement.getAttribute("paginate-style"));
        }

        this.paginate = "true".equals(formElement.getAttribute("paginate"));
        this.skipStart = "true".equals(formElement.getAttribute("skip-start"));
        this.skipEnd = "true".equals(formElement.getAttribute("skip-end"));
        this.hideHeader = "true".equals(formElement.getAttribute("hide-header"));
        this.clientAutocompleteFields = !"false".equals(formElement.getAttribute("client-autocomplete-fields"));
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
            this.rowCountExdr = FlexibleStringExpander.getInstance(formElement.getAttribute("row-count"));
        }

        //alt-row-styles
        for (Element altRowStyleElement : UtilXml.childElementList(formElement, "alt-row-style")) {
            AltRowStyle altRowStyle = new AltRowStyle(altRowStyleElement);
            this.altRowStyles.add(altRowStyle);
        }

        // alt-target
        for (Element altTargetElement: UtilXml.childElementList(formElement, "alt-target")) {
            AltTarget altTarget = new AltTarget(altTargetElement);
            this.addAltTarget(altTarget);
        }

        // on-event-update-area
        for (Element updateAreaElement : UtilXml.childElementList(formElement, "on-event-update-area")) {
            UpdateArea updateArea = new UpdateArea(updateAreaElement);
            this.addOnEventUpdateArea(updateArea);
        }

        // auto-fields-service
        for (Element autoFieldsServiceElement: UtilXml.childElementList(formElement, "auto-fields-service")) {
            AutoFieldsService autoFieldsService = new AutoFieldsService(autoFieldsServiceElement);
            this.addAutoFieldsFromService(autoFieldsService);
        }

        // auto-fields-entity
        for (Element autoFieldsEntityElement: UtilXml.childElementList(formElement, "auto-fields-entity")) {
            AutoFieldsEntity autoFieldsEntity = new AutoFieldsEntity(autoFieldsEntityElement);
            this.addAutoFieldsFromEntity(autoFieldsEntity);
        }

        // read in add field defs, add/override one by one using the fieldList and fieldMap
        String thisType = this.getType();
        for (Element fieldElement: UtilXml.childElementList(formElement, "field")) {
            ModelFormField modelFormField = new ModelFormField(fieldElement, this);
            ModelFormField.FieldInfo fieldInfo = modelFormField.getFieldInfo();
            if (thisType.equals("multi") && fieldInfo instanceof ModelFormField.SubmitField) {
               multiSubmitFields.add(modelFormField);
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
            for (Element sortFieldElement: UtilXml.childElementList(sortOrderElement)) {
                String tagName = sortFieldElement.getTagName();
                if (tagName.equals("sort-field")) {
                    String fieldName = sortFieldElement.getAttribute("name");
                    this.sortOrderFields.add(fieldName);
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
            List<ModelFormField> sortedFields = FastList.newInstance();
            for (String fieldName: this.sortOrderFields) {
                if (UtilValidate.isEmpty(fieldName)) {
                    continue;
                }

                // get all fields with the given name from the existing list and put them in the sorted list
                Iterator<ModelFormField> fieldIter = this.fieldList.iterator();
                while (fieldIter.hasNext()) {
                    ModelFormField modelFormField = fieldIter.next();
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
        if (!modelFormField.isUseWhenEmpty() || useWhenFields.contains(modelFormField.getName())) {
            useWhenFields.add(modelFormField.getName());
            // is a conditional field, add to the List but don't worry about the Map
            //for adding to list, see if there is another field with that name in the list and if so, put it before that one
            boolean inserted = false;
            for (int i = 0; i < this.fieldList.size(); i++) {
                ModelFormField curField = this.fieldList.get(i);
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
            ModelFormField existingField = this.fieldMap.get(modelFormField.getName());
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
        int index = altTargets.indexOf(altTarget);
        if (index != -1) {
            altTargets.set(index, altTarget);
        } else {
            altTargets.add(altTarget);
        }
    }

    public void addOnEventUpdateArea(UpdateArea updateArea) {
        // Event types are sorted as a convenience
        // for the rendering classes
        if ("paginate".equals(updateArea.getEventType())) {
            addOnPaginateUpdateArea(updateArea);
        } else if ("submit".equals(updateArea.getEventType())) {
            addOnSubmitUpdateArea(updateArea);
        }
    }

    protected void addOnSubmitUpdateArea(UpdateArea updateArea) {
        if (onSubmitUpdateAreas == null) {
            onSubmitUpdateAreas = FastList.newInstance();
        }
        int index = onSubmitUpdateAreas.indexOf(updateArea);
        if (index != -1) {
            onSubmitUpdateAreas.set(index, updateArea);
        } else {
            onSubmitUpdateAreas.add(updateArea);
        }
    }

    protected void addOnPaginateUpdateArea(UpdateArea updateArea) {
        if (onPaginateUpdateAreas == null) {
            onPaginateUpdateAreas = FastList.newInstance();
        }
        int index = onPaginateUpdateAreas.indexOf(updateArea);
        if (index != -1) {
            if (UtilValidate.isNotEmpty(updateArea.areaTarget)) {
                onPaginateUpdateAreas.set(index, updateArea);
            } else {
                // blank target indicates a removing override
                onPaginateUpdateAreas.remove(index);
            }
        } else {
            onPaginateUpdateAreas.add(updateArea);
        }
    }

    public void addAutoFieldsFromService(AutoFieldsService autoFieldsService) {
        autoFieldsServices.add(autoFieldsService);

        // read service def and auto-create fields
        ModelService modelService = null;
        try {
            modelService = this.dispatchContext.getModelService(autoFieldsService.serviceName);
        } catch (GenericServiceException e) {
            String errmsg = "Error finding Service with name " + autoFieldsService.serviceName + " for auto-fields-service in a form widget";
            Debug.logError(e, errmsg, module);
            throw new IllegalArgumentException(errmsg);
        }

        for (ModelParam modelParam: modelService.getInModelParamList()) {
            // skip auto params that the service engine populates...
            if ("userLogin".equals(modelParam.name) || "locale".equals(modelParam.name) || "timeZone".equals(modelParam.name) || "login.username".equals(modelParam.name) || "login.password".equals(modelParam.name)) {
                continue;
            }
            if (modelParam.formDisplay) {
                if (UtilValidate.isNotEmpty(modelParam.entityName) && UtilValidate.isNotEmpty(modelParam.fieldName)) {
                    ModelEntity modelEntity;
                    try {
                        modelEntity = this.entityModelReader.getModelEntity(modelParam.entityName);
                        if (modelEntity != null) {
                            ModelField modelField = modelEntity.getField(modelParam.fieldName);
                            if (modelField != null) {
                                // okay, populate using the entity field info...
                                ModelFormField modelFormField = this.addFieldFromEntityField(modelEntity, modelField, autoFieldsService.defaultFieldType, autoFieldsService.defaultPosition);
                                if (UtilValidate.isNotEmpty(autoFieldsService.mapName)) {
                                    modelFormField.setMapName(autoFieldsService.mapName);
                                }
                                modelFormField.setRequiredField(!modelParam.optional);
                                // continue to skip creating based on service param
                                continue;
                            }
                        }
                    } catch (GenericEntityException e) {
                        Debug.logError(e, module);
                    }
                }

                ModelFormField modelFormField = this.addFieldFromServiceParam(modelService, modelParam, autoFieldsService.defaultFieldType, autoFieldsService.defaultPosition);
                if (UtilValidate.isNotEmpty(autoFieldsService.mapName)) {
                    modelFormField.setMapName(autoFieldsService.mapName);
                }
            }
        }
    }

    public ModelFormField addFieldFromServiceParam(ModelService modelService, ModelParam modelParam, String defaultFieldType, int defaultPosition) {
        // create field def from service param def
        ModelFormField newFormField = new ModelFormField(this);
        newFormField.setName(modelParam.name);
        newFormField.setServiceName(modelService.name);
        newFormField.setAttributeName(modelParam.name);
        newFormField.setTitle(modelParam.formLabel);
        newFormField.setRequiredField(!modelParam.optional);
        newFormField.induceFieldInfoFromServiceParam(modelService, modelParam, defaultFieldType);
        newFormField.setPosition(defaultPosition);
        return this.addUpdateField(newFormField);
    }

    public void addAutoFieldsFromEntity(AutoFieldsEntity autoFieldsEntity) {
        autoFieldsEntities.add(autoFieldsEntity);
        // read entity def and auto-create fields
        ModelEntity modelEntity = null;
        try {
            modelEntity = this.entityModelReader.getModelEntity(autoFieldsEntity.entityName);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        if (modelEntity == null) {
            throw new IllegalArgumentException("Error finding Entity with name " + autoFieldsEntity.entityName + " for auto-fields-entity in a form widget");
        }

        Iterator<ModelField> modelFieldIter = modelEntity.getFieldsIterator();
        while (modelFieldIter.hasNext()) {
            ModelField modelField = modelFieldIter.next();
            if (modelField.getIsAutoCreatedInternal()) {
                // don't ever auto-add these, should only be added if explicitly referenced
                continue;
            }
            ModelFormField modelFormField = this.addFieldFromEntityField(modelEntity, modelField, autoFieldsEntity.defaultFieldType, autoFieldsEntity.defaultPosition);
            if (UtilValidate.isNotEmpty(autoFieldsEntity.mapName)) {
                modelFormField.setMapName(autoFieldsEntity.mapName);
            }
        }
    }

    public ModelFormField addFieldFromEntityField(ModelEntity modelEntity, ModelField modelField, String defaultFieldType, int defaultPosition) {
        // create field def from entity field def
        ModelFormField newFormField = new ModelFormField(this);
        newFormField.setName(modelField.getName());
        newFormField.setEntityName(modelEntity.getEntityName());
        newFormField.setFieldName(modelField.getName());
        newFormField.induceFieldInfoFromEntityField(modelEntity, modelField, defaultFieldType);
        newFormField.setPosition(defaultPosition);
        return this.addUpdateField(newFormField);
    }

    public void runFormActions(Map<String, Object> context) {
        ModelFormAction.runSubActions(this.actions, context);
    }

    /**
     * Renders this form to a String, i.e. in a text format, as defined with the
     * FormStringRenderer implementation.
     *
     * @param writer The Writer that the form text will be written to
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
    public void renderFormString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer) throws IOException {
        //  increment the paginator, only for list and multi forms
        if ("list".equals(this.type) || "multi".equals(this.type)) {
            this.incrementPaginatorNumber(context);
        }
        // Populate the viewSize and viewIndex so they are available for use during form actions
        context.put("viewIndex", this.getViewIndex(context));
        context.put("viewSize", this.getViewSize(context));

        runFormActions(context);

        setWidgetBoundaryComments(context);

        // if this is a list form, don't useRequestParameters
        if ("list".equals(this.type) || "multi".equals(this.type)) {
            context.put("useRequestParameters", Boolean.FALSE);
        }

        // find the highest position number to get the max positions used
        int positions = 1;
        for (ModelFormField modelFormField: this.fieldList) {
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
            this.renderSingleFormString(writer, context, formStringRenderer, positions);
        } else if ("list".equals(this.type)) {
            this.renderListFormString(writer, context, formStringRenderer, positions);
        } else if ("multi".equals(this.type)) {
            this.renderMultiFormString(writer, context, formStringRenderer, positions);
        } else if ("upload".equals(this.type)) {
            this.renderSingleFormString(writer, context, formStringRenderer, positions);
        } else {
            if (UtilValidate.isEmpty(this.getType())) {
                throw new IllegalArgumentException("The form 'type' tag is missing or empty on the form with the name " + this.getName());
            } else {
                throw new IllegalArgumentException("The form type " + this.getType() + " is not supported for form with name " + this.getName());
            }
        }
    }

    public void renderSingleFormString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer, int positions) throws IOException {
        List<ModelFormField> tempFieldList = FastList.newInstance();
        tempFieldList.addAll(this.fieldList);

        // Check to see if there is a field, same name and same use-when (could come from extended form)
        for (int j = 0; j < tempFieldList.size(); j++) {
            ModelFormField modelFormField = (ModelFormField) tempFieldList.get(j);
            if (this.useWhenFields.contains(modelFormField.getName())) {
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

        Set<String> alreadyRendered = new TreeSet<String>();
        FieldGroup lastFieldGroup = null;
        // render form open
        if (!skipStart) formStringRenderer.renderFormOpen(writer, context, this);

        // render all hidden & ignored fields
        List<ModelFormField> hiddenIgnoredFieldList = this.getHiddenIgnoredFields(context, alreadyRendered, tempFieldList, -1);
        this.renderHiddenIgnoredFields(writer, context, formStringRenderer, hiddenIgnoredFieldList);

        // render formatting wrapper open
        // This should be covered by fieldGroup.renderStartString
        //formStringRenderer.renderFormatSingleWrapperOpen(writer, context, this);

        // render each field row, except hidden & ignored rows
        Iterator<ModelFormField> fieldIter = tempFieldList.iterator();
        ModelFormField lastFormField = null;
        ModelFormField currentFormField = null;
        ModelFormField nextFormField = null;
        if (fieldIter.hasNext()) {
            currentFormField = fieldIter.next();
        }
        if (fieldIter.hasNext()) {
            nextFormField = fieldIter.next();
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
                List<FieldGroupBase> inbetweenList = getInbetweenList(lastFieldGroup, currentFieldGroup);
                for (FieldGroupBase obj: inbetweenList) {
                    if (obj instanceof ModelForm.Banner) {
                        ((ModelForm.Banner) obj).renderString(writer, context, formStringRenderer);
                    }
                }
                if (currentFieldGroup != null && (lastFieldGroup == null || !lastFieldGroupName.equals(currentFieldGroupName))) {
                    currentFieldGroup.renderStartString(writer, context, formStringRenderer);
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

                if (lastFieldGroup != null) {
                    lastFieldGroupName = lastFieldGroup.getId();
                    if (!lastFieldGroupName.equals(currentFieldGroupName)) {
                        lastFieldGroup.renderEndString(writer, context, formStringRenderer);

                        List<FieldGroupBase> inbetweenList = getInbetweenList(lastFieldGroup, currentFieldGroup);
                        for (FieldGroupBase obj: inbetweenList) {
                            if (obj instanceof ModelForm.Banner) {
                                ((ModelForm.Banner) obj).renderString(writer, context, formStringRenderer);
                            }
                        }
                    }
                }

                if (currentFieldGroup != null && (lastFieldGroup == null || !lastFieldGroupName.equals(currentFieldGroupName))) {
                        currentFieldGroup.renderStartString(writer, context, formStringRenderer);
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
                    nextPositionInRow = Integer.valueOf(nextFormField.getPosition());
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
                //formStringRenderer.renderFormatFieldRowSpacerCell(writer, context, currentFormField);
            } else {
                if (haveRenderedOpenFieldRow) {
                    // render row formatting close
                    formStringRenderer.renderFormatFieldRowClose(writer, context, this);
                    haveRenderedOpenFieldRow = false;
                }

                // render row formatting open
                formStringRenderer.renderFormatFieldRowOpen(writer, context, this);
                haveRenderedOpenFieldRow = true;
            }

            // render title formatting open
            formStringRenderer.renderFormatFieldRowTitleCellOpen(writer, context, currentFormField);

            // render title (unless this is a submit or a reset field)
            if (fieldInfo.getFieldType() != ModelFormField.FieldInfo.SUBMIT && fieldInfo.getFieldType() != ModelFormField.FieldInfo.RESET) {
                formStringRenderer.renderFieldTitle(writer, context, currentFormField);
            } else {
                formStringRenderer.renderFormatEmptySpace(writer, context, this);
            }

            // render title formatting close
            formStringRenderer.renderFormatFieldRowTitleCellClose(writer, context, currentFormField);

            // render separator
            formStringRenderer.renderFormatFieldRowSpacerCell(writer, context, currentFormField);

            // render widget formatting open
            formStringRenderer.renderFormatFieldRowWidgetCellOpen(writer, context, currentFormField, positions, positionSpan, nextPositionInRow);

            // render widget
            currentFormField.renderFieldString(writer, context, formStringRenderer);

            // render widget formatting close
            formStringRenderer.renderFormatFieldRowWidgetCellClose(writer, context, currentFormField, positions, positionSpan, nextPositionInRow);

        }
        // always render row formatting close after the end
        formStringRenderer.renderFormatFieldRowClose(writer, context, this);

        if (lastFieldGroup != null) {
            lastFieldGroup.renderEndString(writer, context, formStringRenderer);
        }
        // render formatting wrapper close
        // should be handled by renderEndString
        //formStringRenderer.renderFormatSingleWrapperClose(writer, context, this);

        // render form close
        if (!skipEnd) formStringRenderer.renderFormClose(writer, context, this);

    }

    public void renderListFormString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer, int positions) throws IOException {
        // render list/tabular type forms

        // prepare the items iterator and compute the pagination parameters
        this.preparePager(context);

        // render formatting wrapper open
        formStringRenderer.renderFormatListWrapperOpen(writer, context, this);

        int numOfColumns = 0;
        // ===== render header row =====
        if (!getHideHeader()) {
            numOfColumns = this.renderHeaderRow(writer, context, formStringRenderer);
        }

        // ===== render the item rows =====
        this.renderItemRows(writer, context, formStringRenderer, true, numOfColumns);

        // render formatting wrapper close
        formStringRenderer.renderFormatListWrapperClose(writer, context, this);

    }

    public void renderMultiFormString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer, int positions) throws IOException {
        if (!skipStart) {
            formStringRenderer.renderFormOpen(writer, context, this);
        }

        // render formatting wrapper open
        formStringRenderer.renderFormatListWrapperOpen(writer, context, this);

        int numOfColumns = 0;
        // ===== render header row =====
        numOfColumns = this.renderHeaderRow(writer, context, formStringRenderer);

        // ===== render the item rows =====
        this.renderItemRows(writer, context, formStringRenderer, false, numOfColumns);

        formStringRenderer.renderFormatListWrapperClose(writer, context, this);

        if (!skipEnd) {
            formStringRenderer.renderMultiFormClose(writer, context, this);
        }

    }

    public int renderHeaderRow(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer) throws IOException {
        int maxNumOfColumns = 0;

        // We will render one title/column for all the fields with the same name
        // in this model: we can have more fields with the same name when use-when
        // conditions are used or when a form is extended or when the fields are
        // automatically retrieved by a service or entity definition.
        List<ModelFormField> tempFieldList = FastList.newInstance();
        tempFieldList.addAll(this.fieldList);
        for (int j = 0; j < tempFieldList.size(); j++) {
            ModelFormField modelFormField = tempFieldList.get(j);
            for (int i = j+1; i < tempFieldList.size(); i++) {
                ModelFormField curField = tempFieldList.get(i);
                if (curField.getName() != null && curField.getName().equals(modelFormField.getName())) {
                    tempFieldList.remove(i--);
                }
            }
        }

        // ===========================
        // Preprocessing
        // ===========================
        // We get a sorted (by position, ascending) set of lists;
        // each list contains all the fields with that position.
        Collection<List<ModelFormField>> fieldListsByPosition = this.getFieldListsByPosition(tempFieldList);
        List<Map<String, List<ModelFormField>>> fieldRowsByPosition = FastList.newInstance(); // this list will contain maps, each one containing the list of fields for a position
        for (List<ModelFormField> mainFieldList: fieldListsByPosition) {
            int numOfColumns = 0;

            List<ModelFormField> innerDisplayHyperlinkFieldsBegin = FastList.newInstance();
            List<ModelFormField> innerFormFields = FastList.newInstance();
            List<ModelFormField> innerDisplayHyperlinkFieldsEnd = FastList.newInstance();

            // render title for each field, except hidden & ignored, etc

            // start by rendering all display and hyperlink fields, until we
            //get to a field that should go into the form cell, then render
            //the form cell with all non-display and non-hyperlink fields, then
            //do a start after the first form input field and
            //render all display and hyperlink fields after the form

            // prepare the two lists of display and hyperlink fields
            // the fields in the first list will be rendered as columns before the
            // combined column for the input fields; the fields in the second list
            // will be rendered as columns after it
            boolean inputFieldFound = false;
            for (ModelFormField modelFormField: mainFieldList) {
                ModelFormField.FieldInfo fieldInfo = modelFormField.getFieldInfo();

                // if the field's title is explicitly set to "" (title="") then
                // the header is not created for it; this is useful for position list
                // where one line can be rendered with more than one row, and we
                // only want to display the title header for the main row
                String modelFormFieldTitle = modelFormField.getTitle(context);
                if ("".equals(modelFormFieldTitle)) {
                    continue;
                }
                // don't do any header for hidden or ignored fields
                if (fieldInfo.getFieldType() == ModelFormField.FieldInfo.HIDDEN || fieldInfo.getFieldType() == ModelFormField.FieldInfo.IGNORED) {
                    continue;
                }

                if (fieldInfo.getFieldType() != ModelFormField.FieldInfo.DISPLAY && fieldInfo.getFieldType() != ModelFormField.FieldInfo.DISPLAY_ENTITY && fieldInfo.getFieldType() != ModelFormField.FieldInfo.HYPERLINK) {
                    inputFieldFound = true;
                    continue;
                }

                // separate into two lists the display/hyperlink fields found before and after the first input fields
                if (!inputFieldFound) {
                    innerDisplayHyperlinkFieldsBegin.add(modelFormField);
                } else {
                    innerDisplayHyperlinkFieldsEnd.add(modelFormField);
                }
                numOfColumns++;
            }

            // prepare the combined title for the column that will contain the form/input fields
            for (ModelFormField modelFormField: mainFieldList) {
                ModelFormField.FieldInfo fieldInfo = modelFormField.getFieldInfo();

                // don't do any header for hidden or ignored fields
                if (fieldInfo.getFieldType() == ModelFormField.FieldInfo.HIDDEN || fieldInfo.getFieldType() == ModelFormField.FieldInfo.IGNORED) {
                    continue;
                }

                // skip all of the display/hyperlink fields
                if (fieldInfo.getFieldType() == ModelFormField.FieldInfo.DISPLAY || fieldInfo.getFieldType() == ModelFormField.FieldInfo.DISPLAY_ENTITY || fieldInfo.getFieldType() == ModelFormField.FieldInfo.HYPERLINK) {
                    continue;
                }

                innerFormFields.add(modelFormField);
            }
            if (innerFormFields.size() > 0) {
                numOfColumns++;
            }

            if (maxNumOfColumns < numOfColumns) {
                maxNumOfColumns = numOfColumns;
            }

            Map<String, List<ModelFormField>> fieldRow = UtilMisc.toMap("displayBefore", innerDisplayHyperlinkFieldsBegin,
                                                   "inputFields", innerFormFields,
                                                   "displayAfter", innerDisplayHyperlinkFieldsEnd);
            fieldRowsByPosition.add(fieldRow);
        }
        // ===========================
        // Rendering
        // ===========================
        for (Map<String, List<ModelFormField>> listsMap: fieldRowsByPosition) {
            List<ModelFormField> innerDisplayHyperlinkFieldsBegin = listsMap.get("displayBefore");
            List<ModelFormField> innerFormFields = listsMap.get("inputFields");
            List<ModelFormField> innerDisplayHyperlinkFieldsEnd = listsMap.get("displayAfter");

            int numOfCells = innerDisplayHyperlinkFieldsBegin.size() +
                             innerDisplayHyperlinkFieldsEnd.size() +
                             (innerFormFields.size() > 0? 1: 0);
            int numOfColumnsToSpan = maxNumOfColumns - numOfCells + 1;
            if (numOfColumnsToSpan < 1) {
                numOfColumnsToSpan = 1;
            }

            if (numOfCells > 0) {
                formStringRenderer.renderFormatHeaderRowOpen(writer, context, this);
                Iterator<ModelFormField> innerDisplayHyperlinkFieldsBeginIt = innerDisplayHyperlinkFieldsBegin.iterator();
                while (innerDisplayHyperlinkFieldsBeginIt.hasNext()) {
                    ModelFormField modelFormField = innerDisplayHyperlinkFieldsBeginIt.next();
                    // span columns only if this is the last column in the row (not just in this first list)
                    if (innerDisplayHyperlinkFieldsBeginIt.hasNext() || numOfCells > innerDisplayHyperlinkFieldsBegin.size()) {
                        formStringRenderer.renderFormatHeaderRowCellOpen(writer, context, this, modelFormField, 1);
                    } else {
                        formStringRenderer.renderFormatHeaderRowCellOpen(writer, context, this, modelFormField, numOfColumnsToSpan);
                    }
                    formStringRenderer.renderFieldTitle(writer, context, modelFormField);
                    formStringRenderer.renderFormatHeaderRowCellClose(writer, context, this, modelFormField);
                }
                if (innerFormFields.size() > 0) {
                    // TODO: manage colspan
                    formStringRenderer.renderFormatHeaderRowFormCellOpen(writer, context, this);
                    Iterator<ModelFormField> innerFormFieldsIt = innerFormFields.iterator();
                    while (innerFormFieldsIt.hasNext()) {
                        ModelFormField modelFormField = innerFormFieldsIt.next();

                        if (separateColumns || modelFormField.getSeparateColumn()) {
                            formStringRenderer.renderFormatItemRowCellOpen(writer, context, this, modelFormField, 1);
                        }

                        // render title (unless this is a submit or a reset field)
                        formStringRenderer.renderFieldTitle(writer, context, modelFormField);

                        if (separateColumns || modelFormField.getSeparateColumn()) {
                            formStringRenderer.renderFormatItemRowCellClose(writer, context, this, modelFormField);
                        }

                        if (innerFormFieldsIt.hasNext()) {
                            // TODO: determine somehow if this is the last one... how?
                           if (!separateColumns && !modelFormField.getSeparateColumn()) {
                                formStringRenderer.renderFormatHeaderRowFormCellTitleSeparator(writer, context, this, modelFormField, false);
                           }
                        }
                    }
                    formStringRenderer.renderFormatHeaderRowFormCellClose(writer, context, this);
                }
                Iterator<ModelFormField> innerDisplayHyperlinkFieldsEndIt = innerDisplayHyperlinkFieldsEnd.iterator();
                while (innerDisplayHyperlinkFieldsEndIt.hasNext()) {
                    ModelFormField modelFormField = innerDisplayHyperlinkFieldsEndIt.next();
                    // span columns only if this is the last column in the row (not just in this first list)
                    if (innerDisplayHyperlinkFieldsEndIt.hasNext() || numOfCells > innerDisplayHyperlinkFieldsEnd.size()) {
                        formStringRenderer.renderFormatHeaderRowCellOpen(writer, context, this, modelFormField, 1);
                    } else {
                        formStringRenderer.renderFormatHeaderRowCellOpen(writer, context, this, modelFormField, numOfColumnsToSpan);
                    }
                    formStringRenderer.renderFieldTitle(writer, context, modelFormField);
                    formStringRenderer.renderFormatHeaderRowCellClose(writer, context, this, modelFormField);
                }
                formStringRenderer.renderFormatHeaderRowClose(writer, context, this);
            }
        }

        return maxNumOfColumns;
    }

    protected <X> X safeNext(Iterator<X> iterator) {
        try {
            return iterator.next();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public void preparePager(Map<String, Object> context) {

        this.rowCount = 0;
        String lookupName = this.getListName();
        if (UtilValidate.isEmpty(lookupName)) {
            Debug.logError("No value for list or iterator name found.", module);
            return;
        }
        Object obj = context.get(lookupName);
        if (obj == null) {
            if (Debug.verboseOn()) Debug.logVerbose("No object for list or iterator name [" + lookupName + "] found, so not running pagination.", module);
            return;
        }
        // if list is empty, do not render rows
        Iterator<?> iter = null;
        if (obj instanceof Iterator) {
            iter = (Iterator<?>) obj;
            setPaginate(true);
        } else if (obj instanceof List) {
            iter = ((List<?>) obj).listIterator();
            setPaginate(true);
        }

        // set low and high index
        getListLimits(context, obj);

        int listSize = ((Integer) context.get("listSize")).intValue();
        int lowIndex = ((Integer) context.get("lowIndex")).intValue();
        int highIndex = ((Integer) context.get("highIndex")).intValue();
        // Debug.logInfo("preparePager: low - high = " + lowIndex + " - " + highIndex, module);

        // we're passed a subset of the list, so use (0, viewSize) range
        if (isOverridenListSize()) {
            lowIndex = 0;
            highIndex = ((Integer) context.get("viewSize")).intValue();
        }

        if (iter == null) return;

        // count item rows
        int itemIndex = -1;
        Object item = this.safeNext(iter);
        while (item != null && itemIndex < highIndex) {
            itemIndex++;
            item = this.safeNext(iter);
        }

        // Debug.logInfo("preparePager: Found rows = " + itemIndex, module);

        // reduce the highIndex if number of items falls short
        if ((itemIndex + 1) < highIndex) {
            highIndex = itemIndex + 1;
            // if list size is overridden, use full listSize
            context.put("highIndex", Integer.valueOf(isOverridenListSize() ? listSize : highIndex));
        }
        context.put("actualPageSize", Integer.valueOf(highIndex - lowIndex));

        if (iter instanceof EntityListIterator) {
            try {
                ((EntityListIterator) iter).beforeFirst();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error rewinding list form render EntityListIterator: " + e.toString(), module);
            }
        }
    }

    public void renderItemRows(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer, boolean formPerItem, int numOfColumns) throws IOException {
        this.rowCount = 0;
        String lookupName = this.getListName();
        if (UtilValidate.isEmpty(lookupName)) {
            Debug.logError("No value for list or iterator name found.", module);
            return;
        }
        Object obj = context.get(lookupName);
        if (obj == null) {
            if (Debug.verboseOn()) Debug.logVerbose("No object for list or iterator name [" + lookupName + "] found, so not rendering rows.", module);
            return;
        }
        // if list is empty, do not render rows
        Iterator<?> iter = null;
        if (obj instanceof Iterator) {
            iter = (Iterator<?>) obj;
            setPaginate(true);
        } else if (obj instanceof List) {
            iter = ((List<?>) obj).listIterator();
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
            context.put("wholeFormContext", context);
            Map<String, Object> previousItem = FastMap.newInstance();
            while ((item = this.safeNext(iter)) != null) {
                itemIndex++;
                if (itemIndex >= highIndex) {
                    break;
                }

                // TODO: this is a bad design, for EntityListIterators we should skip to the lowIndex and go from there, MUCH more efficient...
                if (itemIndex < lowIndex) {
                    continue;
                }

                Map<String, Object> itemMap = UtilGenerics.checkMap(item);
                MapStack<String> localContext = MapStack.create(context);
                if (UtilValidate.isNotEmpty(this.getListEntryName())) {
                    localContext.put(this.getListEntryName(), item);
                } else {
                    localContext.push(itemMap);
                }

                localContext.push();
                localContext.put("previousItem", previousItem);
                previousItem = FastMap.newInstance();
                previousItem.putAll(itemMap);

                ModelFormAction.runSubActions(this.rowActions, localContext);

                localContext.put("itemIndex", Integer.valueOf(itemIndex - lowIndex));
                if (UtilValidate.isNotEmpty(context.get("renderFormSeqNumber"))) {
                    localContext.put("formUniqueId", "_"+context.get("renderFormSeqNumber"));
                }
                
                this.resetBshInterpreter(localContext);

                if (Debug.verboseOn()) Debug.logVerbose("In form got another row, context is: " + localContext, module);

                // Check to see if there is a field, same name and same use-when (could come from extended form)
                List<ModelFormField> tempFieldList = FastList.newInstance();
                tempFieldList.addAll(this.fieldList);
                for (int j = 0; j < tempFieldList.size(); j++) {
                    ModelFormField modelFormField = (ModelFormField) tempFieldList.get(j);
                    if (!modelFormField.isUseWhenEmpty()) {
                        boolean shouldUse1 = modelFormField.shouldUse(localContext);
                        for (int i = j+1; i < tempFieldList.size(); i++) {
                            ModelFormField curField = (ModelFormField) tempFieldList.get(i);
                            if (curField.getName() != null && curField.getName().equals(modelFormField.getName())) {
                                boolean shouldUse2 = curField.shouldUse(localContext);
                                if (shouldUse1 == shouldUse2) {
                                    tempFieldList.remove(i--);
                                }
                            } else {
                                continue;
                            }
                        }
                    }
                }

                // Each single item is rendered in one or more rows if its fields have
                // different "position" attributes. All the fields with the same position
                // are rendered in the same row.
                // The default position is 1, and represents the main row:
                // it contains the fields that are in the list header (columns).
                // The positions lower than 1 are rendered in rows before the main one;
                // positions higher than 1 are rendered after the main one.

                // We get a sorted (by position, ascending) set of lists;
                // each list contains all the fields with that position.
                Collection<List<ModelFormField>> fieldListsByPosition = this.getFieldListsByPosition(tempFieldList);
                //List hiddenIgnoredFieldList = getHiddenIgnoredFields(localContext, null, tempFieldList);
                for (List<ModelFormField> fieldListByPosition: fieldListsByPosition) {
                    // For each position (the subset of fields with the same position attribute)
                    // we have two phases: preprocessing and rendering
                    this.rowCount++;

                    List<ModelFormField> innerDisplayHyperlinkFieldsBegin = FastList.newInstance();
                    List<ModelFormField> innerFormFields = FastList.newInstance();
                    List<ModelFormField> innerDisplayHyperlinkFieldsEnd = FastList.newInstance();

                    // Preprocessing:
                    // all the form fields are evaluated and the ones that will
                    // appear in the form are put into three separate lists:
                    // - hyperlink fields that will appear at the beginning of the row
                    // - fields of other types
                    // - hyperlink fields that will appear at the end of the row
                    Iterator<ModelFormField> innerDisplayHyperlinkFieldIter = fieldListByPosition.iterator();
                    int currentPosition = 1;
                    while (innerDisplayHyperlinkFieldIter.hasNext()) {
                        ModelFormField modelFormField = innerDisplayHyperlinkFieldIter.next();
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
                        innerDisplayHyperlinkFieldsBegin.add(modelFormField);
                        currentPosition = modelFormField.getPosition();
                    }
                    Iterator<ModelFormField> innerFormFieldIter = fieldListByPosition.iterator();
                    while (innerFormFieldIter.hasNext()) {
                        ModelFormField modelFormField = innerFormFieldIter.next();
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
                        innerFormFields.add(modelFormField);
                        currentPosition = modelFormField.getPosition();
                    }
                    while (innerDisplayHyperlinkFieldIter.hasNext()) {
                        ModelFormField modelFormField = innerDisplayHyperlinkFieldIter.next();
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
                        innerDisplayHyperlinkFieldsEnd.add(modelFormField);
                        currentPosition = modelFormField.getPosition();
                    }
                    List<ModelFormField> hiddenIgnoredFieldList = getHiddenIgnoredFields(localContext, null, tempFieldList, currentPosition);

                    // Rendering:
                    // the fields in the three lists created in the preprocessing phase
                    // are now rendered: this will create a visual representation
                    // of one row (for the current position).
                    if (innerDisplayHyperlinkFieldsBegin.size() > 0 || innerFormFields.size() > 0 || innerDisplayHyperlinkFieldsEnd.size() > 0) {
                        this.renderItemRow(writer, localContext, formStringRenderer, formPerItem, hiddenIgnoredFieldList, innerDisplayHyperlinkFieldsBegin, innerFormFields, innerDisplayHyperlinkFieldsEnd, currentPosition, numOfColumns);
                    }
                } // iteration on positions
            } // iteration on items

            // reduce the highIndex if number of items falls short
            if ((itemIndex + 1) < highIndex) {
                highIndex = itemIndex + 1;
                // if list size is overridden, use full listSize
                context.put("highIndex", Integer.valueOf(isOverridenListSize() ? listSize : highIndex));
            }
            context.put("actualPageSize", Integer.valueOf(highIndex - lowIndex));

            if (iter instanceof EntityListIterator) {
                try {
                    ((EntityListIterator) iter).close();
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Error closing list form render EntityListIterator: " + e.toString(), module);
                }
            }
        }
    }

    // The fields in the three lists, usually created in the preprocessing phase
    // of the renderItemRows method are rendered: this will create a visual representation
    // of one row (corresponding to one position).
    public void renderItemRow(Appendable writer, Map<String, Object> localContext, FormStringRenderer formStringRenderer, boolean formPerItem, List<ModelFormField> hiddenIgnoredFieldList, List<ModelFormField> innerDisplayHyperlinkFieldsBegin, List<ModelFormField> innerFormFields, List<ModelFormField> innerDisplayHyperlinkFieldsEnd, int position, int numOfColumns) throws IOException {
        int numOfCells = innerDisplayHyperlinkFieldsBegin.size() +
                         innerDisplayHyperlinkFieldsEnd.size() +
                         (innerFormFields.size() > 0? 1: 0);
        int numOfColumnsToSpan = numOfColumns - numOfCells + 1;
        if (numOfColumnsToSpan < 1) {
            numOfColumnsToSpan = 1;
        }

        // render row formatting open
        formStringRenderer.renderFormatItemRowOpen(writer, localContext, this);

        // do the first part of display and hyperlink fields
        Iterator<ModelFormField> innerDisplayHyperlinkFieldIter = innerDisplayHyperlinkFieldsBegin.iterator();
        while (innerDisplayHyperlinkFieldIter.hasNext()) {
            ModelFormField modelFormField = innerDisplayHyperlinkFieldIter.next();
            // span columns only if this is the last column in the row (not just in this first list)
            if (innerDisplayHyperlinkFieldIter.hasNext() || numOfCells > innerDisplayHyperlinkFieldsBegin.size()) {
                formStringRenderer.renderFormatItemRowCellOpen(writer, localContext, this, modelFormField, 1);
            } else {
                formStringRenderer.renderFormatItemRowCellOpen(writer, localContext, this, modelFormField, numOfColumnsToSpan);
            }
            modelFormField.renderFieldString(writer, localContext, formStringRenderer);
            formStringRenderer.renderFormatItemRowCellClose(writer, localContext, this, modelFormField);
        }

        // The form cell is rendered only if there is at least an input field
        if (innerFormFields.size() > 0) {
            // render the "form" cell
            formStringRenderer.renderFormatItemRowFormCellOpen(writer, localContext, this); // TODO: colspan

            if (formPerItem) {
                formStringRenderer.renderFormOpen(writer, localContext, this);
            }

            // do all of the hidden fields...
            this.renderHiddenIgnoredFields(writer, localContext, formStringRenderer, hiddenIgnoredFieldList);

            Iterator<ModelFormField> innerFormFieldIter = innerFormFields.iterator();
            while (innerFormFieldIter.hasNext()) {
                ModelFormField modelFormField = innerFormFieldIter.next();
                if (separateColumns || modelFormField.getSeparateColumn()) {
                    formStringRenderer.renderFormatItemRowCellOpen(writer, localContext, this, modelFormField, 1);
                }
                // render field widget
                modelFormField.renderFieldString(writer, localContext, formStringRenderer);
                if (separateColumns || modelFormField.getSeparateColumn()) {
                    formStringRenderer.renderFormatItemRowCellClose(writer, localContext, this, modelFormField);
                }
            }

            if (formPerItem) {
                formStringRenderer.renderFormClose(writer, localContext, this);
            }

            formStringRenderer.renderFormatItemRowFormCellClose(writer, localContext, this);
        }

        // render the rest of the display/hyperlink fields
        innerDisplayHyperlinkFieldIter = innerDisplayHyperlinkFieldsEnd.iterator();
        while (innerDisplayHyperlinkFieldIter.hasNext()) {
            ModelFormField modelFormField = innerDisplayHyperlinkFieldIter.next();
            // span columns only if this is the last column in the row
            if (innerDisplayHyperlinkFieldIter.hasNext()) {
                formStringRenderer.renderFormatItemRowCellOpen(writer, localContext, this, modelFormField, 1);
            } else {
                formStringRenderer.renderFormatItemRowCellOpen(writer, localContext, this, modelFormField, numOfColumnsToSpan);
            }
            modelFormField.renderFieldString(writer, localContext, formStringRenderer);
            formStringRenderer.renderFormatItemRowCellClose(writer, localContext, this, modelFormField);
        }

        // render row formatting close
        formStringRenderer.renderFormatItemRowClose(writer, localContext, this);
    }

    public List<ModelFormField> getHiddenIgnoredFields(Map<String, Object> context, Set<String> alreadyRendered, List<ModelFormField> fieldList, int position) {
        List<ModelFormField> hiddenIgnoredFieldList = FastList.newInstance();
        for (ModelFormField modelFormField: fieldList) {
            // with position == -1 then gets all the hidden fields
            if (position != -1 && modelFormField.getPosition() != position) {
                continue;
            }
            ModelFormField.FieldInfo fieldInfo = modelFormField.getFieldInfo();

            // render hidden/ignored field widget
            switch (fieldInfo.getFieldType()) {
                case ModelFormField.FieldInfo.HIDDEN :
                case ModelFormField.FieldInfo.IGNORED :
                    if (modelFormField.shouldUse(context)) {
                        hiddenIgnoredFieldList.add(modelFormField);
                        if (alreadyRendered != null)
                            alreadyRendered.add(modelFormField.getName());
                    }
                    break;

                case ModelFormField.FieldInfo.DISPLAY :
                case ModelFormField.FieldInfo.DISPLAY_ENTITY :
                    ModelFormField.DisplayField displayField = (ModelFormField.DisplayField) fieldInfo;
                    if (displayField.getAlsoHidden() && modelFormField.shouldUse(context)) {
                        hiddenIgnoredFieldList.add(modelFormField);
                        // don't add to already rendered here, or the display won't ger rendered: if (alreadyRendered != null) alreadyRendered.add(modelFormField.getName());
                    }
                    break;

                case ModelFormField.FieldInfo.HYPERLINK :
                    ModelFormField.HyperlinkField hyperlinkField = (ModelFormField.HyperlinkField) fieldInfo;
                    if (hyperlinkField.getAlsoHidden() && modelFormField.shouldUse(context)) {
                        hiddenIgnoredFieldList.add(modelFormField);
                        // don't add to already rendered here, or the hyperlink won't ger rendered: if (alreadyRendered != null) alreadyRendered.add(modelFormField.getName());
                    }
                    break;
            }
        }
        return hiddenIgnoredFieldList;
    }
    public void renderHiddenIgnoredFields(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer, List<ModelFormField> fieldList) throws IOException {
        for (ModelFormField modelFormField: fieldList) {
            ModelFormField.FieldInfo fieldInfo = modelFormField.getFieldInfo();

            // render hidden/ignored field widget
            switch (fieldInfo.getFieldType()) {
                case ModelFormField.FieldInfo.HIDDEN :
                case ModelFormField.FieldInfo.IGNORED :
                    modelFormField.renderFieldString(writer, context, formStringRenderer);
                    break;

                case ModelFormField.FieldInfo.DISPLAY :
                case ModelFormField.FieldInfo.DISPLAY_ENTITY :
                case ModelFormField.FieldInfo.HYPERLINK :
                    formStringRenderer.renderHiddenField(writer, context, modelFormField, modelFormField.getEntry(context));
                    break;
            }
        }
    }

    public Collection<List<ModelFormField>> getFieldListsByPosition(List<ModelFormField> modelFormFieldList) {
        Map<Integer, List<ModelFormField>> fieldsByPosition = new TreeMap<Integer, List<ModelFormField>>();
        for (ModelFormField modelFormField: modelFormFieldList) {
            Integer position = Integer.valueOf(modelFormField.getPosition());
            List<ModelFormField> fieldListByPosition = fieldsByPosition.get(position);
            if (fieldListByPosition == null) {
                fieldListByPosition = FastList.newInstance();
                fieldsByPosition.put(position, fieldListByPosition);
            }
            fieldListByPosition.add(modelFormField);
        }
        return fieldsByPosition.values();
    }

    public List<ModelFormField> getFieldListByPosition(List<ModelFormField> modelFormFieldList, int position) {
        List<ModelFormField> fieldListByPosition = FastList.newInstance();
        for (ModelFormField modelFormField: modelFormFieldList) {
            if (modelFormField.getPosition() == position) {
                fieldListByPosition.add(modelFormField);
            }
        }
        return fieldListByPosition;
    }


    public LocalDispatcher getDispatcher(Map<String, Object> context) {
        LocalDispatcher dispatcher = (LocalDispatcher) context.get("dispatcher");
        return dispatcher;
    }

    public Delegator getDelegator(Map<String, Object> context) {
        Delegator delegator = (Delegator) context.get("delegator");
        return delegator;
    }

    public String getTargetType() {
        return this.targetType;
    }

    public String getParentFormName() {
        return this.parentFormName;
    }

    public String getParentFormLocation() {
        if (this.parentFormName != null && this.parentFormLocation == null) {
            return this.formLocation;
        }
        return this.parentFormLocation;
    }

    public String getDefaultEntityName() {
        return this.defaultEntityName;
    }

    public String getDefaultMapName() {
        return this.defaultMapName.getOriginalName();
    }

    public Map<String, ? extends Object> getDefaultMap(Map<String, ? extends Object> context) {
        return this.defaultMapName.get(context);
    }

    public String getDefaultRequiredFieldStyle() {
        return this.defaultRequiredFieldStyle;
    }

    public String getDefaultSortFieldStyle() {
        return (UtilValidate.isEmpty(this.defaultSortFieldStyle) ? DEFAULT_SORT_FIELD_STYLE : this.defaultSortFieldStyle);
    }

    public String getDefaultSortFieldAscStyle() {
        return (UtilValidate.isEmpty(this.defaultSortFieldAscStyle) ? DEFAULT_SORT_FIELD_ASC_STYLE : this.defaultSortFieldAscStyle);
    }

    public String getDefaultSortFieldDescStyle() {
        return (UtilValidate.isEmpty(this.defaultSortFieldDescStyle) ? DEFAULT_SORT_FIELD_DESC_STYLE : this.defaultSortFieldDescStyle);
    }


    public String getDefaultServiceName() {
        return this.defaultServiceName;
    }

    public String getFormTitleAreaStyle() {
        return this.formTitleAreaStyle;
    }

    public String getFormWidgetAreaStyle() {
        return this.formWidgetAreaStyle;
    }

    public String getDefaultTitleAreaStyle() {
        return this.defaultTitleAreaStyle;
    }

    public String getDefaultWidgetAreaStyle() {
        return this.defaultWidgetAreaStyle;
    }

    public String getOddRowStyle() {
        return this.oddRowStyle;
    }

    public String getEvenRowStyle() {
        return this.evenRowStyle;
    }

    public String getDefaultTableStyle() {
        return this.defaultTableStyle;
    }

    public String getHeaderRowStyle() {
        return this.headerRowStyle;
    }

    public String getDefaultTitleStyle() {
        return this.defaultTitleStyle;
    }

    public String getDefaultWidgetStyle() {
        return this.defaultWidgetStyle;
    }

    public String getDefaultTooltipStyle() {
        return this.defaultTooltipStyle;
    }

    public String getItemIndexSeparator() {
        if (UtilValidate.isNotEmpty(this.itemIndexSeparator)) {
            return this.itemIndexSeparator;
        } else {
            return "_o_";
        }
    }

    public String getListEntryName() {
        return this.listEntryName;
    }

    public String getListName() {
        String lstNm =  this.listName;
        if (UtilValidate.isEmpty(lstNm)) {
            lstNm = DEFAULT_FORM_RESULT_LIST_NAME;
        }
        return lstNm;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public String getCurrentFormName(Map<String, Object> context) {
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
    public String getTarget(Map<String, Object> context, String targetType) {
        Map<String, Object> expanderContext = context;
        StringUtil.SimpleEncoder simpleEncoder = (StringUtil.SimpleEncoder) context.get("simpleEncoder");
        if (simpleEncoder != null) {
            expanderContext = StringUtil.HtmlEncodingMapWrapper.getHtmlEncodingMapWrapper(context, simpleEncoder);
        }

        try {
            // use the same Interpreter (ie with the same context setup) for all evals
            Interpreter bsh = this.getBshInterpreter(context);
            for (AltTarget altTarget: this.altTargets) {
                Object retVal = bsh.eval(StringUtil.convertOperatorSubstitutions(altTarget.useWhen));
                boolean condTrue = false;
                // retVal should be a Boolean, if not something weird is up...
                if (retVal instanceof Boolean) {
                    Boolean boolVal = (Boolean) retVal;
                    condTrue = boolVal.booleanValue();
                } else {
                    throw new IllegalArgumentException(
                        "Return value from target condition eval was not a Boolean: " + retVal.getClass().getName() + " [" + retVal + "] of form " + this.name);
                }

                if (condTrue && !targetType.equals("inter-app")) {
                    return altTarget.targetExdr.expandString(expanderContext);
                }
            }
        } catch (EvalError e) {
            String errmsg = "Error evaluating BeanShell target conditions on form " + this.name;
            Debug.logError(e, errmsg, module);
            throw new IllegalArgumentException(errmsg);
        }

        return target.expandString(expanderContext);
    }

    public String getContainerId() {
        // use the name if there is no id
        if (UtilValidate.isNotEmpty(this.containerId)) {
            return this.containerId;
        } else {
            return this.getName();
        }
    }
    
    public String getCurrentContainerId(Map<String, Object> context) {
        Integer itemIndex = (Integer) context.get("itemIndex");
        if (itemIndex != null && "list".equals(this.getType())) {
            return this.getContainerId() + this.getItemIndexSeparator() + itemIndex.intValue();
        } 
        
        return this.getContainerId();
    }
    
    public String getContainerStyle() {
        return this.containerStyle;
    }

    public String getfocusFieldName() {
        return this.focusFieldName;
    }

    public String getTitle() {
        return this.title;
    }

    public String getTooltip() {
        return this.tooltip;
    }

    public String getType() {
        return this.type;
    }

    @Override
    public String getBoundaryCommentName() {
        return formLocation + "#" + name;
    }

    public void resetBshInterpreter(Map<String, Object> context) {
        context.remove("bshInterpreter");
    }

    public Interpreter getBshInterpreter(Map<String, Object> context) throws EvalError {
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
        this.defaultMapName = FlexibleMapAccessor.getInstance(string);
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
     * @param string Form's location
     */
    public void setFormLocation(String formLocation) {
        this.formLocation = formLocation;
    }

    public String getFormLocation() {
        return this.formLocation;
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
        this.target = FlexibleStringExpander.getInstance(string);
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

    public List<UpdateArea> getOnPaginateUpdateAreas() {
        return this.onPaginateUpdateAreas;
    }

    public String getPaginateTarget(Map<String, Object> context) {
        String targ = this.paginateTarget.expandString(context);
        if (UtilValidate.isEmpty(targ)) {
            Map<String, ?> parameters = UtilGenerics.cast(context.get("parameters"));
            if (parameters != null && parameters.containsKey("targetRequestUri")) {
                targ = (String) parameters.get("targetRequestUri");
            }
        }
        return targ;
    }

    public String getPaginateTargetAnchor() {
        return this.paginateTargetAnchor;
    }

    public String getPaginateIndexField(Map<String, Object> context) {
        String field = this.paginateIndexField.expandString(context);
        if (UtilValidate.isEmpty(field)) {
            field = DEFAULT_PAG_INDEX_FIELD;
        }
        return field;
    }
    
    public String getMultiPaginateIndexField(Map<String, Object> context) {
        String field = this.paginateIndexField.expandString(context);
        if (UtilValidate.isEmpty(field)) {
            field = DEFAULT_PAG_INDEX_FIELD;
        }
        //  append the paginator number
        field = field + "_" + getPaginatorNumber(context);
        return field;
    }

    public int getPaginateIndex(Map<String, Object> context) {
        String field = this.getMultiPaginateIndexField(context);

        int viewIndex = 0;
        try {
            Object value = context.get(field);

            if (value == null) {
                // try parameters.VIEW_INDEX as that is an old OFBiz convention
                Map<String, Object> parameters = UtilGenerics.cast(context.get("parameters"));
                if (parameters != null) {
                    value = parameters.get("VIEW_INDEX" + "_" + getPaginatorNumber(context));

                    if (value == null) {
                        value = parameters.get(field);
                    }
                }
            }
            
            // try paginate index field without paginator number
            if (value == null) {
                field = this.getPaginateIndexField(context);
                value = context.get(field);
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

    public String getPaginateSizeField(Map<String, Object> context) {
        String field = this.paginateSizeField.expandString(context);
        if (UtilValidate.isEmpty(field)) {
            field = DEFAULT_PAG_SIZE_FIELD;
        }
        return field;
    }
    
    public String getMultiPaginateSizeField(Map<String, Object> context) {
        String field = this.paginateSizeField.expandString(context);
        if (UtilValidate.isEmpty(field)) {
            field = DEFAULT_PAG_SIZE_FIELD;
        }
        //  append the paginator number
        field = field + "_" + getPaginatorNumber(context);
        return field;
    }

    public int getPaginateSize(Map<String, Object> context) {
        String field = this.getMultiPaginateSizeField(context);

        int viewSize = this.defaultViewSize;
        try {
            Object value = context.get(field);

            if (value == null) {
                // try parameters.VIEW_SIZE as that is an old OFBiz convention
                Map<String, Object> parameters = UtilGenerics.cast(context.get("parameters"));
                if (parameters != null) {
                    value = parameters.get("VIEW_SIZE" + "_" + getPaginatorNumber(context));

                    if (value == null) {
                        value = parameters.get(field);
                    }
                }
            }
            
            // try the page size field without paginator number
            if (value == null) {
                field = this.getPaginateSizeField(context);
                value = context.get(field);
            }

            if (value instanceof Integer) {
                viewSize = ((Integer) value).intValue();
            } else if (value instanceof String && UtilValidate.isNotEmpty(value)) {
                viewSize = Integer.parseInt((String) value);
            }
        } catch (Exception e) {
            Debug.logWarning(e, "Error getting paginate view size: " + e.toString(), module);
        }

        return viewSize;
    }

    public String getPaginateFirstLabel(Map<String, Object> context) {
        Locale locale = (Locale)context.get("locale");
        String field = this.paginateFirstLabel.expandString(context);
        if (UtilValidate.isEmpty(field)) {
            field = UtilProperties.getMessage("CommonUiLabels", "CommonFirst", locale);
        }
        return field;
    }

    public String getPaginatePreviousLabel(Map<String, Object> context) {
        Locale locale = (Locale)context.get("locale");
        String field = this.paginatePreviousLabel.expandString(context);
        if (UtilValidate.isEmpty(field)) {
            field = UtilProperties.getMessage("CommonUiLabels", "CommonPrevious", locale);
        }
        return field;
    }

    public String getPaginateNextLabel(Map<String, Object> context) {
        Locale locale = (Locale)context.get("locale");
        String field = this.paginateNextLabel.expandString(context);
        if (UtilValidate.isEmpty(field)) {
            field = UtilProperties.getMessage("CommonUiLabels", "CommonNext", locale);
        }
        return field;
    }

    public String getPaginateLastLabel(Map<String, Object> context) {
        Locale locale = (Locale)context.get("locale");
        String field = this.paginateLastLabel.expandString(context);
        if (UtilValidate.isEmpty(field)) {
            field = UtilProperties.getMessage("CommonUiLabels", "CommonLast", locale);
        }
        return field;
    }

    public String getPaginateViewSizeLabel(Map<String, Object> context) {
        Locale locale = (Locale)context.get("locale");
        String field = this.paginateViewSizeLabel.expandString(context);
        if (UtilValidate.isEmpty(field)) {
            field = UtilProperties.getMessage("CommonUiLabels", "CommonItemsPerPage", locale);
        }
        return field;
    }

    public String getPaginateStyle() {
        return this.paginateStyle;
    }

    public String getPaginateFirstStyle() {
        return DEFAULT_PAG_FIRST_STYLE;
    }

    public String getPaginatePreviousStyle() {
        return DEFAULT_PAG_PREV_STYLE;
    }

    public String getPaginateNextStyle() {
        return DEFAULT_PAG_NEXT_STYLE;
    }

    public String getPaginateLastStyle() {
        return DEFAULT_PAG_LAST_STYLE;
    }

    public String getTargetWindow(Map<String, Object> context) {
        return this.targetWindowExdr.expandString(context);
    }

    public void setTargetWindow(String val) {
        this.targetWindowExdr = FlexibleStringExpander.getInstance(val);
    }

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

    public boolean getClientAutocompleteFields() {
        return this.clientAutocompleteFields;
    }

    public void setPaginate(boolean val) {
        paginate = val;
    }

    public void setOverridenListSize(boolean overridenListSize) {
        this.overridenListSize = overridenListSize;
    }

    public void setPaginateTarget(String string) {
        this.paginateTarget = FlexibleStringExpander.getInstance(string);
    }

    public void setPaginateIndexField(String string) {
        this.paginateIndexField = FlexibleStringExpander.getInstance(string);
    }

    public void setPaginateSizeField(String string) {
        this.paginateSizeField = FlexibleStringExpander.getInstance(string);
    }

    public void setPaginateStyle(String string) {
        this.paginateStyle = (UtilValidate.isEmpty(string) ? DEFAULT_PAG_STYLE : string);
    }

    public void setDefaultViewSize(int val) {
        defaultViewSize = val;
    }

    public void setDefaultViewSize(String val) {
        try {
            Integer sz = Integer.valueOf(val);
            defaultViewSize = sz.intValue();
        } catch (NumberFormatException e) {
            defaultViewSize = DEFAULT_PAGE_SIZE;
        }
    }

    public int getListSize(Map<String, Object> context) {
        Integer value = (Integer) context.get("listSize");
        return value != null ? value.intValue() : 0;
    }

    public int getViewIndex(Map<String, Object> context) {
        return getPaginateIndex(context);
    }

    public int getViewSize(Map<String, Object> context) {
        return getPaginateSize(context);
    }

    public int getLowIndex(Map<String, Object> context) {
        Integer value = (Integer) context.get("lowIndex");
        return value != null ? value.intValue() : 0;
    }

    public int getHighIndex(Map<String, Object> context) {
        Integer value = (Integer) context.get("highIndex");
        return value != null ? value.intValue() : 0;
    }

    public int getActualPageSize(Map<String, Object> context) {
        Integer value = (Integer) context.get("actualPageSize");
        return value != null ? value.intValue() : (getHighIndex(context) - getLowIndex(context));
    }

    public List<ModelFormField> getFieldList() {
        return fieldList;
    }

    private int getOverrideListSize(Map<String, Object> context) {
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

    public void getListLimits(Map<String, Object> context, Object entryList) {
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
                listSize = iter.getResultsSizeAfterPartialList();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error getting list size", module);
                listSize = 0;
            }
        } else if (entryList instanceof List) {
            List<?> items = (List<?>) entryList;
            listSize = items.size();
        }

        if (paginate) {
            viewIndex = this.getPaginateIndex(context);
            viewSize = this.getPaginateSize(context);

            lowIndex = viewIndex * viewSize;
            highIndex = (viewIndex + 1) * viewSize;
        } else {
            viewIndex = 0;
            viewSize = defaultViewSize;
            lowIndex = 0;
            highIndex = defaultViewSize;
        }

        context.put("listSize", Integer.valueOf(listSize));
        context.put("viewIndex", Integer.valueOf(viewIndex));
        context.put("viewSize", Integer.valueOf(viewSize));
        context.put("lowIndex", Integer.valueOf(lowIndex));
        context.put("highIndex", Integer.valueOf(highIndex));
    }

    public String getPassedRowCount(Map<String, Object> context) {
        return rowCountExdr.expandString(context);
    }

    public int getRowCount() {
        return this.rowCount;
    }

    public boolean getUseRowSubmit() {
        return this.useRowSubmit;
    }

    public List<ModelFormField> getMultiSubmitFields() {
        return this.multiSubmitFields;
    }

    public List<FieldGroupBase> getInbetweenList(FieldGroup startFieldGroup, FieldGroup endFieldGroup) {
        ArrayList<FieldGroupBase> inbetweenList = new ArrayList<FieldGroupBase>();
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
        Iterator<FieldGroupBase> iter = fieldGroupList.iterator();
        while (iter.hasNext()) {
            FieldGroupBase obj = iter.next();
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

    public String getSortField(Map<String, Object> context) {
        String field = "sortField";
        String value = null;

        try {
            value = (String)context.get(field);
            if (value == null) {
                Map<String, String> parameters = UtilGenerics.cast(context.get("parameters"));
                if (parameters != null) {
                    value = parameters.get(field);
                }
            }
        } catch (Exception e) {
            Debug.logWarning(e, "Error getting sortField: " + e.toString(), module);
        }

        return value;
    }

    /* Returns the list of ModelForm.UpdateArea objects.
     */
    public List<UpdateArea> getOnSubmitUpdateAreas() {
        return this.onSubmitUpdateAreas;
    }

    public static class AltRowStyle {
        public String useWhen;
        public String style;
        public AltRowStyle(Element altRowStyleElement) {
            this.useWhen = altRowStyleElement.getAttribute("use-when");
            this.style = altRowStyleElement.getAttribute("style");
        }
    }

    /**
     * iterate through alt-row-styles list to see if should be used, then add style
     * @return The style for item row
     */
    public String getStyleAltRowStyle(Map<String, Object> context) {
        String styles = "";
        try {
            // use the same Interpreter (ie with the same context setup) for all evals
            Interpreter bsh = this.getBshInterpreter(context);
            for (AltRowStyle altRowStyle : this.altRowStyles) {
                Object retVal = bsh.eval(StringUtil.convertOperatorSubstitutions(altRowStyle.useWhen));
                // retVal should be a Boolean, if not something weird is up...
                if (retVal instanceof Boolean) {
                    Boolean boolVal = (Boolean) retVal;
                    if (boolVal.booleanValue()) {
                        styles += altRowStyle.style;
                    }
                } else {
                    throw new IllegalArgumentException(
                        "Return value from style condition eval was not a Boolean: " + retVal.getClass().getName() + " [" + retVal + "] of form " + this.name);
                }
            }
        } catch (EvalError e) {
            String errmsg = "Error evaluating BeanShell style conditions on form " + this.name;
            Debug.logError(e, errmsg, module);
            throw new IllegalArgumentException(errmsg);
        }

        return styles;
    }

    public static class AltTarget {
        public String useWhen;
        public FlexibleStringExpander targetExdr;
        public AltTarget(Element altTargetElement) {
            this.useWhen = altTargetElement.getAttribute("use-when");
            this.targetExdr = FlexibleStringExpander.getInstance(altTargetElement.getAttribute("target"));
        }
        @Override
        public int hashCode() {
            return useWhen.hashCode();
        }
        @Override
        public boolean equals(Object obj) {
            return obj instanceof AltTarget && obj.hashCode() == this.hashCode();
        }
    }

    /** The UpdateArea class implements the <code>&lt;on-event-update-area&gt;</code>
     * elements used in form widgets.
     */
    public static class UpdateArea {
        protected String eventType;
        protected String areaId;
        protected String areaTarget;
        /** XML constructor.
         * @param updateAreaElement The <code>&lt;on-xxx-update-area&gt;</code>
         * XML element.
         */
        public UpdateArea(Element updateAreaElement) {
            this.eventType = updateAreaElement.getAttribute("event-type");
            this.areaId = updateAreaElement.getAttribute("area-id");
            this.areaTarget = updateAreaElement.getAttribute("area-target");
        }
        /** String constructor.
         * @param areaId The id of the widget element to be updated
         * @param areaTarget The target URL called to update the area
         */
        public UpdateArea(String eventType, String areaId, String areaTarget) {
            this.eventType = eventType;
            this.areaId = areaId;
            this.areaTarget = areaTarget;
        }
        @Override
        public int hashCode() {
            return areaId.hashCode();
        }
        @Override
        public boolean equals(Object obj) {
            return obj instanceof UpdateArea && obj.hashCode() == this.hashCode();
        }
        public String getEventType() {
            return eventType;
        }
        public String getAreaId() {
            return areaId;
        }
        public String getAreaTarget(Map<String, ? extends Object> context) {
            return FlexibleStringExpander.expandString(areaTarget, context);
        }
    }

    public static class AutoFieldsService {
        public String serviceName;
        public String mapName;
        public String defaultFieldType;
        public int defaultPosition;
        public AutoFieldsService(Element element) {
            this.serviceName = element.getAttribute("service-name");
            this.mapName = element.getAttribute("map-name");
            this.defaultFieldType = element.getAttribute("default-field-type");
            String positionStr = element.getAttribute("default-position");
            int position = 1;
            try {
                if (UtilValidate.isNotEmpty(positionStr)) {
                    position = Integer.valueOf(positionStr);
                }
            } catch (Exception e) {
                Debug.logError(e,
                    "Could not convert position attribute of the field element to an integer: [" + positionStr + "], using the default of the form renderer",
                    module);
            }
            this.defaultPosition = position;
        }
    }

    public static class AutoFieldsEntity {
        public String entityName;
        public String mapName;
        public String defaultFieldType;
        public int defaultPosition;
        public AutoFieldsEntity(Element element) {
            this.entityName = element.getAttribute("entity-name");
            this.mapName = element.getAttribute("map-name");
            this.defaultFieldType = element.getAttribute("default-field-type");
            String positionStr = element.getAttribute("default-position");
            int position = 1;
            try {
                if (UtilValidate.isNotEmpty(positionStr)) {
                    position = Integer.valueOf(positionStr);
                }
            } catch (Exception e) {
                Debug.logError(e,
                    "Could not convert position attribute of the field element to an integer: [" + positionStr + "], using the default of the form renderer",
                    module);
            }
            this.defaultPosition = position;
        }
    }

    public static interface FieldGroupBase {}

    public static class FieldGroup implements FieldGroupBase {
        public String id;
        public String style;
        public String title;
        public boolean collapsible = false;
        public boolean initiallyCollapsed = false;
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
                this.title = sortOrderElement.getAttribute("title");
                this.collapsible = "true".equals(sortOrderElement.getAttribute("collapsible"));
                this.initiallyCollapsed = "true".equals(sortOrderElement.getAttribute("initially-collapsed"));
                if (this.initiallyCollapsed) {
                    this.collapsible = true;
                }

                for (Element sortFieldElement: UtilXml.childElementList(sortOrderElement, "sort-field")) {
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

        public void setId(String id) {
            this.id = id;
        }

        public String getStyle() {
            return this.style;
        }

        public String getTitle() {
            return this.title;
        }

        public Boolean collapsible() {
            return this.collapsible;
        }

        public Boolean initiallyCollapsed() {
            return this.initiallyCollapsed;
        }

        public void renderStartString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer) throws IOException {
            formStringRenderer.renderFieldGroupOpen(writer, context, this);
            formStringRenderer.renderFormatSingleWrapperOpen(writer, context, modelForm);
        }

        public void renderEndString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer) throws IOException {
            formStringRenderer.renderFormatSingleWrapperClose(writer, context, modelForm);
            formStringRenderer.renderFieldGroupClose(writer, context, this);
        }
    }

    public static class Banner implements FieldGroupBase {
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
            this.style = FlexibleStringExpander.getInstance(sortOrderElement.getAttribute("style"));
            this.text = FlexibleStringExpander.getInstance(sortOrderElement.getAttribute("text"));
            this.textStyle = FlexibleStringExpander.getInstance(sortOrderElement.getAttribute("text-style"));
            this.leftText = FlexibleStringExpander.getInstance(sortOrderElement.getAttribute("left-text"));
            this.leftTextStyle = FlexibleStringExpander.getInstance(sortOrderElement.getAttribute("left-text-style"));
            this.rightText = FlexibleStringExpander.getInstance(sortOrderElement.getAttribute("right-text"));
            this.rightTextStyle = FlexibleStringExpander.getInstance(sortOrderElement.getAttribute("right-text-style"));
        }

        public String getStyle(Map<String, Object> context) { return this.style.expandString(context); }
        public String getText(Map<String, Object> context) { return this.text.expandString(context); }
        public String getTextStyle(Map<String, Object> context) { return this.textStyle.expandString(context); }
        public String getLeftText(Map<String, Object> context) { return this.leftText.expandString(context); }
        public String getLeftTextStyle(Map<String, Object> context) { return this.leftTextStyle.expandString(context); }
        public String getRightText(Map<String, Object> context) { return this.rightText.expandString(context); }
        public String getRightTextStyle(Map<String, Object> context) { return this.rightTextStyle.expandString(context); }

        public void renderString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer) throws IOException {
            formStringRenderer.renderBanner(writer, context, this);
        }
    }

    public Set<String> getAllEntityNamesUsed() {
        Set<String> allEntityNamesUsed = FastSet.newInstance();
        for (AutoFieldsEntity autoFieldsEntity: this.autoFieldsEntities) {
            allEntityNamesUsed.add(autoFieldsEntity.entityName);
        }
        if (this.actions != null) {
            for (ModelFormAction modelFormAction: this.actions) {
                if (modelFormAction instanceof ModelFormAction.EntityOne) {
                    allEntityNamesUsed.add(((ModelFormAction.EntityOne)modelFormAction).finder.getEntityName());
                } else if (modelFormAction instanceof ModelFormAction.EntityAnd) {
                    allEntityNamesUsed.add(((ModelFormAction.EntityAnd)modelFormAction).finder.getEntityName());
                } else if (modelFormAction instanceof ModelFormAction.EntityCondition) {
                    allEntityNamesUsed.add(((ModelFormAction.EntityCondition)modelFormAction).finder.getEntityName());
                }

            }
        }
        if (this.rowActions != null) {
            for (ModelFormAction modelFormAction: this.rowActions) {
                if (modelFormAction instanceof ModelFormAction.EntityOne) {
                    allEntityNamesUsed.add(((ModelFormAction.EntityOne)modelFormAction).finder.getEntityName());
                } else if (modelFormAction instanceof ModelFormAction.EntityAnd) {
                    allEntityNamesUsed.add(((ModelFormAction.EntityAnd)modelFormAction).finder.getEntityName());
                } else if (modelFormAction instanceof ModelFormAction.EntityCondition) {
                    allEntityNamesUsed.add(((ModelFormAction.EntityCondition)modelFormAction).finder.getEntityName());
                }
            }
        }
        for (ModelFormField modelFormField: this.fieldList) {
            if (UtilValidate.isNotEmpty(modelFormField.getEntityName())) {
                allEntityNamesUsed.add(modelFormField.getEntityName());
            }
            if (modelFormField.getFieldInfo() instanceof ModelFormField.DisplayEntityField) {
                allEntityNamesUsed.add(((ModelFormField.DisplayEntityField)modelFormField.getFieldInfo()).entityName);
            }
            if (modelFormField.getFieldInfo() instanceof ModelFormField.FieldInfoWithOptions) {
                for (ModelFormField.OptionSource optionSource: ((ModelFormField.FieldInfoWithOptions)modelFormField.getFieldInfo()).optionSources) {
                    if (optionSource instanceof ModelFormField.EntityOptions) {
                        allEntityNamesUsed.add(((ModelFormField.EntityOptions)optionSource).entityName);
                    }
                }
            }
        }
        return allEntityNamesUsed;
    }

    public Set<String> getAllServiceNamesUsed() {
        Set<String> allServiceNamesUsed = FastSet.newInstance();
        for (AutoFieldsService autoFieldsService: this.autoFieldsServices) {
            allServiceNamesUsed.add(autoFieldsService.serviceName);
        }
        if (this.actions != null) {
            for (ModelFormAction modelFormAction: this.actions) {
                if (modelFormAction instanceof ModelFormAction.Service) {
                    allServiceNamesUsed.add(((ModelFormAction.Service)modelFormAction).serviceNameExdr.getOriginal());
                }
            }
        }
        if (this.rowActions != null) {
            for (ModelFormAction modelFormAction: this.rowActions) {
                if (modelFormAction instanceof ModelFormAction.Service) {
                    allServiceNamesUsed.add(((ModelFormAction.Service)modelFormAction).serviceNameExdr.getOriginal());
                }
            }
        }
        for (ModelFormField modelFormField: this.fieldList) {
            if (UtilValidate.isNotEmpty(modelFormField.getServiceName())) {
                allServiceNamesUsed.add(modelFormField.getServiceName());
            }
        }
        return allServiceNamesUsed;
    }

    public Set<String> getLinkedRequestsLocationAndUri() throws GeneralException {
        Set<String> allRequestsUsed = FastSet.newInstance();

        if (this.fieldList != null) {
            for (ModelFormField modelFormField: this.fieldList) {
                if (modelFormField.getFieldInfo() instanceof ModelFormField.HyperlinkField) {
                    ModelFormField.HyperlinkField link = (ModelFormField.HyperlinkField) modelFormField.getFieldInfo();
                    String target = link.getTarget(null);
                    String urlMode = link.getTargetType();
                    // Debug.logInfo("In findRequestNamesLinkedtoInWidget found link [" + link.rawString() + "] with target [" + target + "]", module);

                    Set<String> controllerLocAndRequestSet = ConfigXMLReader.findControllerRequestUniqueForTargetType(target, urlMode);
                    if (controllerLocAndRequestSet != null) {
                        allRequestsUsed.addAll(controllerLocAndRequestSet);
                    }
                } else if (modelFormField.getFieldInfo() instanceof ModelFormField.DisplayEntityField) {
                    ModelFormField.DisplayEntityField parentField = (ModelFormField.DisplayEntityField) modelFormField.getFieldInfo();
                    if (parentField.subHyperlink != null) {
                        Set<String> controllerLocAndRequestSet = ConfigXMLReader.findControllerRequestUniqueForTargetType(parentField.subHyperlink.getTarget(null), parentField.subHyperlink.getTargetType());
                        if (controllerLocAndRequestSet != null) {
                            allRequestsUsed.addAll(controllerLocAndRequestSet);
                        }
                    }
                } else if (modelFormField.getFieldInfo() instanceof ModelFormField.TextField) {
                    ModelFormField.TextField parentField = (ModelFormField.TextField) modelFormField.getFieldInfo();
                    if (parentField.subHyperlink != null) {
                        Set<String> controllerLocAndRequestSet = ConfigXMLReader.findControllerRequestUniqueForTargetType(parentField.subHyperlink.getTarget(null), parentField.subHyperlink.getTargetType());
                        if (controllerLocAndRequestSet != null) {
                            allRequestsUsed.addAll(controllerLocAndRequestSet);
                        }
                    }
                } else if (modelFormField.getFieldInfo() instanceof ModelFormField.DropDownField) {
                    ModelFormField.DropDownField parentField = (ModelFormField.DropDownField) modelFormField.getFieldInfo();
                    if (parentField.subHyperlink != null) {
                        Set<String> controllerLocAndRequestSet = ConfigXMLReader.findControllerRequestUniqueForTargetType(parentField.subHyperlink.getTarget(null), parentField.subHyperlink.getTargetType());
                        if (controllerLocAndRequestSet != null) {
                            allRequestsUsed.addAll(controllerLocAndRequestSet);
                        }
                    }
                } else if (modelFormField.getFieldInfo() instanceof ModelFormField.ImageField) {
                    ModelFormField.ImageField parentField = (ModelFormField.ImageField) modelFormField.getFieldInfo();
                    if (parentField.subHyperlink != null) {
                        Set<String> controllerLocAndRequestSet = ConfigXMLReader.findControllerRequestUniqueForTargetType(parentField.subHyperlink.getTarget(null), parentField.subHyperlink.getTargetType());
                        if (controllerLocAndRequestSet != null) {
                            allRequestsUsed.addAll(controllerLocAndRequestSet);
                        }
                    }
                } else if (modelFormField.getFieldInfo() instanceof ModelFormField.DropDownField) {
                    ModelFormField.DropDownField parentField = (ModelFormField.DropDownField) modelFormField.getFieldInfo();
                    if (parentField.subHyperlink != null) {
                        Set<String> controllerLocAndRequestSet = ConfigXMLReader.findControllerRequestUniqueForTargetType(parentField.subHyperlink.getTarget(null), parentField.subHyperlink.getTargetType());
                        if (controllerLocAndRequestSet != null) {
                            allRequestsUsed.addAll(controllerLocAndRequestSet);
                        }
                    }
                }
            }
        }
        return allRequestsUsed;
    }

    public Set<String> getTargetedRequestsLocationAndUri() throws GeneralException {
        Set<String> allRequestsUsed = FastSet.newInstance();

        if (this.altTargets != null) {
            for (AltTarget altTarget: this.altTargets) {
                String target = altTarget.targetExdr.getOriginal();
                String urlMode = "intra-app";

                Set<String> controllerLocAndRequestSet = ConfigXMLReader.findControllerRequestUniqueForTargetType(target, urlMode);
                if (controllerLocAndRequestSet != null) {
                    allRequestsUsed.addAll(controllerLocAndRequestSet);
                }
            }
        }

        if (!this.target.isEmpty()) {
            String target = this.target.getOriginal();
            String urlMode = UtilValidate.isNotEmpty(this.targetType) ? this.targetType : "intra-app";
            if (target.indexOf("${") < 0) {
                Set<String> controllerLocAndRequestSet = ConfigXMLReader.findControllerRequestUniqueForTargetType(target, urlMode);
                if (controllerLocAndRequestSet != null) {
                    allRequestsUsed.addAll(controllerLocAndRequestSet);
                }
            }
        }

        return allRequestsUsed;
    }
}
