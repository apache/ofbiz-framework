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

import java.util.List;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericPK;
import org.ofbiz.entity.condition.EntityCondition;

public class Cache {

    public static final String module = Cache.class.getName();

    protected EntityCache entityCache;
    protected EntityListCache entityListCache;
    protected EntityObjectCache entityObjectCache;

    protected String delegatorName;

    public Cache(String delegatorName) {
        this.delegatorName = delegatorName;
        entityCache = new EntityCache(delegatorName);
        entityObjectCache = new EntityObjectCache(delegatorName);
        entityListCache = new EntityListCache(delegatorName);
    }

    public void clear() {
        entityCache.clear();
        entityListCache.clear();
        entityObjectCache.clear();
    }

    public void remove(String entityName) {
        entityCache.remove(entityName);
        entityListCache.remove(entityName);
    }

    public GenericEntity get(GenericPK pk) {
        return entityCache.get(pk);
    }

    public List get(String entityName, EntityCondition condition, List orderBy) {
        return entityListCache.get(entityName, condition, orderBy);
    }

    public Object get(String entityName, EntityCondition condition, String name) {
        return entityObjectCache.get(entityName, condition, name);
    }

    public List put(String entityName, EntityCondition condition, List orderBy, List entities) {
        return entityListCache.put(entityName, condition, orderBy, entities);
    }

    public Object put(String entityName, EntityCondition condition, String name, Object value) {
        return entityObjectCache.put(entityName, condition, name, value);
    }

    public GenericEntity put(GenericEntity entity) {
        GenericEntity oldEntity = entityCache.put(entity.getPrimaryKey(), entity);
        if (entity.getModelEntity().getAutoClearCache()) {
            entityListCache.storeHook(entity);
            entityObjectCache.storeHook(entity);
        }
        return oldEntity;
    }
    
    public GenericEntity put(GenericPK pk, GenericEntity entity) {
        GenericEntity oldEntity = entityCache.put(pk, entity);
        if (pk.getModelEntity().getAutoClearCache()) {
            entityListCache.storeHook(pk, entity);
            entityObjectCache.storeHook(pk, entity);
        }
        return oldEntity;
    }

    public List remove(String entityName, EntityCondition condition, List orderBy) {
        entityCache.remove(entityName, condition);
        entityObjectCache.remove(entityName, condition);
        return entityListCache.remove(entityName, condition, orderBy);
    }

    public void remove(String entityName, EntityCondition condition) {
        entityCache.remove(entityName, condition);
        entityListCache.remove(entityName, condition);
        entityObjectCache.remove(entityName, condition);
    }

    public Object remove(String entityName, EntityCondition condition, String name) {
        return entityObjectCache.remove(entityName, condition, name);
    }

    public GenericEntity remove(GenericEntity entity) {
        if (Debug.verboseOn()) Debug.logVerbose("Cache remove GenericEntity: " + entity, module);
        GenericEntity oldEntity = entityCache.remove(entity.getPrimaryKey());
        entityListCache.storeHook(entity, null);
        entityObjectCache.storeHook(entity, null);
        return oldEntity;
    }

    public GenericEntity remove(GenericPK pk) {
        if (Debug.verboseOn()) Debug.logVerbose("Cache remove GenericPK: " + pk, module);
        GenericEntity oldEntity = entityCache.remove(pk);
        entityListCache.storeHook(pk, null);
        entityObjectCache.storeHook(pk, null);
        return oldEntity;
    }
}
