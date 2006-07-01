/*
 * $Id: AbstractCache.java 5462 2005-08-05 18:35:48Z jonesde $
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

import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.entity.GenericDelegator;

public abstract class AbstractCache {

    protected String delegatorName, id;

    protected AbstractCache(String delegatorName, String id) {
        this.delegatorName = delegatorName;
        this.id = id;
    }

    public GenericDelegator getDelegator() {
        return GenericDelegator.getGenericDelegator(delegatorName);
    }

    public void remove(String entityName) {
        UtilCache.clearCache(getCacheName(entityName));
    }

    public void clear() {
        UtilCache.clearCachesThatStartWith(getCacheNamePrefix());
    }

    public String getCacheNamePrefix() {
        return "entitycache." + id + "." + delegatorName + ".";
    }

    public String[] getCacheNamePrefixes() {
        return new String[] {
            "entitycache." + id + ".${delegator-name}.",
            "entitycache." + id + "." + delegatorName + "."
        };
    }

    public String getCacheName(String entityName) {
        return getCacheNamePrefix() + entityName;
    }

    public String[] getCacheNames(String entityName) {
        String[] prefixes = getCacheNamePrefixes();
        String[] names = new String[prefixes.length * 2];
        for (int i = 0; i < prefixes.length; i++) {
            names[i] = prefixes[i] + "${entity-name}";
        }
        for (int i = prefixes.length, j = 0; j < prefixes.length; i++, j++) {
            names[i] = prefixes[j] + entityName;
        }
        return names;
    }

    protected UtilCache getCache(String entityName) {
        synchronized (UtilCache.utilCacheTable) {
            return (UtilCache) UtilCache.utilCacheTable.get(getCacheName(entityName));
        }
    }

    protected UtilCache getOrCreateCache(String entityName) {
        synchronized (UtilCache.utilCacheTable) {
            String name = getCacheName(entityName);
            UtilCache cache = (UtilCache) UtilCache.utilCacheTable.get(name);
            if (cache == null) {
                cache = new UtilCache(name, 0, 0, true);
                String[] names = getCacheNames(entityName);
                cache.setPropertiesParams(names);
            }
            return cache;
        }
    }
}
