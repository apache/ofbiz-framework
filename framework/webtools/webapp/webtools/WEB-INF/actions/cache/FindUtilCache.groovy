/*
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
 */
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.security.Security;

context.hasUtilCacheEdit = security.hasEntityPermission("UTIL_CACHE", "_EDIT", session);

rt = Runtime.getRuntime();
context.memory = UtilFormatOut.formatQuantity(rt.totalMemory());
context.freeMemory = UtilFormatOut.formatQuantity(rt.freeMemory());
context.usedMemory = UtilFormatOut.formatQuantity((rt.totalMemory() - rt.freeMemory()));
context.maxMemory = UtilFormatOut.formatQuantity(rt.maxMemory());

cacheList = [];
names = new TreeSet(UtilCache.getUtilCacheTableKeySet());
names.each { cacheName ->
        utilCache = UtilCache.findCache(cacheName);
        cache = [:];

        cache.cacheName = utilCache.getName();
        cache.cacheSize = UtilFormatOut.formatQuantity(utilCache.size());
        cache.hitCount = UtilFormatOut.formatQuantity(utilCache.getHitCount());
        cache.missCountTot = UtilFormatOut.formatQuantity(utilCache.getMissCountTotal());
        cache.missCountNotFound = UtilFormatOut.formatQuantity(utilCache.getMissCountNotFound());
        cache.missCountExpired = UtilFormatOut.formatQuantity(utilCache.getMissCountExpired());
        cache.missCountSoftRef = UtilFormatOut.formatQuantity(utilCache.getMissCountSoftRef());
        cache.removeHitCount = UtilFormatOut.formatQuantity(utilCache.getRemoveHitCount());
        cache.removeMissCount = UtilFormatOut.formatQuantity(utilCache.getRemoveMissCount());
        cache.maxSize = UtilFormatOut.formatQuantity(utilCache.getMaxSize());
        cache.expireTime = UtilFormatOut.formatQuantity(utilCache.getExpireTime());
        cache.useSoftReference = utilCache.getUseSoftReference().toString();
        cache.useFileSystemStore = utilCache.getUseFileSystemStore().toString();

        cacheList.add(cache);
}
context.cacheList = cacheList;
