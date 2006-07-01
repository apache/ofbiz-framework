/*
 * $Id: EntityListCache.java 5462 2005-08-05 18:35:48Z jonesde $
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
import java.util.List;
import java.util.Map;

import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.util.EntityUtil;

public class EntityListCache extends AbstractEntityConditionCache {

    public static final String module = EntityListCache.class.getName();

    public EntityListCache(String delegatorName) {
        super(delegatorName, "entity-list");
    }

    public List get(String entityName, EntityCondition condition) {
        return this.get(entityName, condition, null);
    }

    public List get(String entityName, EntityCondition condition, List orderBy) {
        Map conditionCache = getConditionCache(entityName, condition);
        if (conditionCache == null) return null;
        Object orderByKey = getOrderByKey(orderBy);
        List valueList = (List) conditionCache.get(orderByKey);
        if (valueList == null) {
            // the valueList was not found for the given ordering, so grab the first one and order it in memory
            Iterator it = conditionCache.values().iterator();
            if (it.hasNext()) valueList = (List) it.next();
            
            synchronized (conditionCache) {
                if (valueList != null) {
                    valueList = EntityUtil.orderBy(valueList, orderBy);
                    conditionCache.put(orderByKey, valueList);
                }
            }
        }
        return valueList;
    }

    public void put(String entityName, EntityCondition condition, List entities) {
        this.put(entityName, condition, null, entities);
    }

    public List put(String entityName, EntityCondition condition, List orderBy, List entities) {
        return (List) super.put(entityName, getFrozenConditionKey(condition), getOrderByKey(orderBy), entities);
    }

    public List remove(String entityName, EntityCondition condition, List orderBy) {
        return (List) super.remove(entityName, condition, getOrderByKey(orderBy));
    }

    public static final Object getOrderByKey(List orderBy) {
        return orderBy != null ? (Object) orderBy : "{null}";
    }
}
