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

import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericModelException;
import org.ofbiz.entity.config.DatasourceInfo;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelField;
import org.ofbiz.entity.model.ModelViewEntity;

/**
 * Encapsulates operations between entities and entity fields. This is a immutable class.
 *
 */
@SuppressWarnings("serial")
public class EntityFieldValue extends EntityConditionValue implements Reusable {

    protected static final ObjectFactory<EntityFieldValue> entityFieldValueFactory = new ObjectFactory<EntityFieldValue>() {
        @Override
        protected EntityFieldValue create() {
            return new EntityFieldValue();
        }
    };

    protected String fieldName = null;
    protected String entityAlias = null;
    protected ModelViewEntity modelViewEntity = null;

    public static EntityFieldValue makeFieldValue(String fieldName) {
        EntityFieldValue efv = EntityFieldValue.entityFieldValueFactory.object();
        efv.init(fieldName, null, null);
        return efv;
    }

    public static EntityFieldValue makeFieldValue(String fieldName, String entityAlias, ModelViewEntity modelViewEntity) {
        EntityFieldValue efv = EntityFieldValue.entityFieldValueFactory.object();
        efv.init(fieldName, entityAlias, modelViewEntity);
        return efv;
    }

    protected EntityFieldValue() {}

    /** @deprecated Use EntityFieldValue.makeFieldValue() instead */
    @Deprecated
    public EntityFieldValue(String fieldName) {
        this.init(fieldName, null, null);
    }

    public void init(String fieldName, String entityAlias, ModelViewEntity modelViewEntity) {
        this.fieldName = fieldName;
        this.entityAlias = entityAlias;
        this.modelViewEntity = modelViewEntity;
    }

    public void reset() {
    this.fieldName = null;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public int hashCode() {
        return fieldName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EntityFieldValue)) return false;
        EntityFieldValue otherValue = (EntityFieldValue) obj;
        return fieldName.equals(otherValue.fieldName);
    }

    @Override
    public ModelField getModelField(ModelEntity modelEntity) {
        return getField(modelEntity, fieldName);
    }

    @Override
    public void addSqlValue(StringBuilder sql, Map<String, String> tableAliases, ModelEntity modelEntity, List<EntityConditionParam> entityConditionParams, boolean includeTableNamePrefix, DatasourceInfo datasourceInfo) {
        if (this.modelViewEntity != null) {
            if (UtilValidate.isNotEmpty(entityAlias)) {
                ModelEntity memberModelEntity = modelViewEntity.getMemberModelEntity(entityAlias);
                ModelField modelField = memberModelEntity.getField(fieldName);
                sql.append(entityAlias);
                sql.append(".");
                sql.append(modelField.getColName());
            } else {
                sql.append(getColName(tableAliases, modelViewEntity, fieldName, includeTableNamePrefix, datasourceInfo));
            }
        } else {
            sql.append(getColName(tableAliases, modelEntity, fieldName, includeTableNamePrefix, datasourceInfo));
        }
    }

    @Override
    public void validateSql(ModelEntity modelEntity) throws GenericModelException {
        ModelField field = getModelField(modelEntity);
        if (field == null) {
            throw new GenericModelException("Field with name " + fieldName + " not found in the " + modelEntity.getEntityName() + " Entity");
        }
    }

    @Override
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

    @Override
    public void visit(EntityConditionVisitor visitor) {
        visitor.acceptEntityFieldValue(this);
    }

    @Override
    public void accept(EntityConditionVisitor visitor) {
        visitor.acceptEntityFieldValue(this);
    }

    @Override
    public EntityConditionValue freeze() {
        return this;
    }
}
