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

import java.util.Objects;

/**
 * The settings of one cache.
 * @param name the cache name used to match {@code <cache>} declarations, without the index suffix
 * @param maxSize the maximum number of entries, 0 for no limit
 * @param expireTimeMillis how long an entry lives after it is written, 0 for no expiry
 * @param softValues whether values are held through soft references
 * @param policy how the cache behaves when several servers share one database
 */
public record CacheSettings(String name, long maxSize, long expireTimeMillis, boolean softValues, CachePolicy policy) {

    public CacheSettings {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(policy, "policy");
        if (maxSize < 0) {
            throw new IllegalArgumentException("maxSize must not be negative: " + maxSize);
        }
        if (expireTimeMillis < 0) {
            throw new IllegalArgumentException("expireTimeMillis must not be negative: " + expireTimeMillis);
        }
    }

    public CacheSettings withMaxSize(long newMaxSize) {
        return new CacheSettings(name, newMaxSize, expireTimeMillis, softValues, policy);
    }

    public CacheSettings withExpireTimeMillis(long newExpireTimeMillis) {
        return new CacheSettings(name, maxSize, newExpireTimeMillis, softValues, policy);
    }

    public CacheSettings withSoftValues(boolean newSoftValues) {
        return new CacheSettings(name, maxSize, expireTimeMillis, newSoftValues, policy);
    }

    public CacheSettings withPolicy(CachePolicy newPolicy) {
        return new CacheSettings(name, maxSize, expireTimeMillis, softValues, newPolicy);
    }
}
