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

import java.util.List;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.entity.GenericEntity;
import org.apache.ofbiz.entity.GenericPK;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;

public class Cache {

    private static final String MODULE = Cache.class.getName();

    private EntityCache entityCache;
    private EntityListCache entityListCache;
    private EntityObjectCache entityObjectCache;

    private String delegatorName;

    public Cache(String delegatorName) {
        this.delegatorName = delegatorName;
        entityCache = new EntityCache(delegatorName);
        entityObjectCache = new EntityObjectCache(delegatorName);
        entityListCache = new EntityListCache(delegatorName);
    }

    /**
     * Clear.
     */
    public void clear() {
        entityCache.clear();
        entityListCache.clear();
        entityObjectCache.clear();
    }

    /**
     * Remove.
     * @param entityName the entity name
     */
    public void remove(String entityName) {
        entityCache.remove(entityName);
        entityListCache.remove(entityName);
    }

    /**
     * Get generic value.
     * @param pk the pk
     * @return the generic value
     */
    public GenericValue get(GenericPK pk) {
        return entityCache.get(pk);
    }

    /**
     * Get list.
     * @param entityName the entity name
     * @param condition  the condition
     * @param orderBy    the order by
     * @return the list
     */
    public List<GenericValue> get(String entityName, EntityCondition condition, List<String> orderBy) {
        return entityListCache.get(entityName, condition, orderBy);
    }

    /**
     * Get t.
     * @param <T>        the type parameter
     * @param entityName the entity name
     * @param condition  the condition
     * @param name       the name
     * @return the t
     */
    public <T> T get(String entityName, EntityCondition condition, String name) {
        return UtilGenerics.<T>cast(entityObjectCache.get(entityName, condition, name));
    }

    /**
     * Put list.
     * @param entityName the entity name
     * @param condition  the condition
     * @param orderBy    the order by
     * @param entities   the entities
     * @return the list
     */
    public List<GenericValue> put(String entityName, EntityCondition condition, List<String> orderBy, List<GenericValue> entities) {
        return entityListCache.put(entityName, condition, orderBy, entities);
    }

    /**
     * Put t.
     * @param <T>        the type parameter
     * @param entityName the entity name
     * @param condition  the condition
     * @param name       the name
     * @param value      the value
     * @return the t
     */
    public <T> T put(String entityName, EntityCondition condition, String name, T value) {
        return UtilGenerics.<T>cast(entityObjectCache.put(entityName, condition, name, value));
    }

    /**
     * Put generic value.
     * @param entity the entity
     * @return the generic value
     */
    public GenericValue put(GenericValue entity) {
        GenericValue oldEntity = entityCache.put(entity.getPrimaryKey(), entity);
        if (entity.getModelEntity().getAutoClearCache()) {
            entityListCache.storeHook(entity);
            entityObjectCache.storeHook(entity);
        }
        return oldEntity;
    }

    /**
     * Put generic value.
     * @param pk     the pk
     * @param entity the entity
     * @return the generic value
     */
    public GenericValue put(GenericPK pk, GenericValue entity) {
        GenericValue oldEntity = entityCache.put(pk, entity);
        if (pk.getModelEntity().getAutoClearCache()) {
            entityListCache.storeHook(pk, entity);
            entityObjectCache.storeHook(pk, entity);
        }
        return oldEntity;
    }

    /**
     * Remove list.
     * @param entityName the entity name
     * @param condition  the condition
     * @param orderBy    the order by
     * @return the list
     */
    public List<GenericValue> remove(String entityName, EntityCondition condition, List<String> orderBy) {
        entityCache.remove(entityName, condition);
        entityObjectCache.remove(entityName, condition);
        return entityListCache.remove(entityName, condition, orderBy);
    }

    /**
     * Remove.
     * @param entityName the entity name
     * @param condition  the condition
     */
    public void remove(String entityName, EntityCondition condition) {
        entityCache.remove(entityName, condition);
        entityListCache.remove(entityName, condition);
        entityObjectCache.remove(entityName, condition);
    }

    /**
     * Remove t.
     * @param <T>        the type parameter
     * @param entityName the entity name
     * @param condition  the condition
     * @param name       the name
     * @return the t
     */
    public <T> T remove(String entityName, EntityCondition condition, String name) {
        return UtilGenerics.<T>cast(entityObjectCache.remove(entityName, condition, name));
    }

    /**
     * Remove generic value.
     * @param entity the entity
     * @return the generic value
     */
    public GenericValue remove(GenericEntity entity) {
        if (Debug.verboseOn()) {
            Debug.logVerbose("Cache remove GenericEntity: " + entity, MODULE);
        }
        GenericValue oldEntity = entityCache.remove(entity.getPrimaryKey());
        // Workaround because AbstractEntityConditionCache.storeHook doesn't work.
        entityListCache.remove(entity);
        entityObjectCache.remove(entity);
        return oldEntity;
    }

    /**
     * Remove generic value.
     * @param pk the pk
     * @return the generic value
     */
    public GenericValue remove(GenericPK pk) {
        if (Debug.verboseOn()) {
            Debug.logVerbose("Cache remove GenericPK: " + pk, MODULE);
        }
        GenericValue oldEntity = entityCache.remove(pk);
        // Workaround because AbstractEntityConditionCache.storeHook doesn't work.
        entityListCache.remove(pk);
        entityObjectCache.remove(pk);
        // entityListCache.storeHook(pk, null);
        // entityObjectCache.storeHook(pk, null);
        return oldEntity;
    }
}
