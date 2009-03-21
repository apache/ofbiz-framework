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

import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericModelException;
import org.ofbiz.entity.config.DatasourceInfo;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelField;

/**
 * Encapsulates operations between entities and entity fields. This is a immutable class.
 *
 */
public abstract class EntityFunction<T extends Comparable> extends EntityConditionValue implements Reusable {

    public static interface Fetcher<T> {
        T getValue(Object value);
    }

    public static enum SQLFunction {
        LENGTH {
            public EntityFunction.LENGTH createFunction(EntityConditionValue nested) { EntityFunction.LENGTH ef = EntityFunction.LENGTH.lengthFactory.object(); ef.init(nested); return ef;}
            public EntityFunction.LENGTH createFunction(Object value) { EntityFunction.LENGTH ef = EntityFunction.LENGTH.lengthFactory.object(); ef.init(value); return ef;}
        },
        TRIM {
            public EntityFunction.TRIM createFunction(EntityConditionValue nested) { EntityFunction.TRIM ef = EntityFunction.TRIM.trimFactory.object(); ef.init(nested); return ef;}
            public EntityFunction.TRIM createFunction(Object value) { EntityFunction.TRIM ef = EntityFunction.TRIM.trimFactory.object(); ef.init(value); return ef;}
        },
        UPPER {
            public EntityFunction.UPPER createFunction(EntityConditionValue nested) { EntityFunction.UPPER ef = EntityFunction.UPPER.upperFactory.object(); ef.init(nested); return ef;}
            public EntityFunction.UPPER createFunction(Object value) { EntityFunction.UPPER ef = EntityFunction.UPPER.upperFactory.object(); ef.init(value); return ef;}
        },
        LOWER {
            public EntityFunction.LOWER createFunction(EntityConditionValue nested) { EntityFunction.LOWER ef = EntityFunction.LOWER.lowerFactory.object(); ef.init(nested); return ef;}
            public EntityFunction.LOWER createFunction(Object value) { EntityFunction.LOWER ef = EntityFunction.LOWER.lowerFactory.object(); ef.init(value); return ef;}
        };

        public abstract <T extends Comparable> EntityFunction<T> createFunction(EntityConditionValue nested);
        public abstract <T extends Comparable> EntityFunction<T> createFunction(Object value);
    }

    public static final int ID_LENGTH = SQLFunction.LENGTH.ordinal();
    public static final int ID_TRIM = SQLFunction.TRIM.ordinal();
    public static final int ID_UPPER = SQLFunction.UPPER.ordinal();
    public static final int ID_LOWER = SQLFunction.LOWER.ordinal();

    public static EntityFunction<Integer> LENGTH(EntityConditionValue nested) { return SQLFunction.LENGTH.createFunction(nested); }
    public static EntityFunction<Integer> LENGTH(Object value) { return SQLFunction.LENGTH.createFunction(value); }
    public static EntityFunction<String> TRIM(EntityConditionValue nested) { return SQLFunction.TRIM.createFunction(nested); }
    public static EntityFunction<String> TRIM(Object value) { return SQLFunction.TRIM.createFunction(value); }
    public static EntityFunction<String> UPPER(EntityConditionValue nested) { return SQLFunction.UPPER.createFunction(nested); }
    public static EntityFunction<String> UPPER(Object value) { return SQLFunction.UPPER.createFunction(value); }
    public static EntityFunction<String> UPPER_FIELD(String fieldName) { return SQLFunction.UPPER.createFunction(EntityFieldValue.makeFieldValue(fieldName)); }
    public static EntityFunction<String> LOWER(EntityConditionValue nested) { return SQLFunction.LOWER.createFunction(nested); }
    public static EntityFunction<String> LOWER(Object value) { return SQLFunction.LOWER.createFunction(value); }

    public static class LENGTH extends EntityFunction<Integer> {
        public static Fetcher<Integer> FETCHER = new Fetcher<Integer>() {
            public Integer getValue(Object value) { return value.toString().length(); }
        };
        protected static final ObjectFactory<LENGTH> lengthFactory = new ObjectFactory<LENGTH>() {
            protected LENGTH create() {
                return new LENGTH();
            }
        };
        protected LENGTH() {}
        /** @deprecated Use EntityCondition.LENGTH() instead */
        public LENGTH(EntityConditionValue nested) { init(nested); }
        /** @deprecated Use EntityCondition.LENGTH() instead */
        public LENGTH(Object value) { init(value); }
        public void init(Object value) {
            super.init(FETCHER, SQLFunction.LENGTH, value);
        }
    };

