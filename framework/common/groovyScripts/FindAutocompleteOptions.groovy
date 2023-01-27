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

import org.apache.ofbiz.base.util.StringUtil
import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityConditionList
import org.apache.ofbiz.entity.condition.EntityExpr
import org.apache.ofbiz.entity.condition.EntityFieldValue
import org.apache.ofbiz.entity.condition.EntityFunction
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.entity.util.EntityFindOptions

import java.sql.Timestamp

List mainAndConds = []
List orExprs = []
String entityName = context.entityName
List searchFields = context.searchFields
List displayFields = context.displayFields ?: searchFields
boolean searchDistinct = Boolean.valueOf(context.searchDistinct ?: false)

String searchValueFieldName = parameters.term
String fieldValue = null
if (searchValueFieldName) {
    fieldValue = searchValueFieldName
} else if (parameters.searchValueFieldName) { // This is to find the description of a lookup value on initialization.
    fieldValue = parameters.get(parameters.searchValueFieldName)
    context.description = 'true'
}

String searchType = context.searchType
Set<String> displayFieldsSet = null

Map conditionDates = context.conditionDates
String fromDateName = null
String thruDateName = null
Timestamp filterByDateValue = null

//If conditionDates is present on context, resolve values use add condition date to the condition search
if (conditionDates) {
    filterByDateValue = conditionDates.filterByDateValue ?: UtilDateTime.nowTimestamp()
    fromDateName = conditionDates.fromDateName ?: null
    thruDateName = conditionDates.thruDateName ?: null
    //if the field filterByDate is present, init default value for fromDate and thruDate
    if (!fromDateName && !thruDateName) {
        fromDateName = 'fromDate'
        thruDateName = 'thruDate'
    }
}

if (searchFields && fieldValue) {
    List<String> searchFieldsList = StringUtil.toList(searchFields)
    displayFieldsSet = StringUtil.toSet(displayFields)
    if (context.description && fieldValue instanceof String) {
        returnField = parameters.searchValueFieldName
    } else {
        returnField = searchFieldsList[0] //default to first element of searchFields
        displayFieldsSet.add(returnField) //add it to select fields, in case it is missing
    }
    context.returnField = returnField
    context.displayFieldsSet = displayFieldsSet
    if (searchType == 'STARTS_WITH') {
        searchValue = fieldValue.toUpperCase() + '%'
    } else if (searchType == 'EQUALS') {
        searchValue = fieldValue
    } else {//default is CONTAINS
        searchValue = '%' + fieldValue.toUpperCase() + '%'
    }
    searchFieldsList.each { fieldName ->
        if (searchType == 'EQUALS') {
            orExprs.add(EntityCondition.makeCondition(EntityFieldValue.makeFieldValue(searchFieldsList[0]), EntityOperator.EQUALS, searchValue))
            return //in case of EQUALS, we search only a match for the returned field
        }
        orExprs.add(EntityCondition.makeCondition(EntityFunction.upper(EntityFieldValue.makeFieldValue(fieldName)),
                EntityOperator.LIKE, searchValue))
    }
}

/* the following is part of an attempt to handle additional parameters that are passed in from other form fields at run-time,
 * but that is not supported by the Jquery Autocompleter, but this is still useful to pass parameters from the
 * lookup screen definition:
 */
Map conditionFields = context.conditionFields
if (conditionFields) {
    // these fields are for additonal conditions, this is a Map of name/value pairs
    for (conditionFieldEntry in conditionFields.entrySet()) {
        if (conditionFieldEntry.getValue() instanceof List) {
            List orCondFields = []
            conditionFieldEntry.getValue().each { entry ->
                orCondFields.add(EntityCondition.makeCondition(EntityFieldValue.makeFieldValue(conditionFieldEntry.getKey()),
                        EntityOperator.EQUALS, entry))
            }
            mainAndConds.add(EntityCondition.makeCondition(orCondFields, EntityOperator.OR))
        } else {
            mainAndConds.add(EntityCondition.makeCondition(EntityFieldValue.makeFieldValue(conditionFieldEntry.getKey()),
                    EntityOperator.EQUALS, conditionFieldEntry.getValue()))
        }
    }
}

if (orExprs && entityName && displayFieldsSet) {
    mainAndConds.add(EntityCondition.makeCondition(orExprs, EntityOperator.OR))

    //if there is an extra condition, add it to main condition list
    if (context.andCondition && context.andCondition instanceof EntityCondition) {
        mainAndConds.add(context.andCondition)
    }
    if (conditionDates) {
        List condsDateList = []
        if (thruDateName) {
            List condsByThruDate = []
            condsByThruDate.add(EntityCondition.makeCondition(EntityFieldValue.makeFieldValue(thruDateName),
                    EntityOperator.GREATER_THAN, filterByDateValue))
            condsByThruDate.add(EntityCondition.makeCondition(EntityFieldValue.makeFieldValue(thruDateName), EntityOperator.EQUALS, null))
            condsDateList.add(EntityCondition.makeCondition(condsByThruDate, EntityOperator.OR))
        }

        if (fromDateName) {
            List condsByFromDate = []
            condsByFromDate.add(EntityCondition.makeCondition(EntityFieldValue.makeFieldValue(fromDateName),
                    EntityOperator.LESS_THAN_EQUAL_TO, filterByDateValue))
            condsByFromDate.add(EntityCondition.makeCondition(EntityFieldValue.makeFieldValue(fromDateName), EntityOperator.EQUALS, null))
            condsDateList.add(EntityCondition.makeCondition(condsByFromDate, EntityOperator.OR))
        }

        mainAndConds.add(EntityCondition.makeCondition(condsDateList, EntityOperator.AND))
    }

    EntityConditionList<EntityExpr> entityConditionList = EntityCondition.makeCondition(mainAndConds, EntityOperator.AND)

    String viewSizeStr = context.autocompleterViewSize
    Integer autocompleterViewSize = Integer.valueOf(viewSizeStr ?: 10)
    EntityFindOptions findOptions = new EntityFindOptions()
    findOptions.setMaxRows(autocompleterViewSize)
    findOptions.setDistinct(searchDistinct)

    autocompleteOptions = delegator.findList(entityName, entityConditionList, displayFieldsSet, StringUtil.toList(displayFields), findOptions, false)
    if (autocompleteOptions) {
        context.autocompleteOptions = autocompleteOptions
    }
}
