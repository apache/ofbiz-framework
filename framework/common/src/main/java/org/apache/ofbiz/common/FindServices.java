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
package org.apache.ofbiz.common;

import static org.apache.ofbiz.base.util.UtilGenerics.checkCollection;
import static org.apache.ofbiz.base.util.UtilGenerics.checkMap;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntity;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityComparisonOperator;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.entity.condition.EntityFunction;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelField;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * FindServices Class
 */
public class FindServices {

    public static final String module = FindServices.class.getName();
    public static final String resource = "CommonUiLabels";
    public static final Map<String, EntityComparisonOperator<?, ?>> entityOperators;

    static {
        entityOperators =  new LinkedHashMap<>();
        entityOperators.put("between", EntityOperator.BETWEEN);
        entityOperators.put("equals", EntityOperator.EQUALS);
        entityOperators.put("greaterThan", EntityOperator.GREATER_THAN);
        entityOperators.put("greaterThanEqualTo", EntityOperator.GREATER_THAN_EQUAL_TO);
        entityOperators.put("in", EntityOperator.IN);
        entityOperators.put("not-in", EntityOperator.NOT_IN);
        entityOperators.put("lessThan", EntityOperator.LESS_THAN);
        entityOperators.put("lessThanEqualTo", EntityOperator.LESS_THAN_EQUAL_TO);
        entityOperators.put("like", EntityOperator.LIKE);
        entityOperators.put("notLike", EntityOperator.NOT_LIKE);
        entityOperators.put("not", EntityOperator.NOT);
        entityOperators.put("notEqual", EntityOperator.NOT_EQUAL);
    }

    public FindServices() {}

    /**
     * prepareField, analyse inputFields to created normalizedFields a map with field name and operator.
     *
     * This is use to the generic method that expects entity data affixed with special suffixes
     * to indicate their purpose in formulating an SQL query statement.
     * @param inputFields     Input parameters run thru UtilHttp.getParameterMap
     * @return a map with field name and operator
     */
    public static Map<String, Map<String, Map<String, Object>>> prepareField(Map<String, ?> inputFields, Map<String, Object> queryStringMap, Map<String, List<Object[]>> origValueMap) {
        // Strip the "_suffix" off of the parameter name and
        // build a three-level map of values keyed by fieldRoot name,
        //    fld0 or fld1,  and, then, "op" or "value"
        // ie. id
        //  - fld0
        //      - op:like
        //      - value:abc
        //  - fld1 (if there is a range)
        //      - op:lessThan
        //      - value:55 (note: these two "flds" wouldn't really go together)
        // Also note that op/fld can be in any order. (eg. id_fld1_equals or id_equals_fld1)
        // Note that "normalizedFields" will contain values other than those
        // Contained in the associated entity.
        // Those extra fields will be ignored in the second half of this method.
        Map<String, Map<String, Map<String, Object>>> normalizedFields = new LinkedHashMap<>();
        for (Entry<String, ?> entry : inputFields.entrySet()) { // The name as it appears in the HTML form
            String fieldNameRaw = entry.getKey();
            String fieldNameRoot = null; // The entity field name. Everything to the left of the first "_" if
                                                                 //  it exists, or the whole word, if not.
            String fieldPair = null; // "fld0" or "fld1" - begin/end of range or just fld0 if no range.
            Object fieldValue = null; // If it is a "value" field, it will be the value to be used in the query.
                                                        // If it is an "op" field, it will be "equals", "greaterThan", etc.
            int iPos = -1;
            int iPos2 = -1;
            Map<String, Map<String, Object>> subMap = null;
            Map<String, Object> subMap2 = null;
            String fieldMode = null;

            fieldValue = entry.getValue();
            if (ObjectType.isEmpty(fieldValue)) {
                continue;
            }

            queryStringMap.put(fieldNameRaw, fieldValue);
            iPos = fieldNameRaw.indexOf('_'); // Look for suffix

            // This is a hack to skip fields from "multi" forms
            // These would have the form "fieldName_o_1"
            if (iPos >= 0) {
                String suffix = fieldNameRaw.substring(iPos + 1);
                iPos2 = suffix.indexOf('_');
                if (iPos2 == 1) {
                    continue;
                }
            }

            // If no suffix, assume no range (default to fld0) and operations of equals
            // If no field op is present, it will assume "equals".
            if (iPos < 0) {
                fieldNameRoot = fieldNameRaw;
                fieldPair = "fld0";
                fieldMode = "value";
            } else { // Must have at least "fld0/1" or "equals, greaterThan, etc."
                // Some bogus fields will slip in, like "ENTITY_NAME", but they will be ignored

                fieldNameRoot = fieldNameRaw.substring(0, iPos);
                String suffix = fieldNameRaw.substring(iPos + 1);
                iPos2 = suffix.indexOf('_');
                if (iPos2 < 0) {
                    if (suffix.startsWith("fld")) {
                        // If only one token and it starts with "fld"
                        //  assume it is a value field, not an op
                        fieldPair = suffix;
                        fieldMode = "value";
                    } else {
                        // if it does not start with fld, assume it is an op or the 'ignore case' (ic) field
                        fieldPair = "fld0";
                        fieldMode = suffix;
                    }
                } else {
                    String tkn0 = suffix.substring(0, iPos2);
                    String tkn1 = suffix.substring(iPos2 + 1);
                    // If suffix has two parts, let them be in any order
                    // One will be "fld0/1" and the other will be the op (eg. equals, greaterThan_
                    if (tkn0.startsWith("fld")) {
                        fieldPair = tkn0;
                        fieldMode = tkn1;
                    } else {
                        fieldPair = tkn1;
                        fieldMode = tkn0;
                    }
                }
            }
            subMap = normalizedFields.get(fieldNameRoot);
            if (subMap == null) {
                subMap = new LinkedHashMap<>();
                normalizedFields.put(fieldNameRoot, subMap);
            }
            subMap2 = subMap.get(fieldPair);
            if (subMap2 == null) {
                subMap2 = new LinkedHashMap<>();
                subMap.put(fieldPair, subMap2);
            }
            subMap2.put(fieldMode, fieldValue);

            List<Object[]> origList = origValueMap.get(fieldNameRoot);
            if (origList == null) {
                origList = new LinkedList<>();
                origValueMap.put(fieldNameRoot, origList);
            }
            Object [] origValues = {fieldNameRaw, fieldValue};
            origList.add(origValues);
        }
        return normalizedFields;
    }