    public static class TRIM extends EntityFunction<String> {
        public static Fetcher<String> FETCHER = new Fetcher<String>() {
            public String getValue(Object value) { return value.toString().trim(); }
        };
        protected static final ObjectFactory<TRIM> trimFactory = new ObjectFactory<TRIM>() {
            protected TRIM create() {
                return new TRIM();
            }
        };
        protected TRIM() {}
        /** @deprecated Use EntityCondition.TRIM() instead */
        public TRIM(EntityConditionValue nested) { init(nested); }
        /** @deprecated Use EntityCondition.TRIM() instead */
        public TRIM(Object value) { init(value); }
        public void init(Object value) {
            super.init(FETCHER, SQLFunction.TRIM, value);
        }
    };

    public static class UPPER extends EntityFunction<String> {
        public static Fetcher<String> FETCHER = new Fetcher<String>() {
            public String getValue(Object value) { return value.toString().toUpperCase(); }
        };
        protected static final ObjectFactory<UPPER> upperFactory = new ObjectFactory<UPPER>() {
            protected UPPER create() {
                return new UPPER();
            }
        };
        protected UPPER() {}
        /** @deprecated Use EntityCondition.UPPER() instead */
        public UPPER(EntityConditionValue nested) { init(nested); }
        /** @deprecated Use EntityCondition.UPPER() instead */
        public UPPER(Object value) { init(value); }
        public void init(Object value) {
            super.init(FETCHER, SQLFunction.UPPER, value);
        }
    };

    public static class LOWER extends EntityFunction<String> {
        public static Fetcher<String> FETCHER = new Fetcher<String>() {
            public String getValue(Object value) { return value.toString().toLowerCase(); }
        };
        protected static final ObjectFactory<LOWER> lowerFactory = new ObjectFactory<LOWER>() {
            protected LOWER create() {
                return new LOWER();
            }
        };
        protected LOWER() {}
        /** @deprecated Use EntityCondition.LOWER() instead */
        public LOWER(EntityConditionValue nested) { init(nested); }
        /** @deprecated Use EntityCondition.LOWER() instead */
        public LOWER(Object value) { init(value); }
        public void init(Object value) {
            super.init(FETCHER, SQLFunction.LOWER, value);
        }
    };

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

    public int hashCode() {
        return function.hashCode();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof EntityFunction)) return false;
        EntityFunction otherFunc = (EntityFunction) obj;
        return (this.function == otherFunc.function &&
            (this.nested != null ? nested.equals(otherFunc.nested) : otherFunc.nested == null) &&
            (this.value != null ? value.equals(otherFunc.value) : otherFunc.value == null));
    }

    public void addSqlValue(StringBuilder sql, Map<String, String> tableAliases, ModelEntity modelEntity, List<EntityConditionParam> entityConditionParams, boolean includeTableNamePrefix, DatasourceInfo datasourceinfo) {
        sql.append(function.name()).append('(');
        if (nested != null) {
            nested.addSqlValue(sql, tableAliases, modelEntity, entityConditionParams, includeTableNamePrefix, datasourceinfo);
        } else {
            addValue(sql, null, value, entityConditionParams);
        }
        sql.append(')');
    }

    public void visit(EntityConditionVisitor visitor) {
        if (nested != null) {
            visitor.acceptEntityConditionValue(nested);
        } else {
            visitor.acceptObject(value);
        }
    }

    public void accept(EntityConditionVisitor visitor) {
        visitor.acceptEntityFunction(this);
    }

    public ModelField getModelField(ModelEntity modelEntity) {
        if (nested != null) {
            return nested.getModelField(modelEntity);
        }
        return null;
    }

    public void validateSql(ModelEntity modelEntity) throws GenericModelException {
        if (nested != null) {
            nested.validateSql(modelEntity);
        }
    }

    public Object getValue(GenericDelegator delegator, Map<String, ? extends Object> map) {
        Object value = nested != null ? nested.getValue(delegator, map) : this.value;
        return value != null ? fetcher.getValue(value) : null;
    }
}
