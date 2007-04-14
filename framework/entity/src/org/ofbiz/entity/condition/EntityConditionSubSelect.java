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
package org.ofbiz.entity.condition;

import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericModelException;
import org.ofbiz.entity.config.DatasourceInfo;
import org.ofbiz.entity.jdbc.SqlJdbcUtil;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelField;
import org.ofbiz.entity.model.ModelViewEntity;

public class EntityConditionSubSelect extends EntityConditionValue {
    public static final String module = EntityConditionSubSelect.class.getName();

    protected ModelEntity localModelEntity;
    protected String keyFieldName;
    protected EntityCondition whereCond;
    protected boolean requireAll;
    
    protected EntityConditionSubSelect() { }
    
    public EntityConditionSubSelect(String entityName, String keyFieldName, EntityCondition whereCond, boolean requireAll, GenericDelegator delegator) {
        this(delegator.getModelEntity(entityName), keyFieldName, whereCond, requireAll);
    }
    public EntityConditionSubSelect(ModelEntity localModelEntity, String keyFieldName, EntityCondition whereCond, boolean requireAll) {
        this.localModelEntity = localModelEntity;
        this.keyFieldName = keyFieldName;
        this.whereCond = whereCond;
        this.requireAll = requireAll;
    }

    public void addSqlValue(StringBuffer sql, Map tableAliases, ModelEntity parentModelEntity, List entityConditionParams,
            boolean includeTableNamePrefix, DatasourceInfo datasourceInfo) {
        if (localModelEntity instanceof ModelViewEntity && datasourceInfo == null) {
            throw new IllegalArgumentException("Call to EntityConditionSubSelect.addSqlValue with datasourceInfo=null which is not allowed because the local entity [" + this.localModelEntity.getEntityName() + "] is a view entity");
        }
        try {
            // add select and where and such, based on local entity not on the main entity
            ModelField localModelField = localModelEntity.getField(this.keyFieldName);

            if (this.requireAll) {
                sql.append(" ALL(");
            } else {
                sql.append(" ANY(");
            }
            sql.append("SELECT ");

            sql.append(localModelField.getColName());

            // FROM clause and when necessary the JOIN or LEFT JOIN clause(s) as well
            sql.append(SqlJdbcUtil.makeFromClause(localModelEntity, datasourceInfo));

            // WHERE clause
            StringBuffer whereString = new StringBuffer();
            String entityCondWhereString = "";
            if (this.whereCond != null) {
                entityCondWhereString = this.whereCond.makeWhereString(localModelEntity, entityConditionParams, datasourceInfo);
            }

            String viewClause = SqlJdbcUtil.makeViewWhereClause(localModelEntity, (datasourceInfo != null ? datasourceInfo.joinStyle : null));
            if (viewClause.length() > 0) {
                if (entityCondWhereString.length() > 0) {
                    whereString.append("(");
                    whereString.append(entityCondWhereString);
                    whereString.append(") AND ");
                }

                whereString.append(viewClause);
            } else {
                whereString.append(entityCondWhereString);
            }

            if (whereString.length() > 0) {
                sql.append(" WHERE ");
                sql.append(whereString.toString());
            }

            sql.append(")");
        } catch (GenericEntityException e) {
            String errMsg = "Could not generate sub-select SQL: " + e.toString();
            Debug.logError(e, errMsg, module);
            
        }
    }

    public EntityConditionValue freeze() {
        return new EntityConditionSubSelect(localModelEntity, keyFieldName, (whereCond != null ? whereCond.freeze() : null), requireAll);
    }

    public ModelField getModelField(ModelEntity modelEntity) {
        // do nothing for now
        return null;
    }

    public Object getValue(GenericDelegator delegator, Map map) {
        // do nothing for now
        return null;
    }

    public void validateSql(ModelEntity modelEntity) throws GenericModelException {
        // do nothing for now
    }

    public void visit(EntityConditionVisitor visitor) {
        if (whereCond != null) whereCond.visit(visitor);
    }
}
