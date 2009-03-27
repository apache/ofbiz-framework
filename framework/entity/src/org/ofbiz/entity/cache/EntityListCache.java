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
package org.ofbiz.entity.cache;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.util.EntityUtil;

public class EntityListCache extends AbstractEntityConditionCache<Object, List<GenericValue>> {

    public static final String module = EntityListCache.class.getName();

    public EntityListCache(String delegatorName) {
        super(delegatorName, "entity-list");
    }

    public List<GenericValue> get(String entityName, EntityCondition condition) {
        return this.get(entityName, condition, null);
    }

    public List<GenericValue> get(String entityName, EntityCondition condition, List<String> orderBy) {
        Map<Object, List<GenericValue>> conditionCache = getConditionCache(entityName, condition);
        if (conditionCache == null) return null;
        Object orderByKey = getOrderByKey(orderBy);
        List<GenericValue> valueList = conditionCache.get(orderByKey);
        if (valueList == null) {
            // the valueList was not found for the given ordering, so grab the first one and order it in memory
            Iterator<List<GenericValue>> it = conditionCache.values().iterator();
            if (it.hasNext()) valueList = it.next();

            synchronized (conditionCache) {
                if (valueList != null) {
                    valueList = EntityUtil.orderBy(valueList, orderBy);
                    conditionCache.put(orderByKey, valueList);
                }
            }
        }
        return valueList;
    }

    public void put(String entityName, EntityCondition condition, List<GenericValue> entities) {
        this.put(entityName, condition, null, entities);
    }

    public List<GenericValue> put(String entityName, EntityCondition condition, List<String> orderBy, List<GenericValue> entities) {
        return super.put(entityName, getFrozenConditionKey(condition), getOrderByKey(orderBy), entities);
    }

    public List<GenericValue> remove(String entityName, EntityCondition condition, List<String> orderBy) {
        return super.remove(entityName, condition, getOrderByKey(orderBy));
    }

    public static final Object getOrderByKey(List<String> orderBy) {
        return orderBy != null ? (Object) orderBy : "{null}";
    }
}
