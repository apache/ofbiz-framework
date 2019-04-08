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
package org.apache.ofbiz.entity.finder;

import static org.apache.ofbiz.base.util.UtilGenerics.cast;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityComparisonOperator;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityFunction;
import org.apache.ofbiz.entity.condition.EntityJoinOperator;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelFieldTypeReader;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.w3c.dom.Element;

/**
 * Uses the delegator to find entity values by a condition
 *
 */
public final class EntityFinderUtil {

    public static final String module = EntityFinderUtil.class.getName();

    private EntityFinderUtil () {}

    public static Map<FlexibleMapAccessor<Object>, Object> makeFieldMap(Element element) {
        Map<FlexibleMapAccessor<Object>, Object> fieldMap = null;
        List<? extends Element> fieldMapElementList = UtilXml.childElementList(element, "field-map");
        if (fieldMapElementList.size() > 0) {
            fieldMap = new HashMap<FlexibleMapAccessor<Object>, Object>(fieldMapElementList.size());
            for (Element fieldMapElement: fieldMapElementList) {
                // set the env-name for each field-name, noting that if no field-name is specified it defaults to the env-name
                String fieldName = fieldMapElement.getAttribute("field-name");
                String envName = fieldMapElement.getAttribute("from-field");
                if (envName.isEmpty()) {
                    envName = fieldMapElement.getAttribute("env-name");
                }
                String value = fieldMapElement.getAttribute("value");
                if (fieldName.isEmpty()) {
                    // no fieldName, use envName for both
                    fieldMap.put(FlexibleMapAccessor.getInstance(envName), FlexibleMapAccessor.getInstance(envName));
                } else {
                    if (!value.isEmpty()) {
                        fieldMap.put(FlexibleMapAccessor.getInstance(fieldName), FlexibleStringExpander.getInstance(value));
                    } else {
                        // at this point we have a fieldName and no value, do we have a envName?
                        if (!envName.isEmpty()) {
                            fieldMap.put(FlexibleMapAccessor.getInstance(fieldName), FlexibleMapAccessor.getInstance(envName));
                        } else {
                            // no envName, use fieldName for both
                            fieldMap.put(FlexibleMapAccessor.getInstance(fieldName), FlexibleMapAccessor.getInstance(fieldName));
                        }
                    }
                }
            }
        }
        return fieldMap;
    }

    public static void expandFieldMapToContext(Map<FlexibleMapAccessor<Object>, Object> fieldMap, Map<String, Object> context, Map<String, Object> outContext) {
        //Debug.logInfo("fieldMap: " + fieldMap, module);
        if (fieldMap != null) {
            for (Map.Entry<FlexibleMapAccessor<Object>, Object> entry: fieldMap.entrySet()) {
                FlexibleMapAccessor<Object> serviceContextFieldAcsr = entry.getKey();
                Object valueSrc = entry.getValue();
                if (valueSrc instanceof FlexibleMapAccessor<?>) {
                    FlexibleMapAccessor<Object> contextEnvAcsr = cast(valueSrc);
                    serviceContextFieldAcsr.put(outContext, contextEnvAcsr.get(context));
                } else if (valueSrc instanceof FlexibleStringExpander) {
                    FlexibleStringExpander valueExdr = (FlexibleStringExpander) valueSrc;
                    serviceContextFieldAcsr.put(outContext, valueExdr.expandString(context));
                } else {
                    // hmmmm...
                }
            }
        }
    }

    public static List<FlexibleStringExpander> makeSelectFieldExpanderList(Element element) {
        List<FlexibleStringExpander> selectFieldExpanderList = null;
        List<? extends Element> selectFieldElementList = UtilXml.childElementList(element, "select-field");
        if (selectFieldElementList.size() > 0) {
            selectFieldExpanderList = new ArrayList<FlexibleStringExpander>(selectFieldElementList.size());
            for (Element selectFieldElement: selectFieldElementList) {
                selectFieldExpanderList.add(FlexibleStringExpander.getInstance(selectFieldElement.getAttribute("field-name")));
            }
        }
        return selectFieldExpanderList;
    }

