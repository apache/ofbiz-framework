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

import org.ofbiz.base.util.UtilMisc
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.security.Security;
import org.ofbiz.entity.model.ModelReader;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelViewEntity;
import org.ofbiz.entity.model.ModelViewEntity.ModelAlias;
import org.ofbiz.entity.model.ModelField;
import org.ofbiz.entity.model.ModelFieldType;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityFieldMap;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtilProperties;
import org.ofbiz.base.util.Debug;
import java.sql.Timestamp;
import java.sql.Date;
import java.sql.Time;

entityName = parameters.entityName;

ModelReader reader = delegator.getModelReader();
ModelEntity modelEntity = reader.getModelEntity(entityName);

groupByFields = [];
functionFields = [];

if (modelEntity instanceof ModelViewEntity) {
    aliases = modelEntity.getAliasesCopy()
    for (ModelAlias alias : aliases) {
        if (alias.getGroupBy()) {
            groupByFields.add(alias.getName());
        } else if (alias.getFunction()) {
            functionFields.add(alias.getName());
        }
    }
}

context.entityName = modelEntity.getEntityName();
context.plainTableName = modelEntity.getPlainTableName();

String hasViewPermission = (security.hasEntityPermission("ENTITY_DATA", "_VIEW", session) || security.hasEntityPermission(modelEntity.getPlainTableName(), "_VIEW", session)) == true ? "Y" : "N";
String hasCreatePermission = (security.hasEntityPermission("ENTITY_DATA", "_CREATE", session) || security.hasEntityPermission(modelEntity.getPlainTableName(), "_CREATE", session)) == true ? "Y" : "N";
String hasUpdatePermission = (security.hasEntityPermission("ENTITY_DATA", "_UPDATE", session) || security.hasEntityPermission(modelEntity.getPlainTableName(), "_UPDATE", session)) == true ? "Y" : "N";
String hasDeletePermission = (security.hasEntityPermission("ENTITY_DATA", "_DELETE", session) || security.hasEntityPermission(modelEntity.getPlainTableName(), "_DELETE", session)) == true ? "Y" : "N";

context.hasViewPermission = hasViewPermission;
context.hasCreatePermission = hasCreatePermission;
context.hasUpdatePermission = hasUpdatePermission;
context.hasDeletePermission = hasDeletePermission;

String find = parameters.find;
if (find == null) {
    find = "false";
}

String curFindString = "entityName=" + entityName + "&find=" + find;

GenericEntity findByEntity = delegator.makeValue(entityName);
List errMsgList = [];
Iterator fieldIterator = modelEntity.getFieldsIterator();
while (fieldIterator.hasNext()) {
    ModelField field = fieldIterator.next();
    String fval = parameters.get(field.getName());
    if (fval != null) {
        if (fval.length() > 0) {
            curFindString = curFindString + "&" + field.getName() + "=" + fval;
            try {
                findByEntity.setString(field.getName(), fval);
            } catch (NumberFormatException nfe) {
                Debug.logError(nfe, "Caught an exception : " + nfe.toString(), "FindGeneric.groovy");
                errMsgList.add("Entered value is non-numeric for numeric field: " + field.getName());
            }
        }
    }
}
if (errMsgList) {
    request.setAttribute("_ERROR_MESSAGE_LIST_", errMsgList);
}

curFindString = UtilFormatOut.encodeQuery(curFindString);
context.curFindString = curFindString;

try {
    viewIndex = Integer.valueOf((String)parameters.get("VIEW_INDEX")).intValue();
} catch (NumberFormatException nfe) {
    viewIndex = 0;
}

context.viewIndexFirst = 0;
context.viewIndex = viewIndex;
context.viewIndexPrevious = viewIndex-1;
context.viewIndexNext = viewIndex+1;

try {
    viewSize = Integer.valueOf((String)parameters.get("VIEW_SIZE")).intValue();
} catch (NumberFormatException nfe) {
    viewSize = Integer.valueOf(EntityUtilProperties.getPropertyValue("widget", "widget.form.defaultViewSize", delegator)).intValue();
}

context.viewSize = viewSize;

int lowIndex = viewIndex*viewSize+1;
int highIndex = (viewIndex+1)*viewSize;
context.lowIndex = lowIndex;

int arraySize = 0;
List resultPartialList = null;

