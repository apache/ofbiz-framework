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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.cache.UtilCache;
import org.apache.ofbiz.entity.GenericEntity;
import org.apache.ofbiz.entity.GenericPK;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.model.ModelEntity;

/**
 * The type AbstractEntityConditionCache.
 * @param <K> the type parameter
 * @param <V> the type parameter
 */
public abstract class AbstractEntityConditionCache<K, V> extends AbstractCache<EntityCondition, ConcurrentMap<K, V>> {

    private static final String MODULE = AbstractEntityConditionCache.class.getName();

    protected AbstractEntityConditionCache(String delegatorName, String id) {
        super(delegatorName, id);
    }

    /**
     * Get v.
     * @param entityName the entity name
     * @param condition the condition
     * @param key the key
     * @return the v
     */
    protected V get(String entityName, EntityCondition condition, K key) {
        ConcurrentMap<K, V> conditionCache = getConditionCache(entityName, condition);
        if (conditionCache == null) {
            return null;
        }
        return conditionCache.get(key);
    }

    /**
     * Put v.
     * @param entityName the entity name
     * @param condition the condition
     * @param key the key
     * @param value the value
     * @return the v
     */
    protected V put(String entityName, EntityCondition condition, K key, V value) {
        ModelEntity entity = this.getDelegator().getModelEntity(entityName);
        if (entity.getNeverCache()) {
            Debug.logWarning("Tried to put a value of the " + entityName
                    + " entity in the cache but this entity has never-cache set to true, not caching.", MODULE);
            return null;
        }

        Map<K, V> conditionCache = getOrCreateConditionCache(entityName, condition);
        return conditionCache.put(key, value);
    }

    /**
     * Removes all condition caches that include the specified entity.
     */
    public void remove(GenericEntity entity) {
        UtilCache.clearCache(getCacheName(entity.getEntityName()));
        ModelEntity model = entity.getModelEntity();
        Iterator<String> it = model.getViewConvertorsIterator();
        while (it.hasNext()) {
            String targetEntityName = it.next();
            UtilCache.clearCache(getCacheName(targetEntityName));
        }
    }

    /**
     * Remove.
     * @param entityName the entity name
     * @param condition the condition
     */
    public void remove(String entityName, EntityCondition condition) {
        UtilCache<EntityCondition, ConcurrentMap<K, V>> cache = getCache(entityName);
        if (cache == null) {
            return;
        }
        cache.remove(condition);
    }

    /**
     * Remove v.
     * @param entityName the entity name
     * @param condition the condition
     * @param key the key
     * @return the v
     */
    protected V remove(String entityName, EntityCondition condition, K key) {
        ConcurrentMap<K, V> conditionCache = getConditionCache(entityName, condition);
        if (conditionCache == null) {
            return null;
        }
        return conditionCache.remove(key);
    }

    public static final EntityCondition getConditionKey(EntityCondition condition) {
        return condition != null ? condition : null;
    }

    public static final EntityCondition getFrozenConditionKey(EntityCondition condition) {
        EntityCondition frozenCondition = condition != null ? condition.freeze() : null;
        return frozenCondition;
    }

    /**
     * Gets condition cache.
     * @param entityName the entity name
     * @param condition the condition
     * @return the condition cache
     */
    protected ConcurrentMap<K, V> getConditionCache(String entityName, EntityCondition condition) {
        UtilCache<EntityCondition, ConcurrentMap<K, V>> cache = getCache(entityName);
        if (cache == null) {
            return null;
        }
        return cache.get(getConditionKey(condition));
    }

    /**
     * Gets or create condition cache.
     * @param entityName the entity name
     * @param condition the condition
     * @return the or create condition cache
     */
    protected Map<K, V> getOrCreateConditionCache(String entityName, EntityCondition condition) {
        UtilCache<EntityCondition, ConcurrentMap<K, V>> utilCache = getOrCreateCache(entityName);
        EntityCondition conditionKey = getConditionKey(condition);
        ConcurrentMap<K, V> conditionCache = utilCache.get(conditionKey);
        if (conditionCache == null) {
            conditionCache = new ConcurrentHashMap<>();
            utilCache.put(conditionKey, conditionCache);
        }
        return conditionCache;
    }

    protected static final <K, V> boolean isNull(Map<K, V> value) {
        return value == null || value == GenericEntity.NULL_ENTITY || value == GenericValue.NULL_VALUE;
    }

