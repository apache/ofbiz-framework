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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GroovyUtil;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilCodec;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelField;
import org.apache.ofbiz.entity.model.ModelReader;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ModelParam;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.widget.WidgetWorker;
import org.apache.ofbiz.widget.renderer.FormStringRenderer;
import org.codehaus.groovy.control.CompilationFailedException;
import org.w3c.dom.Element;

/**
 * Abstract base class for the &lt;form&gt; and &lt;grid&gt; elements.
 *
 * @see <code>widget-form.xsd</code>
 */
@SuppressWarnings("serial")
public abstract class ModelForm extends ModelWidget {

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
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 10000;
    public static final String DEFAULT_PAG_INDEX_FIELD = "viewIndex";
    public static final String DEFAULT_PAG_SIZE_FIELD = "viewSize";
    public static final String DEFAULT_PAG_STYLE = "nav-pager";
    public static final String DEFAULT_PAG_FIRST_STYLE = "nav-first";
    public static final String DEFAULT_PAG_PREV_STYLE = "nav-previous";
    public static final String DEFAULT_PAG_NEXT_STYLE = "nav-next";
    public static final String DEFAULT_PAG_LAST_STYLE = "nav-last";
    /** Sort field default styles. */
    public static final String DEFAULT_SORT_FIELD_STYLE = "sort-order";
    public static final String DEFAULT_SORT_FIELD_ASC_STYLE = "sort-order-asc";
    public static final String DEFAULT_SORT_FIELD_DESC_STYLE = "sort-order-desc";
    private final List<ModelAction> actions;
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
    protected final List<FieldGroupBase> fieldGroupList;
    /** This Map is keyed with the field name and has a FieldGroup for the value.
     * Can also include Banner objects.
     */
    protected final Map<String, FieldGroupBase> fieldGroupMap;
    /** This List will contain one copy of each field for each field name in the order
     * they were encountered in the service, entity, or form definition; field definitions
     * with constraints will also be in this list but may appear multiple times for the same
     * field name.
     *
     * When rendering the form the order in this list should be following and it should not be
     * necessary to use the Map. The Map is used when loading the form definition to keep the
     * list clean and implement the override features for field definitions.
     */
    protected final List<ModelFormField> fieldList;
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
    private final ModelForm parentModel;
    private final List<ModelAction> rowActions;
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
    private final FlexibleStringExpander emptyFormDataMessage;
    private final String tooltip;
    private final String type;
    private final boolean useRowSubmit;
    /** Keeps track of conditional fields to help ensure that only one is rendered
     */
    private final Set<String> useWhenFields;

