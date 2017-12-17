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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntity;
import org.apache.ofbiz.entity.GenericModelException;
import org.apache.ofbiz.entity.config.model.Datasource;
import org.apache.ofbiz.entity.model.ModelEntity;

/**
 * Join operator (AND/OR).
 *
 */
@SuppressWarnings("serial")
public class EntityJoinOperator extends EntityOperator<EntityCondition, EntityCondition, Boolean> {

    protected boolean shortCircuitValue;

    protected EntityJoinOperator(int id, String code, boolean shortCircuitValue) {
        super(id, code);
        this.shortCircuitValue = shortCircuitValue;
    }

    @Override
    public void addSqlValue(StringBuilder sql, ModelEntity modelEntity, List<EntityConditionParam> entityConditionParams, boolean compat, EntityCondition lhs, EntityCondition rhs, Datasource datasourceInfo) {
        List<EntityCondition> conditions = new LinkedList<>();
        conditions.add(lhs);
        conditions.add(rhs);
        addSqlValue(sql, modelEntity, entityConditionParams, conditions, datasourceInfo);
    }

    public void addSqlValue(StringBuilder sql, ModelEntity modelEntity, List<EntityConditionParam> entityConditionParams, List<? extends EntityCondition> conditionList, Datasource datasourceInfo) {
        if (UtilValidate.isNotEmpty(conditionList)) {
            boolean hadSomething = false;
            Iterator<? extends EntityCondition> conditionIter = conditionList.iterator();
            while (conditionIter.hasNext()) {
                EntityCondition condition = conditionIter.next();
                if (condition.isEmpty()) {
                    continue;
                }
                if (hadSomething) {
                    sql.append(' ');
                    sql.append(getCode());
                    sql.append(' ');
                } else {
                    hadSomething = true;
                    sql.append('(');
                }
                sql.append(condition.makeWhereString(modelEntity, entityConditionParams, datasourceInfo));
            }
            if (hadSomething) {
                sql.append(')');
            }
        }
    }

    protected EntityCondition freeze(Object item) {
        return ((EntityCondition) item).freeze();
    }

    @Override
    public EntityCondition freeze(EntityCondition lhs, EntityCondition rhs) {
        return EntityCondition.makeCondition(freeze(lhs), this, freeze(rhs));
    }

    public EntityCondition freeze(List<? extends EntityCondition> conditionList) {
        List<EntityCondition> newList = new ArrayList<>(conditionList.size());
        for (EntityCondition condition: conditionList) {
            newList.add(condition.freeze());
        }
        return EntityCondition.makeCondition(newList, this);
    }

    public void visit(EntityConditionVisitor visitor, List<? extends EntityCondition> conditionList) {
        if (UtilValidate.isNotEmpty(conditionList)) {
            for (EntityCondition condition: conditionList) {
                visitor.visit(condition);
            }
        }
    }

    @Override
    public void visit(EntityConditionVisitor visitor, EntityCondition lhs, EntityCondition rhs) {
        lhs.visit(visitor);
        visitor.visit(rhs);
    }

    public Boolean eval(GenericEntity entity, EntityCondition lhs, EntityCondition rhs) {
        return entityMatches(entity, lhs, rhs) ? Boolean.TRUE : Boolean.FALSE;
    }

    @Override
    public boolean isEmpty(EntityCondition lhs, EntityCondition rhs) {
        return lhs.isEmpty() && rhs.isEmpty();
    }

    public boolean isEmpty(List<? extends EntityCondition> conditionList) {
        for (EntityCondition condition: conditionList) {
            if (!condition.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean entityMatches(GenericEntity entity, EntityCondition lhs, EntityCondition rhs) {
        if (lhs.entityMatches(entity) == shortCircuitValue) {
            return shortCircuitValue;
        }
        if (rhs.entityMatches(entity) == shortCircuitValue) {
            return shortCircuitValue;
        }
        return !shortCircuitValue;
    }

    public boolean entityMatches(GenericEntity entity, List<? extends EntityCondition> conditionList) {
        return mapMatches(entity.getDelegator(), entity, conditionList);
    }

    public Boolean eval(Delegator delegator, Map<String, ? extends Object> map, EntityCondition lhs, EntityCondition rhs) {
        return castBoolean(mapMatches(delegator, map, lhs, rhs));
    }

    @Override
    public boolean mapMatches(Delegator delegator, Map<String, ? extends Object> map, EntityCondition lhs, EntityCondition rhs) {
        if (lhs.mapMatches(delegator, map) == shortCircuitValue) {
            return shortCircuitValue;
        }
        if (rhs.mapMatches(delegator, map) == shortCircuitValue) {
            return shortCircuitValue;
        }
        return !shortCircuitValue;
    }

    public Boolean eval(Delegator delegator, Map<String, ? extends Object> map, List<? extends EntityCondition> conditionList) {
        return castBoolean(mapMatches(delegator, map, conditionList));
    }

    public boolean mapMatches(Delegator delegator, Map<String, ? extends Object> map, List<? extends EntityCondition> conditionList) {
        if (UtilValidate.isNotEmpty(conditionList)) {
            for (EntityCondition condition: conditionList) {
                if (condition.mapMatches(delegator, map) == shortCircuitValue) {
                    return shortCircuitValue;
                }
            }
        }
        return !shortCircuitValue;
    }

    @Override
    public void validateSql(ModelEntity modelEntity, EntityCondition lhs, EntityCondition rhs) throws GenericModelException {
        lhs.checkCondition(modelEntity);
        rhs.checkCondition(modelEntity);
    }

    public void validateSql(ModelEntity modelEntity, List<? extends EntityCondition> conditionList) throws GenericModelException {
        if (conditionList == null) {
            throw new GenericModelException("Condition list is null");
        }
        for (EntityCondition condition: conditionList) {
            condition.checkCondition(modelEntity);
        }
    }
}
