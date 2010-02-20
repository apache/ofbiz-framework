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

import java.util.List;
import java.util.Map;

import javolution.context.ObjectFactory;
import javolution.lang.Reusable;

import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericModelException;
import org.ofbiz.entity.config.DatasourceInfo;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelField;

/**
 * Encapsulates operations between entities and entity fields. This is a immutable class.
 *
 */
public abstract class EntityFunction<T extends Comparable<?>> extends EntityConditionValue implements Reusable {

    public static interface Fetcher<T> {
        T getValue(Object value);
    }

    public abstract static class SQLFunctionFactory<T extends Comparable<T>, F extends EntityFunction<T>> extends ObjectFactory<F> {
        protected abstract void init(F function, Object value);

        protected F createFunction(EntityConditionValue nested) {
            F ef = object();
            init(ef, nested);
            return ef;
        }

        protected F createFunction(Object value) {
            F ef = object();
            init(ef, value);
            return ef;
        }
    }

    public static enum SQLFunction {
        LENGTH, TRIM, UPPER, LOWER;
    }

    public static final int ID_LENGTH = SQLFunction.LENGTH.ordinal();
    public static final int ID_TRIM = SQLFunction.TRIM.ordinal();
    public static final int ID_UPPER = SQLFunction.UPPER.ordinal();
    public static final int ID_LOWER = SQLFunction.LOWER.ordinal();

    public static EntityFunction<Integer> LENGTH(EntityConditionValue nested) { return LENGTH.lengthFactory.createFunction(nested); }
    public static EntityFunction<Integer> LENGTH(Object value) { return LENGTH.lengthFactory.createFunction(value); }
    public static EntityFunction<String> TRIM(EntityConditionValue nested) { return TRIM.trimFactory.createFunction(nested); }
    public static EntityFunction<String> TRIM(Object value) { return TRIM.trimFactory.createFunction(value); }
    public static EntityFunction<String> UPPER(EntityConditionValue nested) { return UPPER.upperFactory.createFunction(nested); }
    public static EntityFunction<String> UPPER(Object value) { return UPPER.upperFactory.createFunction(value); }
    public static EntityFunction<String> UPPER_FIELD(String fieldName) { return UPPER.upperFactory.createFunction(EntityFieldValue.makeFieldValue(fieldName)); }
    public static EntityFunction<String> LOWER(EntityConditionValue nested) { return LOWER.lowerFactory.createFunction(nested); }
    public static EntityFunction<String> LOWER(Object value) { return LOWER.lowerFactory.createFunction(value); }

    public static class LENGTH extends EntityFunction<Integer> {
        public static Fetcher<Integer> FETCHER = new Fetcher<Integer>() {
            public Integer getValue(Object value) { return value.toString().length(); }
        };
        protected static final SQLFunctionFactory<Integer, LENGTH> lengthFactory = new SQLFunctionFactory<Integer, LENGTH>() {
            @Override
            protected LENGTH create() {
                return new LENGTH();
            }

            @Override
            protected void init(LENGTH function, Object value) {
                function.init(value);
            }
        };
        protected LENGTH() {}
        public void init(Object value) {
            super.init(FETCHER, SQLFunction.LENGTH, value);
        }
    }

    public static class TRIM extends EntityFunction<String> {
        public static Fetcher<String> FETCHER = new Fetcher<String>() {
            public String getValue(Object value) { return value.toString().trim(); }
        };
        protected static final SQLFunctionFactory<String, TRIM> trimFactory = new SQLFunctionFactory<String, TRIM>() {
            @Override
            protected TRIM create() {
                return new TRIM();
            }

            @Override
            protected void init(TRIM function, Object value) {
                function.init(value);
            }
        };
        protected TRIM() {}
        public void init(Object value) {
            super.init(FETCHER, SQLFunction.TRIM, value);
        }
    }

    public static class UPPER extends EntityFunction<String> {
        public static Fetcher<String> FETCHER = new Fetcher<String>() {
            public String getValue(Object value) { return value.toString().toUpperCase(); }
        };
        protected static final SQLFunctionFactory<String, UPPER> upperFactory = new SQLFunctionFactory<String, UPPER>() {
            @Override
            protected UPPER create() {
                return new UPPER();
            }

            @Override
            protected void init(UPPER function, Object value) {
                function.init(value);
            }
        };
        protected UPPER() {}
        public void init(Object value) {
            super.init(FETCHER, SQLFunction.UPPER, value);
        }
    }

    public static class LOWER extends EntityFunction<String> {
        public static Fetcher<String> FETCHER = new Fetcher<String>() {
            public String getValue(Object value) { return value.toString().toLowerCase(); }
        };
        protected static final SQLFunctionFactory<String, LOWER> lowerFactory = new SQLFunctionFactory<String, LOWER>() {
            @Override
            protected LOWER create() {
                return new LOWER();
            }

            @Override
            protected void init(LOWER function, Object value) {
                function.init(value);
            }
        };
        protected LOWER() {}
        public void init(Object value) {
            super.init(FETCHER, SQLFunction.LOWER, value);
        }
    }

    protected SQLFunction function;
    protected EntityConditionValue nested = null;
    protected Object value = null;
    protected Fetcher<T> fetcher = null;

    protected EntityFunction() {}

    protected EntityFunction(Fetcher<T> fetcher, SQLFunction function, EntityConditionValue nested) {
        this.init(fetcher, function, nested);
    }

    protected EntityFunction(Fetcher<T> fetcher, SQLFunction function, Object value) {
        this.init(fetcher, function, value);
    }

    public void init(Fetcher<T> fetcher, SQLFunction function, Object value) {
        this.fetcher = fetcher;
        this.function = function;
        if (value instanceof EntityConditionValue) {
            this.nested = (EntityConditionValue) value;
        } else if (value instanceof String) {
            this.value = ((String) value).replaceAll("'", "''");
        } else {
            this.value = value;
        }
    }

    public void reset() {
        this.function = null;
        this.nested = null;
        this.value = null;
        this.fetcher = null;
    }

    @Override
    public EntityConditionValue freeze() {
        if (nested != null) {
            return new EntityFunction<T>(fetcher, function, nested.freeze()) {};
        } else {
            return new EntityFunction<T>(fetcher, function, value) {};
        }
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
        if (!(obj instanceof EntityFunction)) return false;
        EntityFunction<?> otherFunc = UtilGenerics.cast(obj);
        return (this.function == otherFunc.function &&
            (this.nested != null ? nested.equals(otherFunc.nested) : otherFunc.nested == null) &&
            (this.value != null ? value.equals(otherFunc.value) : otherFunc.value == null));
    }

    @Override
    public void addSqlValue(StringBuilder sql, Map<String, String> tableAliases, ModelEntity modelEntity, List<EntityConditionParam> entityConditionParams, boolean includeTableNamePrefix, DatasourceInfo datasourceinfo) {
        sql.append(function.name()).append('(');
        if (nested != null) {
            nested.addSqlValue(sql, tableAliases, modelEntity, entityConditionParams, includeTableNamePrefix, datasourceinfo);
        } else {
            addValue(sql, null, value, entityConditionParams);
        }
        sql.append(')');
    }

    @Override
    public void visit(EntityConditionVisitor visitor) {
        if (nested != null) {
            visitor.acceptEntityConditionValue(nested);
        } else {
            visitor.acceptObject(value);
        }
    }

    @Override
    public void accept(EntityConditionVisitor visitor) {
        visitor.acceptEntityFunction(this);
    }

    @Override
    public ModelField getModelField(ModelEntity modelEntity) {
        if (nested != null) {
            return nested.getModelField(modelEntity);
        }
        return null;
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