    /**
     * Gets model check valid.
     * @param oldEntity the old entity
     * @param newEntity the new entity
     * @return the model check valid
     */
    protected ModelEntity getModelCheckValid(GenericEntity oldEntity, GenericEntity newEntity) {
        ModelEntity model;
        if (!isNull(newEntity)) {
            model = newEntity.getModelEntity();
            String entityName = model.getEntityName();
            if (oldEntity != null && !entityName.equals(oldEntity.getEntityName())) {
                throw new IllegalArgumentException("internal error: storeHook called with 2 different entities(old=" + oldEntity.getEntityName()
                        + ", new=" + entityName + ")");
            }
        } else {
            if (!isNull(oldEntity)) {
                model = oldEntity.getModelEntity();
            } else {
                throw new IllegalArgumentException("internal error: storeHook called with 2 null arguments");
            }
        }
        return model;
    }

    /**
     * Store hook.
     * @param newEntity the new entity
     */
    public void storeHook(GenericEntity newEntity) {
        storeHook(null, newEntity);
    }

    /**
     * Store hook.
     * @param oldEntity the old entity
     * @param newEntity the new entity
     * if oldValue == null, then this is a new entity
     * if newValue == null, then
     */
    public void storeHook(GenericEntity oldEntity, GenericEntity newEntity) {
        storeHook(false, oldEntity, newEntity);
    }

    /**
     * Store hook.
     * @param oldPK the old pk
     * @param newEntity the new entity
     * if oldValue == null, then this is a new entity
     * if newValue == null, then
     */
    public void storeHook(GenericPK oldPK, GenericEntity newEntity) {
        storeHook(true, oldPK, newEntity);
    }

    /**
     * Convert list.
     * @param targetEntityName the target entity name
     * @param entity the entity
     * @return the list
     */
    protected List<? extends Map<String, Object>> convert(String targetEntityName, GenericEntity entity) {
        if (isNull(entity)) {
            return null;
        }
        return entity.getModelEntity().convertToViewValues(targetEntityName, entity);
    }

    /**
     * Store hook.
     * @param isPK the is pk
     * @param oldEntity the old entity
     * @param newEntity the new entity
     */
    public void storeHook(boolean isPK, GenericEntity oldEntity, GenericEntity newEntity) {
        ModelEntity model = getModelCheckValid(oldEntity, newEntity);
        String entityName = model.getEntityName();
        // for info about cache clearing
        storeHook(entityName, isPK, UtilMisc.toList(oldEntity), UtilMisc.toList(newEntity));
        Iterator<String> it = model.getViewConvertorsIterator();
        while (it.hasNext()) {
            String targetEntityName = it.next();
            storeHook(targetEntityName, isPK, convert(targetEntityName, oldEntity), convert(targetEntityName, newEntity));
        }
    }

    /**
     * Store hook.
     * @param <T1> the type parameter
     * @param <T2> the type parameter
     * @param entityName the entity name
     * @param isPK the is pk
     * @param oldValues the old values
     * @param newValues the new values
     */
    protected <T1 extends Map<String, Object>, T2 extends Map<String, Object>> void storeHook(String entityName, boolean isPK,
                                                                                              List<T1> oldValues, List<T2> newValues) {
        UtilCache<EntityCondition, Map<K, V>> entityCache = UtilCache.findCache(getCacheName(entityName));
        // for info about cache clearing
        if (entityCache == null) {
            return;
        }
        for (EntityCondition condition: entityCache.getCacheLineKeys()) {
            boolean shouldRemove = false;
            if (condition == null) {
                shouldRemove = true;
            } else if (oldValues == null) {
                Iterator<T2> newValueIter = newValues.iterator();
                while (newValueIter.hasNext() && !shouldRemove) {
                    T2 newValue = newValueIter.next();
                    shouldRemove |= condition.mapMatches(getDelegator(), newValue);
                }
            } else {
                boolean oldMatched = false;
                Iterator<T1> oldValueIter = oldValues.iterator();
                while (oldValueIter.hasNext() && !shouldRemove) {
                    T1 oldValue = oldValueIter.next();
                    if (condition.mapMatches(getDelegator(), oldValue)) {
                        oldMatched = true;
                        if (newValues != null) {
                            Iterator<T2> newValueIter = newValues.iterator();
                            while (newValueIter.hasNext() && !shouldRemove) {
                                T2 newValue = newValueIter.next();
                                shouldRemove |= isNull(newValue) || condition.mapMatches(getDelegator(), newValue);
                            }
                        } else {
                            shouldRemove = true;
                        }
                    }
                }
                // QUESTION: what is this? why would we do this?
                if (!oldMatched && isPK) {
                    shouldRemove = true;
                }
            }
            if (shouldRemove) {
                if (Debug.verboseOn()) {
                    Debug.logVerbose("In storeHook, matched condition, removing from cache for entityName [" + entityName
                            + "] in cache with name [" + entityCache.getName() + "] entry with condition: " + condition, MODULE);
                }
                // doesn't work anymore since this is a copy of the cache keySet, can call remove directly though with a concurrent mod
                // exception: cacheKeyIter.remove();
                entityCache.remove(condition);
            }
        }
    }
}
