/*
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
 */

import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityFieldValue;
import org.ofbiz.entity.condition.EntityFunction;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtilProperties;

def mainAndConds = [];
def orExprs = [];
def entityName = context.entityName;
def searchFields = context.searchFields;
def displayFields = context.displayFields ?: searchFields;
def searchDistinct = Boolean.valueOf(context.searchDistinct ?: false);

def searchValueFieldName = parameters.term;
def fieldValue = null;
if (searchValueFieldName) {
    fieldValue = searchValueFieldName; 
} else if (parameters.searchValueFieldName) { // This is to find the description of a lookup value on initialization.
    fieldValue = parameters.get(parameters.searchValueFieldName);
    context.description = "true";
}

def searchType = context.searchType;
def displayFieldsSet = null;

if (searchFields && fieldValue) {
    def searchFieldsList = StringUtil.toList(searchFields);
    displayFieldsSet = StringUtil.toSet(displayFields);
    if (context.description && fieldValue instanceof java.lang.String) {
        returnField = parameters.searchValueFieldName;
    } else {
        returnField = searchFieldsList[0]; //default to first element of searchFields
        displayFieldsSet.add(returnField); //add it to select fields, in case it is missing
    }
    context.returnField = returnField;
    context.displayFieldsSet = displayFieldsSet;
    if ("STARTS_WITH".equals(searchType)) {
        searchValue = fieldValue.toUpperCase() + "%";
    } else if ("EQUALS".equals(searchType)) {
        searchValue = fieldValue;
    } else {//default is CONTAINS
        searchValue = "%" + fieldValue.toUpperCase() + "%";
    }
    searchFieldsList.each { fieldName ->
        if ("EQUALS".equals(searchType)) {
            orExprs.add(EntityCondition.makeCondition(EntityFieldValue.makeFieldValue(searchFieldsList[0]), EntityOperator.EQUALS, searchValue));
            return;//in case of EQUALS, we search only a match for the returned field
        } else {
            orExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER(EntityFieldValue.makeFieldValue(fieldName)), EntityOperator.LIKE, searchValue));
        }        
    }
}

/* the following is part of an attempt to handle additional parameters that are passed in from other form fields at run-time,
 * but that is not supported by the Jquery Autocompleter, but this is still useful to pass parameters from the
 * lookup screen definition:
 */
def conditionFields = context.conditionFields;
if (conditionFields) {
    // these fields are for additonal conditions, this is a Map of name/value pairs
    for (conditionFieldEntry in conditionFields.entrySet()) {
        if (conditionFieldEntry.getValue() instanceof java.util.List) {
            def orCondFields = [];
            for (entry in conditionFieldEntry.getValue()) {
                orCondFields.add(EntityCondition.makeCondition(EntityFieldValue.makeFieldValue(conditionFieldEntry.getKey()), EntityOperator.EQUALS, entry));
            }
            mainAndConds.add(EntityCondition.makeCondition(orCondFields, EntityOperator.OR));
        } else {
            mainAndConds.add(EntityCondition.makeCondition(EntityFieldValue.makeFieldValue(conditionFieldEntry.getKey()), EntityOperator.EQUALS, conditionFieldEntry.getValue()));
        }
    }
}

if (orExprs && entityName && displayFieldsSet) {
    mainAndConds.add(EntityCondition.makeCondition(orExprs, EntityOperator.OR));

    //if there is an extra condition, add it to main condition list
    if (context.andCondition && context.andCondition instanceof EntityCondition) {
        mainAndConds.add(context.andCondition);
    }

    def entityConditionList = EntityCondition.makeCondition(mainAndConds, EntityOperator.AND);

    String viewSizeStr = context.autocompleterViewSize;
    if (viewSizeStr == null) {
        viewSizeStr = EntityUtilProperties.getPropertyValue("widget", "widget.autocompleter.defaultViewSize", delegator);
    }
    Integer autocompleterViewSize = Integer.valueOf(viewSizeStr ?: 10);
    EntityFindOptions findOptions = new EntityFindOptions();
    findOptions.setMaxRows(autocompleterViewSize);
    findOptions.setDistinct(searchDistinct);

    autocompleteOptions = delegator.findList(entityName, entityConditionList, displayFieldsSet, StringUtil.toList(displayFields), findOptions, false);
    if (autocompleteOptions) {
        context.autocompleteOptions = autocompleteOptions;
    }
}
