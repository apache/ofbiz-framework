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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.ofbiz.base.util.BshUtil;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.collections.MapStack;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelField;
import org.ofbiz.entity.model.ModelReader;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ModelParam;
import org.ofbiz.service.ModelService;
import org.ofbiz.widget.ModelWidget;
import org.ofbiz.widget.ModelWidgetAction;
import org.ofbiz.widget.ModelWidgetVisitor;
import org.ofbiz.widget.WidgetWorker;
import org.w3c.dom.Element;

import bsh.EvalError;
import bsh.Interpreter;

/**
 * Models the &lt;form&gt; element.
 * 
 * @see <code>widget-form.xsd</code>
 */
@SuppressWarnings("serial")
public class ModelForm extends ModelWidget {

    /*
     * ----------------------------------------------------------------------- *
     *                     DEVELOPERS PLEASE READ
     * ----------------------------------------------------------------------- *
     * 
     * This model is intended to be a read-only data structure that represents
     * an XML element. Outside of object construction, the class should not
     * have any behaviors. All behavior should be contained in model visitors.
     * 
     * Instances of this class will be shared by multiple threads - therefore
     * it is immutable. DO NOT CHANGE THE OBJECT'S STATE AT RUN TIME!
     * 
     * BE VERY CAREFUL when implementing "extends" - parent form collections
     * must be added to child collections, not replace them. In other words,
     * do not assign parent collection fields to child collection fields.
     * 
     */

    public static final String module = ModelForm.class.getName();
    public static final String DEFAULT_FORM_RESULT_LIST_NAME = "defaultFormResultList";
    /** Pagination settings and defaults. */
    public static int DEFAULT_PAGE_SIZE = 10;
    public static int MAX_PAGE_SIZE = 10000;
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
    private final List<ModelWidgetAction> actions;
    private final List<AltRowStyle> altRowStyles;
    private final List<AltTarget> altTargets;
    private final List<AutoFieldsEntity> autoFieldsEntities;
    private final List<AutoFieldsService> autoFieldsServices;
    private final boolean clientAutocompleteFields;
    private final String containerId;
    private final String containerStyle;
    private final String defaultEntityName;
    /** This field group will be the "catch-all" group for fields that are not
     *  included in an explicit field-group.
     */
    private final FieldGroup defaultFieldGroup;
    private final FlexibleMapAccessor<Map<String, ? extends Object>> defaultMapName;
    private final String defaultRequiredFieldStyle;
    private final String defaultServiceName;
    private final String defaultSortFieldAscStyle;
    private final String defaultSortFieldDescStyle;
    private final String defaultSortFieldStyle;
    private final String defaultTableStyle;
    private final String defaultTitleAreaStyle;
    private final String defaultTitleStyle;
    private final String defaultTooltipStyle;
    private final int defaultViewSize;
    private final String defaultWidgetAreaStyle;
    private final String defaultWidgetStyle;
    private final String evenRowStyle;
    /** This is a list of FieldGroups in the order they were created.
     * Can also include Banner objects.
     */
    private final List<FieldGroupBase> fieldGroupList;
    /** This Map is keyed with the field name and has a FieldGroup for the value.
     * Can also include Banner objects.
     */
    private final Map<String, FieldGroupBase> fieldGroupMap;
    /** This List will contain one copy of each field for each field name in the order
     * they were encountered in the service, entity, or form definition; field definitions
     * with constraints will also be in this list but may appear multiple times for the same
     * field name.
     *
     * When rendering the form the order in this list should be following and it should not be
     * necessary to use the Map. The Map is used when loading the form definition to keep the
     * list clean and implement the override features for field definitions.
     */
    private final List<ModelFormField> fieldList;
    private final String focusFieldName;
    private final String formLocation;
    private final String formTitleAreaStyle;
    private final String formWidgetAreaStyle;
    private final boolean groupColumns;
    private final String headerRowStyle;
    private final boolean hideHeader;
    private final String itemIndexSeparator;
    private final List<String> lastOrderFields;
    private final String listEntryName;
    private final String listName;
    private final List<ModelFormField> multiSubmitFields;
    private final String oddRowStyle;
    /** On Paginate areas to be updated. */
    private final List<UpdateArea> onPaginateUpdateAreas;
    /** On Sort Column areas to be updated. */
    private final List<UpdateArea> onSortColumnUpdateAreas;
    /** On Submit areas to be updated. */
    private final List<UpdateArea> onSubmitUpdateAreas;
    private final FlexibleStringExpander overrideListSize;
    private final FlexibleStringExpander paginate;
    private final FlexibleStringExpander paginateFirstLabel;
    private final FlexibleStringExpander paginateIndexField;
    private final FlexibleStringExpander paginateLastLabel;
    private final FlexibleStringExpander paginateNextLabel;
    private final FlexibleStringExpander paginatePreviousLabel;
    private final FlexibleStringExpander paginateSizeField;
    private final String paginateStyle;
    private final FlexibleStringExpander paginateTarget;
    private final String paginateTargetAnchor;
    private final FlexibleStringExpander paginateViewSizeLabel;
    private final ModelForm parentModelForm;
    private final List<ModelWidgetAction> rowActions;
    private final FlexibleStringExpander rowCountExdr;
    private final boolean separateColumns;
    private final boolean skipEnd;
    private final boolean skipStart;
    private final String sortFieldParameterName;
    private final List<SortField> sortOrderFields;
    private final FlexibleStringExpander target;
    private final String targetType;
    private final FlexibleStringExpander targetWindowExdr;
    private final String title;
    private final String tooltip;
    private final String type;
    private final boolean useRowSubmit;
    /** Keeps track of conditional fields to help ensure that only one is rendered
     */
    private final Set<String> useWhenFields;

