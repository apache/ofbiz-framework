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
import org.apache.ofbiz.base.util.cache.UtilCache
import org.apache.ofbiz.base.util.UtilFormatOut
import org.apache.ofbiz.base.util.UtilMisc
import org.apache.ofbiz.security.Security

context.hasUtilCacheEdit = security.hasEntityPermission("UTIL_CACHE", "_EDIT", session)

cacheList = []
totalCacheMemory = 0.0
names = new TreeSet(UtilCache.getUtilCacheTableKeySet())
names.each { cacheName ->
        utilCache = UtilCache.findCache(cacheName)
        cache = [:]

        cache.cacheName = utilCache.getName()
        cache.cacheSize = UtilFormatOut.formatQuantity(utilCache.size())
        cache.hitCount = UtilFormatOut.formatQuantity(utilCache.getHitCount())
        cache.missCountTot = UtilFormatOut.formatQuantity(utilCache.getMissCountTotal())
        cache.missCountNotFound = UtilFormatOut.formatQuantity(utilCache.getMissCountNotFound())
        cache.missCountExpired = UtilFormatOut.formatQuantity(utilCache.getMissCountExpired())
        cache.missCountSoftRef = UtilFormatOut.formatQuantity(utilCache.getMissCountSoftRef())
        cache.removeHitCount = UtilFormatOut.formatQuantity(utilCache.getRemoveHitCount())
        cache.removeMissCount = UtilFormatOut.formatQuantity(utilCache.getRemoveMissCount())
        cache.maxInMemory = UtilFormatOut.formatQuantity(utilCache.getMaxInMemory())
        cache.expireTime = UtilFormatOut.formatQuantity(utilCache.getExpireTime())
        cache.useSoftReference = utilCache.getUseSoftReference().toString()
        cache.cacheMemory = utilCache.getSizeInBytes()
        totalCacheMemory += cache.cacheMemory
        cacheList.add(cache)
}
sortField = parameters.sortField
if (sortField) { 
    context.cacheList = UtilMisc.sortMaps(cacheList, UtilMisc.toList(sortField))
} else {
    context.cacheList = cacheList
}
context.totalCacheMemory = totalCacheMemory

rt = Runtime.getRuntime()
memoryInfo = [:]
memoryInfo.memory = UtilFormatOut.formatQuantity(rt.totalMemory())
memoryInfo.freeMemory = UtilFormatOut.formatQuantity(rt.freeMemory())
memoryInfo.usedMemory = UtilFormatOut.formatQuantity((rt.totalMemory() - rt.freeMemory()))
memoryInfo.maxMemory = UtilFormatOut.formatQuantity(rt.maxMemory())
memoryInfo.totalCacheMemory = totalCacheMemory
context.memoryInfo = memoryInfo
