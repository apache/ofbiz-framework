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
 * One {@code <cache>} element of an {@code ofbiz-component.xml} file.
 * @param name the cache name, or the name prefix when {@code prefix} is true
 * @param prefix whether {@code name} is a prefix
 * @param maxSize the declared maximum size, or {@code null}
 * @param expireTimeMillis the declared expire time, or {@code null}
 * @param softValues the declared soft-values setting, or {@code null}
 * @param policy the declared policy, or {@code null}
 */
public record CacheDeclaration(String name, boolean prefix, Long maxSize, Long expireTimeMillis, Boolean softValues,
                               CachePolicy policy) {

    public CacheDeclaration {
        Objects.requireNonNull(name, "name");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("A cache declaration needs a name or a name prefix");
        }
        if (maxSize != null && maxSize < 0) {
            throw new IllegalArgumentException("The max-size of cache declaration [" + name + "] must not be negative: "
                    + maxSize);
        }
        if (expireTimeMillis != null && expireTimeMillis < 0) {
            throw new IllegalArgumentException("The expire-time of cache declaration [" + name
                    + "] must not be negative: " + expireTimeMillis);
        }
    }

    /** Returns whether this declaration applies to the cache named {@code cacheName}. */
    public boolean matches(String cacheName) {
        return prefix ? cacheName.startsWith(name) : cacheName.equals(name);
    }

    /** Returns {@code settings} with the attributes this declaration sets replaced. */
    public CacheSettings applyTo(CacheSettings settings) {
        CacheSettings result = settings;
        if (maxSize != null) {
            result = result.withMaxSize(maxSize);
        }
        if (expireTimeMillis != null) {
            result = result.withExpireTimeMillis(expireTimeMillis);
        }
        if (softValues != null) {
            result = result.withSoftValues(softValues);
        }
        if (policy != null) {
            result = result.withPolicy(policy);
        }
        return result;
    }

    int rank() {
        return prefix ? name.length() : Integer.MAX_VALUE;
    }
}