    /**
     * Parses input parameters and returns an <code>EntityCondition</code> list.
     *
     * @param parameters
     * @param fieldList
     * @param queryStringMap
     * @param delegator
     * @param context
     * @return returns an EntityCondition list
     */
    public static List<EntityCondition> createConditionList(Map<String, ? extends Object> parameters, List<ModelField> fieldList, Map<String, Object> queryStringMap, Delegator delegator, Map<String, ?> context) {
        Set<String> processed = new LinkedHashSet<>();
        Set<String> keys = new LinkedHashSet<>();
        Map<String, ModelField> fieldMap = new LinkedHashMap<>();
        /**
         * When inputFields contains several xxxx_grp, yyyy_grp ... values,
         * Corresponding conditions will grouped by an {@link EntityOperator.AND} then all added to final
         * condition grouped by an {@link EntityOperator.OR}
         * That will allow union of search criteria, instead of default intersection.
         */
        Map<String, List<EntityCondition>> savedGroups = new LinkedHashMap<>();
        for (ModelField modelField : fieldList) {
            fieldMap.put(modelField.getName(), modelField);
        }
        List<EntityCondition> result = new LinkedList<>();
        for (Map.Entry<String, ? extends Object> entry : parameters.entrySet()) {
            String currentGroup = null;
            String parameterName = entry.getKey();
            if (processed.contains(parameterName)) {
                continue;
            }
            keys.clear();
            String fieldName = parameterName;
            Object fieldValue = null;
            String operation = null;
            boolean ignoreCase = false;
            if (parameterName.endsWith("_ic") || parameterName.endsWith("_op")) {
                fieldName = parameterName.substring(0, parameterName.length() - 3);
            } else if (parameterName.endsWith("_value")) {
                fieldName = parameterName.substring(0, parameterName.length() - 6);
            }

            String key = fieldName.concat("_grp");
            if (parameters.containsKey(key)) {
                if (parameters.containsKey(key)) {
                    keys.add(key);
                }
                currentGroup = (String) parameters.get(key);
            }
            key = fieldName.concat("_ic");
            if (parameters.containsKey(key)) {
                keys.add(key);
                ignoreCase = "Y".equals(parameters.get(key));
            }
            key = fieldName.concat("_op");
            if (parameters.containsKey(key)) {
                keys.add(key);
                operation = (String) parameters.get(key);
            }
            key = fieldName.concat("_value");
            if (parameters.containsKey(key)) {
                keys.add(key);
                fieldValue = parameters.get(key);
            }
            if (fieldName.endsWith("_fld0") || fieldName.endsWith("_fld1")) {
                if (parameters.containsKey(fieldName)) {
                    keys.add(fieldName);
                }
                fieldName = fieldName.substring(0, fieldName.length() - 5);
            }
            if (parameters.containsKey(fieldName)) {
                keys.add(fieldName);
            }
            processed.addAll(keys);
            ModelField modelField = fieldMap.get(fieldName);
            if (modelField == null) {
                continue;
            }
            if (fieldValue == null) {
                fieldValue = parameters.get(fieldName);
            }
            if (ObjectType.isEmpty(fieldValue) && !"empty".equals(operation)) {
                continue;
            }
            if (UtilValidate.isNotEmpty(currentGroup)){
                List<EntityCondition> groupedConditions = new LinkedList<>();
                if(savedGroups.get(currentGroup) != null) {
                    groupedConditions.addAll(savedGroups.get(currentGroup));
                }
                groupedConditions.add(createSingleCondition(modelField, operation, fieldValue, ignoreCase, delegator, context));
                savedGroups.put(currentGroup, groupedConditions);
            } else {
                result.add(createSingleCondition(modelField, operation, fieldValue, ignoreCase, delegator, context));
            }

            for (String mapKey : keys) {
                queryStringMap.put(mapKey, parameters.get(mapKey));
            }
        }
        //Add OR-grouped conditions
        List<EntityCondition> orConditions = new LinkedList<>();
        for (String groupedConditions : savedGroups.keySet()) {
            orConditions.add(EntityCondition.makeCondition(savedGroups.get(groupedConditions)));
        }
        if (orConditions.size() > 0) result.add(EntityCondition.makeCondition(orConditions, EntityOperator.OR));

        return result;
    }

