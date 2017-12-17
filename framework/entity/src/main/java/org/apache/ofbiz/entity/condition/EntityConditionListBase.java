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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericModelException;
import org.apache.ofbiz.entity.config.model.Datasource;
import org.apache.ofbiz.entity.model.ModelEntity;

/**
 * Encapsulates a list of EntityConditions to be used as a single EntityCondition combined as specified
 *
 */
@SuppressWarnings("serial")
public abstract class EntityConditionListBase<T extends EntityCondition> extends EntityCondition {
    public static final String module = EntityConditionListBase.class.getName();

    protected final List<T> conditionList;
    protected final EntityJoinOperator operator;

    protected EntityConditionListBase(List<T> conditionList, EntityJoinOperator operator) {
        this.conditionList = conditionList;
        this.operator = operator;
    }

    public EntityJoinOperator getOperator() {
        return this.operator;
    }

    public T getCondition(int index) {
        return this.conditionList.get(index);
    }

    protected int getConditionListSize() {
        return this.conditionList.size();
    }

    protected Iterator<T> getConditionIterator() {
        return this.conditionList.iterator();
    }

    @Override
    public void visit(EntityConditionVisitor visitor) {
        visitor.acceptEntityJoinOperator(operator, conditionList);
    }

    @Override
    public boolean isEmpty() {
        return operator.isEmpty(conditionList);
    }

    @Override
    public String makeWhereString(ModelEntity modelEntity, List<EntityConditionParam> entityConditionParams, Datasource datasourceInfo) {
        StringBuilder sql = new StringBuilder();
        operator.addSqlValue(sql, modelEntity, entityConditionParams, conditionList, datasourceInfo);
        return sql.toString();
    }

    @Override
    public void checkCondition(ModelEntity modelEntity) throws GenericModelException {
        operator.validateSql(modelEntity, conditionList);
    }

    @Override
    public boolean mapMatches(Delegator delegator, Map<String, ? extends Object> map) {
        return operator.mapMatches(delegator, map, conditionList);
    }

    @Override
    public EntityCondition freeze() {
        return operator.freeze(conditionList);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EntityConditionListBase<?>)) {
            return false;
        }
        EntityConditionListBase<?> other = UtilGenerics.cast(obj);

        boolean isEqual = conditionList.equals(other.conditionList) && operator.equals(other.operator);
        return isEqual;
    }

    @Override
    public int hashCode() {
        return conditionList.hashCode() + operator.hashCode();
    }
}
