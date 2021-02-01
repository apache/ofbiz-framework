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
package org.apache.ofbiz.entity.condition;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntity;
import org.apache.ofbiz.entity.GenericModelException;
import org.apache.ofbiz.entity.config.model.Datasource;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelField;
import org.apache.ofbiz.minilang.operation.Convert;

/**
 * Base class for condition expression values.
 *
 */
@SuppressWarnings("serial")
public abstract class EntityConditionValue implements Serializable {

    private static final Map<String, String> EMPTY_ALIASES = Collections.unmodifiableMap(new HashMap<>());
    private static final String MODULE = Convert.class.getName();

    public static EntityConditionValue constantNumber(Number value) {
        return new ConstantNumberValue(value);
    }
    public static final class ConstantNumberValue extends EntityConditionValue {
        private Number value;

        private ConstantNumberValue(Number value) {
            this.value = value;
        }

        @Override
        public void addSqlValue(StringBuilder sql, Map<String, String> tableAliases, ModelEntity modelEntity, List<EntityConditionParam>
                entityConditionParams, boolean includeTableNamePrefix, Datasource datasourceinfo) {
            sql.append(value);
        }

        @Override
        public EntityConditionValue freeze() {
            return this;
        }

        @Override
        public ModelField getModelField(ModelEntity modelEntity) {
            return null;
        }

        @Override
        public void setModelField(ModelField field) {
            Debug.logInfo("Logging to avoid checkstyle issue.", MODULE);
        }

        @Override
        public Object getValue(Delegator delegator, Map<String, ? extends Object> map) {
            return value;
        }

        @Override
        public void validateSql(org.apache.ofbiz.entity.model.ModelEntity modelEntity) {
        }
    }

    public abstract ModelField getModelField(ModelEntity modelEntity);

    public abstract void setModelField(ModelField modelEntity);

    /**
     * Add sql value.
     * @param sql the sql
     * @param modelEntity the model entity
     * @param entityConditionParams the entity condition params
     * @param includeTableNamePrefix the include table name prefix
     * @param datasourceinfo the datasourceinfo
     */
    public void addSqlValue(StringBuilder sql, ModelEntity modelEntity, List<EntityConditionParam> entityConditionParams,
                            boolean includeTableNamePrefix, Datasource datasourceinfo) {
        addSqlValue(sql, EMPTY_ALIASES, modelEntity, entityConditionParams, includeTableNamePrefix, datasourceinfo);
    }

    public abstract void addSqlValue(StringBuilder sql, Map<String, String> tableAliases, ModelEntity modelEntity, List<EntityConditionParam>
            entityConditionParams, boolean includeTableNamePrefix, Datasource datasourceinfo);

    public abstract void validateSql(ModelEntity modelEntity) throws GenericModelException;

    /**
     * Gets value.
     * @param entity the entity
     * @return the value
     */
    public Object getValue(GenericEntity entity) {
        if (entity == null) {
            return null;
        }
        return getValue(entity.getDelegator(), entity);
    }

    public abstract Object getValue(Delegator delegator, Map<String, ? extends Object> map);

    public abstract EntityConditionValue freeze();

    /**
     * To string.
     * @param sb the sb
     */
    public void toString(StringBuilder sb) {
        addSqlValue(sb, null, new ArrayList<EntityConditionParam>(), false, null);
    }

    @Override
    public String toString() {
        StringBuilder sql = new StringBuilder();
        toString(sql);
        return sql.toString();
    }

    @Override
    public boolean equals(Object obj) {
        throw new UnsupportedOperationException("equals:" + getClass().getName());
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException("hashCode: " + getClass().getName());
    }
}
