/*
 * $Id: EntityCache.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
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