    /**
     * Creates a single <code>EntityCondition</code> based on a set of parameters.
     *
     * @param modelField
     * @param operation
     * @param fieldValue
     * @param ignoreCase
     * @param delegator
     * @param context
     * @return return an EntityCondition
     */
    public static EntityCondition createSingleCondition(ModelField modelField, String operation, Object fieldValue, boolean ignoreCase, Delegator delegator, Map<String, ?> context) {
        EntityCondition cond = null;
        String fieldName = modelField.getName();
        Locale locale = (Locale) context.get("locale");
        TimeZone timeZone = (TimeZone) context.get("timeZone");
        EntityComparisonOperator<?, ?> fieldOp = null;
        if (operation != null) {
            if (operation.equals("contains")) {
                fieldOp = EntityOperator.LIKE;
                fieldValue = "%" + fieldValue + "%";
            } else if ("not-contains".equals(operation) || "notContains".equals(operation)) {
                fieldOp = EntityOperator.NOT_LIKE;
                fieldValue = "%" + fieldValue + "%";
            } else if (operation.equals("empty")) {
                return EntityCondition.makeCondition(fieldName, EntityOperator.EQUALS, null);
            } else if (operation.equals("like")) {
                fieldOp = EntityOperator.LIKE;
                fieldValue = fieldValue + "%";
            } else if ("not-like".equals(operation) || "notLike".equals(operation)) {
                fieldOp = EntityOperator.NOT_LIKE;
                fieldValue = fieldValue + "%";
            } else if ("opLessThan".equals(operation)) {
                fieldOp = EntityOperator.LESS_THAN;
            } else if ("upToDay".equals(operation)) {
                fieldOp = EntityOperator.LESS_THAN;
            } else if ("upThruDay".equals(operation)) {
                fieldOp = EntityOperator.LESS_THAN_EQUAL_TO;
            } else if (operation.equals("greaterThanFromDayStart")) {
                String timeStampString = (String) fieldValue;
                Object startValue = modelField.getModelEntity().convertFieldValue(modelField, dayStart(timeStampString, 0, timeZone, locale), delegator, context);
                return EntityCondition.makeCondition(fieldName, EntityOperator.GREATER_THAN_EQUAL_TO, startValue);
            } else if (operation.equals("sameDay")) {
                String timeStampString = (String) fieldValue;
                Object startValue = modelField.getModelEntity().convertFieldValue(modelField, dayStart(timeStampString, 0, timeZone, locale), delegator, context);
                EntityCondition startCond = EntityCondition.makeCondition(fieldName, EntityOperator.GREATER_THAN_EQUAL_TO, startValue);
                Object endValue = modelField.getModelEntity().convertFieldValue(modelField, dayStart(timeStampString, 1, timeZone, locale), delegator, context);
                EntityCondition endCond = EntityCondition.makeCondition(fieldName, EntityOperator.LESS_THAN, endValue);
                return EntityCondition.makeCondition(startCond, endCond);
            } else {
                fieldOp = entityOperators.get(operation);
            }
        } else {
            List<Object> fieldList = (fieldValue instanceof List) ? UtilGenerics.cast(fieldValue) : null;
            if (UtilValidate.isNotEmpty(fieldList)) {
                fieldOp = EntityOperator.IN;
            } else {
                fieldOp = EntityOperator.EQUALS;
            }
        }
        Object fieldObject = fieldValue;
        if ((fieldOp != EntityOperator.IN && fieldOp != EntityOperator.NOT_IN ) || !(fieldValue instanceof Collection<?>)) {
            fieldObject = modelField.getModelEntity().convertFieldValue(modelField, fieldValue, delegator, context);
        }
        if (ignoreCase && fieldObject instanceof String) {
            cond = EntityCondition.makeCondition(EntityFunction.UPPER_FIELD(fieldName), fieldOp, EntityFunction.UPPER(
                    ((String) fieldValue).toUpperCase(Locale.getDefault())));
        } else {
            if (fieldObject.equals(GenericEntity.NULL_FIELD.toString())) {
                fieldObject = null;
            }
            cond = EntityCondition.makeCondition(fieldName, fieldOp, fieldObject);
        }
        if (EntityOperator.NOT_EQUAL.equals(fieldOp) && fieldObject != null) {
            cond = EntityCondition.makeCondition(UtilMisc.toList(cond, EntityCondition.makeCondition(fieldName, null)), EntityOperator.OR);
        }
        return cond;
    }