    /** XML Constructor */
    public ModelForm(Element formElement, String formLocation, ModelReader entityModelReader, DispatchContext dispatchContext) {
        super(formElement);
        this.formLocation = formLocation;
        parentModelForm = getParentForm(formElement, entityModelReader, dispatchContext);
        int defaultViewSizeInt = DEFAULT_PAGE_SIZE;
        if (parentModelForm != null) {
            defaultViewSizeInt = parentModelForm.defaultViewSize;
        } else {
            defaultViewSizeInt = UtilProperties.getPropertyAsInteger("widget.properties", "widget.form.defaultViewSize",
                    defaultViewSizeInt);
        }
        if (formElement.hasAttribute("view-size")) {
            try {
                defaultViewSizeInt = Integer.valueOf(formElement.getAttribute("view-size"));
            } catch (NumberFormatException e) {
            }
        }
        this.defaultViewSize = defaultViewSizeInt;
        String type = null;
        if (formElement.hasAttribute("type")) {
            type = formElement.getAttribute("type");
        } else {
            if (parentModelForm != null) {
                type = parentModelForm.type;
            }
        }
        this.type = type;
        FlexibleStringExpander target = FlexibleStringExpander.getInstance(formElement.getAttribute("target"));
        if (target.isEmpty() && parentModelForm != null) {
            target = parentModelForm.target;
        }
        this.target = target;
        String containerId = null;
        if (formElement.hasAttribute("id")) {
            containerId = formElement.getAttribute("id");
        } else {
            if (parentModelForm != null) {
                containerId = parentModelForm.containerId;
            }
        }
        this.containerId = containerId;
        String containerStyle = "";
        if (formElement.hasAttribute("style")) {
            containerStyle = formElement.getAttribute("style");
        } else {
            if (parentModelForm != null) {
                containerStyle = parentModelForm.containerStyle;
            }
        }
        this.containerStyle = containerStyle;
        String title = null;
        if (formElement.hasAttribute("title")) {
            title = formElement.getAttribute("title");
        } else {
            if (parentModelForm != null) {
                title = parentModelForm.title;
            }
        }
        this.title = title;
        String tooltip = null;
        if (formElement.hasAttribute("tooltip")) {
            tooltip = formElement.getAttribute("tooltip");
        } else {
            if (parentModelForm != null) {
                tooltip = parentModelForm.tooltip;
            }
        }
        this.tooltip = tooltip;
        String listName = DEFAULT_FORM_RESULT_LIST_NAME;
        if (formElement.hasAttribute("list-name")) {
            listName = formElement.getAttribute("list-name");
        } else {
            if (parentModelForm != null) {
                listName = parentModelForm.listName;
            }
        }
        this.listName = listName;
        String listEntryName = null;
        if (formElement.hasAttribute("list-entry-name")) {
            listEntryName = formElement.getAttribute("list-entry-name");
        } else {
            if (parentModelForm != null) {
                listEntryName = parentModelForm.listEntryName;
            }
        }
        this.listEntryName = listEntryName;
        String defaultEntityName = null;
        if (formElement.hasAttribute("default-entity-name")) {
            defaultEntityName = formElement.getAttribute("default-entity-name");
        } else {
            if (parentModelForm != null) {
                defaultEntityName = parentModelForm.defaultEntityName;
            }
        }
        this.defaultEntityName = defaultEntityName;
        String defaultServiceName = null;
        if (formElement.hasAttribute("default-service-name")) {
            defaultServiceName = formElement.getAttribute("default-service-name");
        } else {
            if (parentModelForm != null) {
                defaultServiceName = parentModelForm.defaultServiceName;
            }
        }
        this.defaultServiceName = defaultServiceName;
        String formTitleAreaStyle = "";
        if (formElement.hasAttribute("form-title-area-style")) {
            formTitleAreaStyle = formElement.getAttribute("form-title-area-style");
        } else {
            if (parentModelForm != null) {
                formTitleAreaStyle = parentModelForm.formTitleAreaStyle;
            }
        }
        this.formTitleAreaStyle = formTitleAreaStyle;
        String formWidgetAreaStyle = "";
        if (formElement.hasAttribute("form-widget-area-style")) {
            formWidgetAreaStyle = formElement.getAttribute("form-widget-area-style");
        } else {
            if (parentModelForm != null) {
                formWidgetAreaStyle = parentModelForm.formWidgetAreaStyle;
            }
        }
        this.formWidgetAreaStyle = formWidgetAreaStyle;
        String defaultTitleAreaStyle = "";
        if (formElement.hasAttribute("default-title-area-style")) {
            defaultTitleAreaStyle = formElement.getAttribute("default-title-area-style");
        } else {
            if (parentModelForm != null) {
                defaultTitleAreaStyle = parentModelForm.defaultTitleAreaStyle;
            }
        }
        this.defaultTitleAreaStyle = defaultTitleAreaStyle;
        String defaultWidgetAreaStyle = "";
        if (formElement.hasAttribute("default-widget-area-style")) {
            defaultWidgetAreaStyle = formElement.getAttribute("default-widget-area-style");
        } else {
            if (parentModelForm != null) {
                defaultWidgetAreaStyle = parentModelForm.defaultWidgetAreaStyle;
            }
        }
        this.defaultWidgetAreaStyle = defaultWidgetAreaStyle;
        String oddRowStyle = "";
        if (formElement.hasAttribute("odd-row-style")) {
            oddRowStyle = formElement.getAttribute("odd-row-style");
        } else {
            if (parentModelForm != null) {
                oddRowStyle = parentModelForm.oddRowStyle;
            }
        }
        this.oddRowStyle = oddRowStyle;
        String evenRowStyle = "";
        if (formElement.hasAttribute("even-row-style")) {
            evenRowStyle = formElement.getAttribute("even-row-style");
        } else {
            if (parentModelForm != null) {
                evenRowStyle = parentModelForm.evenRowStyle;
            }
        }
        this.evenRowStyle = evenRowStyle;
        String defaultTableStyle = "";
        if (formElement.hasAttribute("default-table-style")) {
            defaultTableStyle = formElement.getAttribute("default-table-style");
        } else {
            if (parentModelForm != null) {
                defaultTableStyle = parentModelForm.defaultTableStyle;
            }
        }
        this.defaultTableStyle = defaultTableStyle;
        String headerRowStyle = "";
        if (formElement.hasAttribute("header-row-style")) {
            headerRowStyle = formElement.getAttribute("header-row-style");
        } else {
            if (parentModelForm != null) {
                headerRowStyle = parentModelForm.headerRowStyle;
            }
        }
        this.headerRowStyle = headerRowStyle;
        String defaultTitleStyle = "";
        if (formElement.hasAttribute("default-title-style")) {
            defaultTitleStyle = formElement.getAttribute("default-title-style");
        } else {
            if (parentModelForm != null) {
                defaultTitleStyle = parentModelForm.defaultTitleStyle;
            }
        }
        this.defaultTitleStyle = defaultTitleStyle;
        String defaultWidgetStyle = "";
        if (formElement.hasAttribute("default-widget-style")) {
            defaultWidgetStyle = formElement.getAttribute("default-widget-style");
        } else {
            if (parentModelForm != null) {
                defaultWidgetStyle = parentModelForm.defaultWidgetStyle;
            }
        }
        this.defaultWidgetStyle = defaultWidgetStyle;
        String defaultTooltipStyle = "";
        if (formElement.hasAttribute("default-tooltip-style")) {
            defaultTooltipStyle = formElement.getAttribute("default-tooltip-style");
        } else {
            if (parentModelForm != null) {
                defaultTooltipStyle = parentModelForm.defaultTooltipStyle;
            }
        }
        this.defaultTooltipStyle = defaultTooltipStyle;
        String itemIndexSeparator = null;
        if (formElement.hasAttribute("item-index-separator")) {
            itemIndexSeparator = formElement.getAttribute("item-index-separator");
        } else {
            if (parentModelForm != null) {
                itemIndexSeparator = parentModelForm.itemIndexSeparator;
            }
        }
        this.itemIndexSeparator = itemIndexSeparator;
        boolean separateColumns = false;
        if (formElement.hasAttribute("separate-columns")) {
            separateColumns = "true".equals(formElement.getAttribute("separate-columns"));
        } else {
            if (parentModelForm != null) {
                separateColumns = parentModelForm.separateColumns;
            }
        }
        this.separateColumns = separateColumns;
        boolean groupColumns = false;
        if (formElement.hasAttribute("group-columns")) {
            groupColumns = !"false".equals(formElement.getAttribute("group-columns"));
        } else {
            if (parentModelForm != null) {
                groupColumns = parentModelForm.groupColumns;
            }
        }
        this.groupColumns = groupColumns;
        String targetType = null;
        if (formElement.hasAttribute("target-type")) {
            targetType = formElement.getAttribute("target-type");
        } else {
            if (parentModelForm != null) {
                targetType = parentModelForm.targetType;
            }
        }
        this.targetType = targetType;
        FlexibleMapAccessor<Map<String, ? extends Object>> defaultMapName = FlexibleMapAccessor.getInstance(formElement
                .getAttribute("default-map-name"));
        if (defaultMapName.isEmpty() && parentModelForm != null) {
            defaultMapName = parentModelForm.defaultMapName;
        }
        this.defaultMapName = defaultMapName;
        FlexibleStringExpander targetWindowExdr = FlexibleStringExpander.getInstance(formElement.getAttribute("target-window"));
        if (targetWindowExdr.isEmpty() && parentModelForm != null) {
            targetWindowExdr = parentModelForm.targetWindowExdr;
        }
        this.targetWindowExdr = targetWindowExdr;
        boolean hideHeader = false;
        if (formElement.hasAttribute("hide-header")) {
            hideHeader = "true".equals(formElement.getAttribute("hide-header"));
        } else {
            if (parentModelForm != null) {
                hideHeader = parentModelForm.hideHeader;
            }
        }
        this.hideHeader = hideHeader;
        boolean clientAutocompleteFields = true;
        if (formElement.hasAttribute("client-autocomplete-fields")) {
            clientAutocompleteFields = !"false".equals(formElement.getAttribute("client-autocomplete-fields"));
        } else {
            if (parentModelForm != null) {
                clientAutocompleteFields = parentModelForm.clientAutocompleteFields;
            }
        }
        this.clientAutocompleteFields = clientAutocompleteFields;
        FlexibleStringExpander paginateTarget = FlexibleStringExpander.getInstance(formElement.getAttribute("paginate-target"));
        if (paginateTarget.isEmpty() && parentModelForm != null) {
            paginateTarget = parentModelForm.paginateTarget;
        }
        this.paginateTarget = paginateTarget;
        ArrayList<AltTarget> altTargets = new ArrayList<AltTarget>();
        if (parentModelForm != null) {
            altTargets.addAll(parentModelForm.altTargets);
        }
        for (Element altTargetElement : UtilXml.childElementList(formElement, "alt-target")) {
            altTargets.add(new AltTarget(altTargetElement));
        }
        altTargets.trimToSize();
        this.altTargets = Collections.unmodifiableList(altTargets);
        ArrayList<ModelWidgetAction> actions = new ArrayList<ModelWidgetAction>();
        if (parentModelForm != null) {
            actions.addAll(parentModelForm.actions);
        }
        Element actionsElement = UtilXml.firstChildElement(formElement, "actions");
        if (actionsElement != null) {
            actions.addAll(ModelFormAction.readSubActions(this, actionsElement));
        }
        actions.trimToSize();
        this.actions = Collections.unmodifiableList(actions);
        ArrayList<ModelWidgetAction> rowActions = new ArrayList<ModelWidgetAction>();
        if (parentModelForm != null) {
            rowActions.addAll(parentModelForm.rowActions);
        }
        Element rowActionsElement = UtilXml.firstChildElement(formElement, "row-actions");
        if (rowActionsElement != null) {
            rowActions.addAll(ModelFormAction.readSubActions(this, rowActionsElement));
        }
        rowActions.trimToSize();
        this.rowActions = Collections.unmodifiableList(rowActions);
        ArrayList<UpdateArea> onPaginateUpdateAreas = new ArrayList<UpdateArea>();
        ArrayList<UpdateArea> onSubmitUpdateAreas = new ArrayList<UpdateArea>();
        ArrayList<UpdateArea> onSortColumnUpdateAreas = new ArrayList<UpdateArea>();
        if (parentModelForm != null) {
            onPaginateUpdateAreas.addAll(parentModelForm.onPaginateUpdateAreas);
            onSubmitUpdateAreas.addAll(parentModelForm.onSubmitUpdateAreas);
            onSortColumnUpdateAreas.addAll(parentModelForm.onSortColumnUpdateAreas);
        }
        for (Element updateAreaElement : UtilXml.childElementList(formElement, "on-event-update-area")) {
            UpdateArea updateArea = new UpdateArea(updateAreaElement, defaultServiceName, defaultEntityName);
            if ("paginate".equals(updateArea.getEventType())) {
                int index = onPaginateUpdateAreas.indexOf(updateArea);
                if (index != -1) {
                    if (!updateArea.areaTarget.isEmpty()) {
                        onPaginateUpdateAreas.set(index, updateArea);
                    } else {
                        // blank target indicates a removing override
                        onPaginateUpdateAreas.remove(index);
                    }
                } else {
                    onPaginateUpdateAreas.add(updateArea);
                }
            } else if ("submit".equals(updateArea.getEventType())) {
                int index = onSubmitUpdateAreas.indexOf(updateArea);
                if (index != -1) {
                    onSubmitUpdateAreas.set(index, updateArea);
                } else {
                    onSubmitUpdateAreas.add(updateArea);
                }
            } else if ("sort-column".equals(updateArea.getEventType())) {
                int index = onSortColumnUpdateAreas.indexOf(updateArea);
                if (index != -1) {
                    if (!updateArea.areaTarget.isEmpty()) {
                        onSortColumnUpdateAreas.set(index, updateArea);
                    } else {
                        // blank target indicates a removing override
                        onSortColumnUpdateAreas.remove(index);
                    }
                } else {
                    onSortColumnUpdateAreas.add(updateArea);
                }
            }
        }
        onPaginateUpdateAreas.trimToSize();
        this.onPaginateUpdateAreas = Collections.unmodifiableList(onPaginateUpdateAreas);
        onSubmitUpdateAreas.trimToSize();
        this.onSubmitUpdateAreas = Collections.unmodifiableList(onSubmitUpdateAreas);
        onSortColumnUpdateAreas.trimToSize();
        this.onSortColumnUpdateAreas = Collections.unmodifiableList(onSortColumnUpdateAreas);
        ArrayList<AltRowStyle> altRowStyles = new ArrayList<AltRowStyle>();
        if (parentModelForm != null) {
            altRowStyles.addAll(parentModelForm.altRowStyles);
        }
        for (Element altRowStyleElement : UtilXml.childElementList(formElement, "alt-row-style")) {
            AltRowStyle altRowStyle = new AltRowStyle(altRowStyleElement);
            altRowStyles.add(altRowStyle);
        }
        altRowStyles.trimToSize();
        this.altRowStyles = Collections.unmodifiableList(altRowStyles);
        Set<String> useWhenFields = new HashSet<String>();
        if (parentModelForm != null) {
            useWhenFields.addAll(parentModelForm.useWhenFields);
        }
        ArrayList<ModelFormField> fieldList = new ArrayList<ModelFormField>();
        Map<String, ModelFormField> fieldMap = new HashMap<String, ModelFormField>();
        if (parentModelForm != null) {
            // Create this fieldList/Map from clones of parentModelForm's
            for (ModelFormField parentChildField : parentModelForm.fieldList) {
                ModelFormField childField = new ModelFormField(this);
                childField.mergeOverrideModelFormField(parentChildField);
                fieldList.add(childField);
                fieldMap.put(childField.getName(), childField);
            }
        }
        Map<String, FieldGroupBase> fieldGroupMap = new HashMap<String, FieldGroupBase>();
        if (parentModelForm != null) {
            fieldGroupMap.putAll(parentModelForm.fieldGroupMap);
        }
        ArrayList<FieldGroupBase> fieldGroupList = new ArrayList<FieldGroupBase>();
        if (parentModelForm != null) {
            fieldGroupList.addAll(parentModelForm.fieldGroupList);
        }
        ArrayList<String> lastOrderFields = new ArrayList<String>();
        if (parentModelForm != null) {
            lastOrderFields.addAll(parentModelForm.lastOrderFields);
        }
        String sortFieldParameterName = "sortField";
        if (formElement.hasAttribute("sort-field-parameter-name")) {
            sortFieldParameterName = formElement.getAttribute("sort-field-parameter-name");
        } else {
            if (parentModelForm != null) {
                sortFieldParameterName = parentModelForm.targetType;
            }
        }
        this.sortFieldParameterName = sortFieldParameterName;
        String defaultRequiredFieldStyle = "";
        if (formElement.hasAttribute("default-required-field-style")) {
            defaultRequiredFieldStyle = formElement.getAttribute("default-required-field-style");
        } else {
            if (parentModelForm != null) {
                defaultRequiredFieldStyle = parentModelForm.defaultRequiredFieldStyle;
            }
        }
        this.defaultRequiredFieldStyle = defaultRequiredFieldStyle;
        String defaultSortFieldStyle = DEFAULT_SORT_FIELD_STYLE;
        if (formElement.hasAttribute("default-sort-field-style")) {
            defaultSortFieldStyle = formElement.getAttribute("default-sort-field-style");
        } else {
            if (parentModelForm != null) {
                defaultSortFieldStyle = parentModelForm.defaultSortFieldStyle;
            }
        }
        this.defaultSortFieldStyle = defaultSortFieldStyle;
        String defaultSortFieldAscStyle = DEFAULT_SORT_FIELD_ASC_STYLE;
        if (formElement.hasAttribute("default-sort-field-asc-style")) {
            defaultSortFieldAscStyle = formElement.getAttribute("default-sort-field-asc-style");
        } else {
            if (parentModelForm != null) {
                defaultSortFieldAscStyle = parentModelForm.defaultSortFieldAscStyle;
            }
        }
        this.defaultSortFieldAscStyle = defaultSortFieldAscStyle;
        String defaultSortFieldDescStyle = DEFAULT_SORT_FIELD_DESC_STYLE;
        if (formElement.hasAttribute("default-sort-field-desc-style")) {
            defaultSortFieldDescStyle = formElement.getAttribute("default-sort-field-desc-style");
        } else {
            if (parentModelForm != null) {
                defaultSortFieldDescStyle = parentModelForm.defaultSortFieldDescStyle;
            }
        }
        this.defaultSortFieldDescStyle = defaultSortFieldDescStyle;
        String paginateTargetAnchor = null;
        if (formElement.hasAttribute("paginate-target-anchor")) {
            paginateTargetAnchor = formElement.getAttribute("paginate-target-anchor");
        } else {
            if (parentModelForm != null) {
                paginateTargetAnchor = parentModelForm.paginateTargetAnchor;
            }
        }
        this.paginateTargetAnchor = paginateTargetAnchor;
        FlexibleStringExpander paginateIndexField = FlexibleStringExpander.getInstance(formElement
                .getAttribute("paginate-index-field"));
        if (paginateIndexField.isEmpty() && parentModelForm != null) {
            paginateIndexField = parentModelForm.paginateIndexField;
        }
        this.paginateIndexField = paginateIndexField;
        FlexibleStringExpander paginateSizeField = FlexibleStringExpander.getInstance(formElement
                .getAttribute("paginate-size-field"));
        if (paginateSizeField.isEmpty() && parentModelForm != null) {
            paginateSizeField = parentModelForm.paginateSizeField;
        }
        this.paginateSizeField = paginateSizeField;
        FlexibleStringExpander overrideListSize = FlexibleStringExpander.getInstance(formElement
                .getAttribute("override-list-size"));
        if (overrideListSize.isEmpty() && parentModelForm != null) {
            overrideListSize = parentModelForm.overrideListSize;
        }
        this.overrideListSize = overrideListSize;
        FlexibleStringExpander paginateFirstLabel = FlexibleStringExpander.getInstance(formElement
                .getAttribute("paginate-first-label"));
        if (paginateFirstLabel.isEmpty() && parentModelForm != null) {
            paginateFirstLabel = parentModelForm.paginateFirstLabel;
        }
        this.paginateFirstLabel = paginateFirstLabel;
        FlexibleStringExpander paginatePreviousLabel = FlexibleStringExpander.getInstance(formElement
                .getAttribute("paginate-previous-label"));
        if (paginatePreviousLabel.isEmpty() && parentModelForm != null) {
            paginatePreviousLabel = parentModelForm.paginatePreviousLabel;
        }
        this.paginatePreviousLabel = paginatePreviousLabel;
        FlexibleStringExpander paginateNextLabel = FlexibleStringExpander.getInstance(formElement
                .getAttribute("paginate-next-label"));
        if (paginateNextLabel.isEmpty() && parentModelForm != null) {
            paginateNextLabel = parentModelForm.paginateNextLabel;
        }
        this.paginateNextLabel = paginateNextLabel;
        FlexibleStringExpander paginateLastLabel = FlexibleStringExpander.getInstance(formElement
                .getAttribute("paginate-last-label"));
        if (paginateLastLabel.isEmpty() && parentModelForm != null) {
            paginateLastLabel = parentModelForm.paginateLastLabel;
        }
        this.paginateLastLabel = paginateLastLabel;
        FlexibleStringExpander paginateViewSizeLabel = FlexibleStringExpander.getInstance(formElement
                .getAttribute("paginate-viewsize-label"));
        if (paginateViewSizeLabel.isEmpty() && parentModelForm != null) {
            paginateViewSizeLabel = parentModelForm.paginateViewSizeLabel;
        }
        this.paginateViewSizeLabel = paginateViewSizeLabel;
        String paginateStyle = DEFAULT_PAG_STYLE;
        if (formElement.hasAttribute("paginate-style")) {
            paginateStyle = formElement.getAttribute("paginate-style");
        } else {
            if (parentModelForm != null) {
                paginateStyle = parentModelForm.paginateStyle;
            }
        }
        this.paginateStyle = paginateStyle;
        FlexibleStringExpander paginate = FlexibleStringExpander.getInstance(formElement.getAttribute("paginate"));
        if (paginate.isEmpty() && parentModelForm != null) {
            paginate = parentModelForm.paginate;
        }
        this.paginate = paginate;
        boolean skipStart = false;
        if (formElement.hasAttribute("skip-start")) {
            skipStart = "true".equals(formElement.getAttribute("skip-start"));
        } else {
            if (parentModelForm != null) {
                skipStart = parentModelForm.skipStart;
            }
        }
        this.skipStart = skipStart;
        boolean skipEnd = false;
        if (formElement.hasAttribute("skip-end")) {
            skipEnd = "true".equals(formElement.getAttribute("skip-end"));
        } else {
            if (parentModelForm != null) {
                skipEnd = parentModelForm.skipEnd;
            }
        }
        this.skipEnd = skipEnd;
        boolean useRowSubmit = false;
        if (formElement.hasAttribute("use-row-submit")) {
            useRowSubmit = "true".equals(formElement.getAttribute("use-row-submit"));
        } else {
            if (parentModelForm != null) {
                useRowSubmit = parentModelForm.useRowSubmit;
            }
        }
        this.useRowSubmit = useRowSubmit;
        FlexibleStringExpander rowCountExdr = FlexibleStringExpander.getInstance(formElement.getAttribute("row-count"));
        if (rowCountExdr.isEmpty() && parentModelForm != null) {
            rowCountExdr = parentModelForm.rowCountExdr;
        }
        this.rowCountExdr = paginate;
        ArrayList<ModelFormField> multiSubmitFields = new ArrayList<ModelFormField>();
        ArrayList<AutoFieldsService> autoFieldsServices = new ArrayList<AutoFieldsService>();
        ArrayList<AutoFieldsEntity> autoFieldsEntities = new ArrayList<AutoFieldsEntity>();
        ArrayList<SortField> sortOrderFields = new ArrayList<SortField>();
        this.defaultFieldGroup = new FieldGroup(null, this, sortOrderFields, fieldGroupMap);
        for (Element autoFieldsServiceElement : UtilXml.childElementList(formElement, "auto-fields-service")) {
            AutoFieldsService autoFieldsService = new AutoFieldsService(autoFieldsServiceElement);
            autoFieldsServices.add(autoFieldsService);
            addAutoFieldsFromService(autoFieldsService, entityModelReader, dispatchContext, useWhenFields, fieldList, fieldMap);
        }
        for (Element autoFieldsEntityElement : UtilXml.childElementList(formElement, "auto-fields-entity")) {
            AutoFieldsEntity autoFieldsEntity = new AutoFieldsEntity(autoFieldsEntityElement);
            autoFieldsEntities.add(autoFieldsEntity);
            addAutoFieldsFromEntity(autoFieldsEntity, entityModelReader, useWhenFields, fieldList, fieldMap);
        }
        String thisType = this.getType();
        for (Element fieldElement : UtilXml.childElementList(formElement, "field")) {
            ModelFormField modelFormField = new ModelFormField(fieldElement, this, entityModelReader, dispatchContext);
            ModelFormField.FieldInfo fieldInfo = modelFormField.getFieldInfo();
            if (thisType.equals("multi") && fieldInfo instanceof ModelFormField.SubmitField) {
                multiSubmitFields.add(modelFormField);
            } else {
                modelFormField = addUpdateField(modelFormField, useWhenFields, fieldList, fieldMap);
            }
        }
        // get the sort-order
        Element sortOrderElement = UtilXml.firstChildElement(formElement, "sort-order");
        if (sortOrderElement != null) {
            FieldGroup lastFieldGroup = new FieldGroup(null, this, sortOrderFields, fieldGroupMap);
            fieldGroupList.add(lastFieldGroup);
            // read in sort-field
            for (Element sortFieldElement : UtilXml.childElementList(sortOrderElement)) {
                String tagName = sortFieldElement.getTagName();
                if (tagName.equals("sort-field")) {
                    String fieldName = sortFieldElement.getAttribute("name");
                    String position = sortFieldElement.getAttribute("position");
                    sortOrderFields.add(new SortField(fieldName, position));
                    fieldGroupMap.put(fieldName, lastFieldGroup);
                } else if (tagName.equals("last-field")) {
                    String fieldName = sortFieldElement.getAttribute("name");
                    fieldGroupMap.put(fieldName, lastFieldGroup);
                    lastOrderFields.add(fieldName);
                } else if (tagName.equals("banner")) {
                    Banner thisBanner = new Banner(sortFieldElement);
                    fieldGroupList.add(thisBanner);
                    lastFieldGroup = new FieldGroup(null, this, sortOrderFields, fieldGroupMap);
                    fieldGroupList.add(lastFieldGroup);
                } else if (tagName.equals("field-group")) {
                    FieldGroup thisFieldGroup = new FieldGroup(sortFieldElement, this, sortOrderFields, fieldGroupMap);
                    fieldGroupList.add(thisFieldGroup);
                    lastFieldGroup = new FieldGroup(null, this, sortOrderFields, fieldGroupMap);
                    fieldGroupList.add(lastFieldGroup);
                }
            }
        }
        if (sortOrderFields.size() > 0) {
            ArrayList<ModelFormField> sortedFields = new ArrayList<ModelFormField>();
            for (SortField sortField : sortOrderFields) {
                String fieldName = sortField.getFieldName();
                if (UtilValidate.isEmpty(fieldName)) {
                    continue;
                }
                // get all fields with the given name from the existing list and put them in the sorted list
                Iterator<ModelFormField> fieldIter = fieldList.iterator();
                while (fieldIter.hasNext()) {
                    ModelFormField modelFormField = fieldIter.next();
                    if (fieldName.equals(modelFormField.getName())) {
                        // matched the name; remove from the original last and add to the sorted list
                        if (UtilValidate.isNotEmpty(sortField.getPosition())) {
                            modelFormField.setPosition(sortField.getPosition());
                        }
                        fieldIter.remove();
                        sortedFields.add(modelFormField);
                    }
                }
            }
            // now add all of the rest of the fields from fieldList, ie those that were not explicitly listed in the sort order
            sortedFields.addAll(fieldList);
            // sortedFields all done, set fieldList
            fieldList = sortedFields;
        }
        if (UtilValidate.isNotEmpty(lastOrderFields)) {
            List<ModelFormField> lastedFields = new LinkedList<ModelFormField>();
            for (String fieldName : lastOrderFields) {
                if (UtilValidate.isEmpty(fieldName)) {
                    continue;
                }
                // get all fields with the given name from the existing list and put them in the lasted list
                Iterator<ModelFormField> fieldIter = fieldList.iterator();
                while (fieldIter.hasNext()) {
                    ModelFormField modelFormField = fieldIter.next();
                    if (fieldName.equals(modelFormField.getName())) {
                        // matched the name; remove from the original last and add to the lasted list
                        fieldIter.remove();
                        lastedFields.add(modelFormField);
                    }
                }
            }
            //now put all lastedFields at the field list end
            fieldList.addAll(lastedFields);
        }
        this.useWhenFields = Collections.unmodifiableSet(useWhenFields);
        fieldList.trimToSize();
        this.fieldList = Collections.unmodifiableList(fieldList);
        this.fieldGroupMap = Collections.unmodifiableMap(fieldGroupMap);
        fieldGroupList.trimToSize();
        this.fieldGroupList = Collections.unmodifiableList(fieldGroupList);
        lastOrderFields.trimToSize();
        this.lastOrderFields = Collections.unmodifiableList(lastOrderFields);
        multiSubmitFields.trimToSize();
        this.multiSubmitFields = Collections.unmodifiableList(multiSubmitFields);
        autoFieldsServices.trimToSize();
        this.autoFieldsServices = Collections.unmodifiableList(autoFieldsServices);
        autoFieldsEntities.trimToSize();
        this.autoFieldsEntities = Collections.unmodifiableList(autoFieldsEntities);
        sortOrderFields.trimToSize();
        this.sortOrderFields = Collections.unmodifiableList(sortOrderFields);
        String focusFieldName = null;
        if (formElement.hasAttribute("focus-field-name")) {
            focusFieldName = formElement.getAttribute("focus-field-name");
        } else {
            if (parentModelForm != null) {
                focusFieldName = parentModelForm.focusFieldName;
            }
            // TODO: Set this automatically if not specified
        }
        this.focusFieldName = focusFieldName;
    }