if ("true".equals(find)) {
    //EntityCondition condition = EntityCondition.makeCondition(findByEntity, EntityOperator.AND);

    // small variation to support LIKE if a wildcard (%) is found in a String
    conditionList = [];
    findByKeySet = findByEntity.keySet();
    fbksIter = findByKeySet.iterator();
    while (fbksIter.hasNext()) {
        findByKey = fbksIter.next();
        if (findByEntity.getString(findByKey).indexOf("%") >= 0) {
            conditionList.add(EntityCondition.makeCondition(findByKey, EntityOperator.LIKE, findByEntity.getString(findByKey)));
        } else {
            conditionList.add(EntityCondition.makeCondition(findByKey, EntityOperator.EQUALS, findByEntity.get(findByKey)));
        }
    }
    condition = EntityCondition.makeCondition(conditionList, EntityOperator.AND);

    if ((highIndex - lowIndex + 1) > 0) {
        boolean beganTransaction = false;
        try {
            beganTransaction = TransactionUtil.begin();

            EntityFindOptions efo = new EntityFindOptions();
            efo.setMaxRows(highIndex);
            efo.setResultSetType(EntityFindOptions.TYPE_SCROLL_INSENSITIVE);
            EntityListIterator resultEli = null;
            fieldsToSelect = null;

            if (groupByFields || functionFields) {
                fieldsToSelect = [] as Set;

                for (String groupByField : groupByFields) {
                    fieldsToSelect.add(groupByField);
                }

                for (String functionField : functionFields) {
                    fieldsToSelect.add(functionField)
                }
            }
            Collection pkNames = [];
            Iterator iter = modelEntity.getPksIterator();
            while (iter != null && iter.hasNext()) {
                ModelField curField = (ModelField) iter.next();
                pkNames.add(curField.getName());
            }
            resultEli = delegator.find(entityName, condition, null, fieldsToSelect, pkNames, efo);
            resultPartialList = resultEli.getPartialList(lowIndex, highIndex - lowIndex + 1);

            arraySize = resultEli.getResultsSizeAfterPartialList();
            if (arraySize < highIndex) {
                highIndex = arraySize;
            }

            resultEli.close();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Failure in operation, rolling back transaction", "FindGeneric.groovy");
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction, "Error looking up entity values in WebTools Entity Data Maintenance", e);
            } catch (GenericEntityException e2) {
                Debug.logError(e2, "Could not rollback transaction: " + e2.toString(), "FindGeneric.groovy");
            }
            // after rolling back, rethrow the exception
            throw e;
        } finally {
            // only commit the transaction if we started one... this will throw an exception if it fails
            TransactionUtil.commit(beganTransaction);
        }
    }
}
context.highIndex = highIndex;
context.arraySize = arraySize;
context.resultPartialList = resultPartialList;

viewIndexLast = UtilMisc.getViewLastIndex(arraySize, viewSize);
context.viewIndexLast = viewIndexLast;

List fieldList = [];
fieldIterator = modelEntity.getFieldsIterator();
while (fieldIterator.hasNext()) {
    ModelField field = fieldIterator.next();
    ModelFieldType type = delegator.getEntityFieldType(modelEntity, field.getType());

    Map fieldMap = [:];
    fieldMap.put("name", field.getName());
    fieldMap.put("isPk", (field.getIsPk() == true) ? "Y" : "N");
    fieldMap.put("javaType", type.getJavaType());
    fieldMap.put("sqlType", type.getSqlType());
    fieldMap.put("param", (parameters.get(field.getName()) != null ? parameters.get(field.getName()) : ""));

    fieldList.add(fieldMap);
}
context.fieldList = fieldList;
context.columnCount = fieldList.size()+2;

List records = [];
if (resultPartialList != null) {
    Iterator resultPartialIter = resultPartialList.iterator();
    while (resultPartialIter.hasNext()) {
        Map record = [:];

        GenericValue value = (GenericValue)resultPartialIter.next();
        String findString = "entityName=" + entityName;
        Iterator pkIterator = modelEntity.getPksIterator();
        while (pkIterator.hasNext()) {
            ModelField pkField = pkIterator.next();
            ModelFieldType type = delegator.getEntityFieldType(modelEntity, pkField.getType());
            findString += "&" + pkField.getName() + "=" + value.get(pkField.getName());
        }
        record.put("findString", findString);

        record.put("fields", value);
        records.add(record);
    }
}
context.records = records;
context.lowCount = lowIndex;
context.highCount = lowIndex + records.size() - 1;
context.total = arraySize;