    /**
     * createCondition, comparing the normalizedFields with the list of keys, .
     *
     * This is use to the generic method that expects entity data affixed with special suffixes
     * to indicate their purpose in formulating an SQL query statement.
     * @param modelEntity the model entity object
     * @param normalizedFields list of field the user have populated
     * @return a arrayList usable to create an entityCondition
     */
    public static List<EntityCondition> createCondition(ModelEntity modelEntity, Map<String, Map<String, Map<String, Object>>> normalizedFields, Map<String, Object> queryStringMap, Map<String, List<Object[]>> origValueMap, Delegator delegator, Map<String, ?> context) {
        Map<String, Map<String, Object>> subMap = null;
        Map<String, Object> subMap2 = null;
        Object fieldValue = null; // If it is a "value" field, it will be the value to be used in the query.
                                  // If it is an "op" field, it will be "equals", "greaterThan", etc.
        EntityCondition cond = null;
        List<EntityCondition> tmpList = new LinkedList<>();
        String opString = null;
        boolean ignoreCase = false;
        List<ModelField> fields = modelEntity.getFieldsUnmodifiable();
        for (ModelField modelField: fields) {
            String fieldName = modelField.getName();
            subMap = normalizedFields.get(fieldName);
            if (subMap == null) {
                continue;
            }
            subMap2 = subMap.get("fld0");
            fieldValue = subMap2.get("value");
            opString = (String) subMap2.get("op");
            // null fieldValue is OK if operator is "empty"
            if (fieldValue == null && !"empty".equals(opString)) {
                continue;
            }
            ignoreCase = "Y".equals(subMap2.get("ic"));
            cond = createSingleCondition(modelField, opString, fieldValue, ignoreCase, delegator, context);
            tmpList.add(cond);
            subMap2 = subMap.get("fld1");
            if (subMap2 == null) {
                continue;
            }
            fieldValue = subMap2.get("value");
            opString = (String) subMap2.get("op");
            if (fieldValue == null && !"empty".equals(opString)) {
                continue;
            }
            ignoreCase = "Y".equals(subMap2.get("ic"));
            cond = createSingleCondition(modelField, opString, fieldValue, ignoreCase, delegator, context);
            tmpList.add(cond);
            // add to queryStringMap
            List<Object[]> origList = origValueMap.get(fieldName);
            if (UtilValidate.isNotEmpty(origList)) {
                for (Object[] arr: origList) {
                    queryStringMap.put((String) arr[0], arr[1]);
                }
            }
        }
        return tmpList;
    }

