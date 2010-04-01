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
import org.ofbiz.base.util.cache.CacheLine;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.security.Security;

cacheName = parameters.UTIL_CACHE_NAME;
context.cacheName = cacheName;

if (cacheName) {
    utilCache = UtilCache.findCache(cacheName);
    if (utilCache) {
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
        cache.maxInMemory = UtilFormatOut.formatQuantity(utilCache.getMaxInMemory());
        cache.expireTime = UtilFormatOut.formatQuantity(utilCache.getExpireTime());
        cache.useSoftReference = utilCache.getUseSoftReference().toString();
        cache.useFileSystemStore = utilCache.getUseFileSystemStore().toString();

        exp = utilCache.getExpireTime();
        hrs = Math.floor(exp / (60 * 60 * 1000));
        exp = exp % (60 * 60 * 1000);
        mins = Math.floor(exp / (60 * 1000));
        exp = exp % (60 * 1000);
        secs = exp / 1000;
        cache.hrs = hrs;
        cache.mins = mins;
        cache.secs = UtilFormatOut.formatPrice(secs);

        context.cache = cache;
    }
}
