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
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;

import org.apache.ofbiz.base.conversion.ConversionException;
import org.apache.ofbiz.base.conversion.DateTimeConverters;
import org.apache.ofbiz.base.conversion.DateTimeConverters.StringToTimestamp;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.GroovyUtil;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilCodec;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.base.util.collections.MapStack;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntity;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.finder.EntityFinderUtil;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelUtil;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.widget.WidgetWorker;
import org.apache.ofbiz.widget.model.CommonWidgetModels.AutoEntityParameters;
import org.apache.ofbiz.widget.model.CommonWidgetModels.AutoServiceParameters;
import org.apache.ofbiz.widget.model.CommonWidgetModels.Image;
import org.apache.ofbiz.widget.model.CommonWidgetModels.Link;
import org.apache.ofbiz.widget.model.CommonWidgetModels.Parameter;
import org.apache.ofbiz.widget.model.ModelForm.UpdateArea;
import org.apache.ofbiz.widget.renderer.FormRenderer;
import org.apache.ofbiz.widget.renderer.FormStringRenderer;
import org.apache.ofbiz.widget.renderer.MenuStringRenderer;
import org.apache.ofbiz.widget.renderer.ScreenRenderer;
import org.codehaus.groovy.control.CompilationFailedException;
import org.w3c.dom.Element;

/**
 * Models the &lt;field&gt; element.
 *
 * @see <code>widget-form.xsd</code>
 */
public class ModelFormField {

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
     */

    public static final String module = ModelFormField.class.getName();

    public static ModelFormField from(ModelFormFieldBuilder builder) {
        return new ModelFormField(builder);
    }

    private final FlexibleStringExpander action;
    private final String attributeName;
    private final boolean encodeOutput;
    private final String entityName;
    private final FlexibleMapAccessor<Object> entryAcsr;
    private final String event;
    private final FieldInfo fieldInfo;
    protected final String fieldName;
    private final String headerLink;
    private final String headerLinkStyle;
    private final String idName;
    private final FlexibleMapAccessor<Map<String, ? extends Object>> mapAcsr;
    protected final ModelForm modelForm;
    protected final String name;
    private final List<UpdateArea> onChangeUpdateAreas;
    private final List<UpdateArea> onClickUpdateAreas;
    protected final String parameterName;
    private final Integer position;
    private final String redWhen;
    private final Boolean requiredField;
    private final String requiredFieldStyle;
    private final boolean separateColumn;
    private final String serviceName;
    private final Boolean sortField;
    private final String sortFieldAscStyle;
    private final String sortFieldDescStyle;
    private final String sortFieldHelpText;
    private final String sortFieldStyle;
    private final FlexibleStringExpander title;
    private final String titleAreaStyle;
    private final String titleStyle;
    private final FlexibleStringExpander tooltip;
    private final String tooltipStyle;
    private final FlexibleStringExpander useWhen;
    private final FlexibleStringExpander ignoreWhen;
    private final String widgetAreaStyle;
    private final String widgetStyle;
    private final String parentFormName;
    private final String tabindex;

    private ModelFormField(ModelFormFieldBuilder builder) {
        this.action = builder.getAction();
        this.attributeName = builder.getAttributeName();
        this.encodeOutput = builder.getEncodeOutput();
        this.entityName = builder.getEntityName();
        this.entryAcsr = builder.getEntryAcsr();
        this.event = builder.getEvent();
        if (builder.getFieldInfo() != null) {
            this.fieldInfo = builder.getFieldInfo().copy(this);
        } else {
            this.fieldInfo = null;
        }
        this.fieldName = builder.getFieldName();
        this.headerLink = builder.getHeaderLink();
        this.headerLinkStyle = builder.getHeaderLinkStyle();
        this.idName = builder.getIdName();
        this.mapAcsr = builder.getMapAcsr();
        this.modelForm = builder.getModelForm();
        this.name = builder.getName();
        if (builder.getOnChangeUpdateAreas().isEmpty()) {
            this.onChangeUpdateAreas = Collections.emptyList();
        } else {
            this.onChangeUpdateAreas = Collections.unmodifiableList(new ArrayList<>(builder.getOnChangeUpdateAreas()));
        }
        if (builder.getOnClickUpdateAreas().isEmpty()) {
            this.onClickUpdateAreas = Collections.emptyList();
        } else {
            this.onClickUpdateAreas = Collections.unmodifiableList(new ArrayList<>(builder.getOnClickUpdateAreas()));
        }
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
        this.ignoreWhen = builder.getIgnoreWhen();
        this.widgetAreaStyle = builder.getWidgetAreaStyle();
        this.widgetStyle = builder.getWidgetStyle();
        this.parentFormName = builder.getParentFormName();
        this.tabindex = builder.getTabindex();
    }

    public FlexibleStringExpander getAction() {
        return action;
    }

    public String getAction(Map<String, ? extends Object> context) {
        if (UtilValidate.isNotEmpty(this.action)) {
            return action.expandString(context);
        }
        return null;
    }

    /**
     * Gets the name of the Service Attribute (aka Parameter) that corresponds
     * with this field. This can be used to get additional information about the field.
     * Use the getServiceName() method to get the Entity name that the field is in.
     *
     * @return returns the name of the Service Attribute
     */
    public String getAttributeName() {
        if (UtilValidate.isNotEmpty(this.attributeName)) {
            return this.attributeName;
        }
        return this.name;
    }


    /**
     * Gets the current id name of the {@link ModelFormField} and if in
     * a multi type {@link ModelForm}, suffixes it with the index row.
     * @param context
     * @return
     */
    public String getCurrentContainerId(Map<String, Object> context) {
        ModelForm modelForm = this.getModelForm();
        String idName = FlexibleStringExpander.expandString(this.getIdName(), context);

        if (modelForm != null) {
            Integer itemIndex = (Integer) context.get("itemIndex");
            if ("list".equals(modelForm.getType()) || "multi".equals(modelForm.getType())) {
                if (itemIndex != null) {
                    return idName + modelForm.getItemIndexSeparator() + itemIndex.intValue();
                }
            }
        }
        return idName;
    }

    public boolean getEncodeOutput() {
        return this.encodeOutput;
    }

    public String getEntityName() {
        if (UtilValidate.isNotEmpty(this.entityName)) {
            return this.entityName;
        }
        return this.modelForm.getDefaultEntityName();
    }

    /**
     * Gets the entry from the context that corresponds to this field; if this
     * form is being rendered in an error condition (ie isError in the context
     * is true) then the value will be retrieved from the parameters Map in
     * the context.
     *
     * @param context the context
     * @return returns the entry from the context that corresponds to this field
     */
    public String getEntry(Map<String, ? extends Object> context) {
        return this.getEntry(context, "");
    }

