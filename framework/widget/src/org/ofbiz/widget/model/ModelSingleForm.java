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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.ofbiz.base.util.BshUtil;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilCodec;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.model.ModelReader;
import org.ofbiz.service.DispatchContext;
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
public class ModelSingleForm extends ModelForm {

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

    public static final String module = ModelSingleForm.class.getName();

    /** XML Constructor */
    public ModelSingleForm(Element formElement, String formLocation, ModelReader entityModelReader, DispatchContext dispatchContext) {
        super(formElement, formLocation, entityModelReader, dispatchContext);
    }

    @Override
    public void accept(ModelWidgetVisitor visitor) throws Exception {
        visitor.visit(this);
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

    public Interpreter getBshInterpreter(Map<String, Object> context) throws EvalError {
        Interpreter bsh = (Interpreter) context.get("bshInterpreter");
        if (bsh == null) {
            bsh = BshUtil.makeInterpreter(context);
            context.put("bshInterpreter", bsh);
        }
        return bsh;
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
        } else {
            return this.getName();
        }
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

    protected ModelForm getParentModel(Element formElement, ModelReader entityModelReader, DispatchContext dispatchContext) {
        ModelForm parent = null;
        String parentResource = formElement.getAttribute("extends-resource");
        String parentForm = formElement.getAttribute("extends");
        String formType = formElement.getAttribute("type");
        if (!parentForm.isEmpty()) {
            // check if we have a resource name
            if (!parentResource.isEmpty()) {
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
                for (Element parentElement : formElements) {
                    if (parentElement.getAttribute("name").equals(parentForm) && formType.equals(parentElement.getAttribute("type"))) {
                        parent = FormFactory.createModelForm(parentElement, entityModelReader, dispatchContext, parentResource,
                                parentForm);
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
        UtilCodec.SimpleEncoder simpleEncoder = (UtilCodec.SimpleEncoder) context.get("simpleEncoder");
        if (simpleEncoder != null) {
            expanderContext = UtilCodec.HtmlEncodingMapWrapper.getHtmlEncodingMapWrapper(context, simpleEncoder);
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
    public boolean getGroupColumns() {
        return groupColumns;
    }

    public boolean isOverridenListSize() {
        return !this.overrideListSize.isEmpty();
    }

    public void runFormActions(Map<String, Object> context) {
        AbstractModelAction.runSubActions(this.actions, context);
    }
}
