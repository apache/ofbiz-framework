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

import org.apache.ofbiz.base.util.cache.UtilCache;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;

public abstract class AbstractCache<K, V> {

    protected final String delegatorName, id, cacheNamePrefix;

    protected AbstractCache(String delegatorName, String id) {
        this.delegatorName = delegatorName;
        this.id = id;
        this.cacheNamePrefix = "entitycache.".concat(id).concat(".").concat(delegatorName).concat(".");
    }

    public Delegator getDelegator() {
        return DelegatorFactory.getDelegator(this.delegatorName);
    }

    public void remove(String entityName) {
        UtilCache.clearCache(getCacheName(entityName));
    }

    public void clear() {
        UtilCache.clearCachesThatStartWith(getCacheNamePrefix());
    }

    public String getCacheNamePrefix() {
        return cacheNamePrefix;
    }

    public String[] getCacheNamePrefixes() {
        return new String[] {
            "entitycache." + id + ".${delegator-name}.",
            cacheNamePrefix
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

    protected UtilCache<K, V> getCache(String entityName) {
        return UtilCache.findCache(getCacheName(entityName));
    }

    protected UtilCache<K, V> getOrCreateCache(String entityName) {
        String name = getCacheName(entityName);
        return UtilCache.getOrCreateUtilCache(name, 0, 0, 0, true, getCacheNames(entityName));
    }
}