    public static Set<String> makeFieldsToSelect(List<FlexibleStringExpander> selectFieldExpanderList, Map<String, Object> context) {
        Set<String> fieldsToSelect = null;
        if (UtilValidate.isNotEmpty(selectFieldExpanderList)) {
            fieldsToSelect = new HashSet<String>();
            for (FlexibleStringExpander selectFieldExpander: selectFieldExpanderList) {
                fieldsToSelect.add(selectFieldExpander.expandString(context));
            }
        }
        return fieldsToSelect;
    }

    public static List<String> makeOrderByFieldList(List<FlexibleStringExpander> orderByExpanderList, Map<String, Object> context) {
        List<String> orderByFields = null;
        if (UtilValidate.isNotEmpty(orderByExpanderList)) {
            orderByFields = new ArrayList<String>(orderByExpanderList.size());
            for (FlexibleStringExpander orderByExpander: orderByExpanderList) {
                orderByFields.add(orderByExpander.expandString(context));
            }
        }
        return orderByFields;
    }

    public static interface Condition extends Serializable {
        public EntityCondition createCondition(Map<String, ? extends Object> context, ModelEntity modelEntity, ModelFieldTypeReader modelFieldTypeReader);
    }

    @SuppressWarnings("serial")
    public static final class ConditionExpr implements Condition {
        private final String fieldName;
        private final EntityOperator<?,?,?> operator;
        private final FlexibleMapAccessor<Object> envNameAcsr;
        private final FlexibleStringExpander valueExdr;
        private final FlexibleStringExpander ignoreExdr;
        private final boolean ignoreIfNull;
        private final boolean ignoreIfEmpty;
        private final boolean ignoreCase;

        public ConditionExpr(Element conditionExprElement) {
            String fieldNameAttribute = conditionExprElement.getAttribute("field-name");
            if (fieldNameAttribute.isEmpty()) {
                fieldNameAttribute = conditionExprElement.getAttribute("name");
            }
            this.fieldName = fieldNameAttribute;
            String operatorAttribute = conditionExprElement.getAttribute("operator");
            if (operatorAttribute.isEmpty()) {
                operatorAttribute = "equals";
            }
            this.operator = EntityOperator.lookup(operatorAttribute);
            if (this.operator == null) {
                throw new IllegalArgumentException("Could not find an entity operator for the name: " + operatorAttribute);
            }
            String fromFieldAttribute = conditionExprElement.getAttribute("from-field");
            if (fromFieldAttribute.isEmpty()) {
                fromFieldAttribute = conditionExprElement.getAttribute("env-name");
            }
            this.envNameAcsr = FlexibleMapAccessor.getInstance(fromFieldAttribute);
            this.valueExdr = FlexibleStringExpander.getInstance(conditionExprElement.getAttribute("value"));
            this.ignoreIfNull = "true".equals(conditionExprElement.getAttribute("ignore-if-null"));
            this.ignoreIfEmpty = "true".equals(conditionExprElement.getAttribute("ignore-if-empty"));
            this.ignoreCase = "true".equals(conditionExprElement.getAttribute("ignore-case"));
            this.ignoreExdr = FlexibleStringExpander.getInstance(conditionExprElement.getAttribute("ignore"));
        }