    public String getEntry(Map<String, ? extends Object> context, String defaultValue) {
        Boolean isError = (Boolean) context.get("isError");
        Boolean useRequestParameters = (Boolean) context.get("useRequestParameters");

        Locale locale = (Locale) context.get("locale");
        if (locale == null) {
            locale = Locale.getDefault();
        }
        TimeZone timeZone = (TimeZone) context.get("timeZone");
        if (timeZone == null) {
            timeZone = TimeZone.getDefault();
        }

        UtilCodec.SimpleEncoder simpleEncoder = (UtilCodec.SimpleEncoder) context.get("simpleEncoder");

        String returnValue;

        // if useRequestParameters is TRUE then parameters will always be used, if FALSE then parameters will never be used
        // if isError is TRUE and useRequestParameters is not FALSE (ie is null or TRUE) then parameters will be used
        if ((Boolean.TRUE.equals(isError) && !Boolean.FALSE.equals(useRequestParameters))
                || (Boolean.TRUE.equals(useRequestParameters))) {
            Map<String, Object> parameters = UtilGenerics.checkMap(context.get("parameters"), String.class, Object.class);
            String parameterName = this.getParameterName(context);
            if (parameters != null && parameters.get(parameterName) != null) {
                Object parameterValue = parameters.get(parameterName);
                if (parameterValue instanceof String) {
                    returnValue = (String) parameterValue;
                } else {
                    // we might want to do something else here in the future, but for now this is probably best
                    Debug.logWarning("Found a non-String parameter value for field [" + this.getModelForm().getName() + "."
                            + this.getFieldName() + "]", module);
                    returnValue = defaultValue;
                }
            } else {
                returnValue = defaultValue;
            }
        } else {
            Map<String, ? extends Object> dataMap = this.getMap(context);
            boolean dataMapIsContext = false;
            if (dataMap == null) {
                dataMap = context;
                dataMapIsContext = true;
            }
            Object retVal = null;
            if (UtilValidate.isNotEmpty(this.entryAcsr)) {
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
                // if no entry name was specified, use the field's name
                if (dataMap.containsKey(this.name)) {
                    retVal = dataMap.get(this.name);
                }
            }

            // this is a special case to fill in fields during a create by default from parameters passed in
            if (dataMapIsContext && retVal == null && !Boolean.FALSE.equals(useRequestParameters)) {
                Map<String, ? extends Object> parameters = UtilGenerics.checkMap(context.get("parameters"));
                if (parameters != null) {
                    if (UtilValidate.isNotEmpty(this.entryAcsr)) {
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
                    DateFormat df = UtilDateTime.toDateFormat(UtilDateTime.getDateFormat(), timeZone, null);
                    return df.format((java.util.Date) retVal);
                } else if (retVal instanceof java.sql.Time) {
                    DateFormat df = UtilDateTime.toTimeFormat(UtilDateTime.getTimeFormat(), timeZone, null);
                    return df.format((java.util.Date) retVal);
                } else if (retVal instanceof java.sql.Timestamp) {
                    DateFormat df = UtilDateTime.toDateTimeFormat(UtilDateTime.getDateTimeFormat(), timeZone, null);
                    return df.format((java.util.Date) retVal);
                } else if (retVal instanceof java.util.Date) {
                    DateFormat df = UtilDateTime.toDateTimeFormat("EEE MMM dd hh:mm:ss z yyyy", timeZone, null);
                    return df.format((java.util.Date) retVal);
                } else if (retVal instanceof Collection) {
                    Collection<Object> col = UtilGenerics.checkCollection(retVal);
                    Iterator<Object> iter = col.iterator();
                    List<Object> newCol = new ArrayList<>(col.size());
                    while (iter.hasNext()) {
                        Object item = iter.next();
                        if (item == null) {
                            continue;
                        }
                        if (simpleEncoder != null) {
                            newCol.add(simpleEncoder.encode(item.toString()));
                        }
                        else {
                            newCol.add(item.toString());
                        }
                    }
                    return newCol.toString();
                } else {
                    returnValue = retVal.toString();
                }
            } else {
                returnValue = defaultValue;
            }
        }

        if (this.getEncodeOutput() && returnValue != null) {
            if (simpleEncoder != null) {
                returnValue = simpleEncoder.encode(returnValue);
            }
        }

        if (returnValue != null) {
            returnValue = returnValue.trim();
        }
        return returnValue;
    }

    public FlexibleMapAccessor<Object> getEntryAcsr() {
        return entryAcsr;
    }

    public String getEntryName() {
        if (UtilValidate.isNotEmpty(this.entryAcsr)) {
            return this.entryAcsr.getOriginalName();
        }
        return this.name;
    }

    public String getEvent() {
        return event;
    }

    public FieldInfo getFieldInfo() {
        return fieldInfo;
    }

    /**
     * Gets the name of the Entity Field that corresponds
     * with this field. This can be used to get additional information about the field.
     * Use the getEntityName() method to get the Entity name that the field is in.
     *
     * @return return the name of the Entity Field that corresponds with this field
     */
    public String getFieldName() {
        if (UtilValidate.isNotEmpty(this.fieldName)) {
            return this.fieldName;
        }
        return this.name;
    }

    public String getHeaderLink() {
        return headerLink;
    }

    public String getHeaderLinkStyle() {
        return headerLinkStyle;
    }

    /**
     * Gets the id name of the {@link ModelFormField} that is :
     * <ul>
     *     <li>The id-name" specified on the field definition
     *     <li>Else the concatenation of the formName and fieldName
     * </ul>
     * @return
     */
    public String getIdName() {
        if (UtilValidate.isNotEmpty(idName)) {
            return idName;
        }
        String parentFormName = this.getParentFormName();
        if (UtilValidate.isNotEmpty(parentFormName)) {
            return parentFormName + "_" + this.getFieldName();
        }
        return this.modelForm.getName() + "_" + this.getFieldName();
    }

    public String getTabindex() {
        return tabindex;
    }

    public Map<String, ? extends Object> getMap(Map<String, ? extends Object> context) {
        if (UtilValidate.isEmpty(this.mapAcsr)) {
            return this.modelForm.getDefaultMap(context);
        }

        Map<String, ? extends Object> result = null;
        try {
            result = mapAcsr.get(context);
        } catch (ClassCastException e) {
            String errMsg = "Got an unexpected object type (not a Map) for map-name [" + mapAcsr.getOriginalName()
                    + "] in field with name [" + this.getName() + "]: " + e.getMessage();
            Debug.logError(errMsg, module);
            throw new ClassCastException(errMsg);
        }
        return result;
    }

    public FlexibleMapAccessor<Map<String, ? extends Object>> getMapAcsr() {
        return mapAcsr;
    }

    /** Get the name of the Map in the form context that contains the entry,
     * available from the getEntryName() method. This entry is used to
     * pre-populate the field widget when not in an error condition. In an
     * error condition the parameter name is used to get the value from the
     * parameters Map.
     *
     * @return returns the name of the Map in the form context that contains the entry
     */
    public String getMapName() {
        if (UtilValidate.isNotEmpty(this.mapAcsr)) {
            return this.mapAcsr.getOriginalName();
        }
        return this.modelForm.getDefaultMapName();
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

    /**
     * Get the name to use for the parameter for this field in the form interpreter.
     * For HTML forms this is the request parameter name.
     *
     * @return returns the name to use for the parameter for this field in the form interpreter
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
        }
        return baseName;
    }

    public int getPosition() {
        if (this.position == null) {
            return 1;
        }
        return position.intValue();
    }

    public String getRedWhen() {
        return redWhen;
    }

    public boolean getRequiredField() {
        return this.requiredField != null ? this.requiredField : false;
    }

    public String getRequiredFieldStyle() {
        if (UtilValidate.isNotEmpty(this.requiredFieldStyle)) {
            return this.requiredFieldStyle;
        }
        return this.modelForm.getDefaultRequiredFieldStyle();
    }

    public boolean getSeparateColumn() {
        return this.separateColumn;
    }

    public String getServiceName() {
        if (UtilValidate.isNotEmpty(this.serviceName)) {
            return this.serviceName;
        }
        return this.modelForm.getDefaultServiceName();
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

    public String getSortFieldHelpText(Map<String, Object> context) {
        return FlexibleStringExpander.expandString(this.sortFieldHelpText, context);
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

    public FlexibleStringExpander getTitle() {
        return title;
    }

    public String getTitle(Map<String, Object> context) {
        if (UtilValidate.isNotEmpty(this.title)) {
            return title.expandString(context);
        }

        // create a title from the name of this field; expecting a Java method/field style name, ie productName or productCategoryId
        if (UtilValidate.isEmpty(this.name)) {
            return ""; // this should never happen, ie name is required
        }

        // search for a localized label for the field's name
        Map<String, String> uiLabelMap = UtilGenerics.checkMap(context.get("uiLabelMap"));
        if (uiLabelMap != null) {
            String titleFieldName = "FormFieldTitle_" + this.name;
            String localizedName = uiLabelMap.get(titleFieldName);
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

    public String getTitleAreaStyle() {
        if (UtilValidate.isNotEmpty(this.titleAreaStyle)) {
            return this.titleAreaStyle;
        }
        return this.modelForm.getDefaultTitleAreaStyle();
    }

    public String getTitleStyle() {
        if (UtilValidate.isNotEmpty(this.titleStyle)) {
            return this.titleStyle;
        }
        return this.modelForm.getDefaultTitleStyle();
    }

    public FlexibleStringExpander getTooltip() {
        return tooltip;
    }

    public String getTooltip(Map<String, Object> context) {
        String tooltipString = "";
        if (UtilValidate.isNotEmpty(tooltip)) {
            tooltipString = tooltip.expandString(context);
        }
        if (this.getEncodeOutput()) {
            UtilCodec.SimpleEncoder simpleEncoder = (UtilCodec.SimpleEncoder) context.get("simpleEncoder");
            if (simpleEncoder != null) {
                tooltipString = simpleEncoder.encode(tooltipString);
            }
        }
        return tooltipString;
    }

    public String getTooltipStyle() {
        if (UtilValidate.isNotEmpty(this.tooltipStyle)) {
            return this.tooltipStyle;
        }
        return this.modelForm.getDefaultTooltipStyle();
    }

    public FlexibleStringExpander getUseWhen() {
        return useWhen;
    }

    public String getUseWhen(Map<String, Object> context) {
        if (UtilValidate.isNotEmpty(this.useWhen)) {
            return this.useWhen.expandString(context);
        }
        return "";
    }

    public String getIgnoreWhen(Map<String, Object> context) {
        if (UtilValidate.isNotEmpty(this.ignoreWhen)) {
            return this.ignoreWhen.expandString(context);
        }
        return "";
    }

    public String getWidgetAreaStyle() {
        if (UtilValidate.isNotEmpty(this.widgetAreaStyle)) {
            return this.widgetAreaStyle;
        }
        return this.modelForm.getDefaultWidgetAreaStyle();
    }

    public String getWidgetStyle() {
        if (UtilValidate.isNotEmpty(this.widgetStyle)) {
            return this.widgetStyle;
        }
        return this.modelForm.getDefaultWidgetStyle();
    }

    public String getParentFormName() {
        if (UtilValidate.isNotEmpty(this.parentFormName)) {
            return this.parentFormName;
        }
        return "";
    }

    /**
     * Checks if field is a row submit field.
     */
    public boolean isRowSubmit() {
        if (!"multi".equals(getModelForm().getType())) {
            return false;
        }
        if (getFieldInfo().getFieldType() != FieldInfo.CHECK) {
            return false;
        }
        if (!CheckField.ROW_SUBMIT_FIELD_NAME.equals(getName())) {
            return false;
        }
        return true;
    }

    public boolean isSortField() {
        return this.sortField != null && this.sortField.booleanValue();
    }

    public boolean isUseWhenEmpty() {
        if (this.useWhen == null) {
            return true;
        }

        return this.useWhen.isEmpty();
    }

    public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer)
            throws IOException {
        this.fieldInfo.renderFieldString(writer, context, formStringRenderer);
    }

    /**
     * the widget/interaction part will be red if the date value is
     *  before-now (for ex. thruDate), after-now (for ex. fromDate), or by-name (if the
     *  field's name or entry-name or fromDate or thruDate the corresponding
     *  action will be done); only applicable when the field is a timestamp
     *
     * @param context the context
     * @return true if the field should be read otherwise false
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
            } else if ("thruDate".equals(this.name)
                    || (this.entryAcsr != null && "thruDate".equals(this.entryAcsr.getOriginalName()))) {
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
        } catch (IllegalArgumentException e) {
            // okay, not a timestamp...
        }

        if (timestampVal == null) {
            try {
                dateVal = java.sql.Date.valueOf(value);
            } catch (IllegalArgumentException e) {
                // okay, not a date...
            }
        }

        if (timestampVal == null && dateVal == null) {
            try {
                timeVal = java.sql.Time.valueOf(value);
            } catch (IllegalArgumentException e) {
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

    public boolean shouldUse(Map<String, Object> context) {
        String useWhenStr = this.getUseWhen(context);
        if (UtilValidate.isEmpty(useWhenStr)) {
            return true;
        }

        try {
            Object retVal = GroovyUtil.eval(StringUtil.convertOperatorSubstitutions(useWhenStr),context);
            boolean condTrue = false;
            // retVal should be a Boolean, if not something weird is up...
            if (retVal instanceof Boolean) {
                Boolean boolVal = (Boolean) retVal;
                condTrue = boolVal.booleanValue();
            } else {
                throw new IllegalArgumentException("Return value from use-when condition eval was not a Boolean: "
                        + (retVal != null ? retVal.getClass().getName() : "null") + " [" + retVal + "] on the field " + this.name
                        + " of form " + this.modelForm.getName());
            }

            return condTrue;
        } catch (CompilationFailedException e) {
            String errMsg = "Error evaluating groovy use-when condition [" + useWhenStr + "] on the field " + this.name
                    + " of form " + this.modelForm.getName() + ": " + e.toString();
            Debug.logError(e, errMsg, module);
            throw new IllegalArgumentException(errMsg);
        }
    }

    /**
     * Models the &lt;auto-complete&gt; element.
     *
     * @see <code>widget-form.xsd</code>
     */
    public static class AutoComplete {
        private final String autoSelect;
        private final String choices;
        private final String frequency;
        private final String fullSearch;
        private final String ignoreCase;
        private final String minChars;
        private final String partialChars;
        private final String partialSearch;

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

        public String getChoices() {
            return this.choices;
        }

        public String getFrequency() {
            return this.frequency;
        }

        public String getFullSearch() {
            return this.fullSearch;
        }

        public String getIgnoreCase() {
            return this.ignoreCase;
        }

        public String getMinChars() {
            return this.minChars;
        }

        public String getPartialChars() {
            return this.partialChars;
        }

        public String getPartialSearch() {
            return this.partialSearch;
        }
    }

    /**
     * Models the &lt;check&gt; element.
     *
     * @see <code>widget-form.xsd</code>
     */
    public static class CheckField extends FieldInfoWithOptions {
        public final static String ROW_SUBMIT_FIELD_NAME = "_rowSubmit";
        private final FlexibleStringExpander allChecked;

        private CheckField(CheckField original, ModelFormField modelFormField) {
            super(original, modelFormField);
            this.allChecked = original.allChecked;
        }

        public CheckField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
            allChecked = FlexibleStringExpander.getInstance(element.getAttribute("all-checked"));
        }

        public CheckField(int fieldSource, ModelFormField modelFormField) {
            super(fieldSource, FieldInfo.CHECK, modelFormField);
            this.allChecked = FlexibleStringExpander.getInstance("");
        }

        public CheckField(ModelFormField modelFormField) {
            super(FieldInfo.SOURCE_EXPLICIT, FieldInfo.CHECK, modelFormField);
            this.allChecked = FlexibleStringExpander.getInstance("");
        }

        @Override
        public void accept(ModelFieldVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public FieldInfo copy(ModelFormField modelFormField) {
            return new CheckField(this, modelFormField);
        }

        public FlexibleStringExpander getAllChecked() {
            return allChecked;
        }

        public Boolean isAllChecked(Map<String, Object> context) {
            String allCheckedStr = this.allChecked.expandString(context);
            if (!allCheckedStr.isEmpty()) {
                return Boolean.valueOf("true".equals(allCheckedStr));
            }
            return null;
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer)
                throws IOException {
            formStringRenderer.renderCheckField(writer, context, this);
        }
    }

    /**
     * Models the &lt;container&gt; element.
     *
     * @see <code>widget-form.xsd</code>
     */
    public static class ContainerField extends FieldInfo {

        private ContainerField(ContainerField original, ModelFormField modelFormField) {
            super(original.getFieldSource(), original.getFieldType(), modelFormField);
        }

        public ContainerField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
        }

        public ContainerField(int fieldSource, int fieldType, ModelFormField modelFormField) {
            super(fieldSource, fieldType, modelFormField);
        }

        @Override
        public void accept(ModelFieldVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public FieldInfo copy(ModelFormField modelFormField) {
            return new ContainerField(this, modelFormField);
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer)
                throws IOException {
            formStringRenderer.renderContainerFindField(writer, context, this);
        }
    }

    /**
     * Models the &lt;date-find&gt; element.
     *
     * @see <code>widget-form.xsd</code>
     */
    public static class DateFindField extends DateTimeField {
        private final String defaultOptionFrom;
        private final String defaultOptionThru;

        private DateFindField(DateFindField original, ModelFormField modelFormField) {
            super(original, modelFormField);
            this.defaultOptionFrom = original.defaultOptionFrom;
            this.defaultOptionThru = original.defaultOptionThru;
        }

        public DateFindField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
            this.defaultOptionFrom = element.getAttribute("default-option-from");
            this.defaultOptionThru = element.getAttribute("default-option-thru");
        }

        public DateFindField(int fieldSource, ModelFormField modelFormField) {
            super(fieldSource, modelFormField);
            this.defaultOptionFrom = "greaterThan";
            this.defaultOptionThru = "opLessThan";
        }

        public DateFindField(int fieldSource, String type) {
            super(fieldSource, type);
            this.defaultOptionFrom = "greaterThan";
            this.defaultOptionThru = "opLessThan";
        }

        @Override
        public void accept(ModelFieldVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public FieldInfo copy(ModelFormField modelFormField) {
            return new DateFindField(this, modelFormField);
        }

        public String getDefaultOptionFrom() {
            return this.defaultOptionFrom;
        }

        public String getDefaultOptionFrom(Map<String, Object> context) {
            String defaultOption = getDefaultOptionFrom();

            Map<String, Object> parameters = UtilGenerics.checkMap(context.get("parameters"), String.class, Object.class);
            if (UtilValidate.isNotEmpty(parameters)) {
                String fieldName = this.getModelFormField().getName();
                if (parameters.containsKey(fieldName.concat("_fld0_value"))){
                    defaultOption = (String)parameters.get(fieldName.concat("_fld0_op"));
                }
            }
            return defaultOption;
        }

        public String getDefaultOptionThru() {
            return this.defaultOptionThru;
        }

        public String getDefaultOptionThru(Map<String, Object> context) {
            String defaultOption = getDefaultOptionThru();

            Map<String, Object> parameters = UtilGenerics.checkMap(context.get("parameters"), String.class, Object.class);
            if (UtilValidate.isNotEmpty(parameters)) {
                String fieldName = this.getModelFormField().getName();
                if( parameters.containsKey(fieldName.concat("_fld1_value"))) {
                    defaultOption = (String)parameters.get(fieldName.concat("_fld1_op"));
                }
            }
            return defaultOption;
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer)
                throws IOException {
            formStringRenderer.renderDateFindField(writer, context, this);
        }
    }

    /**
     * Models the &lt;date-time&gt; element.
     *
     * @see <code>widget-form.xsd</code>
     */
    public static class DateTimeField extends FieldInfo {
        private final String clock;
        private final FlexibleStringExpander defaultValue;
        private final String inputMethod;
        private final String mask;
        private final String step;
        private final String type;

        protected DateTimeField(DateTimeField original, ModelFormField modelFormField) {
            super(original.getFieldSource(), original.getFieldType(), modelFormField);
            this.defaultValue = original.defaultValue;
            this.type = original.type;
            this.inputMethod = original.inputMethod;
            this.clock = original.clock;
            this.mask = original.mask;
            this.step = original.step;
        }

        public DateTimeField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
            this.defaultValue = FlexibleStringExpander.getInstance(element.getAttribute("default-value"));
            this.type = element.getAttribute("type");
            this.inputMethod = element.getAttribute("input-method");
            this.clock = element.getAttribute("clock");
            this.mask = element.getAttribute("mask");
            String step = element.getAttribute("step");
            if (step.isEmpty()) {
                step = "1";
            }
            this.step = step;
        }

        public DateTimeField(int fieldSource, ModelFormField modelFormField) {
            super(fieldSource, FieldInfo.DATE_TIME, modelFormField);
            this.defaultValue = FlexibleStringExpander.getInstance("");
            this.type = "";
            this.inputMethod = "";
            this.clock = "";
            this.mask = "";
            this.step = "1";
        }

        public DateTimeField(int fieldSource, String type) {
            super(fieldSource, FieldInfo.DATE_TIME, null);
            this.defaultValue = FlexibleStringExpander.getInstance("");
            this.type = type;
            this.inputMethod = "";
            this.clock = "";
            this.mask = "";
            this.step = "1";
        }

        public DateTimeField(ModelFormField modelFormField) {
            super(FieldInfo.SOURCE_EXPLICIT, FieldInfo.DATE_TIME, modelFormField);
            this.defaultValue = FlexibleStringExpander.getInstance("");
            this.type = "";
            this.inputMethod = "";
            this.clock = "";
            this.mask = "";
            this.step = "1";
        }

        @Override
        public void accept(ModelFieldVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public FieldInfo copy(ModelFormField modelFormField) {
            return new DateTimeField(this, modelFormField);
        }

        public String getClock() {
            return this.clock;
        }

        /**
         * Returns the default-value if specified, otherwise the current date, time or timestamp
         *
         * @param context Context Map
         * @return Default value string for date-time
         */
        public String getDefaultDateTimeString(Map<String, Object> context) {
            if (UtilValidate.isNotEmpty(this.defaultValue)) {
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

        public FlexibleStringExpander getDefaultValue() {
            return defaultValue;
        }

        public String getDefaultValue(Map<String, Object> context) {
            if (this.defaultValue != null) {
                return this.defaultValue.expandString(context);
            }
            return "";
        }

        public String getInputMethod() {
            return this.inputMethod;
        }

        public String getMask() {
            return this.mask;
        }

        public String getStep() {
            return this.step;
        }

        public String getType() {
            return type;
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer)
                throws IOException {
            formStringRenderer.renderDateTimeField(writer, context, this);
        }
    }

    /**
     * Models the &lt;display-entity&gt; element.
     *
     * @see <code>widget-form.xsd</code>
     */
    public static class DisplayEntityField extends DisplayField {
        private final boolean cache;
        private final String entityName;
        private final String keyFieldName;
        private final SubHyperlink subHyperlink;

        private DisplayEntityField(DisplayEntityField original, ModelFormField modelFormField) {
            super(original, modelFormField);
            this.cache = original.cache;
            this.entityName = original.entityName;
            this.keyFieldName = original.keyFieldName;
            if (original.subHyperlink != null) {
                this.subHyperlink = new SubHyperlink(original.subHyperlink, modelFormField);
            } else {
                this.subHyperlink = null;
            }
        }

        public DisplayEntityField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
            this.cache = !"false".equals(element.getAttribute("cache"));
            this.entityName = element.getAttribute("entity-name");
            this.keyFieldName = element.getAttribute("key-field-name");
            Element subHyperlinkElement = UtilXml.firstChildElement(element, "sub-hyperlink");
            if (subHyperlinkElement != null) {
                this.subHyperlink = new SubHyperlink(subHyperlinkElement, modelFormField);
            } else {
                this.subHyperlink = null;
            }
        }

        public DisplayEntityField(int fieldSource, ModelFormField modelFormField) {
            super(fieldSource, FieldInfo.DISPLAY_ENTITY, modelFormField);
            this.cache = true;
            this.entityName = "";
            this.keyFieldName = "";
            this.subHyperlink = null;
        }

        public DisplayEntityField(ModelFormField modelFormField) {
            super(FieldInfo.SOURCE_EXPLICIT, FieldInfo.DISPLAY_ENTITY, modelFormField);
            this.cache = true;
            this.entityName = "";
            this.keyFieldName = "";
            this.subHyperlink = null;
        }

        @Override
        public void accept(ModelFieldVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public FieldInfo copy(ModelFormField modelFormField) {
            return new DisplayEntityField(this, modelFormField);
        }

        public boolean getCache() {
            return cache;
        }

        @Override
        public String getDescription(Map<String, Object> context) {
            Locale locale = UtilMisc.ensureLocale(context.get("locale"));

            // rather than using the context to expand the string, lookup the given entity and use it to expand the string
            GenericValue value = null;
            String fieldKey = this.keyFieldName;
            if (UtilValidate.isEmpty(fieldKey)) {
                fieldKey = getModelFormField().fieldName;
            }

            Delegator delegator = WidgetWorker.getDelegator(context);
            String fieldValue = getModelFormField().getEntry(context);
            try {
                value = delegator.findOne(this.entityName, this.cache, fieldKey, fieldValue);
            } catch (GenericEntityException e) {
                String errMsg = "Error getting value from the database for display of field [" + getModelFormField().getName()
                        + "] on form [" + getModelFormField().modelForm.getName() + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new IllegalArgumentException(errMsg);
            }

            String retVal = null;
            if (value != null) {
                // expanding ${} stuff, passing locale explicitly to expand value string because it won't be found in the Entity
                MapStack<String> localContext = MapStack.create(context);
                // Rendering code might try to modify the GenericEntity instance,
                // so we make a copy of it.
                Map<String, Object> genericEntityClone = UtilGenerics.cast(value.clone());
                localContext.push(genericEntityClone);

                // expand with the new localContext, which is locale aware
                retVal = this.getDescription().expandString(localContext, locale);
            }
            // try to get the entry for the field if description doesn't expand to anything
            if (UtilValidate.isEmpty(retVal)) {
                retVal = fieldValue;
            }
            if (UtilValidate.isEmpty(retVal)) {
                retVal = "";
            } else if (this.getModelFormField().getEncodeOutput()) {
                UtilCodec.SimpleEncoder simpleEncoder = (UtilCodec.SimpleEncoder) context.get("simpleEncoder");
                if (simpleEncoder != null) {
                    retVal = simpleEncoder.encode(retVal);
                }
            }
            return retVal;
        }

        public String getEntityName() {
            return entityName;
        }

        public String getKeyFieldName() {
            return keyFieldName;
        }

        public SubHyperlink getSubHyperlink() {
            return this.subHyperlink;
        }
    }

    /**
     * Models the &lt;display&gt; element.
     *
     * @see <code>widget-form.xsd</code>
     */
    public static class DisplayField extends FieldInfo {
        private final boolean alsoHidden;
        private final FlexibleStringExpander currency;
        private final FlexibleStringExpander date;
        private final FlexibleStringExpander defaultValue;
        private final FlexibleStringExpander description;
        private final FlexibleStringExpander imageLocation;
        private final InPlaceEditor inPlaceEditor;
        private final String size; // maximum number of characters to display
        private final String type; // matches type of field, currently text or currency

        protected DisplayField(DisplayField original, ModelFormField modelFormField) {
            super(original.getFieldSource(), original.getFieldType(), modelFormField);
            this.alsoHidden = original.alsoHidden;
            this.currency = original.currency;
            this.date = original.date;
            this.defaultValue = original.defaultValue;
            this.description = original.description;
            this.imageLocation = original.imageLocation;
            this.inPlaceEditor = original.inPlaceEditor;
            this.size = original.size;
            this.type = original.type;
        }

        public DisplayField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
            this.alsoHidden = !"false".equals(element.getAttribute("also-hidden"));
            this.currency = FlexibleStringExpander.getInstance(element.getAttribute("currency"));
            this.date = FlexibleStringExpander.getInstance(element.getAttribute("date"));
            this.defaultValue = FlexibleStringExpander.getInstance(element.getAttribute("default-value"));
            this.description = FlexibleStringExpander.getInstance(element.getAttribute("description"));
            this.imageLocation = FlexibleStringExpander.getInstance(element.getAttribute("image-location"));
            Element inPlaceEditorElement = UtilXml.firstChildElement(element, "in-place-editor");
            if (inPlaceEditorElement != null) {
                this.inPlaceEditor = new InPlaceEditor(inPlaceEditorElement);
            } else {
                this.inPlaceEditor = null;
            }
            this.size = element.getAttribute("size");
            this.type = element.getAttribute("type");
        }

        public DisplayField(int fieldSource, int fieldType, ModelFormField modelFormField) {
            super(fieldSource, fieldType, modelFormField);
            this.alsoHidden = true;
            this.currency = FlexibleStringExpander.getInstance("");
            this.date = FlexibleStringExpander.getInstance("");
            this.defaultValue = FlexibleStringExpander.getInstance("");
            this.description = FlexibleStringExpander.getInstance("");
            this.imageLocation = FlexibleStringExpander.getInstance("");
            this.inPlaceEditor = null;
            this.size = "";
            this.type = "";
        }

        public DisplayField(int fieldSource, ModelFormField modelFormField) {
            super(fieldSource, FieldInfo.DISPLAY, modelFormField);
            this.alsoHidden = true;
            this.currency = FlexibleStringExpander.getInstance("");
            this.date = FlexibleStringExpander.getInstance("");
            this.defaultValue = FlexibleStringExpander.getInstance("");
            this.description = FlexibleStringExpander.getInstance("");
            this.imageLocation = FlexibleStringExpander.getInstance("");
            this.inPlaceEditor = null;
            this.size = "";
            this.type = "";
        }

        public DisplayField(ModelFormField modelFormField) {
            super(FieldInfo.SOURCE_EXPLICIT, FieldInfo.DISPLAY, modelFormField);
            this.alsoHidden = true;
            this.currency = FlexibleStringExpander.getInstance("");
            this.date = FlexibleStringExpander.getInstance("");
            this.defaultValue = FlexibleStringExpander.getInstance("");
            this.description = FlexibleStringExpander.getInstance("");
            this.imageLocation = FlexibleStringExpander.getInstance("");
            this.inPlaceEditor = null;
            this.size = "";
            this.type = "";
        }

        @Override
        public void accept(ModelFieldVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public FieldInfo copy(ModelFormField modelFormField) {
            return new DisplayField(this, modelFormField);
        }

        public boolean getAlsoHidden() {
            return alsoHidden;
        }

        public FlexibleStringExpander getCurrency() {
            return currency;
        }

        public FlexibleStringExpander getDate() {
            return date;
        }

        public FlexibleStringExpander getDefaultValue() {
            return defaultValue;
        }

        public String getDefaultValue(Map<String, Object> context) {
            if (this.defaultValue != null) {
                return this.defaultValue.expandString(context);
            }
            return "";
        }

        public FlexibleStringExpander getDescription() {
            return description;
        }

        public String getDescription(Map<String, Object> context) {
            String retVal = null;
            if (UtilValidate.isNotEmpty(this.description)) {
                retVal = this.description.expandString(context);
            } else {
                retVal = getModelFormField().getEntry(context);
            }

            if (UtilValidate.isEmpty(retVal)) {
                retVal = this.getDefaultValue(context);
            } else if ("currency".equals(type)) {
                retVal = retVal.replaceAll("&nbsp;", " "); // FIXME : encoding currency is a problem for some locale, we should not have any &nbsp; in retVal other case may arise in future...
                Locale locale = (Locale) context.get("locale");
                if (locale == null) {
                    locale = Locale.getDefault();
                }
                String isoCode = null;
                if (UtilValidate.isNotEmpty(this.currency)) {
                    isoCode = this.currency.expandString(context);
                }

                try {
                    BigDecimal parsedRetVal = (BigDecimal) ObjectType.simpleTypeConvert(retVal, "BigDecimal", null, null, locale,
                            true);
                    retVal = UtilFormatOut.formatCurrency(parsedRetVal, isoCode, locale, 10); // we set the max to 10 digits as an hack to not round numbers in the ui
                } catch (GeneralException e) {
                    String errMsg = "Error formatting currency value [" + retVal + "]: " + e.toString();
                    Debug.logError(e, errMsg, module);
                    throw new IllegalArgumentException(errMsg);
                }
            } else if ("date".equals(this.type) && retVal.length() > 9) {
                Locale locale = (Locale) context.get("locale");
                if (locale == null) {
                    locale = Locale.getDefault();
                }

                StringToTimestamp stringToTimestamp = new DateTimeConverters.StringToTimestamp();
                Timestamp timestamp = null;
                try {
                    timestamp = stringToTimestamp.convert(retVal);
                    Date date = new Date(timestamp.getTime());

                    DateFormat dateFormatter = DateFormat.getDateInstance(DateFormat.SHORT, locale);
                    retVal = dateFormatter.format(date);
                } catch (ConversionException e) {
                    String errMsg = "Error formatting date using default instead [" + retVal + "]: " + e.toString();
                    Debug.logError(e, errMsg, module);
                    // create default date value from timestamp string
                    retVal = retVal.substring(0, 10);
                }

            } else if ("date-time".equals(this.type) && retVal.length() > 16) {
                Locale locale = (Locale) context.get("locale");
                TimeZone timeZone = (TimeZone) context.get("timeZone");
                if (locale == null) {
                    locale = Locale.getDefault();
                }
                if (timeZone == null) {
                    timeZone = TimeZone.getDefault();
                }

                StringToTimestamp stringToTimestamp = new DateTimeConverters.StringToTimestamp();
                Timestamp timestamp = null;
                try {
                    timestamp = stringToTimestamp.convert(retVal);
                    Date date = new Date(timestamp.getTime());

                    DateFormat dateFormatter = UtilDateTime.toDateTimeFormat(null, timeZone, locale);
                    retVal = dateFormatter.format(date);
                } catch (ConversionException e) {
                    String errMsg = "Error formatting date/time using default instead [" + retVal + "]: " + e.toString();
                    Debug.logError(e, errMsg, module);
                    // create default date/time value from timestamp string
                    retVal = retVal.substring(0, 16);
                }
            } else if ("accounting-number".equals(this.type)) {
                Locale locale = (Locale) context.get("locale");
                if (locale == null) {
                    locale = Locale.getDefault();
                }
                try {
                    Double parsedRetVal = (Double) ObjectType.simpleTypeConvert(retVal, "Double", null, locale, false);
                    String template = UtilProperties.getPropertyValue("arithmetic", "accounting-number.format",
                            "#,##0.00;(#,##0.00)");
                    retVal = UtilFormatOut.formatDecimalNumber(parsedRetVal.doubleValue(), template, locale);
                } catch (GeneralException e) {
                    String errMsg = "Error formatting number [" + retVal + "]: " + e.toString();
                    Debug.logError(e, errMsg, module);
                    throw new IllegalArgumentException(errMsg);
                }
            }
            if (UtilValidate.isNotEmpty(this.description) && retVal != null && this.getModelFormField().getEncodeOutput()) {
                UtilCodec.SimpleEncoder simpleEncoder = (UtilCodec.SimpleEncoder) context.get("simpleEncoder");
                if (simpleEncoder != null) {
                    retVal = simpleEncoder.encode(retVal);
                }
            }
            return retVal;
        }

        public FlexibleStringExpander getImageLocation() {
            return imageLocation;
        }

        public String getImageLocation(Map<String, Object> context) {
            if (this.imageLocation != null) {
                return this.imageLocation.expandString(context);
            }
            return "";
        }

        public InPlaceEditor getInPlaceEditor() {
            return this.inPlaceEditor;
        }

        public String getSize() {
            return this.size;
        }

        public String getType() {
            return this.type;
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer)
                throws IOException {
            formStringRenderer.renderDisplayField(writer, context, this);
        }
    }

    /**
     * Models the &lt;drop-down&gt; element.
     *
     * @see <code>widget-form.xsd</code>
     */
    public static class DropDownField extends FieldInfoWithOptions {
        private final boolean allowEmpty;
        private final boolean allowMulti;
        private final AutoComplete autoComplete;
        private final String current;
        private final FlexibleStringExpander currentDescription;
        private final int otherFieldSize;
        private final String size;
        private final SubHyperlink subHyperlink;
        private final String textSize;

        private DropDownField(DropDownField original, ModelFormField modelFormField) {
            super(original, modelFormField);
            this.allowEmpty = original.allowEmpty;
            this.allowMulti = original.allowMulti;
            this.autoComplete = original.autoComplete;
            this.current = original.current;
            this.currentDescription = original.currentDescription;
            this.otherFieldSize = original.otherFieldSize;
            this.size = original.size;
            if (original.subHyperlink != null) {
                this.subHyperlink = new SubHyperlink(original.subHyperlink, modelFormField);
            } else {
                this.subHyperlink = null;
            }
            this.textSize = original.textSize;
        }

        public DropDownField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
            this.allowEmpty = "true".equals(element.getAttribute("allow-empty"));
            this.allowMulti = "true".equals(element.getAttribute("allow-multiple"));
            Element autoCompleteElement = UtilXml.firstChildElement(element, "auto-complete");
            if (autoCompleteElement != null) {
                this.autoComplete = new AutoComplete(autoCompleteElement);
            } else {
                this.autoComplete = null;
            }
            this.current = element.getAttribute("current");
            this.currentDescription = FlexibleStringExpander.getInstance(element.getAttribute("current-description"));
            int otherFieldSize = 0;
            String sizeStr = element.getAttribute("other-field-size");
            if (!sizeStr.isEmpty()) {
                try {
                    otherFieldSize = Integer.parseInt(sizeStr);
                } catch (NumberFormatException e) {
                    Debug.logError("Could not parse the size value of the text element: [" + sizeStr
                            + "], setting to the default of 0", module);
                }
            }
            this.otherFieldSize = otherFieldSize;
            String size = element.getAttribute("size");
            if (size.isEmpty()) {
                size = "1";
            }
            this.size = size;
            Element subHyperlinkElement = UtilXml.firstChildElement(element, "sub-hyperlink");
            if (subHyperlinkElement != null) {
                this.subHyperlink = new SubHyperlink(subHyperlinkElement, this.getModelFormField());
            } else {
                this.subHyperlink = null;
            }
            String textSize = element.getAttribute("text-size");
            if (textSize.isEmpty()) {
                textSize = "0";
            }
            this.textSize = textSize;
        }

        public DropDownField(int fieldSource, List<OptionSource> optionSources) {
            super(fieldSource, FieldInfo.DROP_DOWN, optionSources);
            this.allowEmpty = false;
            this.allowMulti = false;
            this.autoComplete = null;
            this.current = "";
            this.currentDescription = FlexibleStringExpander.getInstance("");
            this.otherFieldSize = 0;
            this.size = "1";
            this.subHyperlink = null;
            this.textSize = "0";
        }

        public DropDownField(int fieldSource, ModelFormField modelFormField) {
            super(fieldSource, FieldInfo.DROP_DOWN, modelFormField);
            this.allowEmpty = false;
            this.allowMulti = false;
            this.autoComplete = null;
            this.current = "";
            this.currentDescription = FlexibleStringExpander.getInstance("");
            this.otherFieldSize = 0;
            this.size = "1";
            this.subHyperlink = null;
            this.textSize = "0";
        }

        public DropDownField(ModelFormField modelFormField) {
            super(FieldInfo.SOURCE_EXPLICIT, FieldInfo.DROP_DOWN, modelFormField);
            this.allowEmpty = false;
            this.allowMulti = false;
            this.autoComplete = null;
            this.current = "";
            this.currentDescription = FlexibleStringExpander.getInstance("");
            this.otherFieldSize = 0;
            this.size = "1";
            this.subHyperlink = null;
            this.textSize = "0";
        }

        @Override
        public void accept(ModelFieldVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public FieldInfo copy(ModelFormField modelFormField) {
            return new DropDownField(this, modelFormField);
        }

        public boolean getAllowMulti() {
            return allowMulti;
        }

        public AutoComplete getAutoComplete() {
            return this.autoComplete;
        }

        public String getCurrent() {
            if (UtilValidate.isEmpty(this.current)) {
                return "first-in-list";
            }
            return this.current;
        }

        public FlexibleStringExpander getCurrentDescription() {
            return currentDescription;
        }

        public String getCurrentDescription(Map<String, Object> context) {
            if (this.currentDescription == null) {
                return null;
            }
            return this.currentDescription.expandString(context);
        }

        public int getOtherFieldSize() {
            return this.otherFieldSize;
        }

        /**
         * Get the name to use for the parameter for this field in the form interpreter.
         * For HTML forms this is the request parameter name.
         * @param context the context
         * @return returns the name to use for the parameter for this field in the form interpreter.
         */
        public String getParameterNameOther(Map<String, Object> context) {
            String baseName;
            if (UtilValidate.isNotEmpty(getModelFormField().parameterName)) {
                baseName = getModelFormField().parameterName;
            } else {
                baseName = getModelFormField().name;
            }

            baseName += "_OTHER";
            Integer itemIndex = (Integer) context.get("itemIndex");
            if (itemIndex != null && "multi".equals(getModelFormField().modelForm.getType())) {
                return baseName + getModelFormField().modelForm.getItemIndexSeparator() + itemIndex.intValue();
            }
            return baseName;
        }

        public String getSize() {
            return this.size;
        }

        public SubHyperlink getSubHyperlink() {
            return this.subHyperlink;
        }

        public String getTextSize() {
            return this.textSize;
        }

        public boolean getAllowEmpty() {
            return this.allowEmpty;
        }

        public boolean getAllowMultiple() {
            return this.allowMulti;
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer)
                throws IOException {
            formStringRenderer.renderDropDownField(writer, context, this);
        }
    }

    /**
     * Models the &lt;entity-options&gt; element.
     *
     * @see <code>widget-form.xsd</code>
     */
    public static class EntityOptions extends OptionSource {
        private final boolean cache;
        private final List<EntityFinderUtil.ConditionExpr> constraintList;
        private final FlexibleStringExpander description;
        private final String entityName;
        private final String filterByDate;
        private final String keyFieldName;
        private final List<String> orderByList;

        public EntityOptions(Element entityOptionsElement, ModelFormField modelFormField) {
            super(modelFormField);
            this.cache = !"false".equals(entityOptionsElement.getAttribute("cache"));
            List<? extends Element> constraintElements = UtilXml.childElementList(entityOptionsElement, "entity-constraint");
            if (!constraintElements.isEmpty()) {
                List<EntityFinderUtil.ConditionExpr> constraintList = new ArrayList<>(
                        constraintElements.size());
                for (Element constraintElement : constraintElements) {
                    constraintList.add(new EntityFinderUtil.ConditionExpr(constraintElement));
                }
                this.constraintList = Collections.unmodifiableList(constraintList);
            } else {
                this.constraintList = Collections.emptyList();
            }
            this.description = FlexibleStringExpander.getInstance(entityOptionsElement.getAttribute("description"));
            this.entityName = entityOptionsElement.getAttribute("entity-name");
            this.filterByDate = entityOptionsElement.getAttribute("filter-by-date");
            this.keyFieldName = entityOptionsElement.getAttribute("key-field-name");
            List<? extends Element> orderByElements = UtilXml.childElementList(entityOptionsElement, "entity-order-by");
            if (!orderByElements.isEmpty()) {
                List<String> orderByList = new ArrayList<>(orderByElements.size());
                for (Element orderByElement : orderByElements) {
                    orderByList.add(orderByElement.getAttribute("field-name"));
                }
                this.orderByList = Collections.unmodifiableList(orderByList);
            } else {
                this.orderByList = Collections.emptyList();
            }
        }

        private EntityOptions(EntityOptions original, ModelFormField modelFormField) {
            super(modelFormField);
            this.cache = original.cache;
            this.constraintList = original.constraintList;
            this.description = original.description;
            this.entityName = original.entityName;
            this.filterByDate = original.filterByDate;
            this.keyFieldName = original.keyFieldName;
            this.orderByList = original.orderByList;
        }

        public EntityOptions(ModelFormField modelFormField) {
            super(modelFormField);
            this.cache = true;
            this.constraintList = Collections.emptyList();
            this.description = FlexibleStringExpander.getInstance("");
            this.entityName = "";
            this.filterByDate = "";
            this.keyFieldName = "";
            this.orderByList = Collections.emptyList();
        }

        @Override
        public void addOptionValues(List<OptionValue> optionValues, Map<String, Object> context, Delegator delegator) {
            // first expand any conditions that need expanding based on the current context
            EntityCondition findCondition = null;
            if (UtilValidate.isNotEmpty(this.constraintList)) {
                List<EntityCondition> expandedConditionList = new LinkedList<>();
                for (EntityFinderUtil.Condition condition : constraintList) {
                    ModelEntity modelEntity = delegator.getModelEntity(this.entityName);
                    if (modelEntity == null) {
                        throw new IllegalArgumentException("Error in entity-options: could not find entity [" + this.entityName
                                + "]");
                    }
                    EntityCondition createdCondition = condition.createCondition(context, modelEntity,
                            delegator.getModelFieldTypeReader(modelEntity));
                    if (createdCondition != null) {
                        expandedConditionList.add(createdCondition);
                    }
                }
                findCondition = EntityCondition.makeCondition(expandedConditionList);
            }

            try {
                Locale locale = UtilMisc.ensureLocale(context.get("locale"));
                ModelEntity modelEntity = delegator.getModelEntity(this.entityName);
                Boolean localizedOrderBy = UtilValidate.isNotEmpty(this.orderByList)
                        && ModelUtil.isPotentialLocalizedFields(modelEntity, this.orderByList);

                List<GenericValue> values = null;
                if (!localizedOrderBy) {
                    values = delegator.findList(this.entityName, findCondition, null, this.orderByList, null, this.cache);
                } else {
                    //if entity has localized label
                    values = delegator.findList(this.entityName, findCondition, null, null, null, this.cache);
                    values = EntityUtil.localizedOrderBy(values, this.orderByList, locale);
                }

                // filter-by-date if requested
                if ("true".equals(this.filterByDate)) {
                    values = EntityUtil.filterByDate(values, true);
                } else if (!"false".equals(this.filterByDate)) {
                    // not explicitly true or false, check to see if has fromDate and thruDate, if so do the filter
                    if (modelEntity != null && modelEntity.isField("fromDate") && modelEntity.isField("thruDate")) {
                        values = EntityUtil.filterByDate(values, true);
                    }
                }

                for (GenericValue value : values) {
                    // add key and description with string expansion, ie expanding ${} stuff, passing locale explicitly to expand value string because it won't be found in the Entity
                    MapStack<String> localContext = MapStack.create(context);
                    // Rendering code might try to modify the GenericEntity instance,
                    // so we make a copy of it.
                    Map<String, Object> genericEntityClone = UtilGenerics.cast(value.clone());
                    localContext.push(genericEntityClone);

                    // expand with the new localContext, which is locale aware
                    String optionDesc = this.description.expandString(localContext, locale);

                    Object keyFieldObject = value.get(this.getKeyFieldName());
                    if (keyFieldObject == null) {
                        throw new IllegalArgumentException(
                                "The entity-options identifier (from key-name attribute, or default to the field name) ["
                                        + this.getKeyFieldName() + "], may not be a valid key field name for the entity ["
                                        + this.entityName + "].");
                    }
                    String keyFieldValue = keyFieldObject.toString();
                    optionValues.add(new OptionValue(keyFieldValue, optionDesc));
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error getting entity options in form", module);
            }
        }

        @Override
        public OptionSource copy(ModelFormField modelFormField) {
            return new EntityOptions(this, modelFormField);
        }

        public boolean getCache() {
            return cache;
        }

        public List<EntityFinderUtil.ConditionExpr> getConstraintList() {
            return constraintList;
        }

        public FlexibleStringExpander getDescription() {
            return description;
        }

        public String getEntityName() {
            return entityName;
        }

        public String getFilterByDate() {
            return filterByDate;
        }

        public String getKeyFieldName() {
            if (UtilValidate.isNotEmpty(this.keyFieldName)) {
                return this.keyFieldName;
            }
            return getModelFormField().getFieldName(); // get the modelFormField fieldName
        }

        public List<String> getOrderByList() {
            return orderByList;
        }
    }

    public static abstract class FieldInfoWithOptions extends FieldInfo {

        public static String getDescriptionForOptionKey(String key, List<OptionValue> allOptionValues) {
            if (UtilValidate.isEmpty(key)) {
                return "";
            }

            if (UtilValidate.isEmpty(allOptionValues)) {
                return key;
            }

            for (OptionValue optionValue : allOptionValues) {
                if (key.equals(optionValue.getKey())) {
                    return optionValue.getDescription();
                }
            }

            // if we get here we didn't find a match, just return the key
            return key;
        }

        private final FlexibleStringExpander noCurrentSelectedKey;
        private final List<OptionSource> optionSources;

        public FieldInfoWithOptions(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
            this.noCurrentSelectedKey = FlexibleStringExpander.getInstance(element.getAttribute("no-current-selected-key"));
            // read all option and entity-options sub-elements, maintaining order
            ArrayList<OptionSource> optionSources = new ArrayList<>();
            List<? extends Element> childElements = UtilXml.childElementList(element);
            if (childElements.size() > 0) {
                for (Element childElement : childElements) {
                    if ("option".equals(childElement.getTagName())) {
                        optionSources.add(new SingleOption(childElement, modelFormField));
                    } else if ("list-options".equals(childElement.getTagName())) {
                        optionSources.add(new ListOptions(childElement, modelFormField));
                    } else if ("entity-options".equals(childElement.getTagName())) {
                        optionSources.add(new EntityOptions(childElement, modelFormField));
                    }
                }
            } else {
                // this must be added or the multi-form select box options would not show up
                optionSources.add(new SingleOption("Y", " ", modelFormField));
            }
            optionSources.trimToSize();
            this.optionSources = Collections.unmodifiableList(optionSources);
        }

        // Copy constructor.
        protected FieldInfoWithOptions(FieldInfoWithOptions original, ModelFormField modelFormField) {
            super(original.getFieldSource(), original.getFieldType(), modelFormField);
            this.noCurrentSelectedKey = original.noCurrentSelectedKey;
            if (original.optionSources.isEmpty()) {
                this.optionSources = original.optionSources;
            } else {
                List<OptionSource> optionSources = new ArrayList<>(original.optionSources.size());
                for (OptionSource source : original.optionSources) {
                    optionSources.add(source.copy(modelFormField));
                }
                this.optionSources = Collections.unmodifiableList(optionSources);
            }
        }

        protected FieldInfoWithOptions(int fieldSource, int fieldType, List<OptionSource> optionSources) {
            super(fieldSource, fieldType, null);
            this.noCurrentSelectedKey = FlexibleStringExpander.getInstance("");
            this.optionSources = Collections.unmodifiableList(new ArrayList<>(optionSources));
        }

        public FieldInfoWithOptions(int fieldSource, int fieldType, ModelFormField modelFormField) {
            super(fieldSource, fieldType, modelFormField);
            this.noCurrentSelectedKey = FlexibleStringExpander.getInstance("");
            this.optionSources = Collections.emptyList();
        }

        public List<OptionValue> getAllOptionValues(Map<String, Object> context, Delegator delegator) {
            List<OptionValue> optionValues = new LinkedList<>();
            for (OptionSource optionSource : this.optionSources) {
                optionSource.addOptionValues(optionValues, context, delegator);
            }
            return optionValues;
        }

        public FlexibleStringExpander getNoCurrentSelectedKey() {
            return noCurrentSelectedKey;
        }

        public String getNoCurrentSelectedKey(Map<String, Object> context) {
            return this.noCurrentSelectedKey.expandString(context);
        }

        public List<OptionSource> getOptionSources() {
            return optionSources;
        }
    }

    /**
     * Models the &lt;file&gt; element.
     *
     * @see <code>widget-form.xsd</code>
     */
    public static class FileField extends TextField {

        public FileField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
        }

        private FileField(FileField original, ModelFormField modelFormField) {
            super(original, modelFormField);
        }

        public FileField(int fieldSource, ModelFormField modelFormField) {
            super(fieldSource, FieldInfo.FILE, modelFormField);
        }

        @Override
        public void accept(ModelFieldVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public FieldInfo copy(ModelFormField modelFormField) {
            return new FileField(this, modelFormField);
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer)
                throws IOException {
            formStringRenderer.renderFileField(writer, context, this);
        }
    }

    /**
     * Models the &lt;include-form&gt; element.
     *
     * @see <code>widget-form.xsd</code>
     */
    public static class FormField extends FieldInfo {
        private final FlexibleStringExpander formName;
        private final FlexibleStringExpander formLocation;

        public FormField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
            this.formName = FlexibleStringExpander.getInstance(element.getAttribute("name"));
            this.formLocation = FlexibleStringExpander.getInstance(element.getAttribute("location"));
        }

        private FormField(FormField original, ModelFormField modelFormField) {
            super(original.getFieldSource(), original.getFieldType(), modelFormField);
            this.formName = original.formName;
            this.formLocation = original.formLocation;
        }

        @Override
        public void accept(ModelFieldVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public FieldInfo copy(ModelFormField modelFormField) {
            return new FormField(this, modelFormField);
        }

        public String getFormName(Map<String, Object> context) {
            return this.formName.expandString(context);
        }

        public FlexibleStringExpander getFormName() {
            return formName;
        }

        public String getFormLocation(Map<String, Object> context) {
            return this.formLocation.expandString(context);
        }

        public FlexibleStringExpander getFormLocation() {
            return formLocation;
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer)
                throws IOException {
            // Output format might not support menus, so make menu rendering optional.
            ModelForm modelForm = getModelForm(context);
            try {
                FormRenderer renderer = new FormRenderer(modelForm, formStringRenderer);
                renderer.render(writer, context);
            } catch (Exception e) {
                String errMsg = "Error rendering included form named [" + modelForm.getName() + "] at location [" + modelForm.getFormLocation() + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new RuntimeException(errMsg + e);
            }
        }

        public ModelForm getModelForm(Map<String, Object> context) {
            String name = this.getFormName(context);
            String location = this.getFormLocation(context);
            ModelForm modelForm = null;
            try {
                org.apache.ofbiz.entity.model.ModelReader entityModelReader = ((org.apache.ofbiz.entity.Delegator)context.get("delegator")).getModelReader();
                org.apache.ofbiz.service.DispatchContext dispatchContext = ((org.apache.ofbiz.service.LocalDispatcher)context.get("dispatcher")).getDispatchContext();
                modelForm = FormFactory.getFormFromLocation(location, name, entityModelReader, dispatchContext);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                String errMsg = "Error rendering form named [" + name + "] at location [" + location + "]: ";
                Debug.logError(e, errMsg, module);
                throw new RuntimeException(errMsg + e);
            }
            return modelForm;
        }
    }

    /**
     * Models the &lt;include-grid&gt; element.
     *
     * @see <code>widget-form.xsd</code>
     */
    public static class GridField extends FieldInfo {
        private final FlexibleStringExpander gridName;
        private final FlexibleStringExpander gridLocation;

        public GridField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
            this.gridName = FlexibleStringExpander.getInstance(element.getAttribute("name"));
            this.gridLocation = FlexibleStringExpander.getInstance(element.getAttribute("location"));
        }

        private GridField(GridField original, ModelFormField modelFormField) {
            super(original.getFieldSource(), original.getFieldType(), modelFormField);
            this.gridName = original.gridName;
            this.gridLocation = original.gridLocation;
        }

        @Override
        public void accept(ModelFieldVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public FieldInfo copy(ModelFormField modelFormField) {
            return new GridField(this, modelFormField);
        }

        public String getGridName(Map<String, Object> context) {
            return this.gridName.expandString(context);
        }

        public FlexibleStringExpander getGridName() {
            return gridName;
        }

        public String getGridLocation(Map<String, Object> context) {
            return this.gridLocation.expandString(context);
        }

        public FlexibleStringExpander getGridLocation() {
            return gridLocation;
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer)
                throws IOException {
            // Output format might not support menus, so make menu rendering optional.
            ModelForm modelGrid = getModelGrid(context);
            try {
                FormRenderer renderer = new FormRenderer(modelGrid, formStringRenderer);
                renderer.render(writer, context);
            } catch (Exception e) {
                String errMsg = "Error rendering included grid named [" + modelGrid.getName() + "] at location [" + modelGrid.getFormLocation() + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new RuntimeException(errMsg + e);
            }
        }

        public ModelForm getModelGrid(Map<String, Object> context) {
            String name = this.getGridName(context);
            String location = this.getGridLocation(context);
            ModelForm modelForm = null;
            try {
                org.apache.ofbiz.entity.model.ModelReader entityModelReader = ((org.apache.ofbiz.entity.Delegator)context.get("delegator")).getModelReader();
                org.apache.ofbiz.service.DispatchContext dispatchContext = ((org.apache.ofbiz.service.LocalDispatcher)context.get("dispatcher")).getDispatchContext();
                modelForm = GridFactory.getGridFromLocation(location, name, entityModelReader, dispatchContext);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                String errMsg = "Error rendering grid named [" + name + "] at location [" + location + "]: ";
                Debug.logError(e, errMsg, module);
                throw new RuntimeException(errMsg + e);
            }
            return modelForm;
        }
    }

    /**
     * Models the &lt;hidden&gt; element.
     *
     * @see <code>widget-form.xsd</code>
     */
    public static class HiddenField extends FieldInfo {
        private final FlexibleStringExpander value;

        public HiddenField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
            this.value = FlexibleStringExpander.getInstance(element.getAttribute("value"));
        }

        private HiddenField(HiddenField original, ModelFormField modelFormField) {
            super(original.getFieldSource(), original.getFieldType(), modelFormField);
            this.value = original.value;
        }

        public HiddenField(int fieldSource, ModelFormField modelFormField) {
            super(fieldSource, FieldInfo.HIDDEN, modelFormField);
            this.value = FlexibleStringExpander.getInstance("");
        }

        public HiddenField(ModelFormField modelFormField) {
            super(FieldInfo.SOURCE_EXPLICIT, FieldInfo.HIDDEN, modelFormField);
            this.value = FlexibleStringExpander.getInstance("");
        }

        @Override
        public void accept(ModelFieldVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public FieldInfo copy(ModelFormField modelFormField) {
            return new HiddenField(this, modelFormField);
        }

        public FlexibleStringExpander getValue() {
            return value;
        }

        public String getValue(Map<String, Object> context) {
            if (UtilValidate.isNotEmpty(this.value)) {
                String valueEnc = this.value.expandString(context);
                UtilCodec.SimpleEncoder simpleEncoder = (UtilCodec.SimpleEncoder) context.get("simpleEncoder");
                if (simpleEncoder != null) {
                    valueEnc = simpleEncoder.encode(valueEnc);
                }
                return valueEnc;
            }
            return getModelFormField().getEntry(context);
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer)
                throws IOException {
            formStringRenderer.renderHiddenField(writer, context, this);
        }
    }

    /**
     * Models the &lt;hyperlink&gt; element.
     *
     * @see <code>widget-form.xsd</code>
     */
    public static class HyperlinkField extends FieldInfo {

        private final boolean alsoHidden;
        private final FlexibleStringExpander description;
        private final Link link;

        public HyperlinkField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
            this.alsoHidden = !"false".equals(element.getAttribute("also-hidden"));
            this.description = FlexibleStringExpander.getInstance(element.getAttribute("description"));
            // Backwards-compatible fix
            element.setAttribute("url-mode", element.getAttribute("target-type"));
            this.link = new Link(element);
        }

        private HyperlinkField(HyperlinkField original, ModelFormField modelFormField) {
            super(original.getFieldSource(), original.getFieldType(), modelFormField);
            this.alsoHidden = original.alsoHidden;
            this.description = original.description;
            this.link = original.link;
        }

        @Override
        public void accept(ModelFieldVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public FieldInfo copy(ModelFormField modelFormField) {
            return new HyperlinkField(this, modelFormField);
        }

        public boolean getAlsoHidden() {
            return this.alsoHidden;
        }

        public String getConfirmation(Map<String, Object> context) {
            String message = getConfirmationMsg(context);
            if (UtilValidate.isNotEmpty(message)) {
                return message;
            }
            if (getRequestConfirmation()) {
                String defaultMessage = UtilProperties.getPropertyValue("general", "default.confirmation.message",
                        "${uiLabelMap.CommonConfirm}");
                return FlexibleStringExpander.expandString(defaultMessage, context);
            }
            return "";
        }

        public String getAlternate(Map<String, Object> context) {
            if (link.getImage() != null) {
                return link.getImage().getAlt(context);
            }
            return "";
        }

        public String getImageTitle(Map<String, Object> context) {
            if (link.getImage() != null) {
                return link.getImage().getTitleExdr().expandString(context);
            }
            return "";
        }

        public String getImageLocation(Map<String, Object> context) {
            if (link.getImage() != null) {
                return link.getImage().getSrc(context);
            }
            return "";
        }

        public String getConfirmationMsg(Map<String, Object> context) {
            return link.getConfirmationMsg(context);
        }

        public FlexibleStringExpander getConfirmationMsgExdr() {
            return link.getConfirmationMsgExdr();
        }

        public FlexibleStringExpander getDescription() {
            return description;
        }

        public String getDescription(Map<String, Object> context) {
            return this.description.expandString(context);
        }

        public boolean getRequestConfirmation() {
            return link.getRequestConfirmation();
        }

        public Link getLink() {
            return link;
        }

        public AutoEntityParameters getAutoEntityParameters() {
            return link.getAutoEntityParameters();
        }

        public AutoServiceParameters getAutoServiceParameters() {
            return link.getAutoServiceParameters();
        }

        public boolean getEncode() {
            return link.getEncode();
        }

        public boolean getFullPath() {
            return link.getFullPath();
        }

        public String getHeight() {
            return link.getHeight();
        }

        public String getId(Map<String, Object> context) {
            return link.getId(context);
        }

        public FlexibleStringExpander getIdExdr() {
            return link.getIdExdr();
        }

        public Image getImage() {
            return link.getImage();
        }

        public String getLinkType() {
            return link.getLinkType();
        }

        public String getName() {
            return link.getName();
        }

        public String getName(Map<String, Object> context) {
            return link.getName(context);
        }

        public FlexibleStringExpander getNameExdr() {
            return link.getNameExdr();
        }

        public List<Parameter> getParameterList() {
            return link.getParameterList();
        }

        public Map<String, String> getParameterMap(Map<String, Object> context, String defaultEntityName, String defaultServiceName) {
            return link.getParameterMap(context, defaultEntityName, defaultServiceName);
        }

        public Map<String, String> getParameterMap(Map<String, Object> context) {
            return link.getParameterMap(context);
        }

        public String getPrefix(Map<String, Object> context) {
            return link.getPrefix(context);
        }

        public FlexibleStringExpander getPrefixExdr() {
            return link.getPrefixExdr();
        }

        public boolean getSecure() {
            return link.getSecure();
        }

        public Integer getSize() {
            return link.getSize();
        }

        public String getStyle(Map<String, Object> context) {
            return link.getStyle(context);
        }

        public FlexibleStringExpander getStyleExdr() {
            return link.getStyleExdr();
        }

        public String getTarget(Map<String, Object> context) {
            return link.getTarget(context);
        }

        public FlexibleStringExpander getTargetExdr() {
            return link.getTargetExdr();
        }

        public String getTargetWindow(Map<String, Object> context) {
            return link.getTargetWindow(context);
        }

        public FlexibleStringExpander getTargetWindowExdr() {
            return link.getTargetWindowExdr();
        }

        public String getText(Map<String, Object> context) {
            return link.getText(context);
        }

        public FlexibleStringExpander getTextExdr() {
            return link.getTextExdr();
        }

        public String getUrlMode() {
            return link.getUrlMode();
        }

        public String getWidth() {
            return link.getWidth();
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer)
                throws IOException {
            formStringRenderer.renderHyperlinkField(writer, context, this);
        }
    }

    /**
     * Models the &lt;ignored&gt; element.
     *
     * @see <code>widget-form.xsd</code>
     */
    public static class IgnoredField extends FieldInfo {

        public IgnoredField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
        }

        private IgnoredField(IgnoredField original, ModelFormField modelFormField) {
            super(original.getFieldSource(), original.getFieldType(), modelFormField);
        }

        public IgnoredField(int fieldSource, ModelFormField modelFormField) {
            super(fieldSource, FieldInfo.IGNORED, modelFormField);
        }

        public IgnoredField(ModelFormField modelFormField) {
            super(FieldInfo.SOURCE_EXPLICIT, FieldInfo.IGNORED, modelFormField);
        }

        @Override
        public void accept(ModelFieldVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public FieldInfo copy(ModelFormField modelFormField) {
            return new IgnoredField(this, modelFormField);
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer)
                throws IOException {
            formStringRenderer.renderIgnoredField(writer, context, this);
        }
    }

    /**
     * Models the &lt;image&gt; element.
     *
     * @see <code>widget-form.xsd</code>
     */
    public static class ImageField extends FieldInfo {
        private final FlexibleStringExpander alternate;
        private final FlexibleStringExpander defaultValue;
        private final FlexibleStringExpander description;
        private final FlexibleStringExpander style;
        private final SubHyperlink subHyperlink;
        private final FlexibleStringExpander value;

        public ImageField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
            this.alternate = FlexibleStringExpander.getInstance(element.getAttribute("alternate"));
            this.defaultValue = FlexibleStringExpander.getInstance(element.getAttribute("default-value"));
            this.description = FlexibleStringExpander.getInstance(element.getAttribute("description"));
            this.style = FlexibleStringExpander.getInstance(element.getAttribute("style"));
            Element subHyperlinkElement = UtilXml.firstChildElement(element, "sub-hyperlink");
            if (subHyperlinkElement != null) {
                this.subHyperlink = new SubHyperlink(subHyperlinkElement, modelFormField);
            } else {
                this.subHyperlink = null;
            }
            this.value = FlexibleStringExpander.getInstance(element.getAttribute("value"));
        }

        public ImageField(ImageField original, ModelFormField modelFormField) {
            super(original.getFieldSource(), original.getFieldType(), modelFormField);
            this.alternate = original.alternate;
            this.defaultValue = original.defaultValue;
            this.description = original.description;
            this.style = original.style;
            if (original.subHyperlink != null) {
                this.subHyperlink = new SubHyperlink(original.subHyperlink, modelFormField);
            } else {
                this.subHyperlink = null;
            }
            this.value = original.value;
        }

        public ImageField(int fieldSource, ModelFormField modelFormField) {
            super(fieldSource, FieldInfo.IMAGE, modelFormField);
            this.alternate = FlexibleStringExpander.getInstance("");
            this.defaultValue = FlexibleStringExpander.getInstance("");
            this.description = FlexibleStringExpander.getInstance("");
            this.style = FlexibleStringExpander.getInstance("");
            this.subHyperlink = null;
            this.value = FlexibleStringExpander.getInstance("");
        }

        public ImageField(ModelFormField modelFormField) {
            this(FieldInfo.SOURCE_EXPLICIT, modelFormField);
        }

        @Override
        public void accept(ModelFieldVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public FieldInfo copy(ModelFormField modelFormField) {
            return new ImageField(this, modelFormField);
        }

        public FlexibleStringExpander getAlternate() {
            return alternate;
        }

        public String getAlternate(Map<String, Object> context) {
            if (UtilValidate.isNotEmpty(this.alternate)) {
                return this.alternate.expandString(context);
            }
            return "";
        }

        public FlexibleStringExpander getDefaultValue() {
            return defaultValue;
        }

        public String getDefaultValue(Map<String, Object> context) {
            if (this.defaultValue != null) {
                return this.defaultValue.expandString(context);
            }
            return "";
        }

        public FlexibleStringExpander getDescription() {
            return description;
        }

        public String getDescription(Map<String, Object> context) {
            if (UtilValidate.isNotEmpty(this.description)) {
                return this.description.expandString(context);
            }
            return "";
        }

        public FlexibleStringExpander getStyle() {
            return style;
        }

        public String getStyle(Map<String, Object> context) {
            if (UtilValidate.isNotEmpty(this.style)) {
                return this.style.expandString(context);
            }
            return "";
        }

        public SubHyperlink getSubHyperlink() {
            return this.subHyperlink;
        }

        public FlexibleStringExpander getValue() {
            return value;
        }

        public String getValue(Map<String, Object> context) {
            if (UtilValidate.isNotEmpty(this.value)) {
                return this.value.expandString(context);
            }
            return getModelFormField().getEntry(context);
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer)
                throws IOException {
            formStringRenderer.renderImageField(writer, context, this);
        }
    }

    /**
     * Models the &lt;in-place-editor&gt; element.
     *
     * @see <code>widget-form.xsd</code>
     */
    public static class InPlaceEditor {
        private final String cancelControl;
        private final String cancelText;
        private final String clickToEditText;
        private final String cols;
        private final Map<FlexibleMapAccessor<Object>, Object> fieldMap;
        private final String fieldPostCreation;
        private final String formClassName;
        private final String highlightColor;
        private final String highlightEndColor;
        private final String hoverClassName;
        private final String htmlResponse;
        private final String loadingClassName;
        private final String loadingText;
        private final String okControl;
        private final String okText;
        private final String paramName;
        private final String rows;
        private final String savingClassName;
        private final String savingText;
        private final String submitOnBlur;
        private final String textAfterControls;
        private final String textBeforeControls;
        private final String textBetweenControls;
        private final String updateAfterRequestCall;
        private final FlexibleStringExpander url;

        public InPlaceEditor(Element element) {
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
            if (simpleElement != null) {
                this.rows = simpleElement.getAttribute("rows");
                this.cols = simpleElement.getAttribute("cols");
            } else {
                this.rows = "";
                this.cols = "";
            }
            this.fieldMap = EntityFinderUtil.makeFieldMap(element);
            this.url = FlexibleStringExpander.getInstance(element.getAttribute("url"));
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

        public String getCols() {
            return this.cols;
        }

        public Map<FlexibleMapAccessor<Object>, Object> getFieldMap() {
            return fieldMap;
        }

        public Map<String, Object> getFieldMap(Map<String, Object> context) {
            Map<String, Object> inPlaceEditorContext = new HashMap<>();
            EntityFinderUtil.expandFieldMapToContext(this.fieldMap, context, inPlaceEditorContext);
            return inPlaceEditorContext;
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

        public String getRows() {
            return this.rows;
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

        public String getTextAfterControls() {
            return this.textAfterControls;
        }

        public String getTextBeforeControls() {
            return this.textBeforeControls;
        }

        public String getTextBetweenControls() {
            return this.textBetweenControls;
        }

        public String getUpdateAfterRequestCall() {
            return this.updateAfterRequestCall;
        }

        public FlexibleStringExpander getUrl() {
            return url;
        }

        public String getUrl(Map<String, Object> context) {
            if (this.url != null) {
                return this.url.expandString(context);
            }
            return "";
        }
    }

    /**
     * Models the &lt;list-options&gt; element.
     *
     * @see <code>widget-form.xsd</code>
     */
    public static class ListOptions extends OptionSource {
        private final FlexibleStringExpander description;
        private final FlexibleMapAccessor<Object> keyAcsr;
        private final FlexibleMapAccessor<List<? extends Object>> listAcsr;
        private final String listEntryName;

        public ListOptions(Element optionElement, ModelFormField modelFormField) {
            super(modelFormField);
            this.listEntryName = optionElement.getAttribute("list-entry-name");
            this.keyAcsr = FlexibleMapAccessor.getInstance(optionElement.getAttribute("key-name"));
            this.listAcsr = FlexibleMapAccessor.getInstance(optionElement.getAttribute("list-name"));
            this.description = FlexibleStringExpander.getInstance(optionElement.getAttribute("description"));
        }

        private ListOptions(ListOptions original, ModelFormField modelFormField) {
            super(modelFormField);
            this.listAcsr = original.listAcsr;
            this.listEntryName = original.listEntryName;
            this.keyAcsr = original.keyAcsr;
            this.description = original.description;
        }

        public ListOptions(String listName, String listEntryName, String keyName, String description,
                ModelFormField modelFormField) {
            super(modelFormField);
            this.listAcsr = FlexibleMapAccessor.getInstance(listName);
            this.listEntryName = listEntryName;
            this.keyAcsr = FlexibleMapAccessor.getInstance(keyName);
            this.description = FlexibleStringExpander.getInstance(description);
        }

        @Override
        public void addOptionValues(List<OptionValue> optionValues, Map<String, Object> context, Delegator delegator) {
            List<? extends Object> dataList = UtilGenerics.checkList(this.listAcsr.get(context));
            if (dataList != null && dataList.size() != 0) {
                for (Object data : dataList) {
                    Map<String, Object> localContext = new HashMap<>();
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
                            String errMsg = "Could not convert field value for the field: [" + this.keyAcsr.toString()
                                    + "] to String for the value [" + keyObj + "]: " + e.toString();
                            Debug.logError(e, errMsg, module);
                        }
                    }
                    optionValues.add(new OptionValue(key, description.expandString(localContext)));
                }
            }
        }

        @Override
        public OptionSource copy(ModelFormField modelFormField) {
            return new ListOptions(this, modelFormField);
        }

        public FlexibleStringExpander getDescription() {
            return description;
        }

        public FlexibleMapAccessor<Object> getKeyAcsr() {
            return keyAcsr;
        }

        public FlexibleMapAccessor<List<? extends Object>> getListAcsr() {
            return listAcsr;
        }

        public String getListEntryName() {
            return listEntryName;
        }
    }

    /**
     * Models the &lt;lookup&gt; element.
     *
     * @see <code>widget-form.xsd</code>
     */
    public static class LookupField extends TextField {
        private final String descriptionFieldName;
        private final String fadeBackground;
        private final FlexibleStringExpander formName;
        private final String initiallyCollapsed;
        private final String lookupHeight;
        private final String lookupPosition;
        private final String lookupPresentation;
        private final String lookupWidth;
        private final String showDescription;
        private final String targetParameter;

        public LookupField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
            this.descriptionFieldName = element.getAttribute("description-field-name");
            this.fadeBackground = element.getAttribute("fade-background");
            this.formName = FlexibleStringExpander.getInstance(element.getAttribute("target-form-name"));
            this.initiallyCollapsed = element.getAttribute("initially-collapsed");
            this.lookupHeight = element.getAttribute("height");
            this.lookupPosition = element.getAttribute("position");
            this.lookupPresentation = element.getAttribute("presentation");
            this.lookupWidth = element.getAttribute("width");
            this.showDescription = element.getAttribute("show-description");
            this.targetParameter = element.getAttribute("target-parameter");
        }

        public LookupField(int fieldSource, ModelFormField modelFormField) {
            super(fieldSource, FieldInfo.LOOKUP, modelFormField);
            this.descriptionFieldName = "";
            this.fadeBackground = "";
            this.formName = FlexibleStringExpander.getInstance("");
            this.initiallyCollapsed = "";
            this.lookupHeight = "";
            this.lookupPosition = "";
            this.lookupPresentation = "";
            this.lookupWidth = "";
            this.showDescription = "";
            this.targetParameter = "";
        }

        public LookupField(LookupField original, ModelFormField modelFormField) {
            super(original, modelFormField);
            this.descriptionFieldName = original.descriptionFieldName;
            this.fadeBackground = original.fadeBackground;
            this.formName = original.formName;
            this.initiallyCollapsed = original.initiallyCollapsed;
            this.lookupHeight = original.lookupHeight;
            this.lookupPosition = original.lookupPosition;
            this.lookupPresentation = original.lookupPresentation;
            this.lookupWidth = original.lookupWidth;
            this.showDescription = original.showDescription;
            this.targetParameter = original.targetParameter;
        }

        @Override
        public void accept(ModelFieldVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public FieldInfo copy(ModelFormField modelFormField) {
            return new LookupField(this, modelFormField);
        }

        public String getDescriptionFieldName() {
            return this.descriptionFieldName;
        }

        public String getFadeBackground() {
            return this.fadeBackground;
        }

        public FlexibleStringExpander getFormName() {
            return formName;
        }

        public String getFormName(Map<String, Object> context) {
            return this.formName.expandString(context);
        }

        //initially-collapsed status
        public boolean getInitiallyCollapsed() {
            return "true".equals(this.initiallyCollapsed);
        }

        public String getLookupHeight() {
            return this.lookupHeight;
        }

        public String getLookupPosition() {
            return this.lookupPosition;
        }

        public String getLookupPresentation() {
            return this.lookupPresentation;
        }

        public String getLookupWidth() {
            return this.lookupWidth;
        }

        public Boolean getShowDescription() {
            return UtilValidate.isEmpty(this.showDescription) ? null : "true".equals(this.showDescription);
        }

        public String getTargetParameter() {
            return targetParameter;
        }

        public List<String> getTargetParameterList() {
            List<String> paramList = new LinkedList<>();
            if (UtilValidate.isNotEmpty(this.targetParameter)) {
                StringTokenizer stk = new StringTokenizer(this.targetParameter, ", ");
                while (stk.hasMoreTokens()) {
                    paramList.add(stk.nextToken());
                }
            }
            return paramList;
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer)
                throws IOException {
            formStringRenderer.renderLookupField(writer, context, this);
        }
    }

    /**
     * Models the &lt;include-menu&gt; element.
     *
     * @see <code>widget-form.xsd</code>
     */
    public static class MenuField extends FieldInfo {
        private final FlexibleStringExpander menuName;
        private final FlexibleStringExpander menuLocation;

        public MenuField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
            this.menuName = FlexibleStringExpander.getInstance(element.getAttribute("name"));
            this.menuLocation = FlexibleStringExpander.getInstance(element.getAttribute("location"));
        }

        private MenuField(MenuField original, ModelFormField modelFormField) {
            super(original.getFieldSource(), original.getFieldType(), modelFormField);
            this.menuName = original.menuName;
            this.menuLocation = original.menuLocation;
        }

        @Override
        public void accept(ModelFieldVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public FieldInfo copy(ModelFormField modelFormField) {
            return new MenuField(this, modelFormField);
        }

        public String getMenuName(Map<String, Object> context) {
            return this.menuName.expandString(context);
        }

        public FlexibleStringExpander getMenuName() {
            return menuName;
        }

        public String getMenuLocation(Map<String, Object> context) {
            return this.menuLocation.expandString(context);
        }

        public FlexibleStringExpander getMenuLocation() {
            return menuLocation;
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer)
                throws IOException {
            // Output format might not support menus, so make menu rendering optional.
            MenuStringRenderer menuStringRenderer = (MenuStringRenderer) context.get("menuStringRenderer");
            if (menuStringRenderer == null) {
                if (Debug.verboseOn()) Debug.logVerbose("MenuStringRenderer instance not found in rendering context, menu not rendered.", module);
                return;
            }
            ModelMenu modelMenu = getModelMenu(context);
            modelMenu.renderMenuString(writer, context, menuStringRenderer);
        }

        public ModelMenu getModelMenu(Map<String, Object> context) {
            String name = this.getMenuName(context);
            String location = this.getMenuLocation(context);
            ModelMenu modelMenu = null;
            try {
                modelMenu = MenuFactory.getMenuFromLocation(location, name);
            } catch (Exception e) {
                String errMsg = "Error rendering menu named [" + name + "] at location [" + location + "]: ";
                Debug.logError(e, errMsg, module);
                throw new RuntimeException(errMsg + e);
            }
            return modelMenu;
        }
    }

    public static abstract class OptionSource {

        private final ModelFormField modelFormField;

        protected OptionSource(ModelFormField modelFormField) {
            this.modelFormField = modelFormField;
        }

        public abstract void addOptionValues(List<OptionValue> optionValues, Map<String, Object> context, Delegator delegator);

        public abstract OptionSource copy(ModelFormField modelFormField);

        public ModelFormField getModelFormField() {
            return modelFormField;
        }
    }

    public static class OptionValue {
        private final String description;
        private final String key;

        public OptionValue(String key, String description) {
            this.key = key;
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public String getKey() {
            return key;
        }
    }

    /**
     * Models the &lt;password&gt; element.
     *
     * @see <code>widget-form.xsd</code>
     */
    public static class PasswordField extends TextField {

        public PasswordField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
        }

        public PasswordField(int fieldSource, ModelFormField modelFormField) {
            super(fieldSource, FieldInfo.PASSWORD, modelFormField);
        }

        private PasswordField(PasswordField original, ModelFormField modelFormField) {
            super(original, modelFormField);
        }

        @Override
        public void accept(ModelFieldVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public FieldInfo copy(ModelFormField modelFormField) {
            return new PasswordField(this, modelFormField);
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer)
                throws IOException {
            formStringRenderer.renderPasswordField(writer, context, this);
        }
    }

    /**
     * Models the &lt;radio&gt; element.
     *
     * @see <code>widget-form.xsd</code>
     */
    public static class RadioField extends FieldInfoWithOptions {

        public RadioField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
        }

        public RadioField(int fieldSource, ModelFormField modelFormField) {
            super(fieldSource, FieldInfo.RADIO, modelFormField);
        }

        public RadioField(ModelFormField modelFormField) {
            super(FieldInfo.SOURCE_EXPLICIT, FieldInfo.RADIO, modelFormField);
        }

        private RadioField(RadioField original, ModelFormField modelFormField) {
            super(original, modelFormField);
        }

        @Override
        public void accept(ModelFieldVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public FieldInfo copy(ModelFormField modelFormField) {
            return new RadioField(this, modelFormField);
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer)
                throws IOException {
            formStringRenderer.renderRadioField(writer, context, this);
        }
    }

    /**
     * Models the &lt;range-find&gt; element.
     *
     * @see <code>widget-form.xsd</code>
     */
    public static class RangeFindField extends TextField {
        private final String defaultOptionFrom;
        private final String defaultOptionThru;

        public RangeFindField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
            this.defaultOptionFrom = element.getAttribute("default-option-from");
            this.defaultOptionThru = element.getAttribute("default-option-thru");
        }

        public RangeFindField(int fieldSource, int size, ModelFormField modelFormField) {
            super(fieldSource, size, null, modelFormField, FieldInfo.RANGEQBE);
            this.defaultOptionFrom = "greaterThanEqualTo";
            this.defaultOptionThru = "lessThanEqualTo";
        }

        private RangeFindField(RangeFindField original, ModelFormField modelFormField) {
            super(original, modelFormField);
            this.defaultOptionFrom = original.defaultOptionFrom;
            this.defaultOptionThru = original.defaultOptionThru;
        }

        @Override
        public void accept(ModelFieldVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public FieldInfo copy(ModelFormField modelFormField) {
            return new RangeFindField(this, modelFormField);
        }

        public String getDefaultOptionFrom() {
            return this.defaultOptionFrom;
        }

        public String getDefaultOptionThru() {
            return this.defaultOptionThru;
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer)
                throws IOException {
            formStringRenderer.renderRangeFindField(writer, context, this);
        }
    }

    /**
     * Models the &lt;reset&gt; element.
     *
     * @see <code>widget-form.xsd</code>
     */
    public static class ResetField extends FieldInfo {

        public ResetField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
        }

        public ResetField(int fieldSource, ModelFormField modelFormField) {
            super(fieldSource, FieldInfo.RESET, modelFormField);
        }

        public ResetField(ModelFormField modelFormField) {
            super(FieldInfo.SOURCE_EXPLICIT, FieldInfo.RESET, modelFormField);
        }

        private ResetField(ResetField original, ModelFormField modelFormField) {
            super(original.getFieldSource(), original.getFieldType(), modelFormField);
        }

        @Override
        public void accept(ModelFieldVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public FieldInfo copy(ModelFormField modelFormField) {
            return new ResetField(this, modelFormField);
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer)
                throws IOException {
            formStringRenderer.renderResetField(writer, context, this);
        }
    }

    /**
     * Models the &lt;include-screen&gt; element.
     *
     * @see <code>widget-form.xsd</code>
     */
    public static class ScreenField extends FieldInfo {
        private final FlexibleStringExpander screenName;
        private final FlexibleStringExpander screenLocation;

        public ScreenField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
            this.screenName = FlexibleStringExpander.getInstance(element.getAttribute("name"));
            this.screenLocation = FlexibleStringExpander.getInstance(element.getAttribute("location"));
        }

        private ScreenField(ScreenField original, ModelFormField modelFormField) {
            super(original.getFieldSource(), original.getFieldType(), modelFormField);
            this.screenName = original.screenName;
            this.screenLocation = original.screenLocation;
        }

        @Override
        public void accept(ModelFieldVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public FieldInfo copy(ModelFormField modelFormField) {
            return new ScreenField(this, modelFormField);
        }

        public String getScreenName(Map<String, Object> context) {
            return this.screenName.expandString(context);
        }

        public FlexibleStringExpander getScreenName() {
            return screenName;
        }

        public String getScreenLocation(Map<String, Object> context) {
            return this.screenLocation.expandString(context);
        }

        public FlexibleStringExpander getScreenLocation() {
            return screenLocation;
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer)
                throws IOException {
            String name = this.getScreenName(context);
            String location = this.getScreenLocation(context);
            try {
                ScreenRenderer renderer = (ScreenRenderer)context.get("screens");
                if (renderer != null) {
                    MapStack<String> mapStack = UtilGenerics.cast(context);
                    ScreenRenderer subRenderer = new ScreenRenderer(writer, mapStack, renderer.getScreenStringRenderer());
                    writer.append(subRenderer.render(location, name));
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                String errMsg = "Error rendering included screen named [" + name + "] at location [" + location + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new RuntimeException(errMsg + e);
            }
        }
    }

    /**
     * Models the &lt;option&gt; element.
     *
     * @see <code>widget-form.xsd</code>
     */
    public static class SingleOption extends OptionSource {
        private final FlexibleStringExpander description;
        private final FlexibleStringExpander key;

        public SingleOption(Element optionElement, ModelFormField modelFormField) {
            super(modelFormField);
            this.key = FlexibleStringExpander.getInstance(optionElement.getAttribute("key"));
            this.description = FlexibleStringExpander.getInstance(UtilXml.checkEmpty(optionElement.getAttribute("description"),
                    optionElement.getAttribute("key")));
        }

        private SingleOption(SingleOption original, ModelFormField modelFormField) {
            super(modelFormField);
            this.key = original.key;
            this.description = original.description;
        }

        public SingleOption(String key, String description, ModelFormField modelFormField) {
            super(modelFormField);
            this.key = FlexibleStringExpander.getInstance(key);
            this.description = FlexibleStringExpander.getInstance(UtilXml.checkEmpty(description, key));
        }

        @Override
        public void addOptionValues(List<OptionValue> optionValues, Map<String, Object> context, Delegator delegator) {
            optionValues.add(new OptionValue(key.expandString(context), description.expandString(context)));
        }

        @Override
        public OptionSource copy(ModelFormField modelFormField) {
            return new SingleOption(this, modelFormField);
        }

        public FlexibleStringExpander getDescription() {
            return description;
        }

        public FlexibleStringExpander getKey() {
            return key;
        }
    }

    /**
     * Models the &lt;sub-hyperlink&gt; element.
     *
     * @see <code>widget-form.xsd</code>
     */
    public static class SubHyperlink {
        private final FlexibleStringExpander description;
        private final FlexibleStringExpander useWhen;
        private final Link link;
        private final ModelFormField modelFormField;

        public SubHyperlink(Element element, ModelFormField modelFormField) {
            this.description = FlexibleStringExpander.getInstance(element.getAttribute("description"));
            this.useWhen = FlexibleStringExpander.getInstance(element.getAttribute("use-when"));
            // Backwards compatible support
            element.setAttribute("style", element.getAttribute("link-style"));
            element.setAttribute("url-mode", element.getAttribute("target-type"));
            this.link = new Link(element);
            this.modelFormField = modelFormField;
        }

        public SubHyperlink(SubHyperlink original, ModelFormField modelFormField) {
            this.description = original.description;
            this.useWhen = original.useWhen;
            this.link = original.link;
            this.modelFormField = modelFormField;
        }

        public AutoEntityParameters getAutoEntityParameters() {
            return link.getAutoEntityParameters();
        }

        public AutoServiceParameters getAutoServiceParameters() {
            return link.getAutoServiceParameters();
        }

        public boolean getEncode() {
            return link.getEncode();
        }

        public boolean getFullPath() {
            return link.getFullPath();
        }

        public String getHeight() {
            return link.getHeight();
        }

        public String getId(Map<String, Object> context) {
            return link.getId(context);
        }

        public FlexibleStringExpander getIdExdr() {
            return link.getIdExdr();
        }

        public Image getImage() {
            return link.getImage();
        }

        public String getLinkType() {
            return link.getLinkType();
        }

        public String getName() {
            return link.getName();
        }

        public String getName(Map<String, Object> context) {
            return link.getName(context);
        }

        public FlexibleStringExpander getNameExdr() {
            return link.getNameExdr();
        }

        public List<Parameter> getParameterList() {
            return link.getParameterList();
        }

        public Map<String, String> getParameterMap(Map<String, Object> context, String defaultEntityName, String defaultServiceName) {
            return link.getParameterMap(context, defaultEntityName, defaultServiceName);
        }

        public Map<String, String> getParameterMap(Map<String, Object> context) {
            return link.getParameterMap(context);
        }

        public String getPrefix(Map<String, Object> context) {
            return link.getPrefix(context);
        }

        public FlexibleStringExpander getPrefixExdr() {
            return link.getPrefixExdr();
        }

        public boolean getSecure() {
            return link.getSecure();
        }

        public Integer getSize() {
            return link.getSize();
        }

        public String getStyle(Map<String, Object> context) {
            return link.getStyle(context);
        }

        public FlexibleStringExpander getStyleExdr() {
            return link.getStyleExdr();
        }

        public String getTarget(Map<String, Object> context) {
            return link.getTarget(context);
        }

        public FlexibleStringExpander getTargetExdr() {
            return link.getTargetExdr();
        }

        public String getTargetWindow(Map<String, Object> context) {
            return link.getTargetWindow(context);
        }

        public FlexibleStringExpander getTargetWindowExdr() {
            return link.getTargetWindowExdr();
        }

        public String getText(Map<String, Object> context) {
            return link.getText(context);
        }

        public FlexibleStringExpander getTextExdr() {
            return link.getTextExdr();
        }

        public String getUrlMode() {
            return link.getUrlMode();
        }

        public String getWidth() {
            return link.getWidth();
        }

        public FlexibleStringExpander getDescription() {
            return description;
        }

        public String getDescription(Map<String, Object> context) {
            return description.expandString(context);
        }

        public FlexibleStringExpander getUseWhen() {
            return useWhen;
        }

        public Link getLink() {
            return link;
        }

        public String getUseWhen(Map<String, Object> context) {
            return this.useWhen.expandString(context);
        }

        public ModelFormField getModelFormField() {
            return modelFormField;
        }

        public boolean shouldUse(Map<String, Object> context) {
            boolean shouldUse = true;
            String useWhen = this.getUseWhen(context);
            if (UtilValidate.isNotEmpty(useWhen)) {
                try {
                    Object retVal = GroovyUtil.eval(StringUtil.convertOperatorSubstitutions(useWhen),context);

                    // retVal should be a Boolean, if not something weird is up...
                    if (retVal instanceof Boolean) {
                        Boolean boolVal = (Boolean) retVal;
                        shouldUse = boolVal.booleanValue();
                    } else {
                        throw new IllegalArgumentException("Return value from target condition eval was not a Boolean: "
                                + retVal.getClass().getName() + " [" + retVal + "]");
                    }
                } catch (CompilationFailedException e) {
                    String errmsg = "Error evaluating Groovy target conditions";
                    Debug.logError(e, errmsg, module);
                    throw new IllegalArgumentException(errmsg);
                }
            }
            return shouldUse;
        }
    }

    public boolean shouldIgnore(Map<String, Object> context) {
        boolean shouldIgnore = true;
        String ignoreWhen = this.getIgnoreWhen(context);
        if (UtilValidate.isEmpty(ignoreWhen)) {
            return false;
        }

        try {
            Object retVal = GroovyUtil.eval(StringUtil.convertOperatorSubstitutions(ignoreWhen),context);

            if (retVal instanceof Boolean) {
                shouldIgnore =(Boolean) retVal;
            } else {
                throw new IllegalArgumentException("Return value from ignore-when condition eval was not a Boolean: "  + (retVal != null ? retVal.getClass().getName() : "null") + " [" + retVal + "] on the field " + this.name + " of form " + this.modelForm.getName());
            }

        } catch (CompilationFailedException e) {
            String errMsg = "Error evaluating BeanShell ignore-when condition [" + ignoreWhen + "] on the field " + this.name + " of form " + this.modelForm.getName() + ": " + e.toString();
            Debug.logError(e, errMsg, module);
            throw new IllegalArgumentException(errMsg);
        }

        return shouldIgnore;

    }

    /**
     * Models the &lt;submit&gt; element.
     *
     * @see <code>widget-form.xsd</code>
     */
    public static class SubmitField extends FieldInfo {
        private final FlexibleStringExpander backgroundSubmitRefreshTargetExdr;
        private final String buttonType;
        private final FlexibleStringExpander confirmationMsgExdr;
        private final FlexibleStringExpander imageLocation;
        private final boolean requestConfirmation;

        public SubmitField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
            this.backgroundSubmitRefreshTargetExdr = FlexibleStringExpander.getInstance(element
                    .getAttribute("background-submit-refresh-target"));
            this.buttonType = element.getAttribute("button-type");
            this.confirmationMsgExdr = FlexibleStringExpander.getInstance(element.getAttribute("confirmation-message"));
            this.imageLocation = FlexibleStringExpander.getInstance(element.getAttribute("image-location"));
            this.requestConfirmation = "true".equals(element.getAttribute("request-confirmation"));
        }

        public SubmitField(int fieldInfo, ModelFormField modelFormField) {
            super(fieldInfo, FieldInfo.SUBMIT, modelFormField);
            this.backgroundSubmitRefreshTargetExdr = FlexibleStringExpander.getInstance("");
            this.buttonType = "";
            this.confirmationMsgExdr = FlexibleStringExpander.getInstance("");
            this.imageLocation = FlexibleStringExpander.getInstance("");
            this.requestConfirmation = false;
        }

        public SubmitField(ModelFormField modelFormField) {
            this(FieldInfo.SOURCE_EXPLICIT, modelFormField);
        }

        private SubmitField(SubmitField original, ModelFormField modelFormField) {
            super(original.getFieldSource(), original.getFieldType(), modelFormField);
            this.buttonType = original.buttonType;
            this.imageLocation = original.imageLocation;
            this.backgroundSubmitRefreshTargetExdr = original.backgroundSubmitRefreshTargetExdr;
            this.requestConfirmation = original.requestConfirmation;
            this.confirmationMsgExdr = original.confirmationMsgExdr;
        }

        @Override
        public void accept(ModelFieldVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public FieldInfo copy(ModelFormField modelFormField) {
            return new SubmitField(this, modelFormField);
        }

        public String getBackgroundSubmitRefreshTarget(Map<String, Object> context) {
            return this.backgroundSubmitRefreshTargetExdr.expandString(context);
        }

        public FlexibleStringExpander getBackgroundSubmitRefreshTargetExdr() {
            return backgroundSubmitRefreshTargetExdr;
        }

        public String getButtonType() {
            return buttonType;
        }

        public String getConfirmation(Map<String, Object> context) {
            String message = getConfirmationMsg(context);
            if (UtilValidate.isNotEmpty(message)) {
                return message;
            } else if (getRequestConfirmation()) {
                String defaultMessage = UtilProperties.getPropertyValue("general", "default.confirmation.message",
                        "${uiLabelMap.CommonConfirm}");
                return FlexibleStringExpander.expandString(defaultMessage, context);
            }
            return "";
        }

        public String getConfirmationMsg(Map<String, Object> context) {
            return this.confirmationMsgExdr.expandString(context);
        }

        public FlexibleStringExpander getConfirmationMsgExdr() {
            return confirmationMsgExdr;
        }

        public FlexibleStringExpander getImageLocation() {
            return imageLocation;
        }

        public String getImageLocation(Map<String, Object> context) {
            return this.imageLocation.expandString(context);
        }

        public boolean getRequestConfirmation() {
            return this.requestConfirmation;
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer)
                throws IOException {
            formStringRenderer.renderSubmitField(writer, context, this);
        }
    }

    /**
     * Models the &lt;textarea&gt; element.
     *
     * @see <code>widget-form.xsd</code>
     */
    public static class TextareaField extends FieldInfo {
        private final int cols;
        private final FlexibleStringExpander defaultValue;
        private final boolean readOnly;
        private final int rows;
        private final FlexibleStringExpander visualEditorButtons;
        private final boolean visualEditorEnable;
        private final Integer maxlength;

        public TextareaField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
            int cols = 60;
            String colsStr = element.getAttribute("cols");
            if (!colsStr.isEmpty()) {
                try {
                    cols = Integer.parseInt(colsStr);
                } catch (NumberFormatException e) {
                    Debug.logError("Could not parse the size value of the text element: [" + colsStr
                            + "], setting to default of " + cols, module);
                }
            }
            this.cols = cols;
            this.defaultValue = FlexibleStringExpander.getInstance(element.getAttribute("default-value"));
            this.readOnly = "true".equals(element.getAttribute("read-only"));
            int rows = 2;
            String rowsStr = element.getAttribute("rows");
            if (!rowsStr.isEmpty()) {
                try {
                    rows = Integer.parseInt(rowsStr);
                } catch (NumberFormatException e) {
                    Debug.logError("Could not parse the size value of the text element: [" + rowsStr
                            + "], setting to default of " + rows, module);
                }
            }
            this.rows = rows;
            Integer maxlength = null;
            String maxlengthStr = element.getAttribute("maxlength");
            if (!maxlengthStr.isEmpty()) {
                try {
                    maxlength = Integer.valueOf(maxlengthStr);
                } catch (NumberFormatException e) {
                    Debug.logError("Could not parse the max-length value of the text element: [" + maxlengthStr
                            + "], setting to null; default of no maxlength will be used", module);
                }
            }
            this.maxlength = maxlength;
            this.visualEditorButtons = FlexibleStringExpander.getInstance(element.getAttribute("visual-editor-buttons"));
            this.visualEditorEnable = "true".equals(element.getAttribute("visual-editor-enable"));
        }

        public TextareaField(int fieldSource, ModelFormField modelFormField) {
            super(fieldSource, FieldInfo.TEXTAREA, modelFormField);
            this.cols = 60;
            this.defaultValue = FlexibleStringExpander.getInstance("");
            this.readOnly = false;
            this.rows = 2;
            this.maxlength = null;
            this.visualEditorButtons = FlexibleStringExpander.getInstance("");
            this.visualEditorEnable = false;
        }

        public TextareaField(ModelFormField modelFormField) {
            this(FieldInfo.SOURCE_EXPLICIT, modelFormField);
        }

        private TextareaField(TextareaField original, ModelFormField modelFormField) {
            super(original.getFieldSource(), original.getFieldType(), modelFormField);
            this.defaultValue = original.defaultValue;
            this.visualEditorEnable = original.visualEditorEnable;
            this.visualEditorButtons = original.visualEditorButtons;
            this.readOnly = original.readOnly;
            this.cols = original.cols;
            this.rows = original.rows;
            this.maxlength = original.maxlength;
        }

        @Override
        public void accept(ModelFieldVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public FieldInfo copy(ModelFormField modelFormField) {
            return new TextareaField(this, modelFormField);
        }

        public int getCols() {
            return cols;
        }

        public FlexibleStringExpander getDefaultValue() {
            return defaultValue;
        }

        public String getDefaultValue(Map<String, Object> context) {
            if (this.defaultValue != null) {
                return this.defaultValue.expandString(context);
            }
            return "";
        }

        public int getRows() { return rows; }

        public Integer getMaxlength() {
            return maxlength;
        }

        public FlexibleStringExpander getVisualEditorButtons() {
            return visualEditorButtons;
        }

        public String getVisualEditorButtons(Map<String, Object> context) {
            return this.visualEditorButtons.expandString(context);
        }

        public boolean getVisualEditorEnable() {
            return this.visualEditorEnable;
        }

        public boolean isReadOnly() {
            return readOnly;
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer)
                throws IOException {
            formStringRenderer.renderTextareaField(writer, context, this);
        }
    }

    /**
     * Models the &lt;text&gt; element.
     *
     * @see <code>widget-form.xsd</code>
     */
    public static class TextField extends FieldInfo {
        private final boolean clientAutocompleteField;
        private final FlexibleStringExpander defaultValue;
        private final boolean disabled;
        private final String mask;
        private final Integer maxlength;
        private final FlexibleStringExpander placeholder;
        private final boolean readonly;
        private final int size;
        private final SubHyperlink subHyperlink;

        public TextField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
            this.clientAutocompleteField = !"false".equals(element.getAttribute("client-autocomplete-field"));
            this.defaultValue = FlexibleStringExpander.getInstance(element.getAttribute("default-value"));
            this.disabled = "true".equals(element.getAttribute("disabled"));
            this.mask = element.getAttribute("mask");
            Integer maxlength = null;
            String maxlengthStr = element.getAttribute("maxlength");
            if (!maxlengthStr.isEmpty()) {
                try {
                    maxlength = Integer.valueOf(maxlengthStr);
                } catch (NumberFormatException e) {
                    Debug.logError("Could not parse the maxlength value of the text element: [" + maxlengthStr
                            + "], setting to null; default of no maxlength will be used", module);
                }
            }
            this.maxlength = maxlength;
            this.placeholder = FlexibleStringExpander.getInstance(element.getAttribute("placeholder"));
            this.readonly = "true".equals(element.getAttribute("read-only"));
            int size = 25;
            String sizeStr = element.getAttribute("size");
            if (!sizeStr.isEmpty()) {
                try {
                    size = Integer.parseInt(sizeStr);
                } catch (NumberFormatException e) {
                    Debug.logError("Could not parse the size value of the text element: [" + sizeStr
                            + "], setting to the default of " + size, module);
                }
            }
            this.size = size;
            Element subHyperlinkElement = UtilXml.firstChildElement(element, "sub-hyperlink");
            if (subHyperlinkElement != null) {
                this.subHyperlink = new SubHyperlink(subHyperlinkElement, this.getModelFormField());
            } else {
                this.subHyperlink = null;
            }
        }

        protected TextField(int fieldSource, int size, Integer maxlength, ModelFormField modelFormField, int fieldType) {
            super(fieldSource, fieldType == -1 ? FieldInfo.TEXT : fieldType, modelFormField);
            this.clientAutocompleteField = true;
            this.defaultValue = FlexibleStringExpander.getInstance("");
            this.disabled = false;
            this.mask = "";
            this.maxlength = maxlength;
            this.placeholder = FlexibleStringExpander.getInstance("");
            this.readonly = false;
            this.size = size;
            this.subHyperlink = null;
        }

        protected TextField(int fieldSource, int size, Integer maxlength, ModelFormField modelFormField) {
            super(fieldSource, FieldInfo.TEXT, modelFormField);
            this.clientAutocompleteField = true;
            this.defaultValue = FlexibleStringExpander.getInstance("");
            this.disabled = false;
            this.mask = "";
            this.maxlength = maxlength;
            this.placeholder = FlexibleStringExpander.getInstance("");
            this.readonly = false;
            this.size = size;
            this.subHyperlink = null;
        }

        private TextField(int fieldSource, int fieldType, ModelFormField modelFormField) {
            super(fieldSource, fieldType, modelFormField);
            this.clientAutocompleteField = true;
            this.defaultValue = FlexibleStringExpander.getInstance("");
            this.disabled = false;
            this.mask = "";
            this.maxlength = null;
            this.placeholder = FlexibleStringExpander.getInstance("");
            this.readonly = false;
            this.size = 25;
            this.subHyperlink = null;
        }

        public TextField(int fieldSource, ModelFormField modelFormField) {
            this(fieldSource, FieldInfo.TEXT, modelFormField);
        }

        public TextField(ModelFormField modelFormField) {
            this(FieldInfo.SOURCE_EXPLICIT, FieldInfo.TEXT, modelFormField);
        }

        protected TextField(TextField original, ModelFormField modelFormField) {
            super(original.getFieldSource(), original.getFieldType(), modelFormField);
            this.clientAutocompleteField = original.clientAutocompleteField;
            this.defaultValue = original.defaultValue;
            this.mask = original.mask;
            this.placeholder = original.placeholder;
            this.size = original.size;
            this.maxlength = original.maxlength;
            this.disabled = original.disabled;
            this.readonly = original.readonly;
            if (original.subHyperlink != null) {
                this.subHyperlink = new SubHyperlink(original.subHyperlink, modelFormField);
            } else {
                this.subHyperlink = null;
            }
        }

        @Override
        public void accept(ModelFieldVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public FieldInfo copy(ModelFormField modelFormField) {
            return new TextField(this, modelFormField);
        }

        public boolean getClientAutocompleteField() {
            return this.clientAutocompleteField;
        }

        public FlexibleStringExpander getDefaultValue() {
            return defaultValue;
        }

        public String getDefaultValue(Map<String, Object> context) {
            if (this.defaultValue != null) {
                return this.defaultValue.expandString(context);
            }
            return "";
        }

        public boolean getDisabled() {
            return this.disabled;
        }

        public String getMask() {
            return this.mask;
        }

        public Integer getMaxlength() {
            return maxlength;
        }

        public FlexibleStringExpander getPlaceholder() {
            return placeholder;
        }

        public String getPlaceholder(Map<String, Object> context) {
            return this.placeholder.expandString(context);
        }

        public boolean getReadonly() {
            return this.readonly;
        }

        public int getSize() {
            return size;
        }

        public SubHyperlink getSubHyperlink() {
            return this.subHyperlink;
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer)
                throws IOException {
            formStringRenderer.renderTextField(writer, context, this);
        }
    }

    /**
     * Models the &lt;text-find&gt; element.
     *
     * @see <code>widget-form.xsd</code>
     */
    public static class TextFindField extends TextField {
        private final String defaultOption;
        private final boolean hideIgnoreCase;
        private final boolean hideOptions;
        private final boolean ignoreCase;

        public TextFindField(Element element, ModelFormField modelFormField) {
            super(element, modelFormField);
            if (element.hasAttribute("default-option")) {
                this.defaultOption = element.getAttribute("default-option");
            } else {
                this.defaultOption = UtilProperties.getPropertyValue("widget", "widget.form.defaultTextFindOption", "contains");
            }
            this.hideIgnoreCase = "true".equals(element.getAttribute("hide-options"))
                    || "ignore-case".equals(element.getAttribute("hide-options")) ? true : false;
            this.hideOptions = "true".equals(element.getAttribute("hide-options"))
                    || "options".equals(element.getAttribute("hide-options")) ? true : false;
            this.ignoreCase = "true".equals(element.getAttribute("ignore-case"));
        }

        public TextFindField(int fieldSource, int size, Integer maxlength, ModelFormField modelFormField) {
            super(fieldSource, size, maxlength, modelFormField);
            this.defaultOption = UtilProperties.getPropertyValue("widget", "widget.form.defaultTextFindOption", "contains");
            this.hideIgnoreCase = false;
            this.hideOptions = false;
            this.ignoreCase = true;
        }

        public TextFindField(int fieldSource, ModelFormField modelFormField) {
            super(fieldSource, modelFormField);
            this.defaultOption = UtilProperties.getPropertyValue("widget", "widget.form.defaultTextFindOption", "contains");
            this.hideIgnoreCase = false;
            this.hideOptions = false;
            this.ignoreCase = true;
        }

        private TextFindField(TextFindField original, ModelFormField modelFormField) {
            super(original, modelFormField);
            this.ignoreCase = original.ignoreCase;
            this.hideIgnoreCase = original.hideIgnoreCase;
            this.defaultOption = original.defaultOption;
            this.hideOptions = original.hideOptions;
        }

        @Override
        public void accept(ModelFieldVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public FieldInfo copy(ModelFormField modelFormField) {
            return new TextFindField(this, modelFormField);
        }

        public String getDefaultOption() {
            return this.defaultOption;
        }

        public String getDefaultOption(Map<String, Object> context) {
            String defaultOption = getDefaultOption();

            Map<String, Object> parameters = UtilGenerics.checkMap(context.get("parameters"), String.class, Object.class);
            if (UtilValidate.isNotEmpty(parameters)) {
                String fieldName = this.getModelFormField().getName();
                if (parameters.containsKey(fieldName)) {
                    defaultOption = (String) parameters.get(fieldName.concat("_op"));
                }
            }
            return defaultOption;
        }

        public boolean getHideIgnoreCase() {
            return this.hideIgnoreCase;
        }

        public boolean getHideOptions() {
            return this.hideOptions;
        }

        public boolean getIgnoreCase() {
            return this.ignoreCase;
        }

        public boolean getIgnoreCase(Map<String, Object> context) {
            Boolean ignoreCase = getIgnoreCase();

            Map<String, Object> parameters = UtilGenerics.checkMap(context.get("parameters"), String.class, Object.class);
            if (UtilValidate.isNotEmpty(parameters)) {
                String fieldName = this.getModelFormField().getName();
                if (parameters.containsKey(fieldName)) {
                    ignoreCase = "Y".equals(parameters.get(fieldName.concat("_ic")));
                }
            }
            return ignoreCase;
        }

        @Override
        public void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer)
                throws IOException {
            formStringRenderer.renderTextFindField(writer, context, this);
        }
    }
}
