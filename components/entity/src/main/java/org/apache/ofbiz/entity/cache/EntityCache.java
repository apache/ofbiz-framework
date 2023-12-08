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

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.cache.UtilCache;
import org.apache.ofbiz.entity.GenericPK;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.model.ModelEntity;

public class EntityCache extends AbstractCache<GenericPK, GenericValue> {
    private static final String MODULE = EntityCache.class.getName();

    public EntityCache(String delegatorName) {
        super(delegatorName, "entity");
    }

    /**
     * Get generic value.
     * @param pk the pk
     * @return the generic value
     */
    public GenericValue get(GenericPK pk) {
        UtilCache<GenericPK, GenericValue> entityCache = getCache(pk.getEntityName());
        if (entityCache == null) {
            return null;
        }
        return entityCache.get(pk);
    }

    /**
     * Put generic value.
     * @param entity the entity
     * @return the generic value
     */
    public GenericValue put(GenericValue entity) {
        if (entity == null) {
            return null;
        }
        return put(entity.getPrimaryKey(), entity);
    }

    /**
     * Put generic value.
     * @param pk the pk
     * @param entity the entity
     * @return the generic value
     */
    public GenericValue put(GenericPK pk, GenericValue entity) {
        if (pk.getModelEntity().getNeverCache()) {
            Debug.logWarning("Tried to put a value of the " + pk.getEntityName()
                    + " entity in the BY PRIMARY KEY cache but this entity has never-cache set to true, not caching.", MODULE);
            return null;
        }

        if (entity == null) {
            entity = GenericValue.NULL_VALUE;
        } else {
            // before going into the cache, make this value immutable
            entity.setImmutable();
        }
        UtilCache<GenericPK, GenericValue> entityCache = getOrCreateCache(pk.getEntityName());
        return entityCache.put(pk, entity);
    }

    /**
     * Remove.
     * @param entityName the entity name
     * @param condition the condition
     */
    public void remove(String entityName, EntityCondition condition) {
        UtilCache<GenericPK, GenericValue> entityCache = getCache(entityName);
        if (entityCache == null) {
            return;
        }
        for (GenericPK pk: entityCache.getCacheLineKeys()) {
            GenericValue entity = entityCache.get(pk);
            if (entity == null) {
                continue;
            }
            if (condition.entityMatches(entity)) {
                entityCache.remove(pk);
            }
        }
    }

    /**
     * Remove generic value.
     * @param entity the entity
     * @return the generic value
     */
    public GenericValue remove(GenericValue entity) {
        return remove(entity.getPrimaryKey());
    }

    /**
     * Remove generic value.
     * @param pk the pk
     * @return the generic value
     */
    public GenericValue remove(GenericPK pk) {
        UtilCache<GenericPK, GenericValue> entityCache = getCache(pk.getEntityName());
        if (Debug.verboseOn()) {
            Debug.logVerbose("Removing from EntityCache with PK [" + pk + "], will remove from this cache: "
                    + (entityCache == null ? "[No cache found to remove from]" : entityCache.getName()), MODULE);
        }
        if (entityCache == null) {
            return null;
        }
        GenericValue retVal = entityCache.remove(pk);
        ModelEntity model = pk.getModelEntity();
        if (model != null) {
            Iterator<String> it = model.getViewConvertorsIterator();
            while (it.hasNext()) {
                String targetEntityName = it.next();
                UtilCache.clearCache(getCacheName(targetEntityName));
            }
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose("Removing from EntityCache with PK [" + pk + "], found this in the cache: " + retVal, MODULE);
        }
        return retVal;
    }
}
