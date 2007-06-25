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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericModelException;
import org.ofbiz.entity.config.DatasourceInfo;
import org.ofbiz.entity.model.ModelEntity;

/**
 * Encapsulates operations between entities and entity fields. This is a immutable class.
 *
 */
public class EntityJoinOperator extends EntityOperator {

    protected boolean shortCircuitValue;

    protected EntityJoinOperator(int id, String code, boolean shortCircuitValue) {
        super(id, code);
        this.shortCircuitValue = shortCircuitValue;
    }

    public void addSqlValue(StringBuffer sql, ModelEntity modelEntity, List entityConditionParams, boolean compat, Object lhs, Object rhs, DatasourceInfo datasourceInfo) {
        sql.append('(');
        sql.append(((EntityCondition) lhs).makeWhereString(modelEntity, entityConditionParams, datasourceInfo));
        sql.append(' ');
        sql.append(getCode());
        sql.append(' ');
        if (rhs instanceof EntityCondition) {
            sql.append(((EntityCondition) rhs).makeWhereString(modelEntity, entityConditionParams, datasourceInfo));
        } else {
            addValue(sql, null, rhs, entityConditionParams);
        }
        sql.append(')');
    }

    public void addSqlValue(StringBuffer sql, ModelEntity modelEntity, List entityConditionParams, List conditionList, DatasourceInfo datasourceInfo) {
        if (conditionList != null && conditionList.size() > 0) {
            sql.append('(');
            Iterator conditionIter = conditionList.iterator();
            while (conditionIter.hasNext()) {
                EntityCondition condition = (EntityCondition) conditionIter.next();
                sql.append(condition.makeWhereString(modelEntity, entityConditionParams, datasourceInfo));
                if (conditionIter.hasNext()) {
                    sql.append(' ');
                    sql.append(getCode());
                    sql.append(' ');
                }
            }
            sql.append(')');
        }
    }

    protected EntityCondition freeze(Object item) {
        return ((EntityCondition) item).freeze();
    }

    public EntityCondition freeze(Object lhs, Object rhs) {
        return new EntityExpr(freeze(lhs), this, freeze(rhs));
    }

    public EntityCondition freeze(List conditionList) {
        List newList = new ArrayList(conditionList.size());
        for (int i = 0; i < conditionList.size(); i++) {
            EntityCondition condition = (EntityCondition) conditionList.get(i);
            newList.add(condition.freeze());
        }
        return new EntityConditionList(newList, this);
    }

    public void visit(EntityConditionVisitor visitor, List conditionList) {
        if (conditionList != null && conditionList.size() > 0) {
            for (int i = 0; i < conditionList.size(); i++) {
                visitor.visit(conditionList.get(i));
            }
        }
    }

    public void visit(EntityConditionVisitor visitor, Object lhs, Object rhs) {
        ((EntityCondition) lhs).visit(visitor);
        visitor.visit(rhs);
    }

    public boolean entityMatches(GenericEntity entity, Object lhs, Object rhs) {
        return entityMatches(entity, (EntityCondition) lhs, (EntityCondition) rhs);
    }

    public Object eval(GenericEntity entity, EntityCondition lhs, EntityCondition rhs) {
        return entityMatches(entity, lhs, rhs) ? Boolean.TRUE : Boolean.FALSE;
    }

    public boolean entityMatches(GenericEntity entity, EntityCondition lhs, EntityCondition rhs) {
        if (lhs.entityMatches(entity)) return shortCircuitValue;
        if (rhs.entityMatches(entity)) return shortCircuitValue;
        return !shortCircuitValue;
    }

    public boolean entityMatches(GenericEntity entity, List conditionList) {
        return mapMatches(entity.getDelegator(), entity, conditionList);
    }

    public Object eval(GenericDelegator delegator, Map map, Object lhs, Object rhs) {
        return castBoolean(mapMatches(delegator, map, lhs, rhs));
    }

    public boolean mapMatches(GenericDelegator delegator, Map map, Object lhs, Object rhs) {
        if (((EntityCondition) lhs).mapMatches(delegator, map)) return shortCircuitValue;
        if (((EntityCondition) rhs).mapMatches(delegator, map)) return shortCircuitValue;
        return !shortCircuitValue;
    }

    public Object eval(GenericDelegator delegator, Map map, List conditionList) {
        return castBoolean(mapMatches(delegator, map, conditionList));
    }

    public boolean mapMatches(GenericDelegator delegator, Map map, List conditionList) {
        if (conditionList != null && conditionList.size() > 0) {
            for (int i = 0; i < conditionList.size(); i++) {
                EntityCondition condition = (EntityCondition) conditionList.get(i);
                if (condition.mapMatches(delegator, map) == shortCircuitValue) return shortCircuitValue;
            }
        }
        return !shortCircuitValue;
    }

    public void validateSql(ModelEntity modelEntity, Object lhs, Object rhs) throws GenericModelException {
        validateSql(modelEntity, (EntityCondition) lhs, (EntityCondition) rhs);
    }

    public void validateSql(ModelEntity modelEntity, EntityCondition lhs, EntityCondition rhs) throws GenericModelException {
        lhs.checkCondition(modelEntity);
        rhs.checkCondition(modelEntity);
    }

    public void validateSql(ModelEntity modelEntity, List conditionList) throws GenericModelException {
        if (conditionList == null) {
            throw new GenericModelException("Condition list is null");
        }
        for (int i = 0; i < conditionList.size(); i++) {
            Object condObj = conditionList.get(i);
            if (!(condObj instanceof EntityCondition)) {
                throw new GenericModelException("Object is not a valid EntityCondition [" + condObj.getClass().getName() + "]");
            }
            EntityCondition condition = (EntityCondition) condObj;
            condition.checkCondition(modelEntity);
        }
    }
}