    /** XML Constructor */
    protected ModelForm(Element formElement, String formLocation, ModelReader entityModelReader, DispatchContext dispatchContext, String defaultType) {
        super(formElement);
        this.formLocation = formLocation;
        parentModel = getParentModel(formElement, entityModelReader, dispatchContext);
        int defaultViewSizeInt = DEFAULT_PAGE_SIZE;
        String viewSize = formElement.getAttribute("view-size");
        if (viewSize.isEmpty()) {
            if (parentModel != null) {
                defaultViewSizeInt = parentModel.defaultViewSize;
            } else {
                defaultViewSizeInt = UtilProperties.getPropertyAsInteger("widget", "widget.form.defaultViewSize",
                        defaultViewSizeInt);
            }
        } else {
            try {
                defaultViewSizeInt = Integer.parseInt(viewSize);
            } catch (NumberFormatException e) {
            }
        }
        this.defaultViewSize = defaultViewSizeInt;
        String type = formElement.getAttribute("type");
        if (type.isEmpty()) {
            if (parentModel != null) {
                type = parentModel.type;
            } else {
                type = defaultType;
            }
        }
        this.type = type;
        FlexibleStringExpander target = FlexibleStringExpander.getInstance(formElement.getAttribute("target"));
        if (target.isEmpty() && parentModel != null) {
            target = parentModel.target;
        }
        this.target = target;
        String containerId = formElement.getAttribute("id");
        if (containerId.isEmpty() && parentModel != null) {
            containerId = parentModel.containerId;
        }
        this.containerId = containerId;
        String containerStyle = formElement.getAttribute("style");
        if (containerStyle.isEmpty() && parentModel != null) {
            containerStyle = parentModel.containerStyle;
        }
        this.containerStyle = containerStyle;
        String title = formElement.getAttribute("title");
        if (title.isEmpty() && parentModel != null) {
            title = parentModel.title;
        }
        this.title = title;
        FlexibleStringExpander emptyFormDataMessage = FlexibleStringExpander.getInstance(formElement.getAttribute("empty-form-data-message"));
        if (emptyFormDataMessage.isEmpty() && parentModel != null) {
            emptyFormDataMessage = parentModel.emptyFormDataMessage;
        }
        this.emptyFormDataMessage = emptyFormDataMessage;
        String tooltip = formElement.getAttribute("tooltip");
        if (tooltip.isEmpty() && parentModel != null) {
            tooltip = parentModel.tooltip;
        }
        this.tooltip = tooltip;
        String listName = formElement.getAttribute("list-name");
        if (listName.isEmpty()) {
            if (parentModel != null) {
                listName = parentModel.listName;
            } else {
                listName = DEFAULT_FORM_RESULT_LIST_NAME;
            }
        }
        this.listName = listName;
        String listEntryName = formElement.getAttribute("list-entry-name");
        if (listEntryName.isEmpty() && parentModel != null) {
            listEntryName = parentModel.listEntryName;
        }
        this.listEntryName = listEntryName;
        String defaultEntityName = formElement.getAttribute("default-entity-name");
        if (defaultEntityName.isEmpty() && parentModel != null) {
            defaultEntityName = parentModel.defaultEntityName;
        }
        this.defaultEntityName = defaultEntityName;
        String defaultServiceName = formElement.getAttribute("default-service-name");
        if (defaultServiceName.isEmpty() && parentModel != null) {
            defaultServiceName = parentModel.defaultServiceName;
        }
        this.defaultServiceName = defaultServiceName;
        String formTitleAreaStyle = formElement.getAttribute("form-title-area-style");
        if (formTitleAreaStyle.isEmpty() && parentModel != null) {
            formTitleAreaStyle = parentModel.formTitleAreaStyle;
        }
        this.formTitleAreaStyle = formTitleAreaStyle;
        String formWidgetAreaStyle = formElement.getAttribute("form-widget-area-style");
        if (formWidgetAreaStyle.isEmpty() && parentModel != null) {
            formWidgetAreaStyle = parentModel.formWidgetAreaStyle;
        }
        this.formWidgetAreaStyle = formWidgetAreaStyle;
        String defaultTitleAreaStyle = formElement.getAttribute("default-title-area-style");
        if (defaultTitleAreaStyle.isEmpty() && parentModel != null) {
            defaultTitleAreaStyle = parentModel.defaultTitleAreaStyle;
        }
        this.defaultTitleAreaStyle = defaultTitleAreaStyle;
        String defaultWidgetAreaStyle = formElement.getAttribute("default-widget-area-style");
        if (defaultWidgetAreaStyle.isEmpty() && parentModel != null) {
            defaultWidgetAreaStyle = parentModel.defaultWidgetAreaStyle;
        }
        this.defaultWidgetAreaStyle = defaultWidgetAreaStyle;
        String oddRowStyle = formElement.getAttribute("odd-row-style");
        if (oddRowStyle.isEmpty() && parentModel != null) {
            oddRowStyle = parentModel.oddRowStyle;
        }
        this.oddRowStyle = oddRowStyle;
        String evenRowStyle = formElement.getAttribute("even-row-style");
        if (evenRowStyle.isEmpty() && parentModel != null) {
            evenRowStyle = parentModel.evenRowStyle;
        }
        this.evenRowStyle = evenRowStyle;
        String defaultTableStyle = formElement.getAttribute("default-table-style");
        if (defaultTableStyle.isEmpty() && parentModel != null) {
            defaultTableStyle = parentModel.defaultTableStyle;
        }
        this.defaultTableStyle = defaultTableStyle;
        String headerRowStyle = formElement.getAttribute("header-row-style");
        if (headerRowStyle.isEmpty() && parentModel != null) {
            headerRowStyle = parentModel.headerRowStyle;
        }
        this.headerRowStyle = headerRowStyle;
        String defaultTitleStyle = formElement.getAttribute("default-title-style");
        if (defaultTitleStyle.isEmpty() && parentModel != null) {
            defaultTitleStyle = parentModel.defaultTitleStyle;
        }
        this.defaultTitleStyle = defaultTitleStyle;
        String defaultWidgetStyle = formElement.getAttribute("default-widget-style");
        if (defaultWidgetStyle.isEmpty() && parentModel != null) {
            defaultWidgetStyle = parentModel.defaultWidgetStyle;
        }
        this.defaultWidgetStyle = defaultWidgetStyle;
        String defaultTooltipStyle = formElement.getAttribute("default-tooltip-style");
        if (defaultTooltipStyle.isEmpty() && parentModel != null) {
            defaultTooltipStyle = parentModel.defaultTooltipStyle;
        }
        this.defaultTooltipStyle = defaultTooltipStyle;
        String itemIndexSeparator = formElement.getAttribute("item-index-separator");
        if (itemIndexSeparator.isEmpty() && parentModel != null) {
            itemIndexSeparator = parentModel.itemIndexSeparator;
        }
        this.itemIndexSeparator = itemIndexSeparator;
        String separateColumns = formElement.getAttribute("separate-columns");
        if (separateColumns.isEmpty() && parentModel != null) {
            this.separateColumns = parentModel.separateColumns;
        } else {
            this.separateColumns = "true".equals(separateColumns);
        }
        String groupColumns = formElement.getAttribute("group-columns");
        if (groupColumns.isEmpty() && parentModel != null) {
            this.groupColumns = parentModel.groupColumns;
        } else {
            this.groupColumns = !"false".equals(groupColumns);
        }
        String targetType = formElement.getAttribute("target-type");
        if (targetType.isEmpty() && parentModel != null) {
            targetType = parentModel.targetType;
        }
        this.targetType = targetType;
        FlexibleMapAccessor<Map<String, ? extends Object>> defaultMapName = FlexibleMapAccessor.getInstance(formElement
                .getAttribute("default-map-name"));
        if (defaultMapName.isEmpty() && parentModel != null) {
            defaultMapName = parentModel.defaultMapName;
        }
        this.defaultMapName = defaultMapName;
        FlexibleStringExpander targetWindowExdr = FlexibleStringExpander.getInstance(formElement.getAttribute("target-window"));
        if (targetWindowExdr.isEmpty() && parentModel != null) {
            targetWindowExdr = parentModel.targetWindowExdr;
        }
        this.targetWindowExdr = targetWindowExdr;
        String hideHeader = formElement.getAttribute("hide-header");
        if (hideHeader.isEmpty() && parentModel != null) {
            this.hideHeader = parentModel.hideHeader;
        } else {
            this.hideHeader = "true".equals(hideHeader);
        }
        String clientAutocompleteFields = formElement.getAttribute("client-autocomplete-fields");
        if (clientAutocompleteFields.isEmpty() && parentModel != null) {
            this.clientAutocompleteFields = parentModel.clientAutocompleteFields;
        } else {
            this.clientAutocompleteFields = !"false".equals(formElement.getAttribute("client-autocomplete-fields"));
        }
        FlexibleStringExpander paginateTarget = FlexibleStringExpander.getInstance(formElement.getAttribute("paginate-target"));
        if (paginateTarget.isEmpty() && parentModel != null) {
            paginateTarget = parentModel.paginateTarget;
        }
        this.paginateTarget = paginateTarget;
        ArrayList<AltTarget> altTargets = new ArrayList<>();
        for (Element altTargetElement : UtilXml.childElementList(formElement, "alt-target")) {
            altTargets.add(new AltTarget(altTargetElement));
        }
        if (parentModel != null) {
            altTargets.addAll(parentModel.altTargets);
        }
        altTargets.trimToSize();
        this.altTargets = Collections.unmodifiableList(altTargets);
        ArrayList<ModelAction> actions = new ArrayList<>();
        if (parentModel != null) {
            actions.addAll(parentModel.actions);
        }
        Element actionsElement = UtilXml.firstChildElement(formElement, "actions");
        if (actionsElement != null) {
            actions.addAll(ModelFormAction.readSubActions(this, actionsElement));
        }
        actions.trimToSize();
        this.actions = Collections.unmodifiableList(actions);
        ArrayList<ModelAction> rowActions = new ArrayList<>();
        if (parentModel != null) {
            rowActions.addAll(parentModel.rowActions);
        }
        Element rowActionsElement = UtilXml.firstChildElement(formElement, "row-actions");
        if (rowActionsElement != null) {
            rowActions.addAll(ModelFormAction.readSubActions(this, rowActionsElement));
        }
        rowActions.trimToSize();
        this.rowActions = Collections.unmodifiableList(rowActions);
        ArrayList<UpdateArea> onPaginateUpdateAreas = new ArrayList<>();
        ArrayList<UpdateArea> onSubmitUpdateAreas = new ArrayList<>();
        ArrayList<UpdateArea> onSortColumnUpdateAreas = new ArrayList<>();
        if (parentModel != null) {
            onPaginateUpdateAreas.addAll(parentModel.onPaginateUpdateAreas);
            onSubmitUpdateAreas.addAll(parentModel.onSubmitUpdateAreas);
            onSortColumnUpdateAreas.addAll(parentModel.onSortColumnUpdateAreas);
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
        ArrayList<AltRowStyle> altRowStyles = new ArrayList<>();
        if (parentModel != null) {
            altRowStyles.addAll(parentModel.altRowStyles);
        }
        for (Element altRowStyleElement : UtilXml.childElementList(formElement, "alt-row-style")) {
            AltRowStyle altRowStyle = new AltRowStyle(altRowStyleElement);
            altRowStyles.add(altRowStyle);
        }
        altRowStyles.trimToSize();
        this.altRowStyles = Collections.unmodifiableList(altRowStyles);
        Set<String> useWhenFields = new HashSet<>();
        if (parentModel != null) {
            useWhenFields.addAll(parentModel.useWhenFields);
        }
        List<ModelFormFieldBuilder> fieldBuilderList = new ArrayList<>();
        Map<String, ModelFormFieldBuilder> fieldBuilderMap = new HashMap<>();
        if (parentModel != null) {
            // Create this fieldList/Map from clones of parentModel's
            for (ModelFormField parentChildField : parentModel.fieldList) {
                ModelFormFieldBuilder builder = new ModelFormFieldBuilder(parentChildField);
                builder.setModelForm(this);
                fieldBuilderList.add(builder);
                fieldBuilderMap.put(builder.getName(), builder);
            }
        }
        Map<String, FieldGroupBase> fieldGroupMap = new HashMap<>();
        if (parentModel != null) {
            fieldGroupMap.putAll(parentModel.fieldGroupMap);
        }
        ArrayList<FieldGroupBase> fieldGroupList = new ArrayList<>();
        if (parentModel != null) {
            fieldGroupList.addAll(parentModel.fieldGroupList);
        }
        ArrayList<String> lastOrderFields = new ArrayList<>();
        if (parentModel != null) {
            lastOrderFields.addAll(parentModel.lastOrderFields);
        }
        String sortFieldParameterName = formElement.getAttribute("sort-field-parameter-name");
        if (!sortFieldParameterName.isEmpty()) {
            this.sortFieldParameterName = sortFieldParameterName;
       } else {
            this.sortFieldParameterName = (parentModel != null) ? parentModel.getSortFieldParameterName() : "sortField";
       }
        String defaultRequiredFieldStyle = formElement.getAttribute("default-required-field-style");
        if (defaultRequiredFieldStyle.isEmpty() && parentModel != null) {
            defaultRequiredFieldStyle = parentModel.defaultRequiredFieldStyle;
        }
        this.defaultRequiredFieldStyle = defaultRequiredFieldStyle;
        String defaultSortFieldStyle = formElement.getAttribute("default-sort-field-style");
        if (defaultSortFieldStyle.isEmpty() && parentModel != null) {
            this.defaultSortFieldStyle = parentModel.defaultSortFieldStyle;
        } else {
            this.defaultSortFieldStyle = DEFAULT_SORT_FIELD_STYLE;
        }
        String defaultSortFieldAscStyle = formElement.getAttribute("default-sort-field-asc-style");
        if (defaultSortFieldAscStyle.isEmpty() && parentModel != null) {
            this.defaultSortFieldAscStyle = parentModel.defaultSortFieldAscStyle;
        } else {
            this.defaultSortFieldAscStyle = DEFAULT_SORT_FIELD_ASC_STYLE;
        }
        String defaultSortFieldDescStyle = formElement.getAttribute("default-sort-field-desc-style");
        if (defaultSortFieldDescStyle.isEmpty() && parentModel != null) {
            this.defaultSortFieldDescStyle = parentModel.defaultSortFieldDescStyle;
        } else {
            this.defaultSortFieldDescStyle = DEFAULT_SORT_FIELD_DESC_STYLE;
        }
        String paginateTargetAnchor = formElement.getAttribute("paginate-target-anchor");
        if (paginateTargetAnchor.isEmpty() && parentModel != null) {
            paginateTargetAnchor = parentModel.paginateTargetAnchor;
        }
        this.paginateTargetAnchor = paginateTargetAnchor;
        FlexibleStringExpander paginateIndexField = FlexibleStringExpander.getInstance(formElement
                .getAttribute("paginate-index-field"));
        if (paginateIndexField.isEmpty() && parentModel != null) {
            paginateIndexField = parentModel.paginateIndexField;
        }
        this.paginateIndexField = paginateIndexField;
        FlexibleStringExpander paginateSizeField = FlexibleStringExpander.getInstance(formElement
                .getAttribute("paginate-size-field"));
        if (paginateSizeField.isEmpty() && parentModel != null) {
            paginateSizeField = parentModel.paginateSizeField;
        }
        this.paginateSizeField = paginateSizeField;
        FlexibleStringExpander overrideListSize = FlexibleStringExpander.getInstance(formElement
                .getAttribute("override-list-size"));
        if (overrideListSize.isEmpty() && parentModel != null) {
            overrideListSize = parentModel.overrideListSize;
        }
        this.overrideListSize = overrideListSize;
        FlexibleStringExpander paginateFirstLabel = FlexibleStringExpander.getInstance(formElement
                .getAttribute("paginate-first-label"));
        if (paginateFirstLabel.isEmpty() && parentModel != null) {
            paginateFirstLabel = parentModel.paginateFirstLabel;
        }
        this.paginateFirstLabel = paginateFirstLabel;
        FlexibleStringExpander paginatePreviousLabel = FlexibleStringExpander.getInstance(formElement
                .getAttribute("paginate-previous-label"));
        if (paginatePreviousLabel.isEmpty() && parentModel != null) {
            paginatePreviousLabel = parentModel.paginatePreviousLabel;
        }
        this.paginatePreviousLabel = paginatePreviousLabel;
        FlexibleStringExpander paginateNextLabel = FlexibleStringExpander.getInstance(formElement
                .getAttribute("paginate-next-label"));
        if (paginateNextLabel.isEmpty() && parentModel != null) {
            paginateNextLabel = parentModel.paginateNextLabel;
        }
        this.paginateNextLabel = paginateNextLabel;
        FlexibleStringExpander paginateLastLabel = FlexibleStringExpander.getInstance(formElement
                .getAttribute("paginate-last-label"));
        if (paginateLastLabel.isEmpty() && parentModel != null) {
            paginateLastLabel = parentModel.paginateLastLabel;
        }
        this.paginateLastLabel = paginateLastLabel;
        FlexibleStringExpander paginateViewSizeLabel = FlexibleStringExpander.getInstance(formElement
                .getAttribute("paginate-viewsize-label"));
        if (paginateViewSizeLabel.isEmpty() && parentModel != null) {
            paginateViewSizeLabel = parentModel.paginateViewSizeLabel;
        }
        this.paginateViewSizeLabel = paginateViewSizeLabel;
        String paginateStyle = formElement.getAttribute("paginate-style");
        if (paginateStyle.isEmpty()) {
            if(parentModel != null) {
                this.paginateStyle = parentModel.paginateStyle;
            } else {
                this.paginateStyle = DEFAULT_PAG_STYLE;
            }
        } else {
            this.paginateStyle = paginateStyle;
        }
        FlexibleStringExpander paginate = FlexibleStringExpander.getInstance(formElement.getAttribute("paginate"));
        if (paginate.isEmpty() && parentModel != null) {
            paginate = parentModel.paginate;
        }
        this.paginate = paginate;
        String skipStart = formElement.getAttribute("skip-start");
        if (skipStart.isEmpty() && parentModel != null) {
            this.skipStart = parentModel.skipStart;
        } else {
            this.skipStart = "true".equals(skipStart);
        }
        String skipEnd = formElement.getAttribute("skip-end");
        if (skipEnd.isEmpty() && parentModel != null) {
            this.skipEnd = parentModel.skipEnd;
        } else {
            this.skipEnd = "true".equals(skipEnd);
        }
        String useRowSubmit = formElement.getAttribute("use-row-submit");
        if (useRowSubmit.isEmpty() && parentModel != null) {
            this.useRowSubmit = parentModel.useRowSubmit;
        } else {
            this.useRowSubmit = "true".equals(useRowSubmit);
        }
        this.rowCountExdr = paginate;
        List<ModelFormFieldBuilder> multiSubmitBuilders = new ArrayList<>();
        ArrayList<AutoFieldsService> autoFieldsServices = new ArrayList<>();
        ArrayList<AutoFieldsEntity> autoFieldsEntities = new ArrayList<>();
        ArrayList<SortField> sortOrderFields = new ArrayList<>();
        this.defaultFieldGroup = new FieldGroup(null, this, sortOrderFields, fieldGroupMap);
        for (Element autoFieldsServiceElement : UtilXml.childElementList(formElement, "auto-fields-service")) {
            AutoFieldsService autoFieldsService = new AutoFieldsService(autoFieldsServiceElement);
            autoFieldsServices.add(autoFieldsService);
            addAutoFieldsFromService(autoFieldsService, entityModelReader, dispatchContext, useWhenFields, fieldBuilderList, fieldBuilderMap);
        }
        for (Element autoFieldsEntityElement : UtilXml.childElementList(formElement, "auto-fields-entity")) {
            AutoFieldsEntity autoFieldsEntity = new AutoFieldsEntity(autoFieldsEntityElement);
            autoFieldsEntities.add(autoFieldsEntity);
            addAutoFieldsFromEntity(autoFieldsEntity, entityModelReader, useWhenFields, fieldBuilderList, fieldBuilderMap);
        }
        String thisType = this.getType();
        for (Element fieldElement : UtilXml.childElementList(formElement, "field")) {
            ModelFormFieldBuilder builder = new ModelFormFieldBuilder(fieldElement, this, entityModelReader, dispatchContext);
            FieldInfo fieldInfo = builder.getFieldInfo();
            if ("multi".equals(thisType) && fieldInfo instanceof ModelFormField.SubmitField) {
                multiSubmitBuilders.add(builder);
            } else {
                addUpdateField(builder, useWhenFields, fieldBuilderList, fieldBuilderMap);
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
                if ("sort-field".equals(tagName)) {
                    String fieldName = sortFieldElement.getAttribute("name");
                    String position = sortFieldElement.getAttribute("position");
                    sortOrderFields.add(new SortField(fieldName, position));
                    fieldGroupMap.put(fieldName, lastFieldGroup);
                } else if ("last-field".equals(tagName)) {
                    String fieldName = sortFieldElement.getAttribute("name");
                    fieldGroupMap.put(fieldName, lastFieldGroup);
                    lastOrderFields.add(fieldName);
                } else if ("banner".equals(tagName)) {
                    Banner thisBanner = new Banner(sortFieldElement);
                    fieldGroupList.add(thisBanner);
                    lastFieldGroup = new FieldGroup(null, this, sortOrderFields, fieldGroupMap);
                    fieldGroupList.add(lastFieldGroup);
                } else if ("field-group".equals(tagName)) {
                    FieldGroup thisFieldGroup = new FieldGroup(sortFieldElement, this, sortOrderFields, fieldGroupMap);
                    fieldGroupList.add(thisFieldGroup);
                    lastFieldGroup = new FieldGroup(null, this, sortOrderFields, fieldGroupMap);
                    fieldGroupList.add(lastFieldGroup);
                }
            }
        }
        if (sortOrderFields.size() > 0) {
            List<ModelFormFieldBuilder> sortedFields = new ArrayList<>();
            for (SortField sortField : sortOrderFields) {
                String fieldName = sortField.getFieldName();
                if (UtilValidate.isEmpty(fieldName)) {
                    continue;
                }
                // get all fields with the given name from the existing list and put them in the sorted list
                Iterator<ModelFormFieldBuilder> fieldIter = fieldBuilderList.iterator();
                while (fieldIter.hasNext()) {
                    ModelFormFieldBuilder builder = fieldIter.next();
                    if (fieldName.equals(builder.getName())) {
                        // matched the name; remove from the original last and add to the sorted list
                        if (UtilValidate.isNotEmpty(sortField.getPosition())) {
                            builder.setPosition(sortField.getPosition());
                        }
                        fieldIter.remove();
                        sortedFields.add(builder);
                    }
                }
            }
            // now add all of the rest of the fields from fieldList, ie those that were not explicitly listed in the sort order
            sortedFields.addAll(fieldBuilderList);
            // sortedFields all done, set fieldList
            fieldBuilderList = sortedFields;
        }
        if (UtilValidate.isNotEmpty(lastOrderFields)) {
            List<ModelFormFieldBuilder> lastedFields = new LinkedList<>();
            for (String fieldName : lastOrderFields) {
                if (UtilValidate.isEmpty(fieldName)) {
                    continue;
                }
                // get all fields with the given name from the existing list and put them in the lasted list
                Iterator<ModelFormFieldBuilder> fieldIter = fieldBuilderList.iterator();
                while (fieldIter.hasNext()) {
                    ModelFormFieldBuilder builder = fieldIter.next();
                    if (fieldName.equals(builder.getName())) {
                        // matched the name; remove from the original last and add to the lasted list
                        fieldIter.remove();
                        lastedFields.add(builder);
                    }
                }
            }
            //now put all lastedFields at the field list end
            fieldBuilderList.addAll(lastedFields);
        }
        List<ModelFormField> fieldList = new ArrayList<>(fieldBuilderList.size());
        for (ModelFormFieldBuilder builder : fieldBuilderList) {
            fieldList.add(builder.build());
        }
        this.fieldList = Collections.unmodifiableList(fieldList);
        List<ModelFormField> multiSubmitFields = new ArrayList<>(multiSubmitBuilders.size());
        for (ModelFormFieldBuilder builder : multiSubmitBuilders) {
            multiSubmitFields.add(builder.build());
        }
        this.multiSubmitFields = Collections.unmodifiableList(multiSubmitFields);
        this.useWhenFields = Collections.unmodifiableSet(useWhenFields);
        this.fieldGroupMap = Collections.unmodifiableMap(fieldGroupMap);
        fieldGroupList.trimToSize();
        this.fieldGroupList = Collections.unmodifiableList(fieldGroupList);
        lastOrderFields.trimToSize();
        this.lastOrderFields = Collections.unmodifiableList(lastOrderFields);
        autoFieldsServices.trimToSize();
        this.autoFieldsServices = Collections.unmodifiableList(autoFieldsServices);
        autoFieldsEntities.trimToSize();
        this.autoFieldsEntities = Collections.unmodifiableList(autoFieldsEntities);
        sortOrderFields.trimToSize();
        this.sortOrderFields = Collections.unmodifiableList(sortOrderFields);
        String focusFieldName = formElement.getAttribute("focus-field-name");
        if (focusFieldName.isEmpty() && parentModel != null) {
            focusFieldName = parentModel.focusFieldName;
        }
        this.focusFieldName = focusFieldName;
    }

    private void addAutoFieldsFromEntity(AutoFieldsEntity autoFieldsEntity, ModelReader entityModelReader,
            Set<String> useWhenFields, List<ModelFormFieldBuilder> fieldBuilderList, Map<String, ModelFormFieldBuilder> fieldBuilderMap) {
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
            // auto-add only if field was generated automatically by the entity engine or including internally
            if (modelField.getIsAutoCreatedInternal() && !autoFieldsEntity.includeInternal) {
                // don't ever auto-add these, should only be added if explicitly referenced
                continue;
            }
            ModelFormFieldBuilder builder = new ModelFormFieldBuilder();
            builder.setModelForm(this);
            builder.setName(modelField.getName());
            builder.setEntityName(modelEntity.getEntityName());
            builder.setFieldName(modelField.getName());
            builder.induceFieldInfoFromEntityField(modelEntity, modelField, autoFieldsEntity.defaultFieldType);
            builder.setPosition(autoFieldsEntity.defaultPosition);
            if (UtilValidate.isNotEmpty(autoFieldsEntity.mapName)) {
                builder.setMapName(autoFieldsEntity.mapName);
            }
            addUpdateField(builder, useWhenFields, fieldBuilderList, fieldBuilderMap);
        }
    }