        public EntityCondition createCondition(Map<String, ? extends Object> context, ModelEntity modelEntity, ModelFieldTypeReader modelFieldTypeReader) {
            if ("true".equals(this.ignoreExdr.expandString(context))) {
                return null;
            }
            if (modelEntity.getField(fieldName) == null) {
                throw new IllegalArgumentException("Error in Entity Find: could not find field [" + fieldName + "] in entity with name [" + modelEntity.getEntityName() + "]");
            }

            Object value = envNameAcsr.get(context);
            if (value == null && !valueExdr.isEmpty()) {
                value = valueExdr.expandString(context);
            }
            if (this.ignoreIfNull && value == null) {
                return null;
            }

            // If IN or BETWEEN operator, see if value is a literal list and split it
            if ((operator.equals(EntityOperator.IN) || operator.equals(EntityOperator.BETWEEN) || operator.equals(EntityOperator.NOT_IN))
                    && value instanceof String) {
                String delim = null;
                if (((String)value).indexOf("|") >= 0) {
                    delim = "|";
                } else if (((String)value).indexOf(",") >= 0) {
                    delim = ",";
                }
                if (delim != null) {
                    value = StringUtil.split((String)value, delim);
                }
            }

            // don't convert the field to the desired type if this is an IN or BETWEEN operator and we have a Collection
            if (!((operator.equals(EntityOperator.IN) || operator.equals(EntityOperator.BETWEEN) || operator.equals(EntityOperator.NOT_IN))
                    && value instanceof Collection<?>)) {
                // now to a type conversion for the target fieldName
                value = modelEntity.convertFieldValue(modelEntity.getField(fieldName), value, modelFieldTypeReader, context);
            }

            if (Debug.verboseOn()) Debug.logVerbose("Got value for fieldName [" + fieldName + "]: " + value, module);

            if (this.ignoreIfEmpty && ObjectType.isEmpty(value)) {
                return null;
            }

            if (operator == EntityOperator.NOT_EQUAL && value != null) {
                // since some databases don't consider nulls in != comparisons, explicitly include them
                // this makes more sense logically, but if anyone ever needs it to not behave this way we should add an "or-null" attribute that is true by default
                if (ignoreCase) {
                    return EntityCondition.makeCondition(
                            EntityCondition.makeCondition(EntityFunction.UPPER_FIELD(fieldName), UtilGenerics.<EntityComparisonOperator<?,?>>cast(operator), EntityFunction.UPPER(value)),
                            EntityOperator.OR,
                            EntityCondition.makeCondition(fieldName, EntityOperator.EQUALS, null));
                } else {
                    return EntityCondition.makeCondition(
                            EntityCondition.makeCondition(fieldName, UtilGenerics.<EntityComparisonOperator<?,?>>cast(operator), value),
                            EntityOperator.OR,
                            EntityCondition.makeCondition(fieldName, EntityOperator.EQUALS, null));
                }
            } else {
                if (ignoreCase) {
                    // use the stuff to upper case both sides
                    return EntityCondition.makeCondition(EntityFunction.UPPER_FIELD(fieldName), UtilGenerics.<EntityComparisonOperator<?,?>>cast(operator), EntityFunction.UPPER(value));
                } else {
                    return EntityCondition.makeCondition(fieldName, UtilGenerics.<EntityComparisonOperator<?,?>>cast(operator), value);
                }
            }
        }
    }

    @SuppressWarnings("serial")
    public static final class ConditionList implements Condition {
        private final List<Condition> conditionList;
        private final EntityOperator<?,?,?> operator;

        public ConditionList(Element conditionListElement) {
            String operatorAttribute = conditionListElement.getAttribute("combine");
            if (operatorAttribute.isEmpty()) {
                operatorAttribute = "and";
            }
            this.operator = EntityOperator.lookup(operatorAttribute);
            if (this.operator == null) {
                throw new IllegalArgumentException("Could not find an entity operator for the name: " + operatorAttribute);
            }
            List<? extends Element> subElements = UtilXml.childElementList(conditionListElement);
            if (subElements.isEmpty()) {
                this.conditionList = null;
            } else {
                List<Condition> conditionList = new ArrayList<Condition>(subElements.size());
                for (Element subElement : subElements) {
                    if ("condition-expr".equals(subElement.getNodeName())) {
                        conditionList.add(new ConditionExpr(subElement));
                    } else if ("condition-list".equals(subElement.getNodeName())) {
                        conditionList.add(new ConditionList(subElement));
                    } else if ("condition-object".equals(subElement.getNodeName())) {
                        conditionList.add(new ConditionObject(subElement));
                    } else {
                        throw new IllegalArgumentException("Invalid element with name [" + subElement.getNodeName() + "] found under a condition-list element.");
                    }
                }
                this.conditionList = Collections.unmodifiableList(conditionList);
            }
        }

