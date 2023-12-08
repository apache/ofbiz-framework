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

import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericModelException;
import org.apache.ofbiz.entity.config.model.Datasource;
import org.apache.ofbiz.entity.model.ModelEntity;

/**
 * Represents a combination of multiple condition expressions.
 */
@SuppressWarnings("serial")
abstract class EntityConditionListBase<T extends EntityCondition> implements EntityCondition {

    public List<? extends T> getConditions() {
        return conditions;
    }

    /** The list of condition expressions to combine.  */
    private final List<? extends T> conditions;
    /** The infix operator used to combine every elements in the list of conditions.  */
    private final EntityJoinOperator operator;

    /**
     * Constructs a combination of multiple condition expressions.
     * @param conditions the list of condition expressions to combine
     * @param operator the infix operator used to combine every elements in the list of conditions
     */
    protected EntityConditionListBase(List<? extends T> conditions, EntityJoinOperator operator) {
        this.conditions = conditions;
        this.operator = operator;
    }

    /**
     * Gets the infix operator used to combine every elements in the list of conditions.
     * @return the infix operator used to combine every elements in the list of conditions.
     */
    public EntityJoinOperator getOperator() {
        return operator;
    }

    /**
     * Gets the condition expression stored at a particular of the internal list of conditions.
     * @param index the index of the condition expression to find
     * @return the corresponding condition expression
     */
    public T getCondition(int index) {
        return conditions.get(index);
    }

    @Override
    public boolean isEmpty() {
        return operator.isEmpty(conditions);
    }

    @Override
    public String makeWhereString(ModelEntity modelEntity, List<EntityConditionParam> entityConditionParams, Datasource datasourceInfo) {
        StringBuilder sql = new StringBuilder();
        operator.addSqlValue(sql, modelEntity, entityConditionParams, conditions, datasourceInfo);
        return sql.toString();
    }

    @Override
    public void checkCondition(ModelEntity modelEntity) throws GenericModelException {
        operator.validateSql(modelEntity, conditions);
    }

    @Override
    public boolean mapMatches(Delegator delegator, Map<String, ? extends Object> map) {
        return operator.mapMatches(delegator, map, conditions);
    }

    @Override
    public EntityCondition freeze() {
        return operator.freeze(conditions);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EntityConditionListBase<?>)) {
            return false;
        }
        EntityConditionListBase<?> other = UtilGenerics.cast(obj);

        return conditions.equals(other.conditions) && operator.equals(other.operator);
    }

    @Override
    public int hashCode() {
        return conditions.hashCode() + operator.hashCode();
    }

    @Override
    public String toString() {
        return makeWhereString();
    }
}
