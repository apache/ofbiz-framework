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
package org.ofbiz.entity.condition;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.entity.config.DatasourceInfo;
import org.ofbiz.entity.jdbc.SqlJdbcUtil;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelField;
import org.ofbiz.entity.model.ModelViewEntity;
import org.ofbiz.entity.model.ModelViewEntity.ModelAlias;

/**
 * Represents the conditions to be used to constrain a query
 * <br/>An EntityCondition can represent various type of constraints, including:
 * <ul>
 *  <li>EntityConditionList: a list of EntityConditions, combined with the operator specified
 *  <li>EntityExpr: for simple expressions or expressions that combine EntityConditions
 *  <li>EntityFieldMap: a map of fields where the field (key) equals the value, combined with the operator specified
 * </ul>
 * These can be used in various combinations using the EntityConditionList and EntityExpr objects.
 *
 */
public abstract class EntityConditionBase implements Serializable {

    public static final List emptyList = Collections.unmodifiableList(FastList.newInstance());
    public static final Map emptyMap = Collections.unmodifiableMap(FastMap.newInstance());

    protected ModelField getField(ModelEntity modelEntity, String fieldName) {
        ModelField modelField = null;
        if (modelEntity != null) {
            modelField = modelEntity.getField(fieldName);
        }
        return modelField;
    }

    protected String getColName(Map tableAliases, ModelEntity modelEntity, String fieldName, boolean includeTableNamePrefix, DatasourceInfo datasourceInfo) {
        if (modelEntity == null) return fieldName;
        return getColName(tableAliases, modelEntity, getField(modelEntity, fieldName), fieldName, includeTableNamePrefix, datasourceInfo);
    }

    protected String getColName(ModelField modelField, String fieldName) {
        String colName = null;
        if (modelField != null) {
            colName = modelField.getColName();
        } else {
            colName = fieldName;
        }
        return colName;
    }

    protected String getColName(Map tableAliases, ModelEntity modelEntity, ModelField modelField, String fieldName, boolean includeTableNamePrefix, DatasourceInfo datasourceInfo) {
        if (modelEntity == null || modelField == null) return fieldName;

        // if this is a view entity and we are configured to alias the views, use the alias here instead of the composite (ie table.column) field name 
        if (datasourceInfo != null && datasourceInfo.aliasViews && modelEntity instanceof ModelViewEntity) {
            ModelViewEntity modelViewEntity = (ModelViewEntity) modelEntity;
            ModelAlias modelAlias = modelViewEntity.getAlias(fieldName);
            if (modelAlias != null) {
                return modelAlias.getColAlias();
            }
        }
        
        String colName = getColName(modelField, fieldName);
        if (includeTableNamePrefix && datasourceInfo != null) {
            String tableName = modelEntity.getTableName(datasourceInfo);
            if (tableAliases.containsKey(tableName)) {
                tableName = (String) tableAliases.get(tableName);
            }
            colName = tableName + "." + colName;
        }
        return colName;
    }

    protected void addValue(StringBuffer buffer, ModelField field, Object value, List params) {
        SqlJdbcUtil.addValue(buffer, params == null ? null : field, value, params);
    }

    public boolean equals(Object obj) {
        throw new UnsupportedOperationException("equals:" + getClass().getName());
    }

    public int hashCode() {
        throw new UnsupportedOperationException("hashCode: " + getClass().getName());
    }

    protected static boolean equals(Object o1, Object o2) {
        return o1 == null ? o2 == null : o1.equals(o2);
    }

    protected static int hashCode(Object o) {
        return o != null ? o.hashCode() : 0;
    }

    public static Boolean castBoolean(boolean result) {
        return result ? Boolean.TRUE : Boolean.FALSE;
    }
}
