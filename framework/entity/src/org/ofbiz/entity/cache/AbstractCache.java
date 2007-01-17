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
