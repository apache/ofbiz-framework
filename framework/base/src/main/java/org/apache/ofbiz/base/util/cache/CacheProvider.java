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

import java.util.function.BiConsumer;

/**
 * Creates the {@link CacheStore}s behind {@link UtilCache}. OFBiz uses the Caffeine provider unless a jar on the
 * classpath declares another one in {@code META-INF/services/org.apache.ofbiz.base.util.cache.CacheProvider}.
 */
public interface CacheProvider {

    /** Returns a short name for logs, such as {@code caffeine}. */
    String getName();

    /**
     * Creates a store.
     * @param settings the settings of the cache
     * @param onExpired called with the key and value of each entry removed because it expired or its soft
     *        reference was cleared (the value is {@code null} in that case); not called for size evictions or
     *        explicit removals
     */
    <K, V> CacheStore<K, V> create(CacheSettings settings, BiConsumer<K, V> onExpired);
}
