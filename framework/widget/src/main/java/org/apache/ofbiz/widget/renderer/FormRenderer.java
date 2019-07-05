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
package org.apache.ofbiz.widget.renderer;

import static org.apache.ofbiz.widget.model.ModelFormField.usedFields;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.collections.MapStack;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.entity.GenericEntity;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.widget.WidgetWorker;
import org.apache.ofbiz.widget.model.AbstractModelAction;
import org.apache.ofbiz.widget.model.FieldInfo;
import org.apache.ofbiz.widget.model.ModelForm;
import org.apache.ofbiz.widget.model.ModelForm.FieldGroup;
import org.apache.ofbiz.widget.model.ModelForm.FieldGroupBase;
import org.apache.ofbiz.widget.model.ModelFormField;
import org.apache.ofbiz.widget.model.ModelGrid;

/**
 * A form rendering engine.
 *
 */
public class FormRenderer {

    /*
     * ----------------------------------------------------------------------- *
     *                     DEVELOPERS PLEASE READ
     * ----------------------------------------------------------------------- *
     *
     * An instance of this class is created by each thread for each form that
     * is rendered. If you need to keep track of things while rendering, then
     * this is the place to do it. In other words, feel free to modify this
     * object's state (except for the final fields of course).
     *
     */

    public static final String module = FormRenderer.class.getName();

    public static String getCurrentContainerId(ModelForm modelForm, Map<String, Object> context) {
        Locale locale = UtilMisc.ensureLocale(context.get("locale"));
        String retVal = FlexibleStringExpander.expandString(modelForm.getContainerId(), context, locale);
        Integer itemIndex = (Integer) context.get("itemIndex");
        if (itemIndex != null/* && "list".equals(modelForm.getType()) */) {
            if (UtilValidate.isNotEmpty(context.get("parentItemIndex"))) {
                return retVal + context.get("parentItemIndex") + modelForm.getItemIndexSeparator() + itemIndex;
            }
            return retVal + modelForm.getItemIndexSeparator() + itemIndex;
        }
        return retVal;
    }

    public static String getCurrentFormName(ModelForm modelForm, Map<String, Object> context) {
        Integer itemIndex = (Integer) context.get("itemIndex");
        String formName = (String) context.get("formName");
        if (UtilValidate.isEmpty(formName)) {
            formName = modelForm.getName();
        }
        if (itemIndex != null && "list".equals(modelForm.getType())) {
            return formName + modelForm.getItemIndexSeparator() + itemIndex;
        }
        return formName;
    }

    public static String getFocusFieldName(ModelForm modelForm, Map<String, Object> context) {
        String focusFieldName = (String) context.get(modelForm.getName().concat(".focusFieldName"));
        if (focusFieldName == null) {
            return "";
        }
        return focusFieldName;
    }

    private final ModelForm modelForm;
    private final FormStringRenderer formStringRenderer;
    private String focusFieldName;

    public FormRenderer(ModelForm modelForm, FormStringRenderer formStringRenderer) {
        this.modelForm = modelForm;
        this.formStringRenderer = formStringRenderer;
        this.focusFieldName = modelForm.getFocusFieldName();
    }

    // The ordering of the returned collection is guaranteed by using `TreeMap`.
    private static Collector<ModelFormField, ?, Map<Integer, List<ModelFormField>>> groupingByPosition =
            Collectors.groupingBy(ModelFormField::getPosition, TreeMap::new, Collectors.toList());

    public String getFocusFieldName() {
        return focusFieldName;
    }

    private static Predicate<ModelFormField> filteringIgnoredFields(Map<String, Object> context, Set<String> alreadyRendered) {
       return  modelFormField -> {
            FieldInfo fieldInfo = modelFormField.getFieldInfo();

            // render hidden/ignored field widget
            switch (fieldInfo.getFieldType()) {
            case FieldInfo.HIDDEN:
            case FieldInfo.IGNORED:
                if (modelFormField.shouldUse(context)) {
                    if (alreadyRendered != null) {
                        alreadyRendered.add(modelFormField.getName());
                    }
                    return true;
                }
                break;

            case FieldInfo.DISPLAY:
            case FieldInfo.DISPLAY_ENTITY:
                ModelFormField.DisplayField displayField = (ModelFormField.DisplayField) fieldInfo;
                if (displayField.getAlsoHidden() && modelFormField.shouldUse(context)) {
                    // don't add to already rendered here, or the display won't get rendered.
                    return true;
                }
                break;

            case FieldInfo.HYPERLINK:
                ModelFormField.HyperlinkField hyperlinkField = (ModelFormField.HyperlinkField) fieldInfo;
                if (hyperlinkField.getAlsoHidden() && modelFormField.shouldUse(context)) {
                    // don't add to already rendered here, or the hyperlink won't get rendered.
                    return true;
                }
                break;

            default:
                break;
            }
            return false;
        };
    }

