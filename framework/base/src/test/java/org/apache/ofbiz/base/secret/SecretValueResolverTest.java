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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;
import org.junit.Before;

/**
 * Tests {@link SecretValueResolver} using the default {@link FileBasedSecretProvider}
 * (resolved via {@link SecretProviderFactory} since no plugin-provided {@link SecretProvider}
 * is on the test classpath) and the {@code jdbc-password.h2-ofbiz} entry already present in
 * {@code framework/base/config/passwords.properties}.
 */
public class SecretValueResolverTest {

    private static final String M = SecretValueResolver.MARKER_NAME;

    @Before
    public void clearCache() {
        SecretValueResolver.invalidateAll();
    }

    @Test
    public void nonSecretValuesAreReturnedUnchanged() {
        assertEquals("plainvalue", SecretValueResolver.resolve("plainvalue"));
        assertEquals("ENC(AbCd123)", SecretValueResolver.resolve("ENC(AbCd123)"));
        assertEquals("", SecretValueResolver.resolve(""));
    }

    @Test
    public void nullIsReturnedUnchanged() {
        assertNull(SecretValueResolver.resolve(null));
    }

    @Test
    public void resolvesKnownKeyFromPasswordsProperties() {
        assertEquals("ofbiz", SecretValueResolver.resolve(M + "(jdbc-password.h2-ofbiz)"));
        // Cached lookup should return the same value.
        assertEquals("ofbiz", SecretValueResolver.resolve(M + "(jdbc-password.h2-ofbiz)"));
    }

    @Test
    public void unresolvableKeyReturnsEmptyString() {
        assertEquals("", SecretValueResolver.resolve(M + "(jdbc-password.does-not-exist)"));
    }

    @Test
    public void resolveKeyResolvesDirectly() {
        assertEquals("ofbiz", SecretValueResolver.resolveKey("jdbc-password.h2-ofbiz"));
    }

    @Test
    public void resolveKeyWithNullReturnsNull() {
        assertNull(SecretValueResolver.resolveKey(null));
    }

    @Test
    public void resolveKeyWithEmptyReturnsEmpty() {
        assertEquals("", SecretValueResolver.resolveKey(""));
    }

    @Test
    public void invalidateForcesFreshFetch() {
        // Populate cache, then invalidate and verify re-fetch still returns the same value.
        assertEquals("ofbiz", SecretValueResolver.resolveKey("jdbc-password.h2-ofbiz"));
        SecretValueResolver.invalidate("jdbc-password.h2-ofbiz");
        assertEquals("ofbiz", SecretValueResolver.resolveKey("jdbc-password.h2-ofbiz"));
    }

    @Test
    public void invalidateNullIsNoOp() {
        SecretValueResolver.invalidate(null); // must not throw
    }

    @Test
    public void invalidateAllClearsAllEntries() {
        SecretValueResolver.resolveKey("jdbc-password.h2-ofbiz");
        SecretValueResolver.invalidateAll(); // must not throw
        // After invalidation, the key can still be resolved (re-fetched from provider).
        assertEquals("ofbiz", SecretValueResolver.resolveKey("jdbc-password.h2-ofbiz"));
    }

    @Test
    public void maskSensitiveRedactsEncBlob() {
        assertEquals("ENC(***)", SecretValueResolver.maskSensitive("ENC(abc123==)"));
    }

    @Test
    public void maskSensitiveRedactsMultipleEncBlobs() {
        assertEquals("a=ENC(***) b=ENC(***)",
                SecretValueResolver.maskSensitive("a=ENC(first) b=ENC(second)"));
    }

    @Test
    public void maskSensitiveLeavesPlainValueUnchanged() {
        assertEquals("plain-value", SecretValueResolver.maskSensitive("plain-value"));
    }

    @Test
    public void maskSensitiveWithNullReturnsNull() {
        assertNull(SecretValueResolver.maskSensitive(null));
    }

    @Test
    public void resetUsageStatsClearsAllCounters() {
        // Populate counters by doing some lookups.
        SecretValueResolver.resolveKey("jdbc-password.h2-ofbiz"); // miss then hit
        SecretValueResolver.resolveKey("jdbc-password.h2-ofbiz"); // hit
        SecretValueResolver.resolveKey("jdbc-password.does-not-exist"); // miss

        // Reset and verify summary is zeroed.
        SecretValueResolver.resetUsageStats();
        Map<String, Long> summary = SecretValueResolver.getUsageSummary();
        assertEquals(Long.valueOf(0), summary.get("totalHits"));
        assertEquals(Long.valueOf(0), summary.get("totalMisses"));
        assertEquals(Long.valueOf(0), summary.get("totalLookups"));
    }

    @Test
    public void resetUsageStatsAlsoClearsPerKeyReport() {
        SecretValueResolver.resolveKey("jdbc-password.h2-ofbiz");
        SecretValueResolver.resetUsageStats();
        Map<String, Map<String, Long>> report = SecretValueResolver.getUsageReport();
        assertTrue("Per-key report should be empty after reset", report.isEmpty());
    }

    @Test
    public void getUsageSummaryReturnsRequiredKeys() {
        Map<String, Long> summary = SecretValueResolver.getUsageSummary();
        assertNotNull(summary.get("totalHits"));
        assertNotNull(summary.get("totalMisses"));
        assertNotNull(summary.get("totalLookups"));
        assertNotNull(summary.get("cachedKeys"));
    }

    @Test
    public void getUsageSummaryTotalIsConsistent() {
        SecretValueResolver.resetUsageStats();
        SecretValueResolver.resolveKey("jdbc-password.h2-ofbiz");
        Map<String, Long> summary = SecretValueResolver.getUsageSummary();
        assertEquals(summary.get("totalHits") + summary.get("totalMisses"), (long) summary.get("totalLookups"));
    }

    @Test
    public void getUsageReportTracksKnownKey() {
        SecretValueResolver.resetUsageStats();
        SecretValueResolver.resolveKey("jdbc-password.h2-ofbiz");
        SecretValueResolver.resolveKey("jdbc-password.h2-ofbiz");
        Map<String, Map<String, Long>> report = SecretValueResolver.getUsageReport();
        assertTrue("Known key should appear in per-key report", report.containsKey("jdbc-password.h2-ofbiz"));
        Map<String, Long> stats = report.get("jdbc-password.h2-ofbiz");
        long total = stats.get("hits") + stats.get("misses");
        assertEquals(total, (long) stats.get("total"));
    }
}
