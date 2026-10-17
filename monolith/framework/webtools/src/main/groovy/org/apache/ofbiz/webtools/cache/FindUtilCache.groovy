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
package org.apache.ofbiz.webtools.cache

import org.apache.ofbiz.base.util.UtilFormatOut
import org.apache.ofbiz.base.util.UtilMisc
import org.apache.ofbiz.base.util.cache.UtilCache

context.hasUtilCacheEdit = security.hasEntityPermission('UTIL_CACHE', '_EDIT', session)

cacheList = []
totalCacheMemory = 0.0
names = new TreeSet(UtilCache.getUtilCacheTableKeySet())
names.each { cacheName ->
    utilCache = UtilCache.findCache(cacheName)
    cache = [
            cacheName: utilCache.getName(),
            cacheSize: UtilFormatOut.formatQuantity(utilCache.size()),
            hitCount: UtilFormatOut.formatQuantity(utilCache.getHitCount()),
            missCountTot: UtilFormatOut.formatQuantity(utilCache.getMissCountTotal()),
            missCountNotFound: UtilFormatOut.formatQuantity(utilCache.getMissCountNotFound()),
            missCountExpired: UtilFormatOut.formatQuantity(utilCache.getMissCountExpired()),
            missCountSoftRef: UtilFormatOut.formatQuantity(utilCache.getMissCountSoftRef()),
            removeHitCount: UtilFormatOut.formatQuantity(utilCache.getRemoveHitCount()),
            removeMissCount: UtilFormatOut.formatQuantity(utilCache.getRemoveMissCount()),
            maxInMemory: UtilFormatOut.formatQuantity(utilCache.getMaxInMemory()),
            expireTime: UtilFormatOut.formatQuantity(utilCache.getExpireTime()),
            useSoftReference: utilCache.getUseSoftReference().toString(),
            cacheMemory: utilCache.getSizeInBytes()
    ]
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
memoryInfo = [
        memory: UtilFormatOut.formatQuantity(rt.totalMemory()),
        freeMemory: UtilFormatOut.formatQuantity(rt.freeMemory()),
        usedMemory: UtilFormatOut.formatQuantity((rt.totalMemory() - rt.freeMemory())),
        maxMemory: UtilFormatOut.formatQuantity(rt.maxMemory()),
        totalCacheMemory: totalCacheMemory
]
context.memoryInfo = memoryInfo
