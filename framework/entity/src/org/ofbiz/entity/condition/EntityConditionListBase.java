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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericModelException;
import org.ofbiz.entity.config.DatasourceInfo;
import org.ofbiz.entity.model.ModelEntity;

/**
 * Encapsulates a list of EntityConditions to be used as a single EntityCondition combined as specified
 *
 */
public abstract class EntityConditionListBase<T extends EntityCondition> extends EntityCondition {
    public static final String module = EntityConditionListBase.class.getName();

    protected List<T> conditionList = null;
    protected EntityJoinOperator operator = null;

    protected EntityConditionListBase() {}

    public EntityConditionListBase(EntityJoinOperator operator, T... conditionList) {
        this.init(operator, conditionList);
    }

    public EntityConditionListBase(List<T> conditionList, EntityJoinOperator operator) {
        this.init(conditionList, operator);
    }

    public void init(EntityJoinOperator operator, T... conditionList) {
        this.conditionList = Arrays.asList(conditionList);
        this.operator = operator;
    }

    public void init(List<T> conditionList, EntityJoinOperator operator) {
        this.conditionList = conditionList;
        this.operator = operator;
    }

    public void reset() {
        this.conditionList = null;
        this.operator = null;
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
    public String makeWhereString(ModelEntity modelEntity, List<EntityConditionParam> entityConditionParams, DatasourceInfo datasourceInfo) {
        // if (Debug.verboseOn()) Debug.logVerbose("makeWhereString for entity " + modelEntity.getEntityName(), module);
        StringBuilder sql = new StringBuilder();
        operator.addSqlValue(sql, modelEntity, entityConditionParams, conditionList, datasourceInfo);
        return sql.toString();
    }

    @Override
    public void checkCondition(ModelEntity modelEntity) throws GenericModelException {
        // if (Debug.verboseOn()) Debug.logVerbose("checkCondition for entity " + modelEntity.getEntityName(), module);
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
    public void encryptConditionFields(ModelEntity modelEntity, Delegator delegator) {
        for (T cond: this.conditionList) {
            cond.encryptConditionFields(modelEntity, delegator);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EntityConditionListBase)) return false;
        EntityConditionListBase<?> other = UtilGenerics.cast(obj);

        boolean isEqual = conditionList.equals(other.conditionList) && operator.equals(other.operator);
        //if (!isEqual) {
        //    Debug.logWarning("EntityConditionListBase.equals is false:\n this.operator=" + this.operator + "; other.operator=" + other.operator +
        //            "\nthis.conditionList=" + this.conditionList +
        //            "\nother.conditionList=" + other.conditionList, module);
        //}
        return isEqual;
    }

    @Override
    public int hashCode() {
        return conditionList.hashCode() + operator.hashCode();
    }
}
