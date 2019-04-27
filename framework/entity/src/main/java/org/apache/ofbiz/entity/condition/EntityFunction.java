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

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericModelException;
import org.apache.ofbiz.entity.config.model.Datasource;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelField;

/**
 * Base class for entity functions.
 *
 */
@SuppressWarnings("serial")
public abstract class EntityFunction<T extends Comparable<?>> extends EntityConditionValue {

    public static interface Fetcher<T> {
        T getValue(Object value);
    }

    public static enum SQLFunction {
        LENGTH, TRIM, UPPER, LOWER;
    }

    public static final int ID_LENGTH = SQLFunction.LENGTH.ordinal();
    public static final int ID_TRIM = SQLFunction.TRIM.ordinal();
    public static final int ID_UPPER = SQLFunction.UPPER.ordinal();
    public static final int ID_LOWER = SQLFunction.LOWER.ordinal();

    public static EntityFunction<Integer> LENGTH(EntityConditionValue nested) { return new LENGTH(nested); }
    public static EntityFunction<Integer> LENGTH(Object value) { return new LENGTH(value); }
    public static EntityFunction<String> TRIM(EntityConditionValue nested) { return new TRIM(nested); }
    public static EntityFunction<String> TRIM(Object value) { return new TRIM(value); }
    public static EntityFunction<String> UPPER(EntityConditionValue nested) { return new UPPER(nested); }
    public static EntityFunction<String> UPPER(Object value) { return new UPPER(value); }
    public static EntityFunction<String> UPPER_FIELD(String fieldName) { return new UPPER(EntityFieldValue.makeFieldValue(fieldName)); }
    public static EntityFunction<String> LOWER(EntityConditionValue nested) { return new LOWER(nested); }
    public static EntityFunction<String> LOWER(Object value) { return new LOWER(value); }

    /**
     * Length() entity function.
     *
     */
    public static class LENGTH extends EntityFunctionSingle<Integer> {
        public static final Fetcher<Integer> FETCHER = new Fetcher<Integer>() {
            @Override
            public Integer getValue(Object value) { return value.toString().length(); }
        };

        private LENGTH(Object value) {
            super(FETCHER, SQLFunction.LENGTH, value);
        }
    }

    /**
     * Trim() entity function.
     *
     */
    public static class TRIM extends EntityFunctionSingle<String> {
        public static final Fetcher<String> FETCHER = new Fetcher<String>() {
            @Override
            public String getValue(Object value) { return value.toString().trim(); }
        };

        private TRIM(Object value) {
            super(FETCHER, SQLFunction.TRIM, value);
        }
    }

    /**
     * Upper() entity function.
     *
     */
    public static class UPPER extends EntityFunctionSingle<String> {
        public static final Fetcher<String> FETCHER = new Fetcher<String>() {
            @Override
            public String getValue(Object value) { return value.toString().toUpperCase(Locale.getDefault()); }
        };

        private UPPER(Object value) {
            super(FETCHER, SQLFunction.UPPER, value);
        }
    }

    /**
     * Lower() entity function.
     *
     */
    public static class LOWER extends EntityFunctionSingle<String> {
        public static final Fetcher<String> FETCHER = new Fetcher<String>() {
            @Override
            public String getValue(Object value) { return value.toString().toLowerCase(Locale.getDefault()); }
        };

        private LOWER(Object value) {
            super(FETCHER, SQLFunction.LOWER, value);
        }
    }

    public static abstract class EntityFunctionSingle<T extends Comparable<?>> extends EntityFunction<T> {
        protected EntityFunctionSingle(Fetcher<T> fetcher, SQLFunction function, Object value) {
            super(fetcher, function, value);
        }
    }

    public static abstract class EntityFunctionNested<T extends Comparable<?>> extends EntityFunction<T> {
        protected EntityFunctionNested(Fetcher<T> fetcher, SQLFunction function, EntityConditionValue nested) {
            super(fetcher, function, nested);
        }
    }

    protected final SQLFunction function;
    protected final EntityConditionValue nested;
    protected final Object value;
    protected final Fetcher<T> fetcher;
    protected ModelField field;

    protected EntityFunction(Fetcher<T> fetcher, SQLFunction function, EntityConditionValue nested) {
        this.fetcher = fetcher;
        this.function = function;
        this.nested = nested;
        this.value = null;
    }

    protected EntityFunction(Fetcher<T> fetcher, SQLFunction function, Object value) {
        this.fetcher = fetcher;
        this.function = function;
        if (value instanceof EntityConditionValue) {
            this.nested = (EntityConditionValue) value;
            this.value = null;
        } else {
            this.nested = null;
            this.value = value;
        }
    }

    @Override
    public EntityConditionValue freeze() {
        if (nested != null) {
            return new EntityFunctionNested<T>(fetcher, function, nested.freeze()) {};
        }
        return new EntityFunctionSingle<T>(fetcher, function, value) {};
    }

    public String getCode() {
        return function.name();
    }

    public Object getOriginalValue() {
        return this.value;
    }

    public int getId() {
        return function.ordinal();
    }

    @Override
    public int hashCode() {
        return function.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EntityFunction<?>)) {
            return false;
        }
        EntityFunction<?> otherFunc = UtilGenerics.cast(obj);
        return (this.function == otherFunc.function &&
            (this.nested != null ? nested.equals(otherFunc.nested) : otherFunc.nested == null) &&
            (this.value != null ? value.equals(otherFunc.value) : otherFunc.value == null));
    }

    @Override
    public void addSqlValue(StringBuilder sql, Map<String, String> tableAliases, ModelEntity modelEntity, List<EntityConditionParam> entityConditionParams, boolean includeTableNamePrefix, Datasource datasourceinfo) {
        sql.append(function.name()).append('(');
        if (nested != null) {
            nested.addSqlValue(sql, tableAliases, modelEntity, entityConditionParams, includeTableNamePrefix, datasourceinfo);
        } else {
            EntityConditionUtils.addValue(sql, null, value, entityConditionParams);
        }
        sql.append(')');
    }

    @Override
    public ModelField getModelField(ModelEntity modelEntity) {
        if (nested != null) {
            return nested.getModelField(modelEntity);
        }
        return field;
    }

    @Override
    public void setModelField(ModelField field) {
        this.field = field;
    }

    @Override
    public void validateSql(ModelEntity modelEntity) throws GenericModelException {
        if (nested != null) {
            nested.validateSql(modelEntity);
        }
    }

    @Override
    public Object getValue(Delegator delegator, Map<String, ? extends Object> map) {
        Object value = nested != null ? nested.getValue(delegator, map) : this.value;
        return value != null ? fetcher.getValue(value) : null;
    }
}
