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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Test;

public class CaffeineCacheStoreTest {

    private final List<Map.Entry<String, String>> expired = new ArrayList<>();

    private CacheStore<String, String> store(long maxSize, long expireTimeMillis) {
        CacheSettings settings = new CacheSettings("test.store", maxSize, expireTimeMillis, false, CachePolicy.INVALIDATE);
        return new CaffeineCacheProvider().create(settings, (String k, String v) -> expired.add(Map.entry(k, v)));
    }

    @Test
    public void getCountsHitsAndMisses() {
        CacheStore<String, String> store = store(0, 0);
        assertNull(store.get("a"));
        store.put("a", "1", 0);
        assertEquals("1", store.get("a"));
        assertEquals(new CacheStoreStats(1, 1, 0), store.stats());
    }

    @Test
    public void peekDoesNotCountStats() {
        CacheStore<String, String> store = store(0, 0);
        store.put("a", "1", 0);
        assertEquals("1", store.peek("a"));
        assertEquals(CacheStoreStats.ZERO, store.stats());
    }

    @Test
    public void putReturnsPreviousValue() {
        CacheStore<String, String> store = store(0, 0);
        assertNull(store.put("a", "1", 0));
        assertEquals("1", store.put("a", "2", 0));
        assertEquals("2", store.peek("a"));
    }

    @Test
    public void putIfAbsentKeepsExistingValue() {
        CacheStore<String, String> store = store(0, 0);
        assertNull(store.putIfAbsent("a", "1", 0));
        assertEquals("1", store.putIfAbsent("a", "2", 0));
        assertEquals("1", store.peek("a"));
    }

    @Test
    public void removeReturnsOldValueWithoutExpiryNotice() {
        CacheStore<String, String> store = store(0, 0);
        store.put("a", "1", 0);
        assertEquals("1", store.remove("a"));
        assertNull(store.remove("a"));
        assertTrue(expired.isEmpty());
    }

    @Test
    public void loaderValueIsCachedAndNullIsNot() {
        CacheStore<String, String> store = store(0, 0);
        AtomicInteger calls = new AtomicInteger();
        assertEquals("v", store.get("a", k -> {
            calls.incrementAndGet();
            return "v";
        }));
        assertEquals("v", store.get("a", k -> {
            calls.incrementAndGet();
            return "other";
        }));
        assertNull(store.get("b", k -> null));
        assertNull(store.peek("b"));
        assertEquals(1, calls.get());
    }

    @Test
    public void expiredEntryIsRemovedAndReported() throws InterruptedException {
        CacheStore<String, String> store = store(0, 50);
        store.put("a", "1", 50);
        // reads stop returning the entry at once; Caffeine's timer wheel reports it within about a second
        Thread.sleep(1500);
        assertTrue(store.keys().isEmpty());
        assertEquals(List.of(Map.entry("a", "1")), expired);
    }

    @Test
    public void zeroOrNegativeExpireMeansNever() {
        CacheStore<String, String> store = store(0, 0);
        store.put("a", "1", 0);
        store.put("b", "2", -1);
        assertEquals(-1, store.expiresInMillis("a"));
        assertEquals(-1, store.expiresInMillis("b"));
        assertEquals(-1, store.expiresInMillis("missing"));
    }

    @Test
    public void perEntryExpireIsReported() {
        CacheStore<String, String> store = store(0, 0);
        store.put("a", "1", 60_000);
        long remaining = store.expiresInMillis("a");
        assertTrue(remaining > 50_000 && remaining <= 60_000, "remaining " + remaining);
    }

    @Test
    public void sizeEvictionIsNotReportedAsExpiry() {
        CacheStore<String, String> store = store(2, 0);
        for (int i = 0; i < 5; i++) {
            store.put("k" + i, "v" + i, 0);
        }
        assertEquals(2, store.size());
        assertTrue(expired.isEmpty());
    }

    @Test
    public void settingsAreKept() {
        CacheStore<String, String> store = store(7, 9);
        assertEquals(new CacheSettings("test.store", 7, 9, false, CachePolicy.INVALIDATE), store.settings());
    }

    @Test
    public void providersDefaultToCaffeine() {
        assertEquals("caffeine", CacheProviders.select(List.of()).getName());
    }

    @Test
    public void providersPreferAPluginProvider() {
        CacheProvider plugin = new CacheProvider() {
            @Override
            public String getName() {
                return "plugin";
            }

            @Override
            public <K, V> CacheStore<K, V> create(CacheSettings settings, BiConsumer<K, V> onExpired) {
                return new CaffeineCacheProvider().create(settings, onExpired);
            }
        };
        assertSame(plugin, CacheProviders.select(List.of(plugin)));
    }

    @Test
    public void policyParsing() {
        assertEquals(CachePolicy.LOCAL, CachePolicy.fromAttribute("local"));
        assertEquals(CachePolicy.INVALIDATE, CachePolicy.fromAttribute("invalidate"));
        assertEquals(CachePolicy.NEAR, CachePolicy.fromAttribute("near"));
        assertNull(CachePolicy.fromAttribute(""));
        assertThrows(IllegalArgumentException.class, () -> CachePolicy.fromAttribute("shared"));
    }
}
