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
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.cache.UtilCache;
import org.apache.ofbiz.entity.GenericEntity;
import org.apache.ofbiz.entity.GenericPK;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.model.ModelEntity;

public abstract class AbstractEntityConditionCache<K, V> extends AbstractCache<EntityCondition, ConcurrentMap<K, V>> {

    public static final String module = AbstractEntityConditionCache.class.getName();

    protected AbstractEntityConditionCache(String delegatorName, String id) {
        super(delegatorName, id);
    }

    protected V get(String entityName, EntityCondition condition, K key) {
        ConcurrentMap<K, V> conditionCache = getConditionCache(entityName, condition);
        if (conditionCache == null) return null;
        return conditionCache.get(key);
    }

    protected V put(String entityName, EntityCondition condition, K key, V value) {
        ModelEntity entity = this.getDelegator().getModelEntity(entityName);
        if (entity.getNeverCache()) {
            Debug.logWarning("Tried to put a value of the " + entityName + " entity in the cache but this entity has never-cache set to true, not caching.", module);
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
        if (model != null) {
            Iterator<String> it = model.getViewConvertorsIterator();
            while (it.hasNext()) {
                String targetEntityName = it.next();
                UtilCache.clearCache(getCacheName(targetEntityName));
            }
        }
    }

    public void remove(String entityName, EntityCondition condition) {
        UtilCache<EntityCondition, ConcurrentMap<K, V>> cache = getCache(entityName);
        if (cache == null) return;
        cache.remove(condition);
    }

    protected V remove(String entityName, EntityCondition condition, K key) {
        ConcurrentMap<K, V> conditionCache = getConditionCache(entityName, condition);
        if (conditionCache == null) return null;
        return conditionCache.remove(key);
    }

    public static final EntityCondition getConditionKey(EntityCondition condition) {
        return condition != null ? condition : null;
    }

    public static final EntityCondition getFrozenConditionKey(EntityCondition condition) {
        EntityCondition frozenCondition = condition != null ? condition.freeze() : null;
        // This is no longer needed, fixed issue with unequal conditions after freezing
        //if (condition != null) {
        //    if (!condition.equals(frozenCondition)) {
        //        Debug.logWarning("Frozen condition does not equal condition:\n -=-=-=-Original=" + condition + "\n -=-=-=-Frozen=" + frozenCondition, module);
        //        Debug.logWarning("Frozen condition not equal info: condition class=" + condition.getClass().getName() + "; frozenCondition class=" + frozenCondition.getClass().getName(), module);
        //    }
        //}
        return frozenCondition;
    }

    protected ConcurrentMap<K, V> getConditionCache(String entityName, EntityCondition condition) {
        UtilCache<EntityCondition, ConcurrentMap<K, V>> cache = getCache(entityName);
        if (cache == null) return null;
        return cache.get(getConditionKey(condition));
    }

    protected Map<K, V> getOrCreateConditionCache(String entityName, EntityCondition condition) {
        UtilCache<EntityCondition, ConcurrentMap<K, V>> utilCache = getOrCreateCache(entityName);
        EntityCondition conditionKey = getConditionKey(condition);
        ConcurrentMap<K, V> conditionCache = utilCache.get(conditionKey);
        if (conditionCache == null) {
            conditionCache = new ConcurrentHashMap<K, V>();
            utilCache.put(conditionKey, conditionCache);
        }
        return conditionCache;
    }

    protected static final <K,V> boolean isNull(Map<K,V> value) {
        return value == null || value == GenericEntity.NULL_ENTITY || value == GenericValue.NULL_VALUE;
    }

    protected ModelEntity getModelCheckValid(GenericEntity oldEntity, GenericEntity newEntity) {
        ModelEntity model;
        if (!isNull(newEntity)) {
            model = newEntity.getModelEntity();
            String entityName = model.getEntityName();
            if (oldEntity != null && !entityName.equals(oldEntity.getEntityName())) {
                throw new IllegalArgumentException("internal error: storeHook called with 2 different entities(old=" + oldEntity.getEntityName() + ", new=" + entityName + ")");
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

    public void storeHook(GenericEntity newEntity) {
        storeHook(null, newEntity);
    }

    // if oldValue == null, then this is a new entity
    // if newValue == null, then
    public void storeHook(GenericEntity oldEntity, GenericEntity newEntity) {
        storeHook(false, oldEntity, newEntity);
    }

    // if oldValue == null, then this is a new entity
    // if newValue == null, then
    public void storeHook(GenericPK oldPK, GenericEntity newEntity) {
        storeHook(true, oldPK, newEntity);
    }

    protected List<? extends Map<String, Object>> convert(boolean isPK, String targetEntityName, GenericEntity entity) {
        if (isNull(entity)) return null;
        if (isPK) {
            return entity.getModelEntity().convertToViewValues(targetEntityName, entity);
        } else {
            return entity.getModelEntity().convertToViewValues(targetEntityName, entity);
        }
    }

    public void storeHook(boolean isPK, GenericEntity oldEntity, GenericEntity newEntity) {
        ModelEntity model = getModelCheckValid(oldEntity, newEntity);
        String entityName = model.getEntityName();
        // for info about cache clearing
        if (newEntity == null) {
            //Debug.logInfo("In storeHook calling sub-storeHook for entity name [" + entityName + "] for the oldEntity: " + oldEntity, module);
        }
        storeHook(entityName, isPK, UtilMisc.toList(oldEntity), UtilMisc.toList(newEntity));
        Iterator<String> it = model.getViewConvertorsIterator();
        while (it.hasNext()) {
            String targetEntityName = it.next();
            storeHook(targetEntityName, isPK, convert(isPK, targetEntityName, oldEntity), convert(false, targetEntityName, newEntity));
        }
    }

    protected <T1 extends Map<String, Object>, T2 extends Map<String, Object>> void storeHook(String entityName, boolean isPK, List<T1> oldValues, List<T2> newValues) {
        UtilCache<EntityCondition, Map<K, V>> entityCache = UtilCache.findCache(getCacheName(entityName));
        // for info about cache clearing
        if (UtilValidate.isEmpty(newValues) || newValues.get(0) == null) {
            //Debug.logInfo("In storeHook (cache clear) for entity name [" + entityName + "], got entity cache with name: " + (entityCache == null ? "[No cache found to remove from]" : entityCache.getName()), module);
        }
        if (entityCache == null) {
            return;
        }
        for (EntityCondition condition: entityCache.getCacheLineKeys()) {
            //Debug.logInfo("In storeHook entityName [" + entityName + "] checking against condition: " + condition, module);
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
                        //Debug.logInfo("In storeHook, oldMatched for entityName [" + entityName + "]; shouldRemove is false", module);
                        if (newValues != null) {
                            Iterator<T2> newValueIter = newValues.iterator();
                            while (newValueIter.hasNext() && !shouldRemove) {
                                T2 newValue = newValueIter.next();
                                shouldRemove |= isNull(newValue) || condition.mapMatches(getDelegator(), newValue);
                                //Debug.logInfo("In storeHook, for entityName [" + entityName + "] shouldRemove is now " + shouldRemove, module);
                            }
                        } else {
                            shouldRemove = true;
                        }
                    }
                }
                // QUESTION: what is this? why would we do this?
                if (!oldMatched && isPK) {
                    //Debug.logInfo("In storeHook, for entityName [" + entityName + "] oldMatched is false and isPK is true, so setting shouldRemove to true (will remove from cache)", module);
                    shouldRemove = true;
                }
            }
            if (shouldRemove) {
                if (Debug.verboseOn()) Debug.logVerbose("In storeHook, matched condition, removing from cache for entityName [" + entityName + "] in cache with name [" + entityCache.getName() + "] entry with condition: " + condition, module);
                // doesn't work anymore since this is a copy of the cache keySet, can call remove directly though with a concurrent mod exception: cacheKeyIter.remove();
                entityCache.remove(condition);
            }
        }
    }
}
