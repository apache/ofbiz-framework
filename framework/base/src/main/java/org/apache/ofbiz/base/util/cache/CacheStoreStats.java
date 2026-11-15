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
 *******************************************************************************/
package org.apache.ofbiz.base.util.cache;

/**
 * Counts kept by a {@link CacheStore}.
 * @param hitCount lookups that found an entry
 * @param missCount lookups that found no entry
 * @param evictionCount entries removed by the store because of size, expiry or garbage collection
 */
public record CacheStoreStats(long hitCount, long missCount, long evictionCount) {

    public static final CacheStoreStats ZERO = new CacheStoreStats(0, 0, 0);

    public CacheStoreStats plus(CacheStoreStats other) {
        return new CacheStoreStats(hitCount + other.hitCount, missCount + other.missCount, evictionCount + other.evictionCount);
    }

    public CacheStoreStats minus(CacheStoreStats other) {
        return new CacheStoreStats(hitCount - other.hitCount, missCount - other.missCount, evictionCount - other.evictionCount);
    }
}
