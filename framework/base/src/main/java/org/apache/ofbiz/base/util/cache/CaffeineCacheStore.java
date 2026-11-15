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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.Scheduler;
import com.github.benmanes.caffeine.cache.stats.CacheStats;

/**
 * A {@link CacheStore} on Caffeine. Each entry carries its own expiry, so {@code put} can override the cache's
 * expire time. Caffeine's maintenance runs on the threads that use the cache (the executor is the calling thread),
 * so removal notifications may arrive on any such thread, possibly while it holds locks of the owning cache; the
 * JDK system scheduler additionally triggers maintenance for timed expiry without waiting for the next access.
 */
final class CaffeineCacheStore<K, V> implements CacheStore<K, V> {

    private static final long NEVER = Long.MAX_VALUE;

    private record Entry<T>(T value, long expireNanos) { }

    private static final class EntryExpiry<A, T> implements Expiry<A, Entry<T>> {
        @Override
        public long expireAfterCreate(A key, Entry<T> entry, long currentTime) {
            return entry.expireNanos();
        }

        @Override
        public long expireAfterUpdate(A key, Entry<T> entry, long currentTime, long currentDuration) {
            return entry.expireNanos();
        }

        @Override
        public long expireAfterRead(A key, Entry<T> entry, long currentTime, long currentDuration) {
            return currentDuration;
        }
    }

    private final CacheSettings settings;
    private final Cache<K, Entry<V>> cache;

    CaffeineCacheStore(CacheSettings settings, BiConsumer<K, V> onExpired) {
        this.settings = settings;
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .executor(Runnable::run)
                .scheduler(Scheduler.systemScheduler())
                .recordStats();
        if (settings.maxSize() > 0) {
            builder.maximumSize(settings.maxSize());
        }
        if (settings.softValues()) {
            builder.softValues();
        }
        this.cache = builder
                .<K, Entry<V>>expireAfter(new EntryExpiry<K, V>())
                .<K, Entry<V>>evictionListener((K key, Entry<V> entry, RemovalCause cause) -> {
                    if (cause == RemovalCause.EXPIRED || cause == RemovalCause.COLLECTED) {
                        onExpired.accept(key, entry == null ? null : entry.value());
                    }
                })
                .build();
    }

    private static long toNanos(long expireTimeMillis) {
        return expireTimeMillis <= 0 ? NEVER : TimeUnit.MILLISECONDS.toNanos(expireTimeMillis);
    }

    private static <T> T valueOf(Entry<T> entry) {
        return entry == null ? null : entry.value();
    }

    @Override
    public V get(K key) {
        return valueOf(cache.getIfPresent(key));
    }

    @Override
    public V get(K key, Function<? super K, ? extends V> loader) {
        return valueOf(cache.get(key, k -> {
            V value = loader.apply(k);
            return value == null ? null : new Entry<>(value, toNanos(settings.expireTimeMillis()));
        }));
    }

    @Override
    public V peek(K key) {
        return valueOf(cache.policy().getIfPresentQuietly(key));
    }

    @Override
    public V put(K key, V value, long expireTimeMillis) {
        return valueOf(cache.asMap().put(key, new Entry<>(value, toNanos(expireTimeMillis))));
    }

    @Override
    public V putIfAbsent(K key, V value, long expireTimeMillis) {
        return valueOf(cache.asMap().putIfAbsent(key, new Entry<>(value, toNanos(expireTimeMillis))));
    }

    @Override
    public V remove(K key) {
        return valueOf(cache.asMap().remove(key));
    }

    @Override
    public Set<K> keys() {
        cache.cleanUp();
        return Collections.unmodifiableSet(new LinkedHashSet<>(cache.asMap().keySet()));
    }

    @Override
    public long size() {
        cache.cleanUp();
        return cache.estimatedSize();
    }

    @Override
    public long expiresInMillis(K key) {
        Entry<V> entry = cache.policy().getIfPresentQuietly(key);
        if (entry == null || entry.expireNanos() == NEVER) {
            return -1;
        }
        return cache.policy().expireVariably()
                .map(expiration -> expiration.getExpiresAfter(key, TimeUnit.MILLISECONDS).orElse(-1))
                .orElse(-1L);
    }

    @Override
    public CacheStoreStats stats() {
        CacheStats stats = cache.stats();
        return new CacheStoreStats(stats.hitCount(), stats.missCount(), stats.evictionCount());
    }

    @Override
    public CacheSettings settings() {
        return settings;
    }
}