    /**
     *
     *  same as performFind but now returning a list instead of an iterator
     *  Extra parameters viewIndex: startPage of the partial list (0 = first page)
     *                              viewSize: the length of the page (number of records)
     *  Extra output parameter: listSize: size of the totallist
     *                                         list : the list itself.
     *
     * @param dctx
     * @param context
     * @return Map
     */
    public static Map<String, Object> performFindList(DispatchContext dctx, Map<String, Object> context) {
        Integer viewSize = (Integer) context.get("viewSize");
        if (viewSize == null) {
            viewSize = 20;       // default
        }
        context.put("viewSize", viewSize);
        Integer viewIndex = (Integer) context.get("viewIndex");
        if (viewIndex == null) {
            viewIndex = 0;  // default
        }
        context.put("viewIndex", viewIndex);

        Map<String, Object> result = performFind(dctx,context);

        int start = viewIndex * viewSize;
        List<GenericValue> list = null;
        Integer listSize = 0;
        try (EntityListIterator it = (EntityListIterator) result.get("listIt")) {
            list = it.getPartialList(start+1, viewSize); // list starts at '1'
            listSize = it.getResultsSizeAfterPartialList();
        } catch (ClassCastException | NullPointerException | GenericEntityException e) {
            Debug.logInfo("Problem getting partial list" + e, module);
        }

        result.put("listSize", listSize);
        result.put("list",list);
        result.remove("listIt");
        return result;
    }

