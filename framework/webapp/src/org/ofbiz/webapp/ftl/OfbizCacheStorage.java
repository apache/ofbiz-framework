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
package org.ofbiz.webapp.ftl;

import freemarker.cache.CacheStorage;

import org.ofbiz.base.util.cache.UtilCache;

/**
 * A custom cache wrapper for caching FreeMarker templates
 */
public class OfbizCacheStorage implements CacheStorage {
    //can't have global cache because names/keys are relative to the webapp
    protected final UtilCache localCache;
    
    public OfbizCacheStorage(String id) {
        this.localCache = new UtilCache("webapp.FreeMarkerCache." + id, 0, 0, false);
    }
    
    public Object get(Object key) {
        return localCache.get(key);
    }
    
    public void put(Object key, Object value) {
        localCache.put(key, value);
    }
    
    public void remove(Object key) {
        localCache.remove(key);
    }
    
    public void clear() {
        localCache.clear();
    }
}