        public EntityCondition createCondition(Map<String, ? extends Object> context, ModelEntity modelEntity, ModelFieldTypeReader modelFieldTypeReader) {
            if (this.conditionList == null) {
                return null;
            }
            if (this.conditionList.size() == 1) {
                Condition condition = this.conditionList.get(0);
                return condition.createCondition(context, modelEntity, modelFieldTypeReader);
            }
            List<EntityCondition> entityConditionList = new ArrayList<EntityCondition>(this.conditionList.size());
            for (Condition curCondition: this.conditionList) {
                EntityCondition econd = curCondition.createCondition(context, modelEntity, modelFieldTypeReader);
                if (econd != null) {
                    entityConditionList.add(econd);
                }
            }
            return EntityCondition.makeCondition(entityConditionList, UtilGenerics.<EntityJoinOperator>cast(operator));
        }
    }

    @SuppressWarnings("serial")
    public static final class ConditionObject implements Condition {
        private final FlexibleMapAccessor<Object> fieldNameAcsr;

        public ConditionObject(Element conditionExprElement) {
            String fieldNameAttribute = conditionExprElement.getAttribute("field");
            if (fieldNameAttribute.isEmpty()) {
                fieldNameAttribute = conditionExprElement.getAttribute("field-name");
            }
            this.fieldNameAcsr = FlexibleMapAccessor.getInstance(fieldNameAttribute);
        }

        public EntityCondition createCondition(Map<String, ? extends Object> context, ModelEntity modelEntity, ModelFieldTypeReader modelFieldTypeReader) {
            return (EntityCondition) fieldNameAcsr.get(context);
        }
    }

    public static interface OutputHandler extends Serializable {
        public void handleOutput(EntityListIterator eli, Map<String, Object> context, FlexibleMapAccessor<Object> listAcsr);
        public void handleOutput(List<GenericValue> results, Map<String, Object> context, FlexibleMapAccessor<Object> listAcsr);
    }

    @SuppressWarnings("serial")
    public static class LimitRange implements OutputHandler {
        FlexibleStringExpander startExdr;
        FlexibleStringExpander sizeExdr;

        public LimitRange(Element limitRangeElement) {
            this.startExdr = FlexibleStringExpander.getInstance(limitRangeElement.getAttribute("start"));
            this.sizeExdr = FlexibleStringExpander.getInstance(limitRangeElement.getAttribute("size"));
        }

        int getStart(Map<String, Object> context) {
            String startStr = this.startExdr.expandString(context);
            try {
                return Integer.parseInt(startStr);
            } catch (NumberFormatException e) {
                String errMsg = "The limit-range start number \"" + startStr + "\" was not valid: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new IllegalArgumentException(errMsg);
            }
        }

        int getSize(Map<String, Object> context) {
            String sizeStr = this.sizeExdr.expandString(context);
            try {
                return Integer.parseInt(sizeStr);
            } catch (NumberFormatException e) {
                String errMsg = "The limit-range size number \"" + sizeStr + "\" was not valid: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new IllegalArgumentException(errMsg);
            }
        }

        public void handleOutput(EntityListIterator eli, Map<String, Object> context, FlexibleMapAccessor<Object> listAcsr) {
            int start = getStart(context) + 1; // ELI index is one-based.
            int size = getSize(context);
            try {
                listAcsr.put(context, eli.getPartialList(start, size));
                eli.close();
            } catch (GenericEntityException e) {
                String errMsg = "Error getting partial list in limit-range with start=" + start + " and size=" + size + ": " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new IllegalArgumentException(errMsg);
            }
        }

