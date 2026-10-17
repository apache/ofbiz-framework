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
package org.apache.ofbiz.widget.cache;

import org.apache.ofbiz.base.util.cache.UtilCache;

public abstract class AbstractCache {

    private String id;

    protected AbstractCache(String id) {
        this.id = id;
    }

    /**
     * Remove.
     * @param widgetName the widget name
     */
    public void remove(String widgetName) {
        UtilCache.clearCache(getCacheName(widgetName));
    }

    /**
     * Clear.
     */
    public void clear() {
        UtilCache.clearCachesThatStartWith(getCacheNamePrefix());
    }

    /**
     * Gets cache name prefix.
     * @return the cache name prefix
     */
    public String getCacheNamePrefix() {
        return "widgetcache." + id + ".";
    }

    /**
     * Gets cache name.
     * @param widgetName the widget name
     * @return the cache name
     */
    public String getCacheName(String widgetName) {
        return getCacheNamePrefix() + widgetName;
    }

    /**
     * Gets cache.
     * @param <K> the type parameter
     * @param <V> the type parameter
     * @param widgetName the widget name
     * @return the cache
     */
    protected <K, V> UtilCache<K, V> getCache(String widgetName) {
        return UtilCache.findCache(getCacheName(widgetName));
    }

    /**
     * Gets or create cache.
     * @param widgetName the widget name
     * @return the or create cache
     */
    protected UtilCache<WidgetContextCacheKey, GenericWidgetOutput> getOrCreateCache(String widgetName) {
        String name = getCacheName(widgetName);
        return UtilCache.getOrCreateUtilCache(name, 0, 0, 0, true, name);
    }
}
