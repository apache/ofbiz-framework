/*
 * $Id: Cache.java 5462 2005-08-05 18:35:48Z jonesde $
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
