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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericModelException;
import org.ofbiz.entity.config.model.Datasource;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelField;
import org.ofbiz.entity.model.ModelViewEntity;
import org.ofbiz.entity.model.ModelViewEntity.ModelAlias;

/**
 * Field value expression.
 *
 */
@SuppressWarnings("serial")
public class EntityFieldValue extends EntityConditionValue {

    public static final String module = EntityFieldValue.class.getName();

    protected String fieldName = null;
    protected String entityAlias = null;
    protected List<String> entityAliasStack = null;
    protected ModelViewEntity modelViewEntity = null;

    public static EntityFieldValue makeFieldValue(String fieldName) {
        EntityFieldValue efv = new EntityFieldValue();
        efv.init(fieldName, null, null, null);
        return efv;
    }

    public static EntityFieldValue makeFieldValue(String fieldName, String entityAlias, List<String> entityAliasStack, ModelViewEntity modelViewEntity) {
        EntityFieldValue efv = new EntityFieldValue();
        efv.init(fieldName, entityAlias, entityAliasStack, modelViewEntity);
        return efv;
    }

    public void init(String fieldName, String entityAlias, List<String> entityAliasStack, ModelViewEntity modelViewEntity) {
        this.fieldName = fieldName;
        this.entityAlias = entityAlias;
        if (UtilValidate.isNotEmpty(entityAliasStack)) {
            this.entityAliasStack = new LinkedList<String>();
            this.entityAliasStack.addAll(entityAliasStack);
        }
        this.modelViewEntity = modelViewEntity;
        if (UtilValidate.isNotEmpty(this.entityAliasStack) && UtilValidate.isEmpty(this.entityAlias)) {
            // look it up on the view entity so it can be part of the big list, this only happens for aliased fields, so find the entity-alias and field-name for the alias
            ModelAlias modelAlias = this.modelViewEntity.getAlias(this.fieldName);
            if (modelAlias != null) {
                this.entityAlias = modelAlias.getEntityAlias();
                this.fieldName = modelAlias.getField();
            }
            // TODO/NOTE: this will ignore function, group-by, etc... should maybe support those in conditions too at some point
        }
    }

    public void reset() {
        this.fieldName = null;
        this.entityAlias = null;
        this.entityAliasStack = null;
        this.modelViewEntity = null;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public int hashCode() {
        int hash = fieldName.hashCode();
        if (this.entityAlias != null) hash |= this.entityAlias.hashCode();
        if (this.entityAliasStack != null) hash |= this.entityAliasStack.hashCode();
        if (this.modelViewEntity != null) hash |= this.modelViewEntity.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EntityFieldValue)) return false;
        EntityFieldValue otherValue = (EntityFieldValue) obj;
        if (!fieldName.equals(otherValue.fieldName)) return false;
        if (UtilMisc.compare(this.entityAlias, otherValue.entityAlias) != 0) return false;
        if (UtilMisc.compare(this.entityAliasStack, otherValue.entityAliasStack) != 0) return false;
        return true;
    }

    @Override
    public ModelField getModelField(ModelEntity modelEntity) {
        if (this.modelViewEntity != null) {
            if (this.entityAlias != null) {
                ModelEntity memberModelEntity = modelViewEntity.getMemberModelEntity(entityAlias);
                return getField(memberModelEntity, fieldName);
            } else {
                return getField(modelViewEntity, fieldName);
            }
        }
        return getField(modelEntity, fieldName);
    }


    @Override
    public void setModelField(ModelField field) {
        // do nothing
    }

    @Override
    public void addSqlValue(StringBuilder sql, Map<String, String> tableAliases, ModelEntity modelEntity, List<EntityConditionParam> entityConditionParams, boolean includeTableNamePrefix, Datasource datasourceInfo) {
        if (this.modelViewEntity != null) {
            // NOTE: this section is a bit of a hack; the other code is terribly complex and really needs to be refactored to incorporate support for this

            if (UtilValidate.isNotEmpty(entityAlias)) {
                ModelEntity memberModelEntity = modelViewEntity.getMemberModelEntity(entityAlias);
                ModelField modelField = memberModelEntity.getField(fieldName);

                // using entityAliasStack (ordered top to bottom) build a big long alias; not that dots will be replaced after it is combined with the column name in the SQL gen
                if (UtilValidate.isNotEmpty(this.entityAliasStack)) {
                    boolean dotUsed = false;
                    for (String curEntityAlias: entityAliasStack) {
                        sql.append(curEntityAlias);
                        if (dotUsed) {
                            sql.append("_");
                        } else {
                            sql.append(".");
                            dotUsed = true;
                        }

                    }
                    sql.append(entityAlias);
                    sql.append("_");
                    sql.append(modelField.getColName());
                } else {
                    sql.append(entityAlias);
                    sql.append(".");
                    sql.append(modelField.getColName());
                }
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
    public Object getValue(Delegator delegator, Map<String, ? extends Object> map) {
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
