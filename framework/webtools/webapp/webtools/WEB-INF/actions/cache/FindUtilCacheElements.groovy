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
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.security.Security;

context.hasUtilCacheEdit = security.hasEntityPermission("UTIL_CACHE", "_EDIT", session);

cacheName = parameters.UTIL_CACHE_NAME;
context.cacheName = cacheName;
context.now = (new Date()).toString();

totalSize = 0;

cacheElementsList = [];
if (cacheName) {
    utilCache = UtilCache.findCache(cacheName);
    if (utilCache) {
        cacheElementsList = utilCache.getLineInfos()
        cacheElementsList.each {
            if (it.expireTimeMillis != null) {
                it.expireTimeMillis = (it.expireTimeMillis / 1000) .toString();
            }
            totalSize += it.lineSize;
            it.lineSize = UtilFormatOut.formatQuantity(it.lineSize);
        }
    }
}
context.totalSize = UtilFormatOut.formatQuantity(totalSize);
sortField = parameters.sortField;
if (sortField) { 
    context.cacheElementsList = UtilMisc.sortMaps(cacheElementsList, UtilMisc.toList(sortField));
} else {
    context.cacheElementsList = cacheElementsList;
}