    /**
     * performFind
     *
     * This is a generic method that expects entity data affixed with special suffixes
     * to indicate their purpose in formulating an SQL query statement.
     */
    public static Map<String, Object> performFind(DispatchContext dctx, Map<String, ?> context) {
        String entityName = (String) context.get("entityName");
        String orderBy = (String) context.get("orderBy");
        Map<String, ?> inputFields = checkMap(context.get("inputFields"), String.class, Object.class); // Input
        String noConditionFind = (String) context.get("noConditionFind");
        String distinct = (String) context.get("distinct");
        List<String> fieldList =  UtilGenerics.cast(context.get("fieldList"));
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = dctx.getDelegator();
        if (UtilValidate.isEmpty(noConditionFind)) {
            // try finding in inputFields Map
            noConditionFind = (String) inputFields.get("noConditionFind");
        }
        if (UtilValidate.isEmpty(noConditionFind)) {
            // Use configured default
            noConditionFind = EntityUtilProperties.getPropertyValue("widget", "widget.defaultNoConditionFind", delegator);
        }
        String filterByDate = (String) context.get("filterByDate");
        if (UtilValidate.isEmpty(filterByDate)) {
            // try finding in inputFields Map
            filterByDate = (String) inputFields.get("filterByDate");
        }
        Timestamp filterByDateValue = (Timestamp) context.get("filterByDateValue");
        String fromDateName = (String) context.get("fromDateName");
        if (UtilValidate.isEmpty(fromDateName)) {
            // try finding in inputFields Map
            fromDateName = (String) inputFields.get("fromDateName");
        }
        String thruDateName = (String) context.get("thruDateName");
        if (UtilValidate.isEmpty(thruDateName)) {
            // try finding in inputFields Map
            thruDateName = (String) inputFields.get("thruDateName");
        }

        Integer viewSize = (Integer) context.get("viewSize");
        Integer viewIndex = (Integer) context.get("viewIndex");
        Integer maxRows = null;
        if (viewSize != null && viewIndex != null) {
            maxRows = viewSize * (viewIndex + 1);
        }

        LocalDispatcher dispatcher = dctx.getDispatcher();

        Map<String, Object> prepareResult = null;
        try {
            prepareResult = dispatcher.runSync("prepareFind", UtilMisc.toMap("entityName", entityName, "orderBy", orderBy,
                                               "inputFields", inputFields, "filterByDate", filterByDate, "noConditionFind", noConditionFind,
                                               "filterByDateValue", filterByDateValue, "userLogin", userLogin, "fromDateName", fromDateName, "thruDateName", thruDateName,
                                               "locale", context.get("locale"), "timeZone", context.get("timeZone")));
        } catch (GenericServiceException gse) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "CommonFindErrorPreparingConditions", UtilMisc.toMap("errorString", gse.getMessage()), locale));
        }
        EntityConditionList<EntityCondition> exprList = UtilGenerics.cast(prepareResult.get("entityConditionList"));
        List<String> orderByList = checkCollection(prepareResult.get("orderByList"), String.class);

        Map<String, Object> executeResult = null;
        try {
            executeResult = dispatcher.runSync("executeFind", UtilMisc.toMap("entityName", entityName, "orderByList", orderByList,
                                                                             "fieldList", fieldList, "entityConditionList", exprList,
                                                                             "noConditionFind", noConditionFind, "distinct", distinct,
                                                                             "locale", context.get("locale"), "timeZone", context.get("timeZone"),
                                                                             "maxRows", maxRows));
        } catch (GenericServiceException gse) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "CommonFindErrorRetrieveIterator", UtilMisc.toMap("errorString", gse.getMessage()), locale));
        }

        if (executeResult.get("listIt") == null) {
            if (Debug.verboseOn()) {
                Debug.logVerbose("No list iterator found for query string + [" + prepareResult.get("queryString") + "]", module);
            }
        }

        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put("listIt", executeResult.get("listIt"));
        results.put("listSize", executeResult.get("listSize"));
        results.put("queryString", prepareResult.get("queryString"));
        results.put("queryStringMap", prepareResult.get("queryStringMap"));
        return results;
    }

    /**
     * prepareFind
     *
     * This is a generic method that expects entity data affixed with special suffixes
     * to indicate their purpose in formulating an SQL query statement.
     */
    public static Map<String, Object> prepareFind(DispatchContext dctx, Map<String, ?> context) {
        String entityName = (String) context.get("entityName");
        Delegator delegator = dctx.getDelegator();
        String orderBy = (String) context.get("orderBy");
        Map<String, ?> inputFields = checkMap(context.get("inputFields"), String.class, Object.class); // Input
        String noConditionFind = (String) context.get("noConditionFind");
        if (UtilValidate.isEmpty(noConditionFind)) {
            // try finding in inputFields Map
            noConditionFind = (String) inputFields.get("noConditionFind");
        }
        if (UtilValidate.isEmpty(noConditionFind)) {
            // Use configured default
            noConditionFind = EntityUtilProperties.getPropertyValue("widget", "widget.defaultNoConditionFind", delegator);
        }
        String filterByDate = (String) context.get("filterByDate");
        if (UtilValidate.isEmpty(filterByDate)) {
            // try finding in inputFields Map
            filterByDate = (String) inputFields.get("filterByDate");
        }
        Timestamp filterByDateValue = (Timestamp) context.get("filterByDateValue");
        String fromDateName = (String) context.get("fromDateName");
        String thruDateName = (String) context.get("thruDateName");

        Map<String, Object> queryStringMap = new LinkedHashMap<>();
        ModelEntity modelEntity = delegator.getModelEntity(entityName);
        List<EntityCondition> tmpList = createConditionList(inputFields, modelEntity.getFieldsUnmodifiable(), queryStringMap, delegator, context);

        /* the filter by date condition should only be added when there are other conditions or when
         * the user has specified a noConditionFind.  Otherwise, specifying filterByDate will become
         * its own condition.
         */
        if (tmpList.size() > 0 || "Y".equals(noConditionFind)) {
            if ("Y".equals(filterByDate)) {
                queryStringMap.put("filterByDate", filterByDate);
                if (UtilValidate.isEmpty(fromDateName)) {
                    fromDateName = "fromDate";
                } else {
                    queryStringMap.put("fromDateName", fromDateName);
                }
                if (UtilValidate.isEmpty(thruDateName)) {
                    thruDateName = "thruDate";
                } else {
                    queryStringMap.put("thruDateName", thruDateName);
                }
                if (UtilValidate.isEmpty(filterByDateValue)) {
                    EntityCondition filterByDateCondition = EntityUtil.getFilterByDateExpr(fromDateName, thruDateName);
                    tmpList.add(filterByDateCondition);
                } else {
                    queryStringMap.put("filterByDateValue", filterByDateValue);
                    EntityCondition filterByDateCondition = EntityUtil.getFilterByDateExpr(filterByDateValue, fromDateName, thruDateName);
                    tmpList.add(filterByDateCondition);
                }
            }
        }

        EntityConditionList<EntityCondition> exprList = null;
        if (tmpList.size() > 0) {
            exprList = EntityCondition.makeCondition(tmpList);
        }

        List<String> orderByList = null;
        if (UtilValidate.isNotEmpty(orderBy)) {
            orderByList = StringUtil.split(orderBy,"|");
        }

        Map<String, Object> results = ServiceUtil.returnSuccess();
        queryStringMap.put("noConditionFind", noConditionFind);
        String queryString = UtilHttp.urlEncodeArgs(queryStringMap);
        results.put("queryString", queryString);
        results.put("queryStringMap", queryStringMap);
        results.put("orderByList", orderByList);
        results.put("entityConditionList", exprList);
        return results;
    }

    /**
     * executeFind
     *
     * This is a generic method that returns an EntityListIterator.
     */
    public static Map<String, Object> executeFind(DispatchContext dctx, Map<String, ?> context) {
        String entityName = (String) context.get("entityName");
        EntityConditionList<EntityCondition> entityConditionList = UtilGenerics.cast(context.get("entityConditionList"));
        List<String> orderByList = checkCollection(context.get("orderByList"), String.class);
        boolean noConditionFind = "Y".equals(context.get("noConditionFind"));
        boolean distinct = "Y".equals(context.get("distinct"));
        List<String> fieldList =  UtilGenerics.cast(context.get("fieldList"));
        Locale locale = (Locale) context.get("locale");
        Set<String> fieldSet = null;
        if (fieldList != null) {
            fieldSet = new LinkedHashSet<>(fieldList);
        }
        Integer maxRows = (Integer) context.get("maxRows");
        maxRows = maxRows != null ? maxRows : -1;
        Delegator delegator = dctx.getDelegator();
        // Retrieve entities  - an iterator over all the values
        EntityListIterator listIt = null;
        int listSize = 0;
        try {
            if (noConditionFind || (entityConditionList != null && entityConditionList.getConditionListSize() > 0)) {
                listIt = EntityQuery.use(delegator)
                                    .select(fieldSet)
                                    .from(entityName)
                                    .where(entityConditionList)
                                    .orderBy(orderByList)
                                    .cursorScrollInsensitive()
                                    .maxRows(maxRows)
                                    .distinct(distinct)
                                    .queryIterator();
                listSize = listIt.getResultsSizeAfterPartialList();
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "CommonFindErrorRunning", UtilMisc.toMap("entityName", entityName, "errorString", e.getMessage()), locale));
        }

        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put("listIt", listIt);
        results.put("listSize", listSize);
        return results;
    }

    private static String dayStart(String timeStampString, int daysLater, TimeZone timeZone, Locale locale) {
        String retValue = null;
        Timestamp ts = null;
        Timestamp startTs = null;
        try {
            ts = Timestamp.valueOf(timeStampString);
        } catch (IllegalArgumentException e) {
            timeStampString += " 00:00:00.000";
            try {
                ts = Timestamp.valueOf(timeStampString);
            } catch (IllegalArgumentException e2) {
                return retValue;
            }
        }
        startTs = UtilDateTime.getDayStart(ts, daysLater, timeZone, locale);
        retValue = startTs.toString();
        return retValue;
    }

    public static Map<String, Object> buildReducedQueryString(Map<String, ?> inputFields, String entityName, Delegator delegator) {
        // Strip the "_suffix" off of the parameter name and
        // build a three-level map of values keyed by fieldRoot name,
        //    fld0 or fld1,  and, then, "op" or "value"
        // ie. id
        //  - fld0
        //      - op:like
        //      - value:abc
        //  - fld1 (if there is a range)
        //      - op:lessThan
        //      - value:55 (note: these two "flds" wouldn't really go together)
        // Also note that op/fld can be in any order. (eg. id_fld1_equals or id_equals_fld1)
        // Note that "normalizedFields" will contain values other than those
        // Contained in the associated entity.
        // Those extra fields will be ignored in the second half of this method.
        ModelEntity modelEntity = delegator.getModelEntity(entityName);
        Map<String, Object> normalizedFields = new LinkedHashMap<>();
        //StringBuffer queryStringBuf = new StringBuffer();
        for (Entry<String, ?> entry : inputFields.entrySet()) { // The name as it appears in the HTML form
            String fieldNameRaw = entry.getKey();
            String fieldNameRoot = null; // The entity field name. Everything to the left of the first "_" if
                                                                 //  it exists, or the whole word, if not.
            Object fieldValue = null; // If it is a "value" field, it will be the value to be used in the query.
                                                        // If it is an "op" field, it will be "equals", "greaterThan", etc.
            int iPos = -1;
            int iPos2 = -1;

            fieldValue = entry.getValue();
            if (ObjectType.isEmpty(fieldValue)) {
                continue;
            }

            iPos = fieldNameRaw.indexOf('_'); // Look for suffix

            // This is a hack to skip fields from "multi" forms
            // These would have the form "fieldName_o_1"
            if (iPos >= 0) {
                String suffix = fieldNameRaw.substring(iPos + 1);
                iPos2 = suffix.indexOf('_');
                if (iPos2 == 1) {
                    continue;
                }
            }

            // If no suffix, assume no range (default to fld0) and operations of equals
            // If no field op is present, it will assume "equals".
            if (iPos < 0) {
                fieldNameRoot = fieldNameRaw;
            } else { // Must have at least "fld0/1" or "equals, greaterThan, etc."
                // Some bogus fields will slip in, like "ENTITY_NAME", but they will be ignored

                fieldNameRoot = fieldNameRaw.substring(0, iPos);
            }
            if (modelEntity.isField(fieldNameRoot)) {
                normalizedFields.put(fieldNameRaw, fieldValue);
            }
        }
        return normalizedFields;
    }
    /**
     * Returns the first generic item of the service 'performFind'
     * Same parameters as performFind service but returns a single GenericValue
     *
     * @param dctx
     * @param context
     * @return returns the first item
     */
    public static Map<String, Object> performFindItem(DispatchContext dctx, Map<String, Object> context) {
        context.put("viewSize", 1);
        context.put("viewIndex", 0);
        Map<String, Object> result = org.apache.ofbiz.common.FindServices.performFind(dctx,context);

        List<GenericValue> list = null;
        GenericValue item= null;
        try (EntityListIterator it = (EntityListIterator) result.get("listIt")) {
            list = it.getPartialList(1, 1); // list starts at '1'
            if (UtilValidate.isNotEmpty(list)) {
                item = list.get(0);
            }
        } catch (ClassCastException | NullPointerException | GenericEntityException e) {
            Debug.logInfo("Problem getting list Item" + e,module);
        }

        if (UtilValidate.isNotEmpty(item)) {
            result.put("item",item);
        }
        result.remove("listIt");

        if (result.containsKey("listSize")) {
            result.remove("listSize");
        }
        return result;
    }
}
