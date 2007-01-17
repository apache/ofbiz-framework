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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
public abstract class EntityConditionValue extends EntityConditionBase {

    public abstract ModelField getModelField(ModelEntity modelEntity);

    public void addSqlValue(StringBuffer sql, ModelEntity modelEntity, List entityConditionParams, boolean includeTableNamePrefix,
            DatasourceInfo datasourceinfo) {
        addSqlValue(sql, emptyMap, modelEntity, entityConditionParams, includeTableNamePrefix, datasourceinfo);
    }

    public abstract void addSqlValue(StringBuffer sql, Map tableAliases, ModelEntity modelEntity, List entityConditionParams,
            boolean includeTableNamePrefix, DatasourceInfo datasourceinfo);

    public abstract void validateSql(ModelEntity modelEntity) throws GenericModelException;

    public Object getValue(GenericEntity entity) {
        if (entity == null) {
            return null;
        }
        return getValue(entity.getDelegator(), entity);
    }

    public abstract Object getValue(GenericDelegator delegator, Map map);

    public abstract EntityConditionValue freeze();

    public abstract void visit(EntityConditionVisitor visitor);

    public void accept(EntityConditionVisitor visitor) {
        throw new IllegalArgumentException("accept not implemented");
    }

    public void toString(StringBuffer sb) {
        addSqlValue(sb, null, new ArrayList(), false, null);
    }
    
    public String toString() {
        StringBuffer sql = new StringBuffer();
        toString(sql);
        return sql.toString();
    }
}
