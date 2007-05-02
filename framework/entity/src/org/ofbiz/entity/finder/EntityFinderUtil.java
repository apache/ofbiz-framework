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
package org.ofbiz.entity.finder;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.condition.EntityComparisonOperator;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityJoinOperator;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.util.EntityListIterator;
import org.w3c.dom.Element;

/**
 * Uses the delegator to find entity values by a condition
 *
 */
public class EntityFinderUtil {
    
    public static final String module = EntityFinderUtil.class.getName();         
    
    public static Map makeFieldMap(Element element) {
        Map fieldMap = null;
        List fieldMapElementList = UtilXml.childElementList(element, "field-map");
        if (fieldMapElementList.size() > 0) {
            fieldMap = new HashMap();
            Iterator fieldMapElementIter = fieldMapElementList.iterator();
            while (fieldMapElementIter.hasNext()) {
                Element fieldMapElement = (Element) fieldMapElementIter.next();
                // set the env-name for each field-name, noting that if no field-name is specified it defaults to the env-name
                String fieldName = fieldMapElement.getAttribute("field-name");
                String envName = fieldMapElement.getAttribute("env-name");
                String value = fieldMapElement.getAttribute("value");
                if (UtilValidate.isEmpty(fieldName)) {
                    // no fieldName, use envName for both
                    fieldMap.put(new FlexibleMapAccessor(envName), new FlexibleMapAccessor(envName));
                } else {
                    if (UtilValidate.isNotEmpty(value)) {
                        fieldMap.put(new FlexibleMapAccessor(fieldName), new FlexibleStringExpander(value));
                    } else {
                        // at this point we have a fieldName and no value, do we have a envName?
                        if (UtilValidate.isNotEmpty(envName)) {
                            fieldMap.put(new FlexibleMapAccessor(fieldName), new FlexibleMapAccessor(envName));
                        } else {
                            // no envName, use fieldName for both
                            fieldMap.put(new FlexibleMapAccessor(fieldName), new FlexibleMapAccessor(fieldName));
                        }
                    }
                }
            }
        }
        return fieldMap;
    }

