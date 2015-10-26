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
package org.ofbiz.widget.renderer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.collections.MapStack;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.widget.WidgetWorker;
import org.ofbiz.widget.model.AbstractModelAction;
import org.ofbiz.widget.model.FieldInfo;
import org.ofbiz.widget.model.*;
import org.ofbiz.widget.model.ModelForm.FieldGroup;
import org.ofbiz.widget.model.ModelForm.FieldGroupBase;
import org.ofbiz.widget.model.ModelFormField;

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
        if (itemIndex != null/* && "list".equals(modelForm.getType())*/) {
            if (UtilValidate.isNotEmpty(context.get("parentItemIndex"))) {
                return retVal + context.get("parentItemIndex") + modelForm.getItemIndexSeparator() + itemIndex.intValue();
            }
            return retVal + modelForm.getItemIndexSeparator() + itemIndex.intValue();
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
            return formName + modelForm.getItemIndexSeparator() + itemIndex.intValue();
        } else {
            return formName;
        }
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

    private Collection<List<ModelFormField>> getFieldListsByPosition(List<ModelFormField> modelFormFieldList) {
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

    public String getFocusFieldName() {
        return focusFieldName;
    }

    private List<ModelFormField> getHiddenIgnoredFields(Map<String, Object> context, Set<String> alreadyRendered,
            List<ModelFormField> fieldList, int position) {
        /*
         * Method does not reference internal state - should be moved to another class.
         */
        List<ModelFormField> hiddenIgnoredFieldList = new LinkedList<ModelFormField>();
        for (ModelFormField modelFormField : fieldList) {
            // with position == -1 then gets all the hidden fields
            if (position != -1 && modelFormField.getPosition() != position) {
                continue;
            }
            FieldInfo fieldInfo = modelFormField.getFieldInfo();

            // render hidden/ignored field widget
            switch (fieldInfo.getFieldType()) {
            case FieldInfo.HIDDEN:
            case FieldInfo.IGNORED:
                if (modelFormField.shouldUse(context)) {
                    hiddenIgnoredFieldList.add(modelFormField);
                    if (alreadyRendered != null)
                        alreadyRendered.add(modelFormField.getName());
                }
                break;

            case FieldInfo.DISPLAY:
            case FieldInfo.DISPLAY_ENTITY:
                ModelFormField.DisplayField displayField = (ModelFormField.DisplayField) fieldInfo;
                if (displayField.getAlsoHidden() && modelFormField.shouldUse(context)) {
                    hiddenIgnoredFieldList.add(modelFormField);
                    // don't add to already rendered here, or the display won't ger rendered: if (alreadyRendered != null) alreadyRendered.add(modelFormField.getName());
                }
                break;

            case FieldInfo.HYPERLINK:
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

    private List<FieldGroupBase> getInbetweenList(FieldGroup startFieldGroup, FieldGroup endFieldGroup) {
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
        Iterator<FieldGroupBase> iter = modelForm.getFieldGroupList().iterator();
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

    /**
     * Renders this form to a writer, as defined with the
     * FormStringRenderer implementation.
     *
     * @param writer The Writer that the form text will be written to
     * @param context Map containing the form context; the following are
     *   reserved words in this context: parameters (Map), isError (Boolean),
     *   itemIndex (Integer, for lists only, otherwise null), bshInterpreter,
     *   formName (String, optional alternate name for form, defaults to the
     *   value of the name attribute)
     */
    public void render(Appendable writer, Map<String, Object> context)
            throws Exception {
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
            } else {
                throw new IllegalArgumentException("The form type " + modelForm.getType()
                        + " is not supported for form with name " + modelForm.getName());
            }
        }
    }

    private int renderHeaderRow(Appendable writer, Map<String, Object> context)
            throws IOException {
        int maxNumOfColumns = 0;

        // We will render one title/column for all the fields with the same name
        // in this model: we can have more fields with the same name when use-when
        // conditions are used or when a form is extended or when the fields are
        // automatically retrieved by a service or entity definition.
        List<ModelFormField> tempFieldList = new LinkedList<ModelFormField>();
        tempFieldList.addAll(modelForm.getFieldList());
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

        return maxNumOfColumns;
    }

    private void renderHiddenIgnoredFields(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer,
            List<ModelFormField> fieldList) throws IOException {
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
        Map<String, Integer> fieldCount = new HashMap<String, Integer>();
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
                this.renderHiddenIgnoredFields(writer, localContext, formStringRenderer, hiddenIgnoredFieldList);

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
            this.renderHiddenIgnoredFields(writer, localContext, formStringRenderer, hiddenIgnoredFieldList);

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
        Paginator.getListLimits(modelForm, context, obj);

        int listSize = ((Integer) context.get("listSize")).intValue();
        int lowIndex = ((Integer) context.get("lowIndex")).intValue();
        int highIndex = ((Integer) context.get("highIndex")).intValue();

        // we're passed a subset of the list, so use (0, viewSize) range
        if (modelForm.isOverridenListSize()) {
            lowIndex = 0;
            highIndex = ((Integer) context.get("viewSize")).intValue();
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
            Map<String, Object> previousItem = new HashMap<String, Object>();
            while ((item = safeNext(iter)) != null) {
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

                // reset/remove the BshInterpreter now as well as later because chances are there is an interpreter at this level of the stack too
                this.resetBshInterpreter(localContext);
                localContext.push();
                localContext.put("previousItem", previousItem);
                previousItem = new HashMap<String, Object>();
                previousItem.putAll(itemMap);

                AbstractModelAction.runSubActions(modelForm.getRowActions(), localContext);

                localContext.put("itemIndex", Integer.valueOf(itemIndex - lowIndex));
                if (UtilValidate.isNotEmpty(context.get("renderFormSeqNumber"))) {
                    localContext.put("formUniqueId", "_" + context.get("renderFormSeqNumber"));
                }

                this.resetBshInterpreter(localContext);

                if (Debug.verboseOn())
                    Debug.logVerbose("In form got another row, context is: " + localContext, module);

                // Check to see if there is a field, same name and same use-when (could come from extended form)
                List<ModelFormField> tempFieldList = new LinkedList<ModelFormField>();
                tempFieldList.addAll(modelForm.getFieldList());
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
                context.put("highIndex", Integer.valueOf(modelForm.isOverridenListSize() ? listSize : highIndex));
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

    private void renderListFormString(Appendable writer, Map<String, Object> context,
            int positions) throws IOException {
        // render list/tabular type forms

        // prepare the items iterator and compute the pagination parameters
        Paginator.preparePager(modelForm, context);

        // render formatting wrapper open
        formStringRenderer.renderFormatListWrapperOpen(writer, context, modelForm);

        int numOfColumns = 0;
        // ===== render header row =====
        if (!modelForm.getHideHeader()) {
            numOfColumns = this.renderHeaderRow(writer, context);
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
        // ===== render header row =====
        if (!modelForm.getHideHeader()) {
            numOfColumns = this.renderHeaderRow(writer, context);
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
        List<ModelFormField> tempFieldList = new LinkedList<ModelFormField>();
        tempFieldList.addAll(modelForm.getFieldList());

        // Check to see if there is a field, same name and same use-when (could come from extended form)
        for (int j = 0; j < tempFieldList.size(); j++) {
            ModelFormField modelFormField = tempFieldList.get(j);
            if (modelForm.getUseWhenFields().contains(modelFormField.getName())) {
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
        if (!modelForm.getSkipStart())
            formStringRenderer.renderFormOpen(writer, context, modelForm);

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
                    lastFormField = currentFormField;
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
            //Debug.logInfo("In single form evaluating use-when for field " + currentFormField.getName() + ": " + currentFormField.getUseWhen(), module);
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
        if (!modelForm.getSkipEnd())
            formStringRenderer.renderFormClose(writer, context, modelForm);

    }

    private void resetBshInterpreter(Map<String, Object> context) {
        context.remove("bshInterpreter");
    }

    private static <X> X safeNext(Iterator<X> iterator) {
        try {
            return iterator.next();
        } catch (NoSuchElementException e) {
            return null;
        }
    }
}