        public void handleOutput(List<GenericValue> results, Map<String, Object> context, FlexibleMapAccessor<Object> listAcsr) {
            List<GenericValue> result = null;
            int start = getStart(context);
            if (start < results.size()) {
                int size = getSize(context);
                int end = start + size;
                if (end > results.size()) {
                    end = results.size();
                }
                result = results.subList(start, end);
            } else {
                result = new LinkedList<GenericValue>();
            }
            listAcsr.put(context, result);
        }
    }

    @SuppressWarnings("serial")
    public static class LimitView implements OutputHandler {
        FlexibleStringExpander viewIndexExdr;
        FlexibleStringExpander viewSizeExdr;

        public LimitView(Element limitViewElement) {
            this.viewIndexExdr = FlexibleStringExpander.getInstance(limitViewElement.getAttribute("view-index"));
            this.viewSizeExdr = FlexibleStringExpander.getInstance(limitViewElement.getAttribute("view-size"));
        }

        int getIndex(Map<String, Object> context) {
            String viewIndexStr = this.viewIndexExdr.expandString(context);
            try {
                return Integer.parseInt(viewIndexStr);
            } catch (NumberFormatException e) {
                String errMsg = "The limit-view view-index number \"" + viewIndexStr + "\" was not valid: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new IllegalArgumentException(errMsg);
            }
        }

        int getSize(Map<String, Object> context) {
            String viewSizeStr = this.viewSizeExdr.expandString(context);
            try {
                return Integer.parseInt(viewSizeStr);
            } catch (NumberFormatException e) {
                String errMsg = "The limit-view view-size number \"" + viewSizeStr + "\" was not valid: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new IllegalArgumentException(errMsg);
            }
        }

        public void handleOutput(EntityListIterator eli, Map<String, Object> context, FlexibleMapAccessor<Object> listAcsr) {
            int index = this.getIndex(context);
            int size = this.getSize(context);
            try {
                listAcsr.put(context, eli.getPartialList(((index - 1) * size) + 1, size)); // ELI index is one-based.
                eli.close();
            } catch (GenericEntityException e) {
                String errMsg = "Error getting partial list in limit-view with index=" + index + " and size=" + size + ": " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new IllegalArgumentException(errMsg);
            }
        }

        public void handleOutput(List<GenericValue> results, Map<String, Object> context, FlexibleMapAccessor<Object> listAcsr) {
            List<GenericValue> result = null;
            int index = this.getIndex(context);
            int size = this.getSize(context);
            int begin = (index - 1) * size;
            if (begin < results.size()) {
                int end = begin + size;
                if (end > results.size()) {
                    end = results.size();
                }
                result = results.subList(begin, end);
            } else {
                result = new LinkedList<GenericValue>();
            }
            listAcsr.put(context, result);
        }
    }

    @SuppressWarnings("serial")
    public static class UseIterator implements OutputHandler {
        public UseIterator(Element useIteratorElement) {
            // no parameters, nothing to do
        }

        public void handleOutput(EntityListIterator eli, Map<String, Object> context, FlexibleMapAccessor<Object> listAcsr) {
            listAcsr.put(context, eli);
        }

        public void handleOutput(List<GenericValue> results, Map<String, Object> context, FlexibleMapAccessor<Object> listAcsr) {
            throw new IllegalArgumentException("Cannot handle output with use-iterator when the query is cached, or the result in general is not an EntityListIterator");
        }
    }
    @SuppressWarnings("serial")
    public static class GetAll implements OutputHandler {
        public GetAll() {
            // no parameters, nothing to do
        }

        public void handleOutput(EntityListIterator eli, Map<String, Object> context, FlexibleMapAccessor<Object> listAcsr) {
            try {
                listAcsr.put(context, eli.getCompleteList());
                eli.close();
            } catch (GenericEntityException e) {
                String errorMsg = "Error getting list from EntityListIterator: " + e.toString();
                Debug.logError(e, errorMsg, module);
                throw new IllegalArgumentException(errorMsg);
            }
        }

        public void handleOutput(List<GenericValue> results, Map<String, Object> context, FlexibleMapAccessor<Object> listAcsr) {
            listAcsr.put(context, results);
        }
    }
}

