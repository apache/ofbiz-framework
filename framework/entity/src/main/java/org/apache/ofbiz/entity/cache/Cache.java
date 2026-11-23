/*
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntity;
import org.apache.ofbiz.entity.GenericPK;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.model.ModelEntity;

public class Cache {

    private static final String MODULE = Cache.class.getName();

    private EntityCache entityCache;
    private EntityListCache entityListCache;
    private EntityObjectCache entityObjectCache;
    /** Per-entity counters moved on by invalidations from other servers. */
    private final ConcurrentMap<String, AtomicLong> versions = new ConcurrentHashMap<>();
    /** Moved on by a remote clear-all or a bus gap, which affects every entity. */
    private final AtomicLong epoch = new AtomicLong();

    public Cache(String delegatorName) {
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
     * Removes everything cached for an entity on this server: values, lists and the object cache.
     * @param entityName the entity name
     */
    public void removeEntity(String entityName) {
        entityCache.remove(entityName);
        entityListCache.remove(entityName);
        entityObjectCache.remove(entityName);
    }

    /**
     * Removes everything cached on this server for an entity and for the view entities containing it.
     * @param entityName the entity name
     */
    public void removeEntityAndViews(String entityName) {
        removeEntity(entityName);
        Iterator<String> views = viewEntityNames(entityName);
        while (views.hasNext()) {
            removeEntity(views.next());
        }
    }

    /**
     * Returns the invalidation version of an entity's cache entries. A load records it before reading the database and caches
     * its result only if it has not moved, so a value read before another server's change is not kept.
     * @param entityName the entity name
     * @return the invalidation version
     */
    public long invalidationVersion(String entityName) {
        AtomicLong version = versions.get(entityName);
        return epoch.get() + (version == null ? 0 : version.get());
    }

    /** Moves every entity's invalidation version on, after a remote clear-all or a bus gap. */
    public void invalidateAllVersions() {
        epoch.incrementAndGet();
    }

    /**
     * Moves the invalidation version of an entity, and of the view entities containing it, on, so a load that read the
     * database before does not cache its result.
     * @param entityName the entity name
     */
    public void invalidateVersion(String entityName) {
        bumpVersion(entityName);
        Iterator<String> views = viewEntityNames(entityName);
        while (views.hasNext()) {
            bumpVersion(views.next());
        }
    }

    private void bumpVersion(String entityName) {
        versions.computeIfAbsent(entityName, name -> new AtomicLong()).incrementAndGet();
    }

    /** The view entities containing an entity, as {@link ModelEntity#getViewConvertorsIterator()} gives them. */
    private Iterator<String> viewEntityNames(String entityName) {
        Delegator delegator = entityCache.getDelegator();
        ModelEntity model = delegator == null ? null : delegator.getModelReader().getModelEntityNoCheck(entityName);
        return model == null ? List.<String>of().iterator() : model.getViewConvertorsIterator();
    }

    /** Removes the condition caches (lists and objects) of an entity and of the view entities containing it. */
    private void removeConditionCaches(String entityName, Iterator<String> views) {
        entityListCache.remove(entityName);
        entityObjectCache.remove(entityName);
        while (views.hasNext()) {
            String viewEntityName = views.next();
            entityListCache.remove(viewEntityName);
            entityObjectCache.remove(viewEntityName);
        }
    }

    /**
     * Applies another server's change of one primary key: removes the cached value and, since the field values are not
     * sent, every condition cache of the entity and of the view entities containing it, as a local change does. The
     * invalidation versions move first, so a load in flight does not cache what it read.
     * @param pk the changed primary key
     */
    public void invalidateFromRemote(GenericPK pk) {
        String entityName = pk.getEntityName();
        invalidateVersion(entityName);
        entityCache.remove(pk);
        removeConditionCaches(entityName, pk.getModelEntity().getViewConvertorsIterator());
    }

    /**
     * Applies another server's condition-based or bulk change: removes everything cached for the entity and for the view
     * entities containing it, as the collapsed re-clear on the sending server does. The invalidation versions move first.
     * @param entityName the entity name
     */
    public void invalidateFromRemote(String entityName) {
        invalidateVersion(entityName);
        removeEntityAndViews(entityName);
    }

    /**
     * Caches a value loaded while the entity's invalidation version was {@code versionBeforeLoad}, unless it has moved since.
     * <p>The version guards against a change made, by this server after commit or by another server, while the load ran.
     * It cannot help a find running inside an older REPEATABLE READ (or stricter) snapshot: such a find can still read and
     * cache a row older than an invalidation that already arrived, when it records the version after that invalidation.
     * </p>
     * @param pk the primary key
     * @param entity the loaded value
     * @param versionBeforeLoad the invalidation version read before loading
     * @return whether the value is cached
     */
    public boolean putIfNotInvalidated(GenericPK pk, GenericValue entity, long versionBeforeLoad) {
        String entityName = pk.getEntityName();
        if (invalidationVersion(entityName) != versionBeforeLoad) {
            return false;
        }
        put(pk, entity);
        if (invalidationVersion(entityName) != versionBeforeLoad) {
            // an invalidation arrived between the check and the put
            entityCache.remove(pk);
            return false;
        }
        return true;
    }

    /**
     * Caches a list loaded while the entity's invalidation version was {@code versionBeforeLoad}, unless it has moved since.
     * @param entityName the entity name
     * @param condition the condition
     * @param orderBy the order by
     * @param entities the loaded list
     * @param versionBeforeLoad the invalidation version read before loading
     * @return whether the list is cached
     */
    public boolean putIfNotInvalidated(String entityName, EntityCondition condition, List<String> orderBy,
                                   List<GenericValue> entities, long versionBeforeLoad) {
        if (invalidationVersion(entityName) != versionBeforeLoad) {
            return false;
        }
        put(entityName, condition, orderBy, entities);
        if (invalidationVersion(entityName) != versionBeforeLoad) {
            entityListCache.remove(entityName, condition, orderBy);
            return false;
        }
        return true;
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
