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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericPK;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.model.ModelEntity;

public abstract class AbstractEntityConditionCache extends AbstractCache {

    public static final String module = AbstractEntityConditionCache.class.getName();

    protected AbstractEntityConditionCache(String delegatorName, String id) {
        super(delegatorName, id);
    }

    protected Object get(String entityName, EntityCondition condition, Object key) {
        Map conditionCache = getConditionCache(entityName, condition);
        if (conditionCache == null) return null;
        // the following line was synchronized, but for pretty good safety and better performance, only syncrhnizing the put; synchronized (conditionCache) {
        return conditionCache.get(key);
    }

    protected Object put(String entityName, EntityCondition condition, Object key, Object value) {
        ModelEntity entity = this.getDelegator().getModelEntity(entityName); 
        if (entity.getNeverCache()) {
            Debug.logWarning("Tried to put a value of the " + entityName + " entity in the cache but this entity has never-cache set to true, not caching.", module);
            return null;
        }
        
        Map conditionCache = getOrCreateConditionCache(entityName, condition);
        synchronized (conditionCache) {
            return conditionCache.put(key, value);
        }
    }

    public void remove(String entityName, EntityCondition condition) {
        UtilCache cache = getCache(entityName);
        if (cache == null) return;
        cache.remove(condition);
    }

    protected Object remove(String entityName, EntityCondition condition, Object key) {
        Map conditionCache = getConditionCache(entityName, condition);
        if (conditionCache == null) return null;
        synchronized (conditionCache) {
            return conditionCache.remove(key);
         }
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

    protected Map getConditionCache(String entityName, EntityCondition condition) {
        UtilCache cache = getCache(entityName);
        if (cache == null) return null;
        return (Map) cache.get(getConditionKey(condition));
    }

    protected Map getOrCreateConditionCache(String entityName, EntityCondition condition) {
        UtilCache utilCache = getOrCreateCache(entityName);
        Object conditionKey = getConditionKey(condition);
        Map conditionCache = (Map) utilCache.get(conditionKey);
        if (conditionCache == null) {
            conditionCache = new HashMap();
            utilCache.put(conditionKey, conditionCache);
        }
        return conditionCache;
    }

    protected static final boolean isNull(Map value) {
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

    protected List convert(boolean isPK, String targetEntityName, GenericEntity entity) {
        if (isNull(entity)) return null;
        if (isPK) {
            return entity.getModelEntity().convertToViewValues(targetEntityName, (GenericPK) entity);
        } else {
            return entity.getModelEntity().convertToViewValues(targetEntityName, entity);
        }
    }

    public synchronized void storeHook(boolean isPK, GenericEntity oldEntity, GenericEntity newEntity) {
        ModelEntity model = getModelCheckValid(oldEntity, newEntity);
        String entityName = model.getEntityName();
        // for info about cache clearing
        if (newEntity == null) {
            //Debug.logInfo("In storeHook calling sub-storeHook for entity name [" + entityName + "] for the oldEntity: " + oldEntity, module);
        }
        storeHook(entityName, isPK, UtilMisc.toList(oldEntity), UtilMisc.toList(newEntity));
        Iterator it = model.getViewConvertorsIterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            String targetEntityName = (String) entry.getKey();
            storeHook(targetEntityName, isPK, convert(isPK, targetEntityName, oldEntity), convert(false, targetEntityName, newEntity));
        }
    }

    protected void storeHook(String entityName, boolean isPK, List oldValues, List newValues) {
        UtilCache entityCache = null;
        synchronized (UtilCache.utilCacheTable) {
            entityCache = (UtilCache) UtilCache.utilCacheTable.get(getCacheName(entityName));
        }
        // for info about cache clearing
        if (newValues == null || newValues.size() == 0 || newValues.get(0) == null) {
            //Debug.logInfo("In storeHook (cache clear) for entity name [" + entityName + "], got entity cache with name: " + (entityCache == null ? "[No cache found to remove from]" : entityCache.getName()), module);
        }
        if (entityCache == null) {
            return;
        }
        Iterator cacheKeyIter = entityCache.getCacheLineKeys().iterator();
        while (cacheKeyIter.hasNext()) {
            EntityCondition condition = (EntityCondition) cacheKeyIter.next();
            //Debug.logInfo("In storeHook entityName [" + entityName + "] checking against condition: " + condition, module);
            boolean shouldRemove = false;
            if (condition == null) {
                shouldRemove = true;
            } else if (oldValues == null) {
                Iterator newValueIter = newValues.iterator();
                while (newValueIter.hasNext() && !shouldRemove) {
                    Map newValue = (Map) newValueIter.next();
                    shouldRemove |= condition.mapMatches(getDelegator(), newValue);
                }
            } else {
                boolean oldMatched = false;
                Iterator oldValueIter = oldValues.iterator();
                while (oldValueIter.hasNext() && !shouldRemove) {
                    Map oldValue = (Map) oldValueIter.next();
                    if (condition.mapMatches(getDelegator(), oldValue)) {
                        oldMatched = true;
                        //Debug.logInfo("In storeHook, oldMatched for entityName [" + entityName + "]; shouldRemove is false", module);
                        if (newValues != null) {
                            Iterator newValueIter = newValues.iterator();
                            while (newValueIter.hasNext() && !shouldRemove) {
                                Map newValue = (Map) newValueIter.next();
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