    public static void expandFieldMapToContext(Map fieldMap, Map context, Map outContext) {
        //Debug.logInfo("fieldMap: " + fieldMap, module);
        if (fieldMap != null) {
            Iterator fieldMapEntryIter = fieldMap.entrySet().iterator();
            while (fieldMapEntryIter.hasNext()) {
                Map.Entry entry = (Map.Entry) fieldMapEntryIter.next();
                FlexibleMapAccessor serviceContextFieldAcsr = (FlexibleMapAccessor) entry.getKey();
                Object valueSrc = entry.getValue();
                if (valueSrc instanceof FlexibleMapAccessor) {
                    FlexibleMapAccessor contextEnvAcsr = (FlexibleMapAccessor) valueSrc;
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
    
    public static List makeSelectFieldExpanderList(Element element) {
        List selectFieldExpanderList = null;
        List selectFieldElementList = UtilXml.childElementList(element, "select-field");
        if (selectFieldElementList.size() > 0) {
            selectFieldExpanderList = new LinkedList();
            Iterator selectFieldElementIter = selectFieldElementList.iterator();
            while (selectFieldElementIter.hasNext()) {
                Element selectFieldElement = (Element) selectFieldElementIter.next();
                selectFieldExpanderList.add(new FlexibleStringExpander(selectFieldElement.getAttribute("field-name")));
            }
        }
        return selectFieldExpanderList;
    }
    
    public static Set makeFieldsToSelect(List selectFieldExpanderList, Map context) {
        Set fieldsToSelect = null;
        if (selectFieldExpanderList != null && selectFieldExpanderList.size() > 0) {
            fieldsToSelect = new HashSet();
            Iterator selectFieldExpanderIter = selectFieldExpanderList.iterator();
            while (selectFieldExpanderIter.hasNext()) {
                FlexibleStringExpander selectFieldExpander = (FlexibleStringExpander) selectFieldExpanderIter.next();
                fieldsToSelect.add(selectFieldExpander.expandString(context));
            }
        }
        return fieldsToSelect;
    }
    
    public static List makeOrderByFieldList(List orderByExpanderList, Map context) {
        List orderByFields = null;
        if (orderByExpanderList != null && orderByExpanderList.size() > 0) {
            orderByFields = new LinkedList();
            Iterator orderByExpanderIter = orderByExpanderList.iterator();
            while (orderByExpanderIter.hasNext()) {
                FlexibleStringExpander orderByExpander = (FlexibleStringExpander) orderByExpanderIter.next();
                orderByFields.add(orderByExpander.expandString(context));
            }
        }
        return orderByFields;
    }
    
    public static interface Condition extends Serializable {
        public EntityCondition createCondition(Map context, String entityName, GenericDelegator delegator);
    }
    public static class ConditionExpr implements Condition {
        protected FlexibleStringExpander fieldNameExdr;
        protected FlexibleStringExpander operatorExdr;
        protected FlexibleMapAccessor envNameAcsr;
        protected FlexibleStringExpander valueExdr;
        protected boolean ignoreIfNull;
        protected boolean ignoreIfEmpty;
        protected boolean ignoreCase;
        
        public ConditionExpr(Element conditionExprElement) {
            this.fieldNameExdr = new FlexibleStringExpander(conditionExprElement.getAttribute("field-name"));
            if (this.fieldNameExdr.isEmpty()) {
                // no "field-name"? try "name"
                this.fieldNameExdr = new FlexibleStringExpander(conditionExprElement.getAttribute("name"));
            }

            this.operatorExdr = new FlexibleStringExpander(UtilFormatOut.checkEmpty(conditionExprElement.getAttribute("operator"), "equals"));
            this.envNameAcsr = new FlexibleMapAccessor(conditionExprElement.getAttribute("env-name"));
            this.valueExdr = new FlexibleStringExpander(conditionExprElement.getAttribute("value"));
            this.ignoreIfNull = "true".equals(conditionExprElement.getAttribute("ignore-if-null"));
            this.ignoreIfEmpty = "true".equals(conditionExprElement.getAttribute("ignore-if-empty"));
            this.ignoreCase = "true".equals(conditionExprElement.getAttribute("ignore-case"));
        }
        
        public EntityCondition createCondition(Map context, String entityName, GenericDelegator delegator) {
            ModelEntity modelEntity = delegator.getModelEntity(entityName);
            if (modelEntity == null) {
                throw new IllegalArgumentException("Error in Entity Find: could not find entity with name [" + entityName + "]");
            }
            
            String fieldName = fieldNameExdr.expandString(context);
            
            Object value = null;
            // start with the environment variable, will override if exists and a value is specified
            if (envNameAcsr != null) {
                value = envNameAcsr.get(context);
            }
            // no value so far, and a string value is specified, use that
            if (value == null && valueExdr != null) {
                value = valueExdr.expandString(context);
            }

            String operatorName = operatorExdr.expandString(context);
            EntityOperator operator = EntityOperator.lookup(operatorName);
            if (operator == null) {
                throw new IllegalArgumentException("Could not find an entity operator for the name: " + operatorName);
            }

            // If IN operator, see if value is a literal list and split it
            if (operator == EntityOperator.IN && value instanceof String) {
                String delim = null;
                if (((String)value).indexOf("|") >= 0) {
                    delim = "|";   
                } else if (((String)value).indexOf(",") >= 0) {
                    delim = ",";   
                }
                if (UtilValidate.isNotEmpty(delim)) {
                    value = StringUtil.split((String)value, delim);   
                }
            }
            
            // don't convert the field to the desired type if this is an IN operator and we have a Collection
            if (!(operator == EntityOperator.IN && value instanceof Collection)) {
                // now to a type conversion for the target fieldName
                value = modelEntity.convertFieldValue(fieldName, value, delegator);
            }
            
            if (Debug.verboseOn()) Debug.logVerbose("Got value for fieldName [" + fieldName + "]: " + value, module);

            if (this.ignoreIfNull && value == null) {
                return null;
            }
            if (this.ignoreIfEmpty && ObjectType.isEmpty(value)) {
                return null;
            }

            if (operator == EntityOperator.NOT_EQUAL && value != null) {
                // since some databases don't consider nulls in != comparisons, explicitly include them
                // this makes more sense logically, but if anyone ever needs it to not behave this way we should add an "or-null" attribute that is true by default
                if (ignoreCase) {
                    return new EntityExpr(
                            new EntityExpr(fieldName, true, (EntityComparisonOperator) operator, value, true), 
                            EntityOperator.OR,
                            new EntityExpr(fieldName, EntityOperator.EQUALS, null));
                } else {
                    return new EntityExpr(
                            new EntityExpr(fieldName, (EntityComparisonOperator) operator, value), 
                            EntityOperator.OR,
                            new EntityExpr(fieldName, EntityOperator.EQUALS, null));
                }
            } else {
                if (ignoreCase) {
                    // use the stuff to upper case both sides
                    return new EntityExpr(fieldName, true, (EntityComparisonOperator) operator, value, true);
                } else {
                    return new EntityExpr(fieldName, (EntityComparisonOperator) operator, value);
                }
            }
        }
    }
    
    public static class ConditionList implements Condition {
        List conditionList = new LinkedList();
        FlexibleStringExpander combineExdr;
        
        public ConditionList(Element conditionListElement) {
            this.combineExdr = new FlexibleStringExpander(conditionListElement.getAttribute("combine"));
            
            List subElements = UtilXml.childElementList(conditionListElement);
            Iterator subElementIter = subElements.iterator();
            while (subElementIter.hasNext()) {
                Element subElement = (Element) subElementIter.next();
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
        }
        
        public EntityCondition createCondition(Map context, String entityName, GenericDelegator delegator) {
            if (this.conditionList.size() == 0) {
                return null;
            }
            if (this.conditionList.size() == 1) {
                Condition condition = (Condition) this.conditionList.get(0);
                return condition.createCondition(context, entityName, delegator);
            }
            
            List entityConditionList = new LinkedList();
            Iterator conditionIter = conditionList.iterator();
            while (conditionIter.hasNext()) {
                Condition curCondition = (Condition) conditionIter.next();
                EntityCondition econd = curCondition.createCondition(context, entityName, delegator);
                if (econd != null) {
                    entityConditionList.add(econd);
                }
            }
            
            String operatorName = combineExdr.expandString(context);
            EntityOperator operator = EntityOperator.lookup(operatorName);
            if (operator == null) {
                throw new IllegalArgumentException("Could not find an entity operator for the name: " + operatorName);
            }
            
            return new EntityConditionList(entityConditionList, (EntityJoinOperator) operator);
        }
    }
    public static class ConditionObject implements Condition {
        protected FlexibleMapAccessor fieldNameAcsr;
        
        public ConditionObject(Element conditionExprElement) {
            this.fieldNameAcsr = new FlexibleMapAccessor(conditionExprElement.getAttribute("field-name"));
            if (this.fieldNameAcsr.isEmpty()) {
                // no "field-name"? try "name"
                this.fieldNameAcsr = new FlexibleMapAccessor(conditionExprElement.getAttribute("name"));
            }
        }
        
        public EntityCondition createCondition(Map context, String entityName, GenericDelegator delegator) {
            EntityCondition condition = (EntityCondition) fieldNameAcsr.get(context);
            return condition;
        }
    }
    
    public static interface OutputHandler extends Serializable {
        public void handleOutput(EntityListIterator eli, Map context, FlexibleMapAccessor listAcsr);
        public void handleOutput(List results, Map context, FlexibleMapAccessor listAcsr);
    }
    public static class LimitRange implements OutputHandler {
        FlexibleStringExpander startExdr;
        FlexibleStringExpander sizeExdr;
        
        public LimitRange(Element limitRangeElement) {
            this.startExdr = new FlexibleStringExpander(limitRangeElement.getAttribute("start"));
            this.sizeExdr = new FlexibleStringExpander(limitRangeElement.getAttribute("size"));
        }
        
        int getStart(Map context) {
            String startStr = this.startExdr.expandString(context);
            try {
                return Integer.parseInt(startStr);
            } catch (NumberFormatException e) {
                String errMsg = "The limit-range start number \"" + startStr + "\" was not valid: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new IllegalArgumentException(errMsg);
            }
        }
        
        int getSize(Map context) {
            String sizeStr = this.sizeExdr.expandString(context);
            try {
                return Integer.parseInt(sizeStr);
            } catch (NumberFormatException e) {
                String errMsg = "The limit-range size number \"" + sizeStr + "\" was not valid: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new IllegalArgumentException(errMsg);
            }
        }
        
        public void handleOutput(EntityListIterator eli, Map context, FlexibleMapAccessor listAcsr) {
            int start = getStart(context);
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

        public void handleOutput(List results, Map context, FlexibleMapAccessor listAcsr) {
            int start = getStart(context);
            int size = getSize(context);
            
            int end = start + size;
            if (end > results.size()) end = results.size();
            
            listAcsr.put(context, results.subList(start, end));
        }
    }
    public static class LimitView implements OutputHandler {
        FlexibleStringExpander viewIndexExdr;
        FlexibleStringExpander viewSizeExdr;
        
        public LimitView(Element limitViewElement) {
            this.viewIndexExdr = new FlexibleStringExpander(limitViewElement.getAttribute("view-index"));
            this.viewSizeExdr = new FlexibleStringExpander(limitViewElement.getAttribute("view-size"));
        }
        
        int getIndex(Map context) {
            String viewIndexStr = this.viewIndexExdr.expandString(context);
            try {
                return Integer.parseInt(viewIndexStr);
            } catch (NumberFormatException e) {
                String errMsg = "The limit-view view-index number \"" + viewIndexStr + "\" was not valid: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new IllegalArgumentException(errMsg);
            }
        }
        
        int getSize(Map context) {
            String viewSizeStr = this.viewSizeExdr.expandString(context);
            try {
                return Integer.parseInt(viewSizeStr);
            } catch (NumberFormatException e) {
                String errMsg = "The limit-view view-size number \"" + viewSizeStr + "\" was not valid: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new IllegalArgumentException(errMsg);
            }
        }
        
        public void handleOutput(EntityListIterator eli, Map context, FlexibleMapAccessor listAcsr) {
            int index = this.getIndex(context);
            int size = this.getSize(context);
            
            try {
                listAcsr.put(context, eli.getPartialList(((index - 1) * size) + 1, size));
                eli.close();
            } catch (GenericEntityException e) {
                String errMsg = "Error getting partial list in limit-view with index=" + index + " and size=" + size + ": " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new IllegalArgumentException(errMsg);
            }
        }

        public void handleOutput(List results, Map context, FlexibleMapAccessor listAcsr) {
            int index = this.getIndex(context);
            int size = this.getSize(context);
            
            int begin = index * size;
            int end = index * size + size;
            if (end > results.size()) end = results.size();
            
            listAcsr.put(context, results.subList(begin, end));
        }
    }
    public static class UseIterator implements OutputHandler {
        public UseIterator(Element useIteratorElement) {
            // no parameters, nothing to do
        }
        
        public void handleOutput(EntityListIterator eli, Map context, FlexibleMapAccessor listAcsr) {
            listAcsr.put(context, eli);
        }

        public void handleOutput(List results, Map context, FlexibleMapAccessor listAcsr) {
            throw new IllegalArgumentException("Cannot handle output with use-iterator when the query is cached, or the result in general is not an EntityListIterator");
        }
    }
    public static class GetAll implements OutputHandler {
        public GetAll() {
            // no parameters, nothing to do
        }
        
        public void handleOutput(EntityListIterator eli, Map context, FlexibleMapAccessor listAcsr) {
            try {
                listAcsr.put(context, eli.getCompleteList());
                eli.close();
            } catch (GenericEntityException e) {
                String errorMsg = "Error getting list from EntityListIterator: " + e.toString();
                Debug.logError(e, errorMsg, module);
                throw new IllegalArgumentException(errorMsg);
            }
        }

        public void handleOutput(List results, Map context, FlexibleMapAccessor listAcsr) {
            listAcsr.put(context, results);
        }
    }
}

