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
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericModelException;
import org.ofbiz.entity.config.DatasourceInfo;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelField;

/**
 * Encapsulates operations between entities and entity fields. This is a immutable class.
 *
 */
public class EntityFieldValue extends EntityConditionValue implements Reusable {

    protected static final ObjectFactory<EntityFieldValue> entityFieldValueFactory = new ObjectFactory<EntityFieldValue>() {
        protected EntityFieldValue create() {
            return new EntityFieldValue();
        }
    };

    protected String fieldName = null;

    public static EntityFieldValue makeFieldValue(String fieldName) {
        EntityFieldValue efv = EntityFieldValue.entityFieldValueFactory.object();
        efv.init(fieldName);
        return efv;
    }

    protected EntityFieldValue() {}

    /** @deprecated Use EntityFieldValue.makeFieldValue() instead */
    public EntityFieldValue(String fieldName) {
    this.init(fieldName);
    }

    public void init(String fieldName) {
        this.fieldName = fieldName;
    }

    public void reset() {
    this.fieldName = null;
    }

    public String getFieldName() {
        return fieldName;
    }

    public int hashCode() {
        return fieldName.hashCode();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof EntityFieldValue)) return false;
        EntityFieldValue otherValue = (EntityFieldValue) obj;
        return fieldName.equals(otherValue.fieldName);
    }

    public ModelField getModelField(ModelEntity modelEntity) {
        return getField(modelEntity, fieldName);
    }

    public void addSqlValue(StringBuilder sql, Map<String, String> tableAliases, ModelEntity modelEntity, List<EntityConditionParam> entityConditionParams, boolean includeTableNamePrefix, DatasourceInfo datasourceInfo) {
        sql.append(getColName(tableAliases, modelEntity, fieldName, includeTableNamePrefix, datasourceInfo));
    }

    public void validateSql(ModelEntity modelEntity) throws GenericModelException {
        ModelField field = getModelField(modelEntity);
        if (field == null) {
            throw new GenericModelException("Field with name " + fieldName + " not found in the " + modelEntity.getEntityName() + " Entity");
        }
    }

    public Object getValue(GenericDelegator delegator, Map<String, ? extends Object> map) {
        if (map == null) {
            return null;
        }
        if (map instanceof GenericEntity.NULL) {
            return null;
        } else {
            return map.get(fieldName);
        }
    }

    public void visit(EntityConditionVisitor visitor) {
        visitor.acceptEntityFieldValue(this);
    }

    public void accept(EntityConditionVisitor visitor) {
        visitor.acceptEntityFieldValue(this);
    }

    public EntityConditionValue freeze() {
        return this;
    }
}