    private void addAutoFieldsFromService(AutoFieldsService autoFieldsService, ModelReader entityModelReader,
            DispatchContext dispatchContext, Set<String> useWhenFields, List<ModelFormFieldBuilder> fieldBuilderList,
            Map<String, ModelFormFieldBuilder> fieldBuilderMap) {
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
            if (modelParam.internal) {
                // skip auto params that the service engine populates...
                continue;
            }
            if (modelParam.formDisplay) {
                if (UtilValidate.isNotEmpty(modelParam.entityName) && UtilValidate.isNotEmpty(modelParam.fieldName)) {
                    ModelEntity modelEntity;
                    try {
                        modelEntity = entityModelReader.getModelEntity(modelParam.entityName);
                        ModelField modelField = modelEntity.getField(modelParam.fieldName);
                        if (modelField != null) {
                            // okay, populate using the entity field info...
                            ModelFormFieldBuilder builder = new ModelFormFieldBuilder();
                            builder.setModelForm(this);
                            builder.setName(modelField.getName());
                            builder.setEntityName(modelEntity.getEntityName());
                            builder.setFieldName(modelField.getName());
                            builder.induceFieldInfoFromEntityField(modelEntity, modelField, autoFieldsService.defaultFieldType);
                            if (UtilValidate.isNotEmpty(autoFieldsService.mapName)) {
                                builder.setMapName(autoFieldsService.mapName);
                            }
                            builder.setRequiredField(!modelParam.optional);
                            addUpdateField(builder, useWhenFields, fieldBuilderList, fieldBuilderMap);
                            // continue to skip creating based on service param
                            continue;
                        }
                    } catch (GenericEntityException e) {
                        Debug.logError(e, module);
                    }
                }
                ModelFormFieldBuilder builder = new ModelFormFieldBuilder();
                builder.setModelForm(this);
                builder.setName(modelParam.name);
                builder.setServiceName(modelService.name);
                builder.setAttributeName(modelParam.name);
                builder.setTitle(modelParam.formLabel);
                builder.setRequiredField(!modelParam.optional);
                builder.induceFieldInfoFromServiceParam(modelService, modelParam, autoFieldsService.defaultFieldType);
                builder.setPosition(autoFieldsService.defaultPosition);
                if (UtilValidate.isNotEmpty(autoFieldsService.mapName)) {
                    builder.setMapName(autoFieldsService.mapName);
                }
                addUpdateField(builder, useWhenFields, fieldBuilderList, fieldBuilderMap);
            }
        }
    }

    private static void addUpdateField(ModelFormFieldBuilder builder, Set<String> useWhenFields,
            List<ModelFormFieldBuilder> fieldBuilderList, Map<String, ModelFormFieldBuilder> fieldBuilderMap) {
        if (!builder.getUseWhen().isEmpty() || useWhenFields.contains(builder.getName())) {
            useWhenFields.add(builder.getName());
            // is a conditional field, add to the List but don't worry about the Map
            //for adding to list, see if there is another field with that name in the list and if so, put it before that one
            boolean inserted = false;
            for (int i = 0; i < fieldBuilderList.size(); i++) {
                ModelFormFieldBuilder curField = fieldBuilderList.get(i);
                if (curField.getName() != null && curField.getName().equals(builder.getName())) {
                    fieldBuilderList.add(i, builder);
                    inserted = true;
                    break;
                }
            }
            if (!inserted) {
                fieldBuilderList.add(builder);
            }
            return;
        }
        // not a conditional field, see if a named field exists in Map
        ModelFormFieldBuilder existingField = fieldBuilderMap.get(builder.getName());
        if (existingField != null) {
            // does exist, update the field by doing a merge/override
            existingField.mergeOverrideModelFormField(builder);
        } else {
            // does not exist, add to List and Map
            fieldBuilderList.add(builder);
            fieldBuilderMap.put(builder.getName(), builder);
        }
    }

    public List<ModelAction> getActions() {
        return actions;
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

    public boolean getClientAutocompleteFields() {
        return this.clientAutocompleteFields;
    }

    public String getContainerId() {
        // use the name if there is no id
        if (UtilValidate.isNotEmpty(this.containerId)) {
            return this.containerId;
        }
        return this.getName();
    }

    public String getContainerStyle() {
        return this.containerStyle;
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

    public boolean getHideHeader() {
        return this.hideHeader;
    }

    public String getItemIndexSeparator() {
        if (UtilValidate.isNotEmpty(this.itemIndexSeparator)) {
            return this.itemIndexSeparator;
        }
        return "_o_";
    }

    public List<String> getLastOrderFields() {
        return lastOrderFields;
    }

    public String getListEntryName() {
        return this.listEntryName;
    }

    public String getListName() {
        return this.listName;
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

    public int getOverrideListSize(Map<String, Object> context) {
        int listSize = 0;
        if (!this.overrideListSize.isEmpty()) {
            String size = this.overrideListSize.expandString(context);
            try {
                size = size.replaceAll("[^0-9.]", "");
                if (!size.isEmpty()) {
                    listSize = Integer.parseInt(size);
                }
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
            return Boolean.valueOf(paginate);
        }
        return true;
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

    protected abstract ModelForm getParentModel(Element formElement, ModelReader entityModelReader, DispatchContext dispatchContext);

    public String getParentFormLocation() {
        return this.parentModel == null ? null : this.parentModel.getFormLocation();
    }

    public String getParentFormName() {
        return this.parentModel == null ? null : this.parentModel.getName();
    }

    public ModelForm getParentModelForm() {
        return parentModel;
    }

    public String getPassedRowCount(Map<String, Object> context) {
        return rowCountExdr.expandString(context);
    }

    public List<ModelAction> getRowActions() {
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
            for (AltRowStyle altRowStyle : this.altRowStyles) {
                Object retVal = GroovyUtil.eval(StringUtil.convertOperatorSubstitutions(altRowStyle.useWhen),context);
                // retVal should be a Boolean, if not something weird is up...
                if (retVal instanceof Boolean) {
                    Boolean boolVal = (Boolean) retVal;
                    if (boolVal) {
                        styles += altRowStyle.style;
                    }
                } else {
                    throw new IllegalArgumentException("Return value from style condition eval was not a Boolean: "
                            + retVal.getClass().getName() + " [" + retVal + "] of form " + getName());
                }
            }
        } catch (CompilationFailedException e) {
            String errmsg = "Error evaluating groovy style conditions on form " + getName();
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
        UtilCodec.SimpleEncoder simpleEncoder = (UtilCodec.SimpleEncoder) context.get("simpleEncoder");
        if (simpleEncoder != null) {
            expanderContext = UtilCodec.HtmlEncodingMapWrapper.getHtmlEncodingMapWrapper(context, simpleEncoder);
        }
        try {
            for (AltTarget altTarget : this.altTargets) {
                String useWhen = FlexibleStringExpander.expandString(altTarget.useWhen, context);
                Object retVal = GroovyUtil.eval(StringUtil.convertOperatorSubstitutions(useWhen),context);
                boolean condTrue = false;
                // retVal should be a Boolean, if not something weird is up...
                if (retVal instanceof Boolean) {
                    Boolean boolVal = (Boolean) retVal;
                    condTrue = boolVal;
                } else {
                    throw new IllegalArgumentException("Return value from target condition eval was not a Boolean: "
                            + retVal.getClass().getName() + " [" + retVal + "] of form " + getName());
                }

                if (condTrue && !"inter-app".equals(targetType)) {
                    return altTarget.targetExdr.expandString(expanderContext);
                }
            }
        } catch (CompilationFailedException e) {
            String errmsg = "Error evaluating Groovy target conditions on form " + getName();
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

    public String getEmptyFormDataMessage(Map<String, Object> context) {
        return this.emptyFormDataMessage.expandString(context);
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
    public boolean getGroupColumns() {
        return groupColumns;
    }

    public boolean isOverridenListSize() {
        return !this.overrideListSize.isEmpty();
    }

    public void runFormActions(Map<String, Object> context) {
        AbstractModelAction.runSubActions(this.actions, context);
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
        public final boolean includeInternal;

        public AutoFieldsEntity(Element element) {
            this.entityName = element.getAttribute("entity-name");
            this.mapName = element.getAttribute("map-name");
            this.defaultFieldType = element.getAttribute("default-field-type");
            this.includeInternal = !"false".equals(element.getAttribute("include-internal"));
            String positionStr = element.getAttribute("default-position");
            int position = 1;
            try {
                if (UtilValidate.isNotEmpty(positionStr)) {
                    position = Integer.parseInt(positionStr);
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
                    position = Integer.parseInt(positionStr);
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
                } catch (Exception e) {
                    Debug.logInfo("The class SortField caused an exception", module);
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
        private final CommonWidgetModels.AutoEntityParameters autoEntityParameters;
        private final CommonWidgetModels.AutoServiceParameters autoServiceParameters;
        private final List<CommonWidgetModels.Parameter> parameterList;

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
                List<CommonWidgetModels.Parameter> parameterList = new ArrayList<>(parameterElementList.size());
                for (Element parameterElement : parameterElementList) {
                    parameterList.add(new CommonWidgetModels.Parameter(parameterElement));
                }
                this.parameterList = Collections.unmodifiableList(parameterList);
            }
            Element autoServiceParamsElement = UtilXml.firstChildElement(updateAreaElement, "auto-parameters-service");
            if (autoServiceParamsElement != null) {
                this.autoServiceParameters = new CommonWidgetModels.AutoServiceParameters(autoServiceParamsElement);
            } else {
                this.autoServiceParameters = null;
            }
            Element autoEntityParamsElement = UtilXml.firstChildElement(updateAreaElement, "auto-parameters-entity");
            if (autoEntityParamsElement != null) {
                this.autoEntityParameters = new CommonWidgetModels.AutoEntityParameters(autoEntityParamsElement);
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
            Map<String, String> fullParameterMap = new HashMap<>();
            if (autoServiceParameters != null) {
                fullParameterMap.putAll(autoServiceParameters.getParametersMap(context, defaultServiceName));
            }
            if (autoEntityParameters != null) {
                fullParameterMap.putAll(autoEntityParameters.getParametersMap(context, defaultEntityName));
            }
            for (CommonWidgetModels.Parameter parameter : this.parameterList) {
                fullParameterMap.put(parameter.getName(), parameter.getValue(context));
            }

            return fullParameterMap;
        }

        @Override
        public int hashCode() {
            return areaId.hashCode();
        }

        public String getAreaTarget() {
            return areaTarget;
        }

        public String getDefaultServiceName() {
            return defaultServiceName;
        }

        public String getDefaultEntityName() {
            return defaultEntityName;
        }

        public CommonWidgetModels.AutoEntityParameters getAutoEntityParameters() {
            return autoEntityParameters;
        }

        public CommonWidgetModels.AutoServiceParameters getAutoServiceParameters() {
            return autoServiceParameters;
        }

        public List<CommonWidgetModels.Parameter> getParameterList() {
            return parameterList;
        }
    }
}
