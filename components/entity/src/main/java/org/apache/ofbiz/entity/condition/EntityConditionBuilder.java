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
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericModelException;
import org.apache.ofbiz.entity.config.model.Datasource;
import org.apache.ofbiz.entity.model.ModelEntity;

import groovy.util.BuilderSupport;

public class EntityConditionBuilder extends BuilderSupport {
    private static final String MODULE = EntityConditionBuilder.class.getName();

    @SuppressWarnings("serial")
    private static class ConditionHolder implements EntityCondition {
        private EntityCondition condition;

        protected ConditionHolder(EntityCondition condition) {
            this.condition = condition;
        }

        @Override
        public boolean isEmpty() {
            return condition.isEmpty();
        }

        @Override
        public String makeWhereString(ModelEntity modelEntity, List<EntityConditionParam> entityConditionParams, Datasource datasourceInfo) {
            return condition.makeWhereString(modelEntity, entityConditionParams, datasourceInfo);
        }

        @Override
        public void checkCondition(ModelEntity modelEntity) throws GenericModelException {
            condition.checkCondition(modelEntity);
        }

        @Override
        public boolean mapMatches(Delegator delegator, Map<String, ? extends Object> map) {
            return condition.mapMatches(delegator, map);
        }

        @Override
        public EntityCondition freeze() {
            return condition.freeze();
        }

        @Override
        public int hashCode() {
            return condition.hashCode();
        }
        @Override
        public boolean equals(Object obj) {
            return condition.equals(obj);
        }

        @Override
        public void accept(EntityConditionVisitor visitor) {
            throw new IllegalArgumentException(getClass().getName() + ".accept not implemented");
        }

        @Override
        public String toString() {
            return makeWhereString();
        }
    }

    @Override
    protected Object createNode(Object methodName) {
        String operatorName = ((String) methodName).toLowerCase(Locale.getDefault());
        EntityJoinOperator operator = EntityOperator.lookupJoin(operatorName);
        List<EntityCondition> condList = new LinkedList<>();
        return new ConditionHolder(EntityCondition.makeCondition(condList, operator));
    }

    @Override
    protected Object createNode(Object methodName, Object objArg) {
        Object node = createNode(methodName);
        setParent(node, objArg);
        return node;
    }

    @Override
    protected Object createNode(Object methodName, @SuppressWarnings("rawtypes") Map mapArg) {
        Map<String, Object> fieldValueMap = UtilGenerics.cast(mapArg);
        String operatorName = ((String) methodName).toLowerCase(Locale.getDefault());
        EntityComparisonOperator<String, Object> operator = EntityOperator.lookupComparison(operatorName);
        List<EntityCondition> conditionList = new LinkedList<>();
        for (Map.Entry<String, Object> entry : fieldValueMap.entrySet()) {
            conditionList.add(EntityCondition.makeCondition(entry.getKey(), operator, entry.getValue()));
        }
        if (conditionList.size() == 1) {
            return new ConditionHolder(conditionList.get(0));
        }
        return new ConditionHolder(EntityCondition.makeCondition(conditionList));
    }

    @Override
    protected Object createNode(Object methodName, @SuppressWarnings("rawtypes") Map mapArg, Object objArg) {
        return null;
    }

    @Override
    protected void setParent(Object parent, Object child) {
        ConditionHolder holder = (ConditionHolder) parent;
        EntityConditionList<EntityCondition> parentConList = UtilGenerics.cast(holder.condition);
        Iterator<EntityCondition> iterator = parentConList.getConditionIterator();
        List<EntityCondition> tempList = new LinkedList<>();
        while (iterator.hasNext()) {
            tempList.add(iterator.next());
        }
        if (child instanceof EntityCondition) {
            tempList.add((EntityCondition) child);
        } else if (child instanceof ConditionHolder) {
            tempList.add(((ConditionHolder) child).condition);
        } else {
            tempList.addAll(UtilGenerics.cast(child));
        }
        holder.condition = EntityCondition.makeCondition(tempList, parentConList.getOperator());
    }

}
