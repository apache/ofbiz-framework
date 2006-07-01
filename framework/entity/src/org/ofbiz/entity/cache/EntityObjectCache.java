/*
 * $Id: EntityObjectCache.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 *  Copyright (c) 2004 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.ofbiz.entity.cache;

import org.ofbiz.entity.condition.EntityCondition;

public class EntityObjectCache extends AbstractEntityConditionCache {

    public static final String module = EntityObjectCache.class.getName();

    public EntityObjectCache(String delegatorName) {
        super(delegatorName, "object-list");
    }

    public Object get(String entityName, EntityCondition condition, String name) {
        return super.get(entityName, condition, name);
    }

    public Object put(String entityName, EntityCondition condition, String name, Object value) {
        return super.put(entityName, getFrozenConditionKey(condition), name, value);
    }

    public Object remove(String entityName, EntityCondition condition, String name) {
        return super.remove(entityName, condition, name);
    }
}
