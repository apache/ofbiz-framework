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
import java.util.Objects;

import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericModelException;
import org.apache.ofbiz.entity.config.model.Datasource;
import org.apache.ofbiz.entity.model.ModelEntity;

/**
 * A condition expression which is prefixed by the {@code NOT} unary operator.
 */
@SuppressWarnings("serial")
public class EntityNotCondition implements EntityCondition {
    private EntityCondition condition;

    /**
     * Instantiates a negation condition expression.
     * @param cond the condition to negate
     */
    public EntityNotCondition(EntityCondition cond) {
        condition = cond;
    }

    @Override
    public void accept(EntityConditionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof EntityNotCondition)
                && Objects.equals(condition, ((EntityNotCondition) obj).condition);
    }

    @Override
    public int hashCode() {
        return "NOT".hashCode() ^ condition.hashCode();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public String makeWhereString(ModelEntity modelEntity, List<EntityConditionParam> entityConditionParams, Datasource datasourceInfo) {
        return new StringBuilder()
                .append("NOT(")
                .append(condition.makeWhereString(modelEntity, entityConditionParams, datasourceInfo))
                .append(')')
                .toString();
    }

    @Override
    public void checkCondition(ModelEntity modelEntity) throws GenericModelException {
        condition.checkCondition(modelEntity);
    }

    @Override
    public boolean mapMatches(Delegator delegator, Map<String, ? extends Object> map) {
        return !condition.mapMatches(delegator, map);
    }

    @Override
    public String toString() {
        return makeWhereString();
    }

    @Override
    public EntityCondition freeze() {
        return new EntityNotCondition(condition.freeze());
    }
}
