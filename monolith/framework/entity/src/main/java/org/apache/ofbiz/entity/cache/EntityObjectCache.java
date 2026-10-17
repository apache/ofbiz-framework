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

package org.apache.ofbiz.entity.cache;

import org.apache.ofbiz.entity.condition.EntityCondition;

public class EntityObjectCache extends AbstractEntityConditionCache<String, Object> {

    private static final String MODULE = EntityObjectCache.class.getName();

    public EntityObjectCache(String delegatorName) {
        super(delegatorName, "object-list");
    }

    @Override
    public Object get(String entityName, EntityCondition condition, String name) {
        return super.get(entityName, condition, name);
    }

    @Override
    public Object put(String entityName, EntityCondition condition, String name, Object value) {
        return super.put(entityName, getFrozenConditionKey(condition), name, value);
    }

    @Override
    public Object remove(String entityName, EntityCondition condition, String name) {
        return super.remove(entityName, condition, name);
    }
}
