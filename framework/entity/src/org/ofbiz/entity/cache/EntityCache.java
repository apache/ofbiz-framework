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

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.base.util.cache.CacheLine;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericPK;
import org.ofbiz.entity.condition.EntityCondition;

public class EntityCache extends AbstractCache {
    public static final String module = EntityCache.class.getName();

    public EntityCache(String delegatorName) {
        super(delegatorName, "entity");
    }

    public GenericEntity get(GenericPK pk) {
        UtilCache entityCache = getCache(pk.getEntityName());
        if (entityCache == null) return null;
        return (GenericEntity) entityCache.get(pk);
    }

    public GenericEntity put(GenericEntity entity) {
        if (entity == null) return null;
        return put(entity.getPrimaryKey(), entity);
    }

    public GenericEntity put(GenericPK pk, GenericEntity entity) {
        if (pk.getModelEntity().getNeverCache()) {
            Debug.logWarning("Tried to put a value of the " + pk.getEntityName() + " entity in the BY PRIMARY KEY cache but this entity has never-cache set to true, not caching.", module);
            return null;
        }

        if (entity == null) {
            entity = GenericEntity.NULL_ENTITY;
        } else {
            // before going into the cache, make this value immutable
            entity.setImmutable();
        }
        UtilCache entityCache = getOrCreateCache(pk.getEntityName());
        return (GenericEntity)entityCache.put(pk, entity);
    }

    public void remove(String entityName, EntityCondition condition) {
        UtilCache entityCache = getCache(entityName);
        if (entityCache == null) return;
        Iterator it = entityCache.getCacheLineValues().iterator();
        while (it.hasNext()) {
            CacheLine line = (CacheLine) it.next();
            if (entityCache.hasExpired(line)) continue;
            GenericEntity entity = (GenericEntity) line.getValue();
            if (entity == null) continue;
            if (condition.entityMatches(entity)) it.remove();
        }
    }

    public GenericEntity remove(GenericEntity entity) {
        return remove(entity.getPrimaryKey());
    }

    public GenericEntity remove(GenericPK pk) {
        UtilCache entityCache = getCache(pk.getEntityName());
        if (Debug.verboseOn()) Debug.logVerbose("Removing from EntityCache with PK [" + pk + "], will remove from this cache: " + (entityCache == null ? "[No cache found to remove from]" : entityCache.getName()), module);
        if (entityCache == null) return null;
        GenericEntity retVal = (GenericEntity) entityCache.remove(pk);
        if (Debug.verboseOn()) Debug.logVerbose("Removing from EntityCache with PK [" + pk + "], found this in the cache: " + retVal, module);
        return retVal;
    }
}
