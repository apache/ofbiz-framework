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

import java.util.Set;
import java.util.function.Function;

/**
 * The storage behind one {@link UtilCache}, created by a {@link CacheProvider}.
 * Keys and values are never {@code null}; {@link UtilCache} maps a {@code null} key to a marker object.
 * A store has no notion of other servers.
 * @param <K> the key type
 * @param <V> the value type
 */
public interface CacheStore<K, V> {

    /** Returns the value for {@code key}, or {@code null}, and counts a hit or a miss. */
    V get(K key);

    /**
     * Returns the value for {@code key}, loading and storing it with {@code loader} on a miss.
     * A {@code null} result from the loader is returned and not stored.
     */
    V get(K key, Function<? super K, ? extends V> loader);

    /** Returns the value for {@code key}, or {@code null}, without counting a hit or a miss. */
    V peek(K key);

    /**
     * Stores a value.
     * @param expireTimeMillis how long the entry lives, 0 or less for no expiry
     * @return the previous value, or {@code null}
     */
    V put(K key, V value, long expireTimeMillis);

    /**
     * Stores a value unless the key already has one.
     * @param expireTimeMillis how long the entry lives, 0 or less for no expiry
     * @return the existing value, or {@code null} when {@code value} was stored
     */
    V putIfAbsent(K key, V value, long expireTimeMillis);

    /** Removes an entry and returns its value, or {@code null}. Expiry listeners are not told. */
    V remove(K key);

    /** Returns a snapshot of the live keys, after removing expired entries. */
    Set<K> keys();

    /** Returns the number of live entries, after removing expired entries. */
    long size();

    /** Returns the milliseconds left before {@code key} expires, or -1 when it is absent or never expires. */
    long expiresInMillis(K key);

    /** Returns the counts since the store was created. */
    CacheStoreStats stats();

    /** Returns the settings the store was created with. */
    CacheSettings settings();
}