    @Override
    public void accept(ModelWidgetVisitor visitor) {
        visitor.visit(this);
    }

    private void addAutoFieldsFromEntity(AutoFieldsEntity autoFieldsEntity, ModelReader entityModelReader,
            Set<String> useWhenFields, List<ModelFormField> fieldList, Map<String, ModelFormField> fieldMap) {
        // read entity def and auto-create fields
        ModelEntity modelEntity = null;
        try {
            modelEntity = entityModelReader.getModelEntity(autoFieldsEntity.entityName);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        if (modelEntity == null) {
            throw new IllegalArgumentException("Error finding Entity with name " + autoFieldsEntity.entityName
                    + " for auto-fields-entity in a form widget");
        }

        Iterator<ModelField> modelFieldIter = modelEntity.getFieldsIterator();
        while (modelFieldIter.hasNext()) {
            ModelField modelField = modelFieldIter.next();
            if (modelField.getIsAutoCreatedInternal()) {
                // don't ever auto-add these, should only be added if explicitly referenced
                continue;
            }
            ModelFormField modelFormField = this.addFieldFromEntityField(modelEntity, modelField,
                    autoFieldsEntity.defaultFieldType, autoFieldsEntity.defaultPosition, useWhenFields, fieldList, fieldMap);
            if (UtilValidate.isNotEmpty(autoFieldsEntity.mapName)) {
                modelFormField.setMapName(autoFieldsEntity.mapName);
            }
        }
    }

    private void addAutoFieldsFromService(AutoFieldsService autoFieldsService, ModelReader entityModelReader,
            DispatchContext dispatchContext, Set<String> useWhenFields, List<ModelFormField> fieldList,
            Map<String, ModelFormField> fieldMap) {

        // read service def and auto-create fields
        ModelService modelService = null;
        try {
            modelService = dispatchContext.getModelService(autoFieldsService.serviceName);
        } catch (GenericServiceException e) {
            String errmsg = "Error finding Service with name " + autoFieldsService.serviceName
                    + " for auto-fields-service in a form widget";
            Debug.logError(e, errmsg, module);
            throw new IllegalArgumentException(errmsg);
        }

        for (ModelParam modelParam : modelService.getInModelParamList()) {
            // skip auto params that the service engine populates...
            if ("userLogin".equals(modelParam.name) || "locale".equals(modelParam.name) || "timeZone".equals(modelParam.name)
                    || "login.username".equals(modelParam.name) || "login.password".equals(modelParam.name)) {
                continue;
            }
            if (modelParam.formDisplay) {
                if (UtilValidate.isNotEmpty(modelParam.entityName) && UtilValidate.isNotEmpty(modelParam.fieldName)) {
                    ModelEntity modelEntity;
                    try {
                        modelEntity = entityModelReader.getModelEntity(modelParam.entityName);
                        if (modelEntity != null) {
                            ModelField modelField = modelEntity.getField(modelParam.fieldName);
                            if (modelField != null) {
                                // okay, populate using the entity field info...
                                ModelFormField modelFormField = addFieldFromEntityField(modelEntity, modelField,
                                        autoFieldsService.defaultFieldType, autoFieldsService.defaultPosition, useWhenFields,
                                        fieldList, fieldMap);
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

                ModelFormField modelFormField = this
                        .addFieldFromServiceParam(modelService, modelParam, autoFieldsService.defaultFieldType,
                                autoFieldsService.defaultPosition, useWhenFields, fieldList, fieldMap);
                if (UtilValidate.isNotEmpty(autoFieldsService.mapName)) {
                    modelFormField.setMapName(autoFieldsService.mapName);
                }
            }
        }
    }

    private ModelFormField addFieldFromEntityField(ModelEntity modelEntity, ModelField modelField, String defaultFieldType,
            int defaultPosition, Set<String> useWhenFields, List<ModelFormField> fieldList, Map<String, ModelFormField> fieldMap) {
        // create field def from entity field def
        ModelFormField newFormField = new ModelFormField(this);
        newFormField.setName(modelField.getName());
        newFormField.setEntityName(modelEntity.getEntityName());
        newFormField.setFieldName(modelField.getName());
        newFormField.induceFieldInfoFromEntityField(modelEntity, modelField, defaultFieldType);
        newFormField.setPosition(defaultPosition);
        return this.addUpdateField(newFormField, useWhenFields, fieldList, fieldMap);
    }

    private ModelFormField addFieldFromServiceParam(ModelService modelService, ModelParam modelParam, String defaultFieldType,
            int defaultPosition, Set<String> useWhenFields, List<ModelFormField> fieldList, Map<String, ModelFormField> fieldMap) {
        // create field def from service param def
        ModelFormField newFormField = new ModelFormField(this);
        newFormField.setName(modelParam.name);
        newFormField.setServiceName(modelService.name);
        newFormField.setAttributeName(modelParam.name);
        newFormField.setTitle(modelParam.formLabel);
        newFormField.setRequiredField(!modelParam.optional);
        newFormField.induceFieldInfoFromServiceParam(modelService, modelParam, defaultFieldType);
        newFormField.setPosition(defaultPosition);
        return this.addUpdateField(newFormField, useWhenFields, fieldList, fieldMap);
    }

    /**
     * add/override modelFormField using the fieldList and fieldMap
     *
     * @return The same ModelFormField, or if merged with an existing field, the existing field.
     */
    private ModelFormField addUpdateField(ModelFormField modelFormField, Set<String> useWhenFields,
            List<ModelFormField> fieldList, Map<String, ModelFormField> fieldMap) {
        if (!modelFormField.isUseWhenEmpty() || useWhenFields.contains(modelFormField.getName())) {
            useWhenFields.add(modelFormField.getName());
            // is a conditional field, add to the List but don't worry about the Map
            //for adding to list, see if there is another field with that name in the list and if so, put it before that one
            boolean inserted = false;
            for (int i = 0; i < fieldList.size(); i++) {
                ModelFormField curField = fieldList.get(i);
                if (curField.getName() != null && curField.getName().equals(modelFormField.getName())) {
                    fieldList.add(i, modelFormField);
                    inserted = true;
                    break;
                }
            }
            if (!inserted) {
                fieldList.add(modelFormField);
            }
            return modelFormField;
        } else {
            // not a conditional field, see if a named field exists in Map
            ModelFormField existingField = fieldMap.get(modelFormField.getName());
            if (existingField != null) {
                // does exist, update the field by doing a merge/override
                existingField.mergeOverrideModelFormField(modelFormField);
                return existingField;
            } else {
                // does not exist, add to List and Map
                fieldList.add(modelFormField);
                fieldMap.put(modelFormField.getName(), modelFormField);
                return modelFormField;
            }
        }
    }

    public List<ModelWidgetAction> getActions() {
        return actions;
    }

    public int getActualPageSize(Map<String, Object> context) {
        Integer value = (Integer) context.get("actualPageSize");
        return value != null ? value.intValue() : (getHighIndex(context) - getLowIndex(context));
    }

    public List<AltRowStyle> getAltRowStyles() {
        return altRowStyles;
    }

    public List<AltTarget> getAltTargets() {
        return altTargets;
    }

    public List<AutoFieldsEntity> getAutoFieldsEntities() {
        return autoFieldsEntities;
    }

    public List<AutoFieldsService> getAutoFieldsServices() {
        return autoFieldsServices;
    }

    @Override
    public String getBoundaryCommentName() {
        return formLocation + "#" + getName();
    }

    public Interpreter getBshInterpreter(Map<String, Object> context) throws EvalError {
        Interpreter bsh = (Interpreter) context.get("bshInterpreter");
        if (bsh == null) {
            bsh = BshUtil.makeInterpreter(context);
            context.put("bshInterpreter", bsh);
        }
        return bsh;
    }

    public boolean getClientAutocompleteFields() {
        return this.clientAutocompleteFields;
    }

    public String getContainerId() {
        // use the name if there is no id
        if (UtilValidate.isNotEmpty(this.containerId)) {
            return this.containerId;
        } else {
            return this.getName();
        }
    }

    public String getContainerStyle() {
        return this.containerStyle;
    }

    public String getCurrentContainerId(Map<String, Object> context) {
        Locale locale = UtilMisc.ensureLocale(context.get("locale"));

        String retVal = FlexibleStringExpander.expandString(this.getContainerId(), context, locale);

        Integer itemIndex = (Integer) context.get("itemIndex");
        if (itemIndex != null && "list".equals(this.getType())) {
            return retVal + this.getItemIndexSeparator() + itemIndex.intValue();
        }

        return retVal;
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

    public String getDefaultEntityName() {
        return this.defaultEntityName;
    }

    public FieldGroup getDefaultFieldGroup() {
        return defaultFieldGroup;
    }

    public Map<String, ? extends Object> getDefaultMap(Map<String, ? extends Object> context) {
        return this.defaultMapName.get(context);
    }

    public String getDefaultMapName() {
        return this.defaultMapName.getOriginalName();
    }

    public String getDefaultRequiredFieldStyle() {
        return this.defaultRequiredFieldStyle;
    }

    public String getDefaultServiceName() {
        return this.defaultServiceName;
    }

    public String getDefaultSortFieldAscStyle() {
        return this.defaultSortFieldAscStyle;
    }

    public String getDefaultSortFieldDescStyle() {
        return this.defaultSortFieldDescStyle;
    }

    public String getDefaultSortFieldStyle() {
        return this.defaultSortFieldStyle;
    }

    public String getDefaultTableStyle() {
        return this.defaultTableStyle;
    }

    public String getDefaultTitleAreaStyle() {
        return this.defaultTitleAreaStyle;
    }

    public String getDefaultTitleStyle() {
        return this.defaultTitleStyle;
    }

    public String getDefaultTooltipStyle() {
        return this.defaultTooltipStyle;
    }

    public int getDefaultViewSize() {
        return defaultViewSize;
    }

    public String getDefaultWidgetAreaStyle() {
        return this.defaultWidgetAreaStyle;
    }

    public String getDefaultWidgetStyle() {
        return this.defaultWidgetStyle;
    }

    public String getEvenRowStyle() {
        return this.evenRowStyle;
    }

    public List<FieldGroupBase> getFieldGroupList() {
        return fieldGroupList;
    }

    public Map<String, FieldGroupBase> getFieldGroupMap() {
        return fieldGroupMap;
    }

    public List<ModelFormField> getFieldList() {
        return fieldList;
    }

    public List<ModelFormField> getFieldListByPosition(List<ModelFormField> modelFormFieldList, int position) {
        List<ModelFormField> fieldListByPosition = new LinkedList<ModelFormField>();
        for (ModelFormField modelFormField : modelFormFieldList) {
            if (modelFormField.getPosition() == position) {
                fieldListByPosition.add(modelFormField);
            }
        }
        return fieldListByPosition;
    }

    public Collection<List<ModelFormField>> getFieldListsByPosition(List<ModelFormField> modelFormFieldList) {
        Map<Integer, List<ModelFormField>> fieldsByPosition = new TreeMap<Integer, List<ModelFormField>>();
        for (ModelFormField modelFormField : modelFormFieldList) {
            Integer position = Integer.valueOf(modelFormField.getPosition());
            List<ModelFormField> fieldListByPosition = fieldsByPosition.get(position);
            if (fieldListByPosition == null) {
                fieldListByPosition = new LinkedList<ModelFormField>();
                fieldsByPosition.put(position, fieldListByPosition);
            }
            fieldListByPosition.add(modelFormField);
        }
        return fieldsByPosition.values();
    }

    public String getfocusFieldName() {
        return this.focusFieldName;
    }

    public String getFocusFieldName() {
        return focusFieldName;
    }

    public String getFormLocation() {
        return this.formLocation;
    }

    public String getFormTitleAreaStyle() {
        return this.formTitleAreaStyle;
    }

    public String getFormWidgetAreaStyle() {
        return this.formWidgetAreaStyle;
    }

    public String getHeaderRowStyle() {
        return this.headerRowStyle;
    }

    public List<ModelFormField> getHiddenIgnoredFields(Map<String, Object> context, Set<String> alreadyRendered,
            List<ModelFormField> fieldList, int position) {
        List<ModelFormField> hiddenIgnoredFieldList = new LinkedList<ModelFormField>();
        for (ModelFormField modelFormField : fieldList) {
            // with position == -1 then gets all the hidden fields
            if (position != -1 && modelFormField.getPosition() != position) {
                continue;
            }
            ModelFormField.FieldInfo fieldInfo = modelFormField.getFieldInfo();

            // render hidden/ignored field widget
            switch (fieldInfo.getFieldType()) {
            case ModelFormField.FieldInfo.HIDDEN:
            case ModelFormField.FieldInfo.IGNORED:
                if (modelFormField.shouldUse(context)) {
                    hiddenIgnoredFieldList.add(modelFormField);
                    if (alreadyRendered != null)
                        alreadyRendered.add(modelFormField.getName());
                }
                break;

            case ModelFormField.FieldInfo.DISPLAY:
            case ModelFormField.FieldInfo.DISPLAY_ENTITY:
                ModelFormField.DisplayField displayField = (ModelFormField.DisplayField) fieldInfo;
                if (displayField.getAlsoHidden() && modelFormField.shouldUse(context)) {
                    hiddenIgnoredFieldList.add(modelFormField);
                    // don't add to already rendered here, or the display won't ger rendered: if (alreadyRendered != null) alreadyRendered.add(modelFormField.getName());
                }
                break;

            case ModelFormField.FieldInfo.HYPERLINK:
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

    public boolean getHideHeader() {
        return this.hideHeader;
    }

    public int getHighIndex(Map<String, Object> context) {
        Integer value = (Integer) context.get("highIndex");
        return value != null ? value.intValue() : 0;
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
                if (firstFound)
                    inbetweenList.add(obj);
            } else {
                FieldGroup fieldGroup = (FieldGroup) obj;
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

    public String getItemIndexSeparator() {
        if (UtilValidate.isNotEmpty(this.itemIndexSeparator)) {
            return this.itemIndexSeparator;
        } else {
            return "_o_";
        }
    }

    public List<String> getLastOrderFields() {
        return lastOrderFields;
    }

    public String getListEntryName() {
        return this.listEntryName;
    }

    public void getListLimits(Map<String, Object> context, Object entryList) {
        int viewIndex = 0;
        int viewSize = 0;
        int lowIndex = 0;
        int highIndex = 0;

        int listSize = getOverrideListSize(context);
        if (listSize > 0) {
            //setOverridenListSize(true);
        } else if (entryList instanceof EntityListIterator) {
            EntityListIterator iter = (EntityListIterator) entryList;
            try {
                listSize = iter.getResultsSizeAfterPartialList();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error getting list size", module);
                listSize = 0;
            }
        } else if (entryList instanceof List<?>) {
            List<?> items = (List<?>) entryList;
            listSize = items.size();
        }

        if (getPaginate(context)) {
            viewIndex = this.getViewIndex(context);
            viewSize = this.getViewSize(context);
            lowIndex = viewIndex * viewSize;
            highIndex = (viewIndex + 1) * viewSize;
        } else {
            viewIndex = 0;
            viewSize = MAX_PAGE_SIZE;
            lowIndex = 0;
            highIndex = MAX_PAGE_SIZE;
        }

        context.put("listSize", Integer.valueOf(listSize));
        context.put("viewIndex", Integer.valueOf(viewIndex));
        context.put("viewSize", Integer.valueOf(viewSize));
        context.put("lowIndex", Integer.valueOf(lowIndex));
        context.put("highIndex", Integer.valueOf(highIndex));
    }

    public String getListName() {
        return this.listName;
    }

    public int getListSize(Map<String, Object> context) {
        Integer value = (Integer) context.get("listSize");
        return value != null ? value.intValue() : 0;
    }

    public int getLowIndex(Map<String, Object> context) {
        Integer value = (Integer) context.get("lowIndex");
        return value != null ? value.intValue() : 0;
    }

    public String getMultiPaginateIndexField(Map<String, Object> context) {
        String field = this.paginateIndexField.expandString(context);
        if (UtilValidate.isEmpty(field)) {
            field = DEFAULT_PAG_INDEX_FIELD;
        }
        //  append the paginator number
        field = field + "_" + WidgetWorker.getPaginatorNumber(context);
        return field;
    }

    public String getMultiPaginateSizeField(Map<String, Object> context) {
        String field = this.paginateSizeField.expandString(context);
        if (UtilValidate.isEmpty(field)) {
            field = DEFAULT_PAG_SIZE_FIELD;
        }
        //  append the paginator number
        field = field + "_" + WidgetWorker.getPaginatorNumber(context);
        return field;
    }

    public List<ModelFormField> getMultiSubmitFields() {
        return this.multiSubmitFields;
    }

    public String getOddRowStyle() {
        return this.oddRowStyle;
    }

    public List<UpdateArea> getOnPaginateUpdateAreas() {
        return this.onPaginateUpdateAreas;
    }

    public List<UpdateArea> getOnSortColumnUpdateAreas() {
        return this.onSortColumnUpdateAreas;
    }

    /* Returns the list of ModelForm.UpdateArea objects.
     */
    public List<UpdateArea> getOnSubmitUpdateAreas() {
        return this.onSubmitUpdateAreas;
    }

    public String getOverrideListSize() {
        return overrideListSize.getOriginal();
    }

    private int getOverrideListSize(Map<String, Object> context) {
        int listSize = 0;
        if (!this.overrideListSize.isEmpty()) {
            String size = this.overrideListSize.expandString(context);
            try {
                size = size.replaceAll("[^0-9.]", "");
                listSize = Integer.parseInt(size);
            } catch (NumberFormatException e) {
                Debug.logError(e, "Error getting override list size from value " + size, module);
            }
        }
        return listSize;
    }

    public String getPaginate() {
        return paginate.getOriginal();
    }

    public boolean getPaginate(Map<String, Object> context) {
        String paginate = this.paginate.expandString(context);
        if (!paginate.isEmpty()) {
            return Boolean.valueOf(paginate).booleanValue();
        } else {
            return true;
        }
    }

    public String getPaginateFirstLabel() {
        return paginateFirstLabel.getOriginal();
    }

    public String getPaginateFirstLabel(Map<String, Object> context) {
        Locale locale = (Locale) context.get("locale");
        String field = this.paginateFirstLabel.expandString(context);
        if (UtilValidate.isEmpty(field)) {
            field = UtilProperties.getMessage("CommonUiLabels", "CommonFirst", locale);
        }
        return field;
    }

    public String getPaginateFirstStyle() {
        return DEFAULT_PAG_FIRST_STYLE;
    }

    public String getPaginateIndexField() {
        return paginateIndexField.getOriginal();
    }

    public String getPaginateIndexField(Map<String, Object> context) {
        String field = this.paginateIndexField.expandString(context);
        if (field.isEmpty()) {
            return DEFAULT_PAG_INDEX_FIELD;
        }
        return field;
    }

    public String getPaginateLastLabel() {
        return paginateLastLabel.getOriginal();
    }

    public String getPaginateLastLabel(Map<String, Object> context) {
        Locale locale = (Locale) context.get("locale");
        String field = this.paginateLastLabel.expandString(context);
        if (UtilValidate.isEmpty(field)) {
            field = UtilProperties.getMessage("CommonUiLabels", "CommonLast", locale);
        }
        return field;
    }

    public String getPaginateLastStyle() {
        return DEFAULT_PAG_LAST_STYLE;
    }

    public String getPaginateNextLabel() {
        return paginateNextLabel.getOriginal();
    }

    public String getPaginateNextLabel(Map<String, Object> context) {
        String field = this.paginateNextLabel.expandString(context);
        if (field.isEmpty()) {
            Locale locale = (Locale) context.get("locale");
            return UtilProperties.getMessage("CommonUiLabels", "CommonNext", locale);
        }
        return field;
    }

    public String getPaginateNextStyle() {
        return DEFAULT_PAG_NEXT_STYLE;
    }

    public String getPaginatePreviousLabel() {
        return paginatePreviousLabel.getOriginal();
    }

    public String getPaginatePreviousLabel(Map<String, Object> context) {
        String field = this.paginatePreviousLabel.expandString(context);
        if (field.isEmpty()) {
            Locale locale = (Locale) context.get("locale");
            field = UtilProperties.getMessage("CommonUiLabels", "CommonPrevious", locale);
        }
        return field;
    }

    public String getPaginatePreviousStyle() {
        return DEFAULT_PAG_PREV_STYLE;
    }

    public String getPaginateSizeField() {
        return paginateSizeField.getOriginal();
    }

    public String getPaginateSizeField(Map<String, Object> context) {
        String field = this.paginateSizeField.expandString(context);
        if (field.isEmpty()) {
            return DEFAULT_PAG_SIZE_FIELD;
        }
        return field;
    }

    public String getPaginateStyle() {
        return this.paginateStyle;
    }

    public String getPaginateTarget() {
        return paginateTarget.getOriginal();
    }

    public String getPaginateTarget(Map<String, Object> context) {
        String targ = this.paginateTarget.expandString(context);
        if (targ.isEmpty()) {
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

    public String getPaginateViewSizeLabel() {
        return paginateViewSizeLabel.getOriginal();
    }

    public String getPaginateViewSizeLabel(Map<String, Object> context) {
        String field = this.paginateViewSizeLabel.expandString(context);
        if (field.isEmpty()) {
            Locale locale = (Locale) context.get("locale");
            return UtilProperties.getMessage("CommonUiLabels", "CommonItemsPerPage", locale);
        }
        return field;
    }

    private ModelForm getParentForm(Element formElement, ModelReader entityModelReader, DispatchContext dispatchContext) {
        ModelForm parent = null;
        String parentResource = formElement.getAttribute("extends-resource");
        String parentForm = formElement.getAttribute("extends");
        if (parentForm.length() > 0) {
            // check if we have a resource name (part of the string before the ?)
            if (parentResource.length() > 0) {
                try {
                    parent = FormFactory.getFormFromLocation(parentResource, parentForm, entityModelReader, dispatchContext);
                } catch (Exception e) {
                    Debug.logError(e, "Failed to load parent form definition '" + parentForm + "' at resource '" + parentResource
                            + "'", module);
                }
            } else if (!parentForm.equals(formElement.getAttribute("name"))) {
                // try to find a form definition in the same file
                Element rootElement = formElement.getOwnerDocument().getDocumentElement();
                List<? extends Element> formElements = UtilXml.childElementList(rootElement, "form");
                //Uncomment below to add support for abstract forms
                //formElements.addAll(UtilXml.childElementList(rootElement, "abstract-form"));
                for (Element formElementEntry : formElements) {
                    if (formElementEntry.getAttribute("name").equals(parentForm)) {
                        parent = new ModelForm(formElementEntry, parentResource, entityModelReader, dispatchContext);
                        break;
                    }
                }
                if (parent == null) {
                    Debug.logError("Failed to find parent form definition '" + parentForm + "' in same document.", module);
                }
            } else {
                Debug.logError("Recursive form definition found for '" + formElement.getAttribute("name") + ".'", module);
            }
        }
        return parent;
    }

    public String getParentFormLocation() {
        return this.parentModelForm == null ? null : this.parentModelForm.getFormLocation();
    }

    public String getParentFormName() {
        return this.parentModelForm == null ? null : this.parentModelForm.getName();
    }

    public ModelForm getParentModelForm() {
        return parentModelForm;
    }

    public String getPassedRowCount(Map<String, Object> context) {
        return rowCountExdr.expandString(context);
    }

    public List<ModelWidgetAction> getRowActions() {
        return rowActions;
    }

    public String getRowCount() {
        return rowCountExdr.getOriginal();
    }

    public boolean getSeparateColumns() {
        return this.separateColumns;
    }

    public boolean getSkipEnd() {
        return this.skipEnd;
    }

    public boolean getSkipStart() {
        return this.skipStart;
    }

    public String getSortField(Map<String, Object> context) {
        String value = null;
        try {
            value = (String) context.get(this.sortFieldParameterName);
            if (value == null) {
                Map<String, String> parameters = UtilGenerics.cast(context.get("parameters"));
                if (parameters != null) {
                    value = parameters.get(this.sortFieldParameterName);
                }
            }
        } catch (Exception e) {
            Debug.logWarning(e, "Error getting sortField: " + e.toString(), module);
        }
        return value;
    }

    public String getSortFieldParameterName() {
        return this.sortFieldParameterName;
    }

    public List<SortField> getSortOrderFields() {
        return sortOrderFields;
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
                    throw new IllegalArgumentException("Return value from style condition eval was not a Boolean: "
                            + retVal.getClass().getName() + " [" + retVal + "] of form " + getName());
                }
            }
        } catch (EvalError e) {
            String errmsg = "Error evaluating BeanShell style conditions on form " + getName();
            Debug.logError(e, errmsg, module);
            throw new IllegalArgumentException(errmsg);
        }

        return styles;
    }

    public String getTarget() {
        return target.getOriginal();
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
            for (AltTarget altTarget : this.altTargets) {
                String useWhen = FlexibleStringExpander.expandString(altTarget.useWhen, context);
                Object retVal = bsh.eval(StringUtil.convertOperatorSubstitutions(useWhen));
                boolean condTrue = false;
                // retVal should be a Boolean, if not something weird is up...
                if (retVal instanceof Boolean) {
                    Boolean boolVal = (Boolean) retVal;
                    condTrue = boolVal.booleanValue();
                } else {
                    throw new IllegalArgumentException("Return value from target condition eval was not a Boolean: "
                            + retVal.getClass().getName() + " [" + retVal + "] of form " + getName());
                }

                if (condTrue && !targetType.equals("inter-app")) {
                    return altTarget.targetExdr.expandString(expanderContext);
                }
            }
        } catch (EvalError e) {
            String errmsg = "Error evaluating BeanShell target conditions on form " + getName();
            Debug.logError(e, errmsg, module);
            throw new IllegalArgumentException(errmsg);
        }

        return target.expandString(expanderContext);
    }

    public String getTargetType() {
        return this.targetType;
    }

    public String getTargetWindow() {
        return targetWindowExdr.getOriginal();
    }

    public String getTargetWindow(Map<String, Object> context) {
        return this.targetWindowExdr.expandString(context);
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

    public boolean getUseRowSubmit() {
        return this.useRowSubmit;
    }

    public Set<String> getUseWhenFields() {
        return useWhenFields;
    }

    public int getViewIndex(Map<String, Object> context) {
        String field = this.getMultiPaginateIndexField(context);
        int viewIndex = 0;
        try {
            Object value = context.get(field);
            if (value == null) {
                // try parameters.VIEW_INDEX as that is an old OFBiz convention
                Map<String, Object> parameters = UtilGenerics.cast(context.get("parameters"));
                if (parameters != null) {
                    value = parameters.get("VIEW_INDEX" + "_" + WidgetWorker.getPaginatorNumber(context));

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

    public int getViewSize(Map<String, Object> context) {
        String field = this.getMultiPaginateSizeField(context);
        int viewSize = this.defaultViewSize;
        try {
            Object value = context.get(field);
            if (value == null) {
                // try parameters.VIEW_SIZE as that is an old OFBiz convention
                Map<String, Object> parameters = UtilGenerics.cast(context.get("parameters"));
                if (parameters != null) {
                    value = parameters.get("VIEW_SIZE" + "_" + WidgetWorker.getPaginatorNumber(context));

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

    public boolean getGroupColumns() {
        return groupColumns;
    }

    private boolean isOverridenListSize() {
        return !this.overrideListSize.isEmpty();
    }

    public void preparePager(Map<String, Object> context) {

        String lookupName = this.getListName();
        if (UtilValidate.isEmpty(lookupName)) {
            Debug.logError("No value for list or iterator name found.", module);
            return;
        }
        Object obj = context.get(lookupName);
        if (obj == null) {
            if (Debug.verboseOn())
                Debug.logVerbose("No object for list or iterator name [" + lookupName + "] found, so not running pagination.",
                        module);
            return;
        }
        // if list is empty, do not render rows
        Iterator<?> iter = null;
        if (obj instanceof Iterator<?>) {
            iter = (Iterator<?>) obj;
        } else if (obj instanceof List<?>) {
            iter = ((List<?>) obj).listIterator();
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

        if (iter == null)
            return;

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
    public void renderFormString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer)
            throws IOException {
        //  increment the paginator, only for list and multi forms
        if ("list".equals(this.type) || "multi".equals(this.type)) {
            WidgetWorker.incrementPaginatorNumber(context);
        }

        //if pagination is disabled, update the default view size
        if (!getPaginate(context)) {
            // FIXME: Don't modify model state
            //setDefaultViewSize(ModelForm.MAX_PAGE_SIZE);
        }

        // Populate the viewSize and viewIndex so they are available for use during form actions
        context.put("viewIndex", this.getViewIndex(context));
        context.put("viewSize", this.getViewSize(context));

        runFormActions(context);

        // if this is a list form, don't useRequestParameters
        if ("list".equals(this.type) || "multi".equals(this.type)) {
            context.put("useRequestParameters", Boolean.FALSE);
        }

        // find the highest position number to get the max positions used
        int positions = 1;
        for (ModelFormField modelFormField : this.fieldList) {
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
                throw new IllegalArgumentException(
                        "Error rendering form, a field has no FieldInfo, ie no sub-element for the type of field for field named: "
                                + modelFormField.getName());
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
                throw new IllegalArgumentException("The form 'type' tag is missing or empty on the form with the name "
                        + this.getName());
            } else {
                throw new IllegalArgumentException("The form type " + this.getType() + " is not supported for form with name "
                        + this.getName());
            }
        }
    }

    public int renderHeaderRow(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer)
            throws IOException {
        int maxNumOfColumns = 0;

        // We will render one title/column for all the fields with the same name
        // in this model: we can have more fields with the same name when use-when
        // conditions are used or when a form is extended or when the fields are
        // automatically retrieved by a service or entity definition.
        List<ModelFormField> tempFieldList = new LinkedList<ModelFormField>();
        tempFieldList.addAll(this.fieldList);
        for (int j = 0; j < tempFieldList.size(); j++) {
            ModelFormField modelFormField = tempFieldList.get(j);
            for (int i = j + 1; i < tempFieldList.size(); i++) {
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
        List<Map<String, List<ModelFormField>>> fieldRowsByPosition = new LinkedList<Map<String, List<ModelFormField>>>(); // this list will contain maps, each one containing the list of fields for a position
        for (List<ModelFormField> mainFieldList : fieldListsByPosition) {
            int numOfColumns = 0;

            List<ModelFormField> innerDisplayHyperlinkFieldsBegin = new LinkedList<ModelFormField>();
            List<ModelFormField> innerFormFields = new LinkedList<ModelFormField>();
            List<ModelFormField> innerDisplayHyperlinkFieldsEnd = new LinkedList<ModelFormField>();

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
            for (ModelFormField modelFormField : mainFieldList) {
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
                if (fieldInfo.getFieldType() == ModelFormField.FieldInfo.HIDDEN
                        || fieldInfo.getFieldType() == ModelFormField.FieldInfo.IGNORED) {
                    continue;
                }

                if (fieldInfo.getFieldType() != ModelFormField.FieldInfo.DISPLAY
                        && fieldInfo.getFieldType() != ModelFormField.FieldInfo.DISPLAY_ENTITY
                        && fieldInfo.getFieldType() != ModelFormField.FieldInfo.HYPERLINK) {
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
            for (ModelFormField modelFormField : mainFieldList) {
                ModelFormField.FieldInfo fieldInfo = modelFormField.getFieldInfo();

                // don't do any header for hidden or ignored fields
                if (fieldInfo.getFieldType() == ModelFormField.FieldInfo.HIDDEN
                        || fieldInfo.getFieldType() == ModelFormField.FieldInfo.IGNORED) {
                    continue;
                }

                // skip all of the display/hyperlink fields
                if (fieldInfo.getFieldType() == ModelFormField.FieldInfo.DISPLAY
                        || fieldInfo.getFieldType() == ModelFormField.FieldInfo.DISPLAY_ENTITY
                        || fieldInfo.getFieldType() == ModelFormField.FieldInfo.HYPERLINK) {
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
                    "inputFields", innerFormFields, "displayAfter", innerDisplayHyperlinkFieldsEnd, "mainFieldList",
                    mainFieldList);
            fieldRowsByPosition.add(fieldRow);
        }
        // ===========================
        // Rendering
        // ===========================
        for (Map<String, List<ModelFormField>> listsMap : fieldRowsByPosition) {
            List<ModelFormField> innerDisplayHyperlinkFieldsBegin = listsMap.get("displayBefore");
            List<ModelFormField> innerFormFields = listsMap.get("inputFields");
            List<ModelFormField> innerDisplayHyperlinkFieldsEnd = listsMap.get("displayAfter");
            List<ModelFormField> mainFieldList = listsMap.get("mainFieldList");

            int numOfCells = innerDisplayHyperlinkFieldsBegin.size() + innerDisplayHyperlinkFieldsEnd.size()
                    + (innerFormFields.size() > 0 ? 1 : 0);
            int numOfColumnsToSpan = maxNumOfColumns - numOfCells + 1;
            if (numOfColumnsToSpan < 1) {
                numOfColumnsToSpan = 1;
            }

            if (numOfCells > 0) {
                formStringRenderer.renderFormatHeaderRowOpen(writer, context, this);

                if (this.groupColumns) {
                    Iterator<ModelFormField> innerDisplayHyperlinkFieldsBeginIt = innerDisplayHyperlinkFieldsBegin.iterator();
                    while (innerDisplayHyperlinkFieldsBeginIt.hasNext()) {
                        ModelFormField modelFormField = innerDisplayHyperlinkFieldsBeginIt.next();
                        // span columns only if this is the last column in the row (not just in this first list)
                        if (innerDisplayHyperlinkFieldsBeginIt.hasNext() || numOfCells > innerDisplayHyperlinkFieldsBegin.size()) {
                            formStringRenderer.renderFormatHeaderRowCellOpen(writer, context, this, modelFormField, 1);
                        } else {
                            formStringRenderer.renderFormatHeaderRowCellOpen(writer, context, this, modelFormField,
                                    numOfColumnsToSpan);
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
                                    formStringRenderer.renderFormatHeaderRowFormCellTitleSeparator(writer, context, this,
                                            modelFormField, false);
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
                            formStringRenderer.renderFormatHeaderRowCellOpen(writer, context, this, modelFormField,
                                    numOfColumnsToSpan);
                        }
                        formStringRenderer.renderFieldTitle(writer, context, modelFormField);
                        formStringRenderer.renderFormatHeaderRowCellClose(writer, context, this, modelFormField);
                    }
                } else {
                    Iterator<ModelFormField> mainFieldListIter = mainFieldList.iterator();
                    while (mainFieldListIter.hasNext()) {
                        ModelFormField modelFormField = mainFieldListIter.next();

                        // don't do any header for hidden or ignored fields
                        ModelFormField.FieldInfo fieldInfo = modelFormField.getFieldInfo();
                        if (fieldInfo.getFieldType() == ModelFormField.FieldInfo.HIDDEN
                                || fieldInfo.getFieldType() == ModelFormField.FieldInfo.IGNORED) {
                            continue;
                        }

                        // span columns only if this is the last column in the row (not just in this first list)
                        if (mainFieldListIter.hasNext() || numOfCells > mainFieldList.size()) {
                            formStringRenderer.renderFormatHeaderRowCellOpen(writer, context, this, modelFormField, 1);
                        } else {
                            formStringRenderer.renderFormatHeaderRowCellOpen(writer, context, this, modelFormField,
                                    numOfColumnsToSpan);
                        }
                        formStringRenderer.renderFieldTitle(writer, context, modelFormField);
                        formStringRenderer.renderFormatHeaderRowCellClose(writer, context, this, modelFormField);
                    }
                }

                formStringRenderer.renderFormatHeaderRowClose(writer, context, this);
            }
        }

        return maxNumOfColumns;
    }

    public void renderHiddenIgnoredFields(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer,
            List<ModelFormField> fieldList) throws IOException {
        for (ModelFormField modelFormField : fieldList) {
            ModelFormField.FieldInfo fieldInfo = modelFormField.getFieldInfo();

            // render hidden/ignored field widget
            switch (fieldInfo.getFieldType()) {
            case ModelFormField.FieldInfo.HIDDEN:
            case ModelFormField.FieldInfo.IGNORED:
                modelFormField.renderFieldString(writer, context, formStringRenderer);
                break;

            case ModelFormField.FieldInfo.DISPLAY:
            case ModelFormField.FieldInfo.DISPLAY_ENTITY:
            case ModelFormField.FieldInfo.HYPERLINK:
                formStringRenderer.renderHiddenField(writer, context, modelFormField, modelFormField.getEntry(context));
                break;
            }
        }
    }

    // The fields in the three lists, usually created in the preprocessing phase
    // of the renderItemRows method are rendered: this will create a visual representation
    // of one row (corresponding to one position).
    public void renderItemRow(Appendable writer, Map<String, Object> localContext, FormStringRenderer formStringRenderer,
            boolean formPerItem, List<ModelFormField> hiddenIgnoredFieldList,
            List<ModelFormField> innerDisplayHyperlinkFieldsBegin, List<ModelFormField> innerFormFields,
            List<ModelFormField> innerDisplayHyperlinkFieldsEnd, List<ModelFormField> mainFieldList, int position,
            int numOfColumns) throws IOException {
        int numOfCells = innerDisplayHyperlinkFieldsBegin.size() + innerDisplayHyperlinkFieldsEnd.size()
                + (innerFormFields.size() > 0 ? 1 : 0);
        int numOfColumnsToSpan = numOfColumns - numOfCells + 1;
        if (numOfColumnsToSpan < 1) {
            numOfColumnsToSpan = 1;
        }

        // render row formatting open
        formStringRenderer.renderFormatItemRowOpen(writer, localContext, this);
        Iterator<ModelFormField> innerDisplayHyperlinkFieldsBeginIter = innerDisplayHyperlinkFieldsBegin.iterator();
        Map<String, Integer> fieldCount = new HashMap<String, Integer>();
        while (innerDisplayHyperlinkFieldsBeginIter.hasNext()) {
            ModelFormField modelFormField = innerDisplayHyperlinkFieldsBeginIter.next();
            if (fieldCount.containsKey(modelFormField.getFieldName())) {
                fieldCount.put(modelFormField.getFieldName(), fieldCount.get(modelFormField.getFieldName()) + 1);
            } else {
                fieldCount.put(modelFormField.getFieldName(), 1);
            }
        }

        if (this.groupColumns) {
            // do the first part of display and hyperlink fields
            Iterator<ModelFormField> innerDisplayHyperlinkFieldIter = innerDisplayHyperlinkFieldsBegin.iterator();
            while (innerDisplayHyperlinkFieldIter.hasNext()) {
                boolean cellOpen = false;
                ModelFormField modelFormField = innerDisplayHyperlinkFieldIter.next();
                // span columns only if this is the last column in the row (not just in this first list)
                if (fieldCount.get(modelFormField.getName()) < 2) {
                    if ((innerDisplayHyperlinkFieldIter.hasNext() || numOfCells > innerDisplayHyperlinkFieldsBegin.size())) {
                        formStringRenderer.renderFormatItemRowCellOpen(writer, localContext, this, modelFormField, 1);
                    } else {
                        formStringRenderer.renderFormatItemRowCellOpen(writer, localContext, this, modelFormField,
                                numOfColumnsToSpan);
                    }
                    cellOpen = true;
                }
                if ((!"list".equals(this.getType()) && !"multi".equals(this.getType())) || modelFormField.shouldUse(localContext)) {
                    if ((fieldCount.get(modelFormField.getName()) > 1)) {
                        if ((innerDisplayHyperlinkFieldIter.hasNext() || numOfCells > innerDisplayHyperlinkFieldsBegin.size())) {
                            formStringRenderer.renderFormatItemRowCellOpen(writer, localContext, this, modelFormField, 1);
                        } else {
                            formStringRenderer.renderFormatItemRowCellOpen(writer, localContext, this, modelFormField,
                                    numOfColumnsToSpan);
                        }
                        cellOpen = true;
                    }
                    modelFormField.renderFieldString(writer, localContext, formStringRenderer);
                }
                if (cellOpen) {
                    formStringRenderer.renderFormatItemRowCellClose(writer, localContext, this, modelFormField);
                }
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
                    if ((!"list".equals(this.getType()) && !"multi".equals(this.getType()))
                            || modelFormField.shouldUse(localContext)) {
                        modelFormField.renderFieldString(writer, localContext, formStringRenderer);
                    }

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
                    formStringRenderer
                            .renderFormatItemRowCellOpen(writer, localContext, this, modelFormField, numOfColumnsToSpan);
                }
                if ((!"list".equals(this.getType()) && !"multi".equals(this.getType())) || modelFormField.shouldUse(localContext)) {
                    modelFormField.renderFieldString(writer, localContext, formStringRenderer);
                }
                formStringRenderer.renderFormatItemRowCellClose(writer, localContext, this, modelFormField);
            }
        } else {
            // do all of the hidden fields...
            this.renderHiddenIgnoredFields(writer, localContext, formStringRenderer, hiddenIgnoredFieldList);

            Iterator<ModelFormField> mainFieldIter = mainFieldList.iterator();
            while (mainFieldIter.hasNext()) {
                ModelFormField modelFormField = mainFieldIter.next();

                // don't do any header for hidden or ignored fields inside this loop
                ModelFormField.FieldInfo fieldInfo = modelFormField.getFieldInfo();
                if (fieldInfo.getFieldType() == ModelFormField.FieldInfo.HIDDEN
                        || fieldInfo.getFieldType() == ModelFormField.FieldInfo.IGNORED) {
                    continue;
                }

                // span columns only if this is the last column in the row
                if (mainFieldIter.hasNext()) {
                    formStringRenderer.renderFormatItemRowCellOpen(writer, localContext, this, modelFormField, 1);
                } else {
                    formStringRenderer
                            .renderFormatItemRowCellOpen(writer, localContext, this, modelFormField, numOfColumnsToSpan);
                }
                if ((!"list".equals(this.getType()) && !"multi".equals(this.getType())) || modelFormField.shouldUse(localContext)) {
                    modelFormField.renderFieldString(writer, localContext, formStringRenderer);
                }
                formStringRenderer.renderFormatItemRowCellClose(writer, localContext, this, modelFormField);
            }
        }

        // render row formatting close
        formStringRenderer.renderFormatItemRowClose(writer, localContext, this);
    }

    public void renderItemRows(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer,
            boolean formPerItem, int numOfColumns) throws IOException {
        String lookupName = this.getListName();
        if (UtilValidate.isEmpty(lookupName)) {
            Debug.logError("No value for list or iterator name found.", module);
            return;
        }
        Object obj = context.get(lookupName);
        if (obj == null) {
            if (Debug.verboseOn())
                Debug.logVerbose("No object for list or iterator name [" + lookupName + "] found, so not rendering rows.", module);
            return;
        }
        // if list is empty, do not render rows
        Iterator<?> iter = null;
        if (obj instanceof Iterator<?>) {
            iter = (Iterator<?>) obj;
        } else if (obj instanceof List<?>) {
            iter = ((List<?>) obj).listIterator();
        }

        // set low and high index
        getListLimits(context, obj);

        int listSize = ((Integer) context.get("listSize")).intValue();
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
            Map<String, Object> previousItem = new HashMap<String, Object>();
            while ((item = this.safeNext(iter)) != null) {
                itemIndex++;
                if (itemIndex >= highIndex) {
                    break;
                }

                // TODO: this is a bad design, for EntityListIterators we should skip to the lowIndex and go from there, MUCH more efficient...
                if (itemIndex < lowIndex) {
                    continue;
                }

                // reset/remove the BshInterpreter now as well as later because chances are there is an interpreter at this level of the stack too
                this.resetBshInterpreter(context);

                Map<String, Object> itemMap = UtilGenerics.checkMap(item);
                MapStack<String> localContext = MapStack.create(context);
                if (UtilValidate.isNotEmpty(this.getListEntryName())) {
                    localContext.put(this.getListEntryName(), item);
                } else {
                    if (itemMap instanceof GenericEntity) {
                        // Rendering code might try to modify the GenericEntity instance,
                        // so we make a copy of it.
                        Map<String, Object> genericEntityClone = UtilGenerics.cast(((GenericEntity) itemMap).clone());
                        localContext.push(genericEntityClone);
                    } else {
                        localContext.push(itemMap);
                    }
                }

                // reset/remove the BshInterpreter now as well as later because chances are there is an interpreter at this level of the stack too
                this.resetBshInterpreter(localContext);
                localContext.push();
                localContext.put("previousItem", previousItem);
                previousItem = new HashMap<String, Object>();
                previousItem.putAll(itemMap);

                ModelWidgetAction.runSubActions(this.rowActions, localContext);

                localContext.put("itemIndex", Integer.valueOf(itemIndex - lowIndex));
                if (UtilValidate.isNotEmpty(context.get("renderFormSeqNumber"))) {
                    localContext.put("formUniqueId", "_" + context.get("renderFormSeqNumber"));
                }

                this.resetBshInterpreter(localContext);

                if (Debug.verboseOn())
                    Debug.logVerbose("In form got another row, context is: " + localContext, module);

                // Check to see if there is a field, same name and same use-when (could come from extended form)
                List<ModelFormField> tempFieldList = new LinkedList<ModelFormField>();
                tempFieldList.addAll(this.fieldList);
                for (int j = 0; j < tempFieldList.size(); j++) {
                    ModelFormField modelFormField = tempFieldList.get(j);
                    if (!modelFormField.isUseWhenEmpty()) {
                        boolean shouldUse1 = modelFormField.shouldUse(localContext);
                        for (int i = j + 1; i < tempFieldList.size(); i++) {
                            ModelFormField curField = tempFieldList.get(i);
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
                for (List<ModelFormField> fieldListByPosition : fieldListsByPosition) {
                    // For each position (the subset of fields with the same position attribute)
                    // we have two phases: preprocessing and rendering

                    List<ModelFormField> innerDisplayHyperlinkFieldsBegin = new LinkedList<ModelFormField>();
                    List<ModelFormField> innerFormFields = new LinkedList<ModelFormField>();
                    List<ModelFormField> innerDisplayHyperlinkFieldsEnd = new LinkedList<ModelFormField>();

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
                        if (fieldInfo.getFieldType() == ModelFormField.FieldInfo.HIDDEN
                                || fieldInfo.getFieldType() == ModelFormField.FieldInfo.IGNORED) {
                            continue;
                        }

                        if (fieldInfo.getFieldType() != ModelFormField.FieldInfo.DISPLAY
                                && fieldInfo.getFieldType() != ModelFormField.FieldInfo.DISPLAY_ENTITY
                                && fieldInfo.getFieldType() != ModelFormField.FieldInfo.HYPERLINK) {
                            // okay, now do the form cell
                            break;
                        }

                        // if this is a list or multi form don't skip here because we don't want to skip the table cell, will skip the actual field later
                        if (!"list".equals(this.getType()) && !"multi".equals(this.getType())
                                && !modelFormField.shouldUse(localContext)) {
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
                        if (fieldInfo.getFieldType() == ModelFormField.FieldInfo.HIDDEN
                                || fieldInfo.getFieldType() == ModelFormField.FieldInfo.IGNORED) {
                            continue;
                        }

                        // skip all of the display/hyperlink fields
                        if (fieldInfo.getFieldType() == ModelFormField.FieldInfo.DISPLAY
                                || fieldInfo.getFieldType() == ModelFormField.FieldInfo.DISPLAY_ENTITY
                                || fieldInfo.getFieldType() == ModelFormField.FieldInfo.HYPERLINK) {
                            continue;
                        }

                        // if this is a list or multi form don't skip here because we don't want to skip the table cell, will skip the actual field later
                        if (!"list".equals(this.getType()) && !"multi".equals(this.getType())
                                && !modelFormField.shouldUse(localContext)) {
                            continue;
                        }
                        innerFormFields.add(modelFormField);
                        currentPosition = modelFormField.getPosition();
                    }
                    while (innerDisplayHyperlinkFieldIter.hasNext()) {
                        ModelFormField modelFormField = innerDisplayHyperlinkFieldIter.next();
                        ModelFormField.FieldInfo fieldInfo = modelFormField.getFieldInfo();

                        // don't do any header for hidden or ignored fields
                        if (fieldInfo.getFieldType() == ModelFormField.FieldInfo.HIDDEN
                                || fieldInfo.getFieldType() == ModelFormField.FieldInfo.IGNORED) {
                            continue;
                        }

                        // skip all non-display and non-hyperlink fields
                        if (fieldInfo.getFieldType() != ModelFormField.FieldInfo.DISPLAY
                                && fieldInfo.getFieldType() != ModelFormField.FieldInfo.DISPLAY_ENTITY
                                && fieldInfo.getFieldType() != ModelFormField.FieldInfo.HYPERLINK) {
                            continue;
                        }

                        // if this is a list or multi form don't skip here because we don't want to skip the table cell, will skip the actual field later
                        if (!"list".equals(this.getType()) && !"multi".equals(this.getType())
                                && !modelFormField.shouldUse(localContext)) {
                            continue;
                        }
                        innerDisplayHyperlinkFieldsEnd.add(modelFormField);
                        currentPosition = modelFormField.getPosition();
                    }
                    List<ModelFormField> hiddenIgnoredFieldList = getHiddenIgnoredFields(localContext, null, tempFieldList,
                            currentPosition);

                    // Rendering:
                    // the fields in the three lists created in the preprocessing phase
                    // are now rendered: this will create a visual representation
                    // of one row (for the current position).
                    if (innerDisplayHyperlinkFieldsBegin.size() > 0 || innerFormFields.size() > 0
                            || innerDisplayHyperlinkFieldsEnd.size() > 0) {
                        this.renderItemRow(writer, localContext, formStringRenderer, formPerItem, hiddenIgnoredFieldList,
                                innerDisplayHyperlinkFieldsBegin, innerFormFields, innerDisplayHyperlinkFieldsEnd,
                                fieldListByPosition, currentPosition, numOfColumns);
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

    public void renderListFormString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer,
            int positions) throws IOException {
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

    public void renderMultiFormString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer,
            int positions) throws IOException {
        if (!skipStart) {
            formStringRenderer.renderFormOpen(writer, context, this);
        }

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
        this.renderItemRows(writer, context, formStringRenderer, false, numOfColumns);

        formStringRenderer.renderFormatListWrapperClose(writer, context, this);

        if (!skipEnd) {
            formStringRenderer.renderMultiFormClose(writer, context, this);
        }

    }

    public void renderSingleFormString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer,
            int positions) throws IOException {
        List<ModelFormField> tempFieldList = new LinkedList<ModelFormField>();
        tempFieldList.addAll(this.fieldList);

        // Check to see if there is a field, same name and same use-when (could come from extended form)
        for (int j = 0; j < tempFieldList.size(); j++) {
            ModelFormField modelFormField = tempFieldList.get(j);
            if (this.useWhenFields.contains(modelFormField.getName())) {
                boolean shouldUse1 = modelFormField.shouldUse(context);
                for (int i = j + 1; i < tempFieldList.size(); i++) {
                    ModelFormField curField = tempFieldList.get(i);
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
        if (!skipStart)
            formStringRenderer.renderFormOpen(writer, context, this);

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
            currentFieldGroup = (FieldGroup) fieldGroupMap.get(currentFormField.getFieldName());
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
                for (FieldGroupBase obj : inbetweenList) {
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
                    nextFormField = fieldIter.next();
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
                        if (haveRenderedOpenFieldRow) {
                            formStringRenderer.renderFormatFieldRowClose(writer, context, this);
                            haveRenderedOpenFieldRow = false;
                        }
                        lastFieldGroup.renderEndString(writer, context, formStringRenderer);

                        List<FieldGroupBase> inbetweenList = getInbetweenList(lastFieldGroup, currentFieldGroup);
                        for (FieldGroupBase obj : inbetweenList) {
                            if (obj instanceof ModelForm.Banner) {
                                ((ModelForm.Banner) obj).renderString(writer, context, formStringRenderer);
                            }
                        }
                    }
                }

                if (lastFieldGroup == null || !lastFieldGroupName.equals(currentFieldGroupName)) {
                    currentFieldGroup.renderStartString(writer, context, formStringRenderer);
                    lastFieldGroup = currentFieldGroup;
                }
            }

            ModelFormField.FieldInfo fieldInfo = currentFormField.getFieldInfo();
            if (fieldInfo.getFieldType() == ModelFormField.FieldInfo.HIDDEN
                    || fieldInfo.getFieldType() == ModelFormField.FieldInfo.IGNORED) {
                continue;
            }
            if (alreadyRendered.contains(currentFormField.getName())) {
                continue;
            }
            //Debug.logInfo("In single form evaluating use-when for field " + currentFormField.getName() + ": " + currentFormField.getUseWhen(), module);
            if (!currentFormField.shouldUse(context)) {
                if (UtilValidate.isNotEmpty(lastFormField)) {
                    currentFormField = lastFormField;
                }
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

            //
            // It must be a row open before rendering a field. If not, open it
            //
            if (!haveRenderedOpenFieldRow) {
                formStringRenderer.renderFormatFieldRowOpen(writer, context, this);
                haveRenderedOpenFieldRow = true;
            }

            // render title formatting open
            formStringRenderer.renderFormatFieldRowTitleCellOpen(writer, context, currentFormField);

            // render title (unless this is a submit or a reset field)
            if (fieldInfo.getFieldType() != ModelFormField.FieldInfo.SUBMIT
                    && fieldInfo.getFieldType() != ModelFormField.FieldInfo.RESET) {
                formStringRenderer.renderFieldTitle(writer, context, currentFormField);
            } else {
                formStringRenderer.renderFormatEmptySpace(writer, context, this);
            }

            // render title formatting close
            formStringRenderer.renderFormatFieldRowTitleCellClose(writer, context, currentFormField);

            // render separator
            formStringRenderer.renderFormatFieldRowSpacerCell(writer, context, currentFormField);

            // render widget formatting open
            formStringRenderer.renderFormatFieldRowWidgetCellOpen(writer, context, currentFormField, positions, positionSpan,
                    nextPositionInRow);

            // render widget
            currentFormField.renderFieldString(writer, context, formStringRenderer);

            // render widget formatting close
            formStringRenderer.renderFormatFieldRowWidgetCellClose(writer, context, currentFormField, positions, positionSpan,
                    nextPositionInRow);

        }
        // render row formatting close after the end if needed
        if (haveRenderedOpenFieldRow) {
            formStringRenderer.renderFormatFieldRowClose(writer, context, this);
        }

        if (lastFieldGroup != null) {
            lastFieldGroup.renderEndString(writer, context, formStringRenderer);
        }
        // render formatting wrapper close
        // should be handled by renderEndString
        //formStringRenderer.renderFormatSingleWrapperClose(writer, context, this);

        // render form close
        if (!skipEnd)
            formStringRenderer.renderFormClose(writer, context, this);

    }

    public void resetBshInterpreter(Map<String, Object> context) {
        context.remove("bshInterpreter");
    }

    public void runFormActions(Map<String, Object> context) {
        ModelWidgetAction.runSubActions(this.actions, context);
    }

    private <X> X safeNext(Iterator<X> iterator) {
        try {
            return iterator.next();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public static class AltRowStyle {
        public final String useWhen;
        public final String style;

        public AltRowStyle(Element altRowStyleElement) {
            this.useWhen = altRowStyleElement.getAttribute("use-when");
            this.style = altRowStyleElement.getAttribute("style");
        }
    }

    public static class AltTarget {
        public final String useWhen;
        public final FlexibleStringExpander targetExdr;

        public AltTarget(Element altTargetElement) {
            this.useWhen = altTargetElement.getAttribute("use-when");
            this.targetExdr = FlexibleStringExpander.getInstance(altTargetElement.getAttribute("target"));
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof AltTarget && obj.hashCode() == this.hashCode();
        }

        @Override
        public int hashCode() {
            return useWhen.hashCode();
        }
    }

    public static class AutoFieldsEntity {
        public final String entityName;
        public final String mapName;
        public final String defaultFieldType;
        public final int defaultPosition;

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
                Debug.logError(e, "Could not convert position attribute of the field element to an integer: [" + positionStr
                        + "], using the default of the form renderer", module);
            }
            this.defaultPosition = position;
        }
    }

    public static class AutoFieldsService {
        public final String serviceName;
        public final String mapName;
        public final String defaultFieldType;
        public final int defaultPosition;

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
                Debug.logError(e, "Could not convert position attribute of the field element to an integer: [" + positionStr
                        + "], using the default of the form renderer", module);
            }
            this.defaultPosition = position;
        }
    }

    public static class Banner implements FieldGroupBase {
        public final FlexibleStringExpander style;
        public final FlexibleStringExpander text;
        public final FlexibleStringExpander textStyle;
        public final FlexibleStringExpander leftText;
        public final FlexibleStringExpander leftTextStyle;
        public final FlexibleStringExpander rightText;
        public final FlexibleStringExpander rightTextStyle;

        public Banner(Element sortOrderElement) {
            this.style = FlexibleStringExpander.getInstance(sortOrderElement.getAttribute("style"));
            this.text = FlexibleStringExpander.getInstance(sortOrderElement.getAttribute("text"));
            this.textStyle = FlexibleStringExpander.getInstance(sortOrderElement.getAttribute("text-style"));
            this.leftText = FlexibleStringExpander.getInstance(sortOrderElement.getAttribute("left-text"));
            this.leftTextStyle = FlexibleStringExpander.getInstance(sortOrderElement.getAttribute("left-text-style"));
            this.rightText = FlexibleStringExpander.getInstance(sortOrderElement.getAttribute("right-text"));
            this.rightTextStyle = FlexibleStringExpander.getInstance(sortOrderElement.getAttribute("right-text-style"));
        }

        public String getLeftText(Map<String, Object> context) {
            return this.leftText.expandString(context);
        }

        public String getLeftTextStyle(Map<String, Object> context) {
            return this.leftTextStyle.expandString(context);
        }

        public String getRightText(Map<String, Object> context) {
            return this.rightText.expandString(context);
        }

        public String getRightTextStyle(Map<String, Object> context) {
            return this.rightTextStyle.expandString(context);
        }

        public String getStyle(Map<String, Object> context) {
            return this.style.expandString(context);
        }

        public String getText(Map<String, Object> context) {
            return this.text.expandString(context);
        }

        public String getTextStyle(Map<String, Object> context) {
            return this.textStyle.expandString(context);
        }

        public void renderString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer)
                throws IOException {
            formStringRenderer.renderBanner(writer, context, this);
        }
    }

    public static class FieldGroup implements FieldGroupBase {
        private static AtomicInteger baseSeqNo = new AtomicInteger(0);
        private static final String baseId = "_G";
        private final String id;
        private final String style;
        private final String title;
        private final boolean collapsible;
        private final boolean initiallyCollapsed;
        private final ModelForm modelForm;

        public FieldGroup(Element sortOrderElement, ModelForm modelForm, List<SortField> sortOrderFields,
                Map<String, FieldGroupBase> fieldGroupMap) {
            this.modelForm = modelForm;
            String id;
            String style = "";
            String title = "";
            boolean collapsible = false;
            boolean initiallyCollapsed = false;
            if (sortOrderElement != null) {
                id = sortOrderElement.getAttribute("id");
                if (id.isEmpty()) {
                    String lastGroupId = baseId + baseSeqNo.getAndIncrement() + "_";
                    id = lastGroupId;
                }
                style = sortOrderElement.getAttribute("style");
                title = sortOrderElement.getAttribute("title");
                collapsible = "true".equals(sortOrderElement.getAttribute("collapsible"));
                initiallyCollapsed = "true".equals(sortOrderElement.getAttribute("initially-collapsed"));
                if (initiallyCollapsed) {
                    collapsible = true;
                }
                for (Element sortFieldElement : UtilXml.childElementList(sortOrderElement, "sort-field")) {
                    sortOrderFields.add(new SortField(sortFieldElement.getAttribute("name"), sortFieldElement
                            .getAttribute("position")));
                    fieldGroupMap.put(sortFieldElement.getAttribute("name"), this);
                }
            } else {
                String lastGroupId = baseId + baseSeqNo.getAndIncrement() + "_";
                id = lastGroupId;
            }
            this.id = id;
            this.style = style;
            this.title = title;
            this.collapsible = collapsible;
            this.initiallyCollapsed = initiallyCollapsed;
        }

        public Boolean collapsible() {
            return this.collapsible;
        }

        public String getId() {
            return this.id;
        }

        public String getStyle() {
            return this.style;
        }

        public String getTitle() {
            return this.title;
        }

        public Boolean initiallyCollapsed() {
            return this.initiallyCollapsed;
        }

        public void renderEndString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer)
                throws IOException {
            formStringRenderer.renderFormatSingleWrapperClose(writer, context, modelForm);
            if (!modelForm.fieldGroupList.isEmpty()) {
                if (shouldUse(context)) {
                    formStringRenderer.renderFieldGroupClose(writer, context, this);
                }
            }
        }

        public void renderStartString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer)
                throws IOException {
            if (!modelForm.fieldGroupList.isEmpty()) {
                if (shouldUse(context)) {
                    formStringRenderer.renderFieldGroupOpen(writer, context, this);
                }
            }
            formStringRenderer.renderFormatSingleWrapperOpen(writer, context, modelForm);
        }

        public boolean shouldUse(Map<String, Object> context) {
            for (String fieldName : modelForm.fieldGroupMap.keySet()) {
                FieldGroupBase group = modelForm.fieldGroupMap.get(fieldName);
                if (group instanceof FieldGroup) {
                    FieldGroup fieldgroup = (FieldGroup) group;
                    if (this.id.equals(fieldgroup.getId())) {
                        for (ModelFormField modelField : modelForm.fieldList) {
                            if (fieldName.equals(modelField.getName()) && modelField.shouldUse(context)) {
                                return true;
                            }
                        }
                    }
                }
            }
            return false;
        }
    }

    public static interface FieldGroupBase {
    }

    public static class SortField {
        private final String fieldName;
        private final Integer position;

        public SortField(String name) {
            this(name, null);
        }

        public SortField(String name, String position) {
            this.fieldName = name;
            if (UtilValidate.isNotEmpty(position)) {
                Integer posParam = null;
                try {
                    posParam = Integer.valueOf(position);
                } catch (Exception e) {/* just ignore the exception*/
                }
                this.position = posParam;
            } else {
                this.position = null;
            }
        }

        public String getFieldName() {
            return this.fieldName;
        }

        public Integer getPosition() {
            return this.position;
        }
    }

    /** The UpdateArea class implements the <code>&lt;on-event-update-area&gt;</code>
     * elements used in form widgets.
     */
    public static class UpdateArea {
        private final String eventType;
        private final String areaId;
        private final String areaTarget;
        private final String defaultServiceName;
        private final String defaultEntityName;
        private final WidgetWorker.AutoEntityParameters autoEntityParameters;
        private final WidgetWorker.AutoEntityParameters autoServiceParameters;
        private final List<WidgetWorker.Parameter> parameterList;

        public UpdateArea(Element updateAreaElement) {
            this(updateAreaElement, null, null);
        }

        /** XML constructor.
         * @param updateAreaElement The <code>&lt;on-xxx-update-area&gt;</code>
         * XML element.
         */
        public UpdateArea(Element updateAreaElement, String defaultServiceName, String defaultEntityName) {
            this.eventType = updateAreaElement.getAttribute("event-type");
            this.areaId = updateAreaElement.getAttribute("area-id");
            this.areaTarget = updateAreaElement.getAttribute("area-target");
            this.defaultServiceName = defaultServiceName;
            this.defaultEntityName = defaultEntityName;
            List<? extends Element> parameterElementList = UtilXml.childElementList(updateAreaElement, "parameter");
            if (parameterElementList.isEmpty()) {
                this.parameterList = Collections.emptyList();
            } else {
                List<WidgetWorker.Parameter> parameterList = new ArrayList<WidgetWorker.Parameter>(parameterElementList.size());
                for (Element parameterElement : parameterElementList) {
                    parameterList.add(new WidgetWorker.Parameter(parameterElement));
                }
                this.parameterList = Collections.unmodifiableList(parameterList);
            }
            Element autoServiceParamsElement = UtilXml.firstChildElement(updateAreaElement, "auto-parameters-service");
            if (autoServiceParamsElement != null) {
                this.autoServiceParameters = new WidgetWorker.AutoEntityParameters(autoServiceParamsElement);
            } else {
                this.autoServiceParameters = null;
            }
            Element autoEntityParamsElement = UtilXml.firstChildElement(updateAreaElement, "auto-parameters-entity");
            if (autoEntityParamsElement != null) {
                this.autoEntityParameters = new WidgetWorker.AutoEntityParameters(autoEntityParamsElement);
            } else {
                this.autoEntityParameters = null;
            }
        }

        /** String constructor.
         * @param areaId The id of the widget element to be updated
         * @param areaTarget The target URL called to update the area
         */
        public UpdateArea(String eventType, String areaId, String areaTarget) {
            this.eventType = eventType;
            this.areaId = areaId;
            this.areaTarget = areaTarget;
            this.defaultServiceName = null;
            this.defaultEntityName = null;
            this.parameterList = Collections.emptyList();
            this.autoServiceParameters = null;
            this.autoEntityParameters = null;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof UpdateArea && obj.hashCode() == this.hashCode();
        }

        public String getAreaId() {
            return areaId;
        }

        public String getAreaTarget(Map<String, ? extends Object> context) {
            return FlexibleStringExpander.expandString(areaTarget, context);
        }

        public String getEventType() {
            return eventType;
        }

        public Map<String, String> getParameterMap(Map<String, Object> context) {
            Map<String, String> fullParameterMap = new HashMap<String, String>();
            if (autoServiceParameters != null) {
                fullParameterMap.putAll(autoServiceParameters.getParametersMap(context, defaultServiceName));
            }
            if (autoEntityParameters != null) {
                fullParameterMap.putAll(autoEntityParameters.getParametersMap(context, defaultEntityName));
            }
            for (WidgetWorker.Parameter parameter : this.parameterList) {
                fullParameterMap.put(parameter.getName(), parameter.getValue(context));
            }

            return fullParameterMap;
        }

        @Override
        public int hashCode() {
            return areaId.hashCode();
        }
    }
}
