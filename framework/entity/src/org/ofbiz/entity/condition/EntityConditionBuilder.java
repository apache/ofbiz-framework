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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.UtilGenerics;

import javolution.util.FastList;

import groovy.util.BuilderSupport;

public class EntityConditionBuilder extends BuilderSupport {

    @Override
    protected Object createNode(Object methodName) {
        String operatorName = ((String)methodName).toLowerCase();
        EntityJoinOperator operator = EntityOperator.lookupJoin(operatorName);
        List<EntityCondition> condList = FastList.newInstance();
        return EntityCondition.makeCondition(condList, operator);
    }

    @Override
    protected Object createNode(Object methodName, Object objArg) {
        Object node = createNode(methodName);
        setParent(node, objArg);
        return node;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Object createNode(Object methodName, Map mapArg) {
        Map<String, Object> fieldValueMap = UtilGenerics.checkMap(mapArg);
        String operatorName = ((String)methodName).toLowerCase();
        EntityComparisonOperator<String, Object> operator = EntityOperator.lookupComparison(operatorName);
        List<EntityCondition> conditionList = FastList.newInstance();
        for (Map.Entry<String, Object> entry : fieldValueMap.entrySet()) {
            conditionList.add(EntityCondition.makeCondition(entry.getKey(), operator, entry.getValue()));
        }
        if (conditionList.size() == 1) {
            return conditionList.get(0);
        } else {
            return EntityCondition.makeCondition(conditionList);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Object createNode(Object methodName, Map mapArg, Object objArg) {
        return null;
    }

    @Override
    protected void setParent(Object parent, Object child) {
        // No add method on EntityConditionList?
        EntityConditionList<EntityCondition> parentConList = UtilGenerics.cast(parent);
        Iterator<EntityCondition> iterator = parentConList.getConditionIterator();
        List<EntityCondition> tempList = FastList.newInstance();
        while (iterator.hasNext()) {
            tempList.add(iterator.next());
        }
        if (child instanceof EntityCondition) {
            tempList.add((EntityCondition)child);
        } else if (child instanceof List<?>) {
            tempList.addAll(UtilGenerics.<EntityCondition>checkList(child));
        }
        parentConList.init(tempList, parentConList.getOperator());
    }

}