    private static List<ModelFormField> getHiddenIgnoredFields(Map<String, Object> context, Set<String> alreadyRendered,
            List<ModelFormField> fields, int position) {
        return fields.stream()
                // with position == -1 then gets all the hidden fields
                .filter(modelFormField -> position == -1 || modelFormField.getPosition() == position)
                .filter(filteringIgnoredFields(context, alreadyRendered))
                .collect(Collectors.toList());
    }

    private List<FieldGroupBase> getInbetweenList(FieldGroup startFieldGroup, FieldGroup endFieldGroup) {
        List<FieldGroupBase> inbetweenList = new ArrayList<>();
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
        Iterator<FieldGroupBase> iter = modelForm.getFieldGroupList().iterator();
        while (iter.hasNext()) {
            FieldGroupBase obj = iter.next();
            if (obj instanceof ModelForm.Banner) {
                if (firstFound) {
                    inbetweenList.add(obj);
                }
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
                    }
                    inbetweenList.add(fieldGroup);
                }
            }
        }
        return inbetweenList;
    }

    /**
     * Renders this form to a writer, as defined with the
     * FormStringRenderer implementation.
     *
     * @param writer The Writer that the form text will be written to
     * @param context Map containing the form context; the following are
     *   reserved words in this context: parameters (Map), isError (Boolean),
     *   itemIndex (Integer, for lists only, otherwise null), formName
     *   (String, optional alternate name for form, defaults to the
     *   value of the name attribute)
     */
    public void render(Appendable writer, Map<String, Object> context) throws Exception {
        //  increment the paginator, only for list and multi forms
        if (modelForm instanceof ModelGrid) {
            WidgetWorker.incrementPaginatorNumber(context);
        }

        // Populate the viewSize and viewIndex so they are available for use during form actions
        context.put("viewIndex", Paginator.getViewIndex(modelForm, context));
        context.put("viewSize", Paginator.getViewSize(modelForm, context));

        modelForm.runFormActions(context);

        // if this is a list form, don't use Request Parameters
        if (modelForm instanceof ModelGrid) {
            context.put("useRequestParameters", Boolean.FALSE);
        }

        // find the highest position number to get the max positions used
        int positions = 1;
        for (ModelFormField modelFormField : modelForm.getFieldList()) {
            int curPos = modelFormField.getPosition();
            if (curPos > positions) {
                positions = curPos;
            }
            FieldInfo currentFieldInfo = modelFormField.getFieldInfo();
            if (currentFieldInfo == null) {
                throw new IllegalArgumentException(
                        "Error rendering form, a field has no FieldInfo, ie no sub-element for the type of field for field named: "
                                + modelFormField.getName());
            }
        }

        if ("single".equals(modelForm.getType())) {
            this.renderSingleFormString(writer, context, positions);
        } else if ("list".equals(modelForm.getType())) {
            this.renderListFormString(writer, context, positions);
        } else if ("multi".equals(modelForm.getType())) {
            this.renderMultiFormString(writer, context, positions);
        } else if ("upload".equals(modelForm.getType())) {
            this.renderSingleFormString(writer, context, positions);
        } else {
            if (UtilValidate.isEmpty(modelForm.getType())) {
                throw new IllegalArgumentException("The form 'type' tag is missing or empty on the form with the name "
                        + modelForm.getName());
            }
            throw new IllegalArgumentException("The form type " + modelForm.getType()
                    + " is not supported for form with name " + modelForm.getName());
        }
    }

    // Return a stateful predicate that satisfies only the first time a field name is encountered.
    static private Predicate<ModelFormField> filteringDuplicateNames() {
        Set<String> seenFieldNames = new HashSet<>();
        return field -> seenFieldNames.add(field.getName());
    }

    private int renderHeaderRow(Appendable writer, Map<String, Object> context)
            throws IOException {
        int maxNumOfColumns = 0;

        // We will render one title/column for all the fields with the same name
        // in this model: we can have more fields with the same name when use-when
        // conditions are used or when a form is extended or when the fields are
        // automatically retrieved by a service or entity definition.
        Collection<List<ModelFormField>> fieldListsByPosition = modelForm.getFieldList().stream()
                .filter(filteringDuplicateNames())
                .collect(groupingByPosition)
                .values();

        // ===========================
        // Preprocessing
        // ===========================
        // `fieldRowsByPosition` will contain maps containing the list of fields for a position
        List<Map<String, List<ModelFormField>>> fieldRowsByPosition = new LinkedList<>();
        for (List<ModelFormField> mainFieldList : fieldListsByPosition) {
            int numOfColumns = 0;

            List<ModelFormField> innerDisplayHyperlinkFieldsBegin = new LinkedList<>();
            List<ModelFormField> innerFormFields = new LinkedList<>();
            List<ModelFormField> innerDisplayHyperlinkFieldsEnd = new LinkedList<>();

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
                FieldInfo fieldInfo = modelFormField.getFieldInfo();

                // if the field's title is explicitly set to "" (title="") then
                // the header is not created for it; this is useful for position list
                // where one line can be rendered with more than one row, and we
                // only want to display the title header for the main row
                String modelFormFieldTitle = modelFormField.getTitle(context);
                if ("".equals(modelFormFieldTitle)) {
                    continue;
                }
                // don't do any header for hidden or ignored fields
                if (fieldInfo.getFieldType() == FieldInfo.HIDDEN
                        || fieldInfo.getFieldType() == FieldInfo.IGNORED) {
                    continue;
                }

                if (modelFormField.shouldIgnore(context)) {
                    continue;
                }

                if (FieldInfo.isInputFieldType(fieldInfo.getFieldType())) {
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
                FieldInfo fieldInfo = modelFormField.getFieldInfo();

                // don't do any header for hidden or ignored fields
                if (fieldInfo.getFieldType() == FieldInfo.HIDDEN
                        || fieldInfo.getFieldType() == FieldInfo.IGNORED) {
                    continue;
                }

                // skip all of the display/hyperlink fields
                if (!FieldInfo.isInputFieldType(fieldInfo.getFieldType())) {
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
        formStringRenderer.renderFormatHeaderOpen(writer, context, modelForm);
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
                formStringRenderer.renderFormatHeaderRowOpen(writer, context, modelForm);

                if (modelForm.getGroupColumns()) {
                    Iterator<ModelFormField> innerDisplayHyperlinkFieldsBeginIt = innerDisplayHyperlinkFieldsBegin.iterator();
                    while (innerDisplayHyperlinkFieldsBeginIt.hasNext()) {
                        ModelFormField modelFormField = innerDisplayHyperlinkFieldsBeginIt.next();
                        // span columns only if this is the last column in the row (not just in this first list)
                        if (innerDisplayHyperlinkFieldsBeginIt.hasNext() || numOfCells > innerDisplayHyperlinkFieldsBegin.size()) {
                            formStringRenderer.renderFormatHeaderRowCellOpen(writer, context, modelForm, modelFormField, 1);
                        } else {
                            formStringRenderer.renderFormatHeaderRowCellOpen(writer, context, modelForm, modelFormField,
                                    numOfColumnsToSpan);
                        }
                        formStringRenderer.renderFieldTitle(writer, context, modelFormField);
                        formStringRenderer.renderFormatHeaderRowCellClose(writer, context, modelForm, modelFormField);
                    }
                    if (innerFormFields.size() > 0) {
                        // TODO: manage colspan
                        formStringRenderer.renderFormatHeaderRowFormCellOpen(writer, context, modelForm);
                        Iterator<ModelFormField> innerFormFieldsIt = innerFormFields.iterator();
                        while (innerFormFieldsIt.hasNext()) {
                            ModelFormField modelFormField = innerFormFieldsIt.next();

                            if (modelForm.getSeparateColumns() || modelFormField.getSeparateColumn()) {
                                formStringRenderer.renderFormatItemRowCellOpen(writer, context, modelForm, modelFormField, 1);
                            }

                            // render title (unless this is a submit or a reset field)
                            formStringRenderer.renderFieldTitle(writer, context, modelFormField);

                            if (modelForm.getSeparateColumns() || modelFormField.getSeparateColumn()) {
                                formStringRenderer.renderFormatItemRowCellClose(writer, context, modelForm, modelFormField);
                            }

                            if (innerFormFieldsIt.hasNext()) {
                                // TODO: determine somehow if this is the last one... how?
                                if (!modelForm.getSeparateColumns() && !modelFormField.getSeparateColumn()) {
                                    formStringRenderer.renderFormatHeaderRowFormCellTitleSeparator(writer, context, modelForm,
                                            modelFormField, false);
                                }
                            }
                        }
                        formStringRenderer.renderFormatHeaderRowFormCellClose(writer, context, modelForm);
                    }
                    Iterator<ModelFormField> innerDisplayHyperlinkFieldsEndIt = innerDisplayHyperlinkFieldsEnd.iterator();
                    while (innerDisplayHyperlinkFieldsEndIt.hasNext()) {
                        ModelFormField modelFormField = innerDisplayHyperlinkFieldsEndIt.next();
                        // span columns only if this is the last column in the row (not just in this first list)
                        if (innerDisplayHyperlinkFieldsEndIt.hasNext() || numOfCells > innerDisplayHyperlinkFieldsEnd.size()) {
                            formStringRenderer.renderFormatHeaderRowCellOpen(writer, context, modelForm, modelFormField, 1);
                        } else {
                            formStringRenderer.renderFormatHeaderRowCellOpen(writer, context, modelForm, modelFormField,
                                    numOfColumnsToSpan);
                        }
                        formStringRenderer.renderFieldTitle(writer, context, modelFormField);
                        formStringRenderer.renderFormatHeaderRowCellClose(writer, context, modelForm, modelFormField);
                    }
                } else {
                    Iterator<ModelFormField> mainFieldListIter = mainFieldList.iterator();
                    while (mainFieldListIter.hasNext()) {
                        ModelFormField modelFormField = mainFieldListIter.next();

                        // don't do any header for hidden or ignored fields
                        FieldInfo fieldInfo = modelFormField.getFieldInfo();
                        if (fieldInfo.getFieldType() == FieldInfo.HIDDEN
                                || fieldInfo.getFieldType() == FieldInfo.IGNORED) {
                            continue;
                        }

                        // span columns only if this is the last column in the row (not just in this first list)
                        if (mainFieldListIter.hasNext() || numOfCells > mainFieldList.size()) {
                            formStringRenderer.renderFormatHeaderRowCellOpen(writer, context, modelForm, modelFormField, 1);
                        } else {
                            formStringRenderer.renderFormatHeaderRowCellOpen(writer, context, modelForm, modelFormField,
                                    numOfColumnsToSpan);
                        }
                        formStringRenderer.renderFieldTitle(writer, context, modelFormField);
                        formStringRenderer.renderFormatHeaderRowCellClose(writer, context, modelForm, modelFormField);
                    }
                }

                formStringRenderer.renderFormatHeaderRowClose(writer, context, modelForm);
            }
        }
        formStringRenderer.renderFormatHeaderClose(writer, context, modelForm);

        return maxNumOfColumns;
    }

    private static void renderHiddenIgnoredFields(Appendable writer, Map<String, Object> context,
            FormStringRenderer formStringRenderer, List<ModelFormField> fieldList) throws IOException {
        for (ModelFormField modelFormField : fieldList) {
            FieldInfo fieldInfo = modelFormField.getFieldInfo();

            // render hidden/ignored field widget
            switch (fieldInfo.getFieldType()) {
            case FieldInfo.HIDDEN:
            case FieldInfo.IGNORED:
                modelFormField.renderFieldString(writer, context, formStringRenderer);
                break;

            case FieldInfo.DISPLAY:
            case FieldInfo.DISPLAY_ENTITY:
            case FieldInfo.HYPERLINK:
                formStringRenderer.renderHiddenField(writer, context, modelFormField, modelFormField.getEntry(context));
                break;
            default:
                break;
            }
        }
    }

    // The fields in the three lists, usually created in the preprocessing phase
    // of the renderItemRows method are rendered: this will create a visual representation
    // of one row (corresponding to one position).
    private void renderItemRow(Appendable writer, Map<String, Object> localContext, FormStringRenderer formStringRenderer,
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
        formStringRenderer.renderFormatItemRowOpen(writer, localContext, modelForm);
        Iterator<ModelFormField> innerDisplayHyperlinkFieldsBeginIter = innerDisplayHyperlinkFieldsBegin.iterator();
        Map<String, Integer> fieldCount = new HashMap<>();
        while (innerDisplayHyperlinkFieldsBeginIter.hasNext()) {
            ModelFormField modelFormField = innerDisplayHyperlinkFieldsBeginIter.next();
            if (fieldCount.containsKey(modelFormField.getFieldName())) {
                fieldCount.put(modelFormField.getFieldName(), fieldCount.get(modelFormField.getFieldName()) + 1);
            } else {
                fieldCount.put(modelFormField.getFieldName(), 1);
            }
        }

        if (modelForm.getGroupColumns()) {
            // do the first part of display and hyperlink fields
            Iterator<ModelFormField> innerDisplayHyperlinkFieldIter = innerDisplayHyperlinkFieldsBegin.iterator();
            while (innerDisplayHyperlinkFieldIter.hasNext()) {
                boolean cellOpen = false;
                ModelFormField modelFormField = innerDisplayHyperlinkFieldIter.next();

                if(modelFormField.shouldIgnore(localContext)) {
                    continue;
                }

                // span columns only if this is the last column in the row (not just in this first list)
                if (fieldCount.get(modelFormField.getName()) < 2) {
                    if ((innerDisplayHyperlinkFieldIter.hasNext() || numOfCells > innerDisplayHyperlinkFieldsBegin.size())) {
                        formStringRenderer.renderFormatItemRowCellOpen(writer, localContext, modelForm, modelFormField, 1);
                    } else {
                        formStringRenderer.renderFormatItemRowCellOpen(writer, localContext, modelForm, modelFormField,
                                numOfColumnsToSpan);
                    }
                    cellOpen = true;
                }
                if ((!"list".equals(modelForm.getType()) && !"multi".equals(modelForm.getType()))
                        || modelFormField.shouldUse(localContext)) {
                    if ((fieldCount.get(modelFormField.getName()) > 1)) {
                        if ((innerDisplayHyperlinkFieldIter.hasNext() || numOfCells > innerDisplayHyperlinkFieldsBegin.size())) {
                            formStringRenderer.renderFormatItemRowCellOpen(writer, localContext, modelForm, modelFormField, 1);
                        } else {
                            formStringRenderer.renderFormatItemRowCellOpen(writer, localContext, modelForm, modelFormField,
                                    numOfColumnsToSpan);
                        }
                        cellOpen = true;
                    }
                    modelFormField.renderFieldString(writer, localContext, formStringRenderer);
                }
                if (cellOpen) {
                    formStringRenderer.renderFormatItemRowCellClose(writer, localContext, modelForm, modelFormField);
                }
            }

            // The form cell is rendered only if there is at least an input field
            if (innerFormFields.size() > 0) {
                // render the "form" cell
                formStringRenderer.renderFormatItemRowFormCellOpen(writer, localContext, modelForm); // TODO: colspan

                if (formPerItem) {
                    formStringRenderer.renderFormOpen(writer, localContext, modelForm);
                }

                // do all of the hidden fields...
                renderHiddenIgnoredFields(writer, localContext, formStringRenderer, hiddenIgnoredFieldList);

                Iterator<ModelFormField> innerFormFieldIter = innerFormFields.iterator();
                while (innerFormFieldIter.hasNext()) {
                    ModelFormField modelFormField = innerFormFieldIter.next();
                    if (modelForm.getSeparateColumns() || modelFormField.getSeparateColumn()) {
                        formStringRenderer.renderFormatItemRowCellOpen(writer, localContext, modelForm, modelFormField, 1);
                    }
                    // render field widget
                    if ((!"list".equals(modelForm.getType()) && !"multi".equals(modelForm.getType()))
                            || modelFormField.shouldUse(localContext)) {
                        modelFormField.renderFieldString(writer, localContext, formStringRenderer);
                    }

                    if (modelForm.getSeparateColumns() || modelFormField.getSeparateColumn()) {
                        formStringRenderer.renderFormatItemRowCellClose(writer, localContext, modelForm, modelFormField);
                    }
                }

                if (formPerItem) {
                    formStringRenderer.renderFormClose(writer, localContext, modelForm);
                }

                formStringRenderer.renderFormatItemRowFormCellClose(writer, localContext, modelForm);
            }

            // render the rest of the display/hyperlink fields
            innerDisplayHyperlinkFieldIter = innerDisplayHyperlinkFieldsEnd.iterator();
            while (innerDisplayHyperlinkFieldIter.hasNext()) {
                ModelFormField modelFormField = innerDisplayHyperlinkFieldIter.next();
                // span columns only if this is the last column in the row
                if (innerDisplayHyperlinkFieldIter.hasNext()) {
                    formStringRenderer.renderFormatItemRowCellOpen(writer, localContext, modelForm, modelFormField, 1);
                } else {
                    formStringRenderer.renderFormatItemRowCellOpen(writer, localContext, modelForm, modelFormField,
                            numOfColumnsToSpan);
                }
                if ((!"list".equals(modelForm.getType()) && !"multi".equals(modelForm.getType()))
                        || modelFormField.shouldUse(localContext)) {
                    modelFormField.renderFieldString(writer, localContext, formStringRenderer);
                }
                formStringRenderer.renderFormatItemRowCellClose(writer, localContext, modelForm, modelFormField);
            }
        } else {
            // do all of the hidden fields...
            renderHiddenIgnoredFields(writer, localContext, formStringRenderer, hiddenIgnoredFieldList);

            Iterator<ModelFormField> mainFieldIter = mainFieldList.iterator();
            while (mainFieldIter.hasNext()) {
                ModelFormField modelFormField = mainFieldIter.next();

                // don't do any header for hidden or ignored fields inside this loop
                FieldInfo fieldInfo = modelFormField.getFieldInfo();
                if (fieldInfo.getFieldType() == FieldInfo.HIDDEN
                        || fieldInfo.getFieldType() == FieldInfo.IGNORED) {
                    continue;
                }

                // span columns only if this is the last column in the row
                if (mainFieldIter.hasNext()) {
                    formStringRenderer.renderFormatItemRowCellOpen(writer, localContext, modelForm, modelFormField, 1);
                } else {
                    formStringRenderer.renderFormatItemRowCellOpen(writer, localContext, modelForm, modelFormField,
                            numOfColumnsToSpan);
                }
                if ((!"list".equals(modelForm.getType()) && !"multi".equals(modelForm.getType()))
                        || modelFormField.shouldUse(localContext)) {
                    modelFormField.renderFieldString(writer, localContext, formStringRenderer);
                }
                formStringRenderer.renderFormatItemRowCellClose(writer, localContext, modelForm, modelFormField);
            }
        }

        // render row formatting close
        formStringRenderer.renderFormatItemRowClose(writer, localContext, modelForm);
    }

    private void renderItemRows(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer,
            boolean formPerItem, int numOfColumns) throws IOException {
        String lookupName = modelForm.getListName();
        if (UtilValidate.isEmpty(lookupName)) {
            Debug.logError("No value for list or iterator name found.", module);
            return;
        }
        Object obj = context.get(lookupName);
        if (obj == null) {
            if (Debug.verboseOn()) {
                 Debug.logVerbose("No object for list or iterator name [" + lookupName + "] found, so not rendering rows.", module);
            }
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
        Paginator.getListLimits(modelForm, context, obj);

        int listSize = (Integer) context.get("listSize");
        int lowIndex = (Integer) context.get("lowIndex");
        int highIndex = (Integer) context.get("highIndex");

        // we're passed a subset of the list, so use (0, viewSize) range
        if (modelForm.isOverridenListSize()) {
            lowIndex = 0;
            highIndex = (Integer) context.get("viewSize");
        }

        if (iter != null) {
            // render item rows
            if (UtilValidate.isNotEmpty(context.get("itemIndex"))) {
                if (UtilValidate.isNotEmpty(context.get("parentItemIndex"))) {
                    context.put("parentItemIndex", context.get("parentItemIndex") + modelForm.getItemIndexSeparator() + context.get("itemIndex"));
                } else {
                    context.put("parentItemIndex", modelForm.getItemIndexSeparator() + context.get("itemIndex"));
                }
            }
            int itemIndex = -1;
            Object item = null;
            context.put("wholeFormContext", context);
            // Initialize previousItem with a sentry value since the first Item has no previous Item.
            Map<String, Object> previousItem = new HashMap<>();
            while ((item = safeNext(iter)) != null) {
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
                if (UtilValidate.isNotEmpty(modelForm.getListEntryName())) {
                    localContext.put(modelForm.getListEntryName(), item);
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

                localContext.push();
                localContext.put("previousItem", previousItem);
                previousItem = new HashMap<>(itemMap);

                AbstractModelAction.runSubActions(modelForm.getRowActions(), localContext);

                localContext.put("itemIndex", itemIndex - lowIndex);
                if (UtilValidate.isNotEmpty(context.get("renderFormSeqNumber"))) {
                    localContext.put("formUniqueId", "_" + context.get("renderFormSeqNumber"));
                }

                if (Debug.verboseOn()) {
                     Debug.logVerbose("In form got another row, context is: " + localContext, module);
                }

                List<ModelFormField> tempFieldList = modelForm.getFieldList().stream()
                        .filter(usedFields(localContext))
                        .collect(Collectors.toList());

                // Each single item is rendered in one or more rows if its fields have
                // different "position" attributes. All the fields with the same position
                // are rendered in the same row.
                // The default position is 1, and represents the main row:
                // it contains the fields that are in the list header (columns).
                // The positions lower than 1 are rendered in rows before the main one;
                // positions higher than 1 are rendered after the main one.
                Collection<List<ModelFormField>> fieldListsByPosition = tempFieldList.stream()
                        .collect(groupingByPosition)
                        .values();

                for (List<ModelFormField> fieldListByPosition : fieldListsByPosition) {
                    // For each position (the subset of fields with the same position attribute)
                    // we have two phases: preprocessing and rendering

                    List<ModelFormField> innerDisplayHyperlinkFieldsBegin = new LinkedList<>();
                    List<ModelFormField> innerFormFields = new LinkedList<>();
                    List<ModelFormField> innerDisplayHyperlinkFieldsEnd = new LinkedList<>();

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
                        FieldInfo fieldInfo = modelFormField.getFieldInfo();

                        // don't do any header for hidden or ignored fields
                        if (fieldInfo.getFieldType() == FieldInfo.HIDDEN
                                || fieldInfo.getFieldType() == FieldInfo.IGNORED) {
                            continue;
                        }

                        if (FieldInfo.isInputFieldType(fieldInfo.getFieldType())) {
                            // okay, now do the form cell
                            break;
                        }

                        // if this is a list or multi form don't skip here because we don't want to skip the table cell, will skip the actual field later
                        if (!"list".equals(modelForm.getType()) && !"multi".equals(modelForm.getType())
                                && !modelFormField.shouldUse(localContext)) {
                            continue;
                        }
                        innerDisplayHyperlinkFieldsBegin.add(modelFormField);
                        currentPosition = modelFormField.getPosition();
                    }
                    Iterator<ModelFormField> innerFormFieldIter = fieldListByPosition.iterator();
                    while (innerFormFieldIter.hasNext()) {
                        ModelFormField modelFormField = innerFormFieldIter.next();
                        FieldInfo fieldInfo = modelFormField.getFieldInfo();

                        // don't do any header for hidden or ignored fields
                        if (fieldInfo.getFieldType() == FieldInfo.HIDDEN
                                || fieldInfo.getFieldType() == FieldInfo.IGNORED) {
                            continue;
                        }

                        // skip all of the display/hyperlink fields
                        if (!FieldInfo.isInputFieldType(fieldInfo.getFieldType())) {
                            continue;
                        }

                        // if this is a list or multi form don't skip here because we don't want to skip the table cell, will skip the actual field later
                        if (!"list".equals(modelForm.getType()) && !"multi".equals(modelForm.getType())
                                && !modelFormField.shouldUse(localContext)) {
                            continue;
                        }
                        innerFormFields.add(modelFormField);
                        currentPosition = modelFormField.getPosition();
                    }
                    while (innerDisplayHyperlinkFieldIter.hasNext()) {
                        ModelFormField modelFormField = innerDisplayHyperlinkFieldIter.next();
                        FieldInfo fieldInfo = modelFormField.getFieldInfo();

                        // don't do any header for hidden or ignored fields
                        if (fieldInfo.getFieldType() == FieldInfo.HIDDEN
                                || fieldInfo.getFieldType() == FieldInfo.IGNORED) {
                            continue;
                        }

                        // skip all non-display and non-hyperlink fields
                        if (FieldInfo.isInputFieldType(fieldInfo.getFieldType())) {
                            continue;
                        }

                        // if this is a list or multi form don't skip here because we don't want to skip the table cell, will skip the actual field later
                        if (!"list".equals(modelForm.getType()) && !"multi".equals(modelForm.getType())
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
                context.put("highIndex", modelForm.isOverridenListSize() ? listSize : highIndex);
            }
            context.put("actualPageSize", highIndex - lowIndex);

            if (iter instanceof EntityListIterator) {
                try {
                    ((EntityListIterator) iter).close();
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Error closing list form render EntityListIterator: " + e.toString(), module);
                }
            }
        }
    }

    private void renderListFormString(Appendable writer, Map<String, Object> context,
            int positions) throws IOException {
        // render list/tabular type forms

        // prepare the items iterator and compute the pagination parameters
        Paginator.preparePager(modelForm, context);

        // render formatting wrapper open
        formStringRenderer.renderFormatListWrapperOpen(writer, context, modelForm);

        int numOfColumns = 0;
        boolean containsData = this.checkFormData(context);
        // ===== render header row =====
        if (!modelForm.getHideHeader() && containsData) {
            numOfColumns = this.renderHeaderRow(writer, context);
        }
        if (!containsData){
            formStringRenderer.renderEmptyFormDataMessage(writer, context, modelForm);
        }
        // ===== render the item rows =====
        this.renderItemRows(writer, context, formStringRenderer, true, numOfColumns);

        // render formatting wrapper close
        formStringRenderer.renderFormatListWrapperClose(writer, context, modelForm);

    }

    private void renderMultiFormString(Appendable writer, Map<String, Object> context,
            int positions) throws IOException {
        if (!modelForm.getSkipStart()) {
            formStringRenderer.renderFormOpen(writer, context, modelForm);
        }

        // prepare the items iterator and compute the pagination parameters
        Paginator.preparePager(modelForm, context);

        // render formatting wrapper open
        formStringRenderer.renderFormatListWrapperOpen(writer, context, modelForm);

        int numOfColumns = 0;
        boolean containsData = this.checkFormData(context);
        // ===== render header row =====
        if (!modelForm.getHideHeader() && containsData) {
            numOfColumns = this.renderHeaderRow(writer, context);
        }
        if (!containsData){
            formStringRenderer.renderEmptyFormDataMessage(writer, context, modelForm);
        }
        // ===== render the item rows =====
        this.renderItemRows(writer, context, formStringRenderer, false, numOfColumns);

        formStringRenderer.renderFormatListWrapperClose(writer, context, modelForm);

        if (!modelForm.getSkipEnd()) {
            formStringRenderer.renderMultiFormClose(writer, context, modelForm);
        }

    }


    private void renderSingleFormString(Appendable writer, Map<String, Object> context,
            int positions) throws IOException {
        List<ModelFormField> tempFieldList = modelForm.getFieldList().stream()
                .filter(usedFields(context))
                .collect(Collectors.toList());
        Set<String> alreadyRendered = new TreeSet<>();
        FieldGroup lastFieldGroup = null;
        // render form open
        if (!modelForm.getSkipStart()) {
            formStringRenderer.renderFormOpen(writer, context, modelForm);
        }

        // render all hidden & ignored fields
        List<ModelFormField> hiddenIgnoredFieldList =
        getHiddenIgnoredFields(context, alreadyRendered, tempFieldList, -1);
        renderHiddenIgnoredFields(writer, context, formStringRenderer, hiddenIgnoredFieldList);

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
            currentFieldGroup = (FieldGroup) modelForm.getFieldGroupMap().get(currentFormField.getFieldName());
            if (currentFieldGroup == null) {
                currentFieldGroup = modelForm.getDefaultFieldGroup();
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
                    currentFormField = null;
                    // nextFormField is already null
                    break;
                }
                currentFieldGroup = null;
                if (currentFormField != null) {
                    currentFieldGroup = (FieldGroup) modelForm.getFieldGroupMap().get(currentFormField.getName());
                }
                if (currentFieldGroup == null) {
                    currentFieldGroup = modelForm.getDefaultFieldGroup();
                }
                currentFieldGroupName = currentFieldGroup.getId();

                if (lastFieldGroup != null) {
                    lastFieldGroupName = lastFieldGroup.getId();
                    if (!lastFieldGroupName.equals(currentFieldGroupName)) {
                        if (haveRenderedOpenFieldRow) {
                            formStringRenderer.renderFormatFieldRowClose(writer, context, modelForm);
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

            FieldInfo fieldInfo = currentFormField.getFieldInfo();
            if (fieldInfo.getFieldType() == FieldInfo.HIDDEN
                    || fieldInfo.getFieldType() == FieldInfo.IGNORED) {
                continue;
            }
            if (alreadyRendered.contains(currentFormField.getName())) {
                continue;
            }
            if (!currentFormField.shouldUse(context)) {
                if (UtilValidate.isNotEmpty(lastFormField)) {
                    currentFormField = lastFormField;
                }
                continue;
            }
            alreadyRendered.add(currentFormField.getName());
            if (focusFieldName.isEmpty()) {
                if (fieldInfo.getFieldType() != FieldInfo.DISPLAY && fieldInfo.getFieldType() != FieldInfo.HIDDEN
                        && fieldInfo.getFieldType() != FieldInfo.DISPLAY_ENTITY
                        && fieldInfo.getFieldType() != FieldInfo.IGNORED
                        && fieldInfo.getFieldType() != FieldInfo.IMAGE) {
                    focusFieldName = currentFormField.getName();
                    context.put(modelForm.getName().concat(".focusFieldName"), focusFieldName);
                }
            }

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
                    nextPositionInRow = nextFormField.getPosition();
                } else {
                    positionSpan = positions - currentFormField.getPosition();
                }
            }

            if (stayingOnRow) {
                // no spacer cell, might add later though...
                //formStringRenderer.renderFormatFieldRowSpacerCell(writer, context, currentFormField);
            } else {
                if (haveRenderedOpenFieldRow) {
                    // render row formatting close
                    formStringRenderer.renderFormatFieldRowClose(writer, context, modelForm);
                    haveRenderedOpenFieldRow = false;
                }

                // render row formatting open
                formStringRenderer.renderFormatFieldRowOpen(writer, context, modelForm);
                haveRenderedOpenFieldRow = true;
            }

            //
            // It must be a row open before rendering a field. If not, open it
            //
            if (!haveRenderedOpenFieldRow) {
                formStringRenderer.renderFormatFieldRowOpen(writer, context, modelForm);
                haveRenderedOpenFieldRow = true;
            }

            // render title formatting open
            formStringRenderer.renderFormatFieldRowTitleCellOpen(writer, context, currentFormField);

            // render title (unless this is a submit or a reset field)
            if (fieldInfo.getFieldType() != FieldInfo.SUBMIT
                    && fieldInfo.getFieldType() != FieldInfo.RESET) {
                formStringRenderer.renderFieldTitle(writer, context, currentFormField);
            } else {
                formStringRenderer.renderFormatEmptySpace(writer, context, modelForm);
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
            formStringRenderer.renderFormatFieldRowClose(writer, context, modelForm);
        }

        if (lastFieldGroup != null) {
            lastFieldGroup.renderEndString(writer, context, formStringRenderer);
        }
        // render formatting wrapper close
        // should be handled by renderEndString
        //formStringRenderer.renderFormatSingleWrapperClose(writer, context, this);

        // render form close
        if (!modelForm.getSkipEnd()) {
            formStringRenderer.renderFormClose(writer, context, modelForm);
        }

    }
    private boolean checkFormData(Map<String, Object> context) {
        String lookupName = modelForm.getListName();
        Object obj = context.get(lookupName);
        if (obj == null) {
            if (Debug.verboseOn())
                Debug.logVerbose("No object for list or iterator name [" + lookupName + "] found, so not rendering rows.", module);
            return true;
        }
        // if list is empty, do not render rows
        Iterator<?> iter = null;
        if (obj instanceof Iterator<?>) {
            iter = (Iterator<?>) obj;
        } else if (obj instanceof List<?>) {
            iter = ((List<?>) obj).listIterator();
        }
        int itemIndex = -1;
        if (iter instanceof EntityListIterator) {
            EntityListIterator eli = (EntityListIterator) iter;
            try {
                if(eli.getResultsSizeAfterPartialList() > 0){
                    itemIndex++;
                }
            } catch (GenericEntityException gee) {
                Debug.logError(gee,module);
            }
        } else {
            while (iter.hasNext()) {
                itemIndex++;
                break;
            }
        }
        if (itemIndex < 0) {
            return false;
        }
        return true;
    }

    private static <X> X safeNext(Iterator<X> iterator) {
        try {
            return iterator.next();
        } catch (NoSuchElementException e) {
            return null;
        }
    }
}
