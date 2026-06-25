/*******************************************************************************
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
package org.apache.ofbiz.base.secret;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

/**
 * Tests for {@link SecretProviderUtil}, the shared {@link SecretProvider} plugin helper:
 * the {@link SecretProviderUtil.Cache} TTL cache, {@link SecretProviderUtil#readTtlMs} and
 * {@link SecretProviderUtil#loadKeyAliases}. The latter two share the
 * {@code secrethelperstest.properties} fixture in {@code framework/base/src/test/resources}.
 */
public class SecretProviderHelpersTest {

    // -- SecretProviderUtil.Cache --

    @Test
    public void ttlCacheGetReturnsNullForMissingKey() {
        SecretProviderUtil.Cache<String, String> cache = new SecretProviderUtil.Cache<>();

        assertNull(cache.get("missing"));
    }

    @Test
    public void ttlCachePutThenGetReturnsStoredValue() {
        SecretProviderUtil.Cache<String, String> cache = new SecretProviderUtil.Cache<>();

        cache.put("key", "value", 3_600_000L);

        assertEquals("value", cache.get("key"));
    }

    @Test
    public void ttlCacheGetReturnsNullForExpiredEntry() {
        SecretProviderUtil.Cache<String, String> cache = new SecretProviderUtil.Cache<>();

        cache.put("key", "value", -1L); // expires immediately

        assertNull(cache.get("key"));
    }

    @Test
    public void ttlCachePutOverwritesExistingEntry() {
        SecretProviderUtil.Cache<String, String> cache = new SecretProviderUtil.Cache<>();

        cache.put("key", "first", 3_600_000L);
        cache.put("key", "second", 3_600_000L);

        assertEquals("second", cache.get("key"));
    }

    @Test
    public void ttlCacheClearRemovesAllEntries() {
        SecretProviderUtil.Cache<String, String> cache = new SecretProviderUtil.Cache<>();

        cache.put("key1", "value1", 3_600_000L);
        cache.put("key2", "value2", 3_600_000L);
        cache.clear();

        assertNull(cache.get("key1"));
        assertNull(cache.get("key2"));
    }

    @Test
    public void ttlCacheSupportsNonStringValueTypes() {
        SecretProviderUtil.Cache<String, Integer> cache = new SecretProviderUtil.Cache<>();

        cache.put("count", 42, 3_600_000L);

        assertEquals(Integer.valueOf(42), cache.get("count"));
    }

    // -- SecretProviderUtil.readTtlMs --

    @Test
    public void readTtlMsReturnsConfiguredValueInMilliseconds() {
        long ttlMs = SecretProviderUtil.readTtlMs("secrethelperstest", "test.cache.ttl.seconds", 3600,
                "SecretProviderHelpersTest");

        assertEquals(7_200_000L, ttlMs);
    }

    @Test
    public void readTtlMsFallsBackToDefaultForMissingProperty() {
        long ttlMs = SecretProviderUtil.readTtlMs("secrethelperstest", "test.no.such.property", 3600,
                "SecretProviderHelpersTest");

        assertEquals(3_600_000L, ttlMs);
    }

    @Test
    public void readTtlMsFallsBackToDefaultForInvalidValue() {
        long ttlMs = SecretProviderUtil.readTtlMs("secrethelperstest", "test.cache.ttl.invalid", 3600,
                "SecretProviderHelpersTest");

        assertEquals(3_600_000L, ttlMs);
    }

    @Test
    public void readTtlMsFallsBackToDefaultForMissingResource() {
        long ttlMs = SecretProviderUtil.readTtlMs("no-such-resource-xyz", "any.key", 1800, "SecretProviderHelpersTest");

        assertEquals(1_800_000L, ttlMs);
    }

    // -- SecretProviderUtil.loadKeyAliases --

    @Test
    public void loadParsesKeyAliasEntries() {
        Map<String, String> aliases = SecretProviderUtil.loadKeyAliases("secrethelperstest");

        assertEquals("prod/ofbiz/mysql_db_password", aliases.get("jdbc-password.mysql-ofbiz"));
        assertEquals("prod/ofbiz/anet_txn_key", aliases.get("payment.authorizedotnet.transactionKey"));
    }

    @Test
    public void loadIgnoresNonAliasEntries() {
        Map<String, String> aliases = SecretProviderUtil.loadKeyAliases("secrethelperstest");

        assertTrue(aliases.keySet().stream().noneMatch(k -> k.contains("not.a.key.alias.entry")));
        assertEquals(2, aliases.size());
    }

    @Test
    public void loadReturnsEmptyMapForResourceWithNoAliasEntries() {
        // "general" exists on the classpath (framework/common/config/general.properties)
        // but has no key.alias.* entries.
        Map<String, String> aliases = SecretProviderUtil.loadKeyAliases("general");

        assertTrue(aliases.isEmpty());
    }

    @Test
    public void loadReturnsEmptyMapForMissingResource() {
        Map<String, String> aliases = SecretProviderUtil.loadKeyAliases("no-such-resource-xyz");

        assertTrue(aliases.isEmpty());
    }
}
