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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ofbiz.base.lang.ThreadSafe;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilProperties;

/**
 * Resolves configuration values of the form {@code LOOKUP(key)} to the
 * corresponding secret value via {@link SecretProviderFactory}.
 *
 * <p>This allows any property read through {@code UtilProperties} or
 * {@code EntityUtilProperties} (e.g. payment, SMS or shipment gateway
 * credentials stored in a {@code .properties} file or as a
 * {@code SystemProperty}) to be backed by a remote secret manager, using the
 * same {@link SecretProvider} SPI and providers already used for
 * {@code jdbc-password-lookup}.</p>
 *
 * <p>Values that do not match {@code LOOKUP(key)} are returned unchanged.
 * The {@code jdbc-password-lookup} mechanism in {@code entityengine.xml}
 * ({@code EntityConfig.getJdbcPassword()}) does not go through this class and
 * is unaffected.</p>
 *
 * <p>The marker name ({@code LOOKUP} by default) can be changed via the
 * {@code secret.value.marker} property in {@code security.properties}, e.g. to
 * {@code SECRET} or {@code ENCRYPT}, in case it collides with existing property
 * values.</p>
 *
 * <p>Resolved values are cached for {@code secret.cache.ttl.seconds}
 * (default 300) to avoid calling the remote secret manager on every property
 * lookup.</p>
 */
@ThreadSafe
public final class SecretValueResolver {

    private static final String MODULE = SecretValueResolver.class.getName();

    // Read the marker name and cache TTL directly from the Properties object, bypassing
    // UtilProperties.getPropertyValue(), which calls back into resolve() and would otherwise
    // deadlock/NPE on these fields during static initialization.
    private static final Properties SECURITY_PROPERTIES = UtilProperties.getProperties("security");

    public static final String MARKER_NAME = SECURITY_PROPERTIES != null
            ? SECURITY_PROPERTIES.getProperty("secret.value.marker", "LOOKUP").trim()
            : "LOOKUP";

    private static final Pattern SECRET_PATTERN =
            Pattern.compile("^" + Pattern.quote(MARKER_NAME) + "\\((.+)\\)$");

    // Cap at 86400 seconds (24 h) to prevent silent long-overflow when an operator enters
    // an unreasonably large value in security.properties.
    private static final long MAX_TTL_SECONDS = 86400L;
    private static final long CACHE_TTL_MILLIS = parseTtlMillis();

    private static long parseTtlMillis() {
        long seconds = 300L;
        if (SECURITY_PROPERTIES != null) {
            try {
                seconds = Long.parseLong(
                        SECURITY_PROPERTIES.getProperty("secret.cache.ttl.seconds", "300").trim());
            } catch (NumberFormatException ignored) {
                seconds = 300L;
            }
        }
        return Math.min(Math.max(seconds, 1L), MAX_TTL_SECONDS) * 1000L;
    }

    // Phase 2 audit flags — read once at class load; both off by default (opt-in only).
    // Enabling these adds a non-blocking queue offer on every cache hit / provider fetch.
    public static final boolean LOG_CACHE_HITS = SECURITY_PROPERTIES != null
            && "true".equalsIgnoreCase(
                    SECURITY_PROPERTIES.getProperty("secret.audit.log.cache.hits", "false").trim());
    public static final boolean LOG_FETCH_EVENTS = SECURITY_PROPERTIES != null
            && "true".equalsIgnoreCase(
                    SECURITY_PROPERTIES.getProperty("secret.audit.log.fetch.events", "false").trim());

    /** Set once at startup by webtools SecretAuditContainer; null until then — all audit calls no-op. */
    private static volatile SecretAuditSink auditSink;

    private static final ConcurrentHashMap<String, CacheEntry> CACHE = new ConcurrentHashMap<>();

    // Per-key locks used for stampede protection: only one thread fetches from the provider
    // when a cache entry expires; others wait and then read the freshly cached value.
    private static final ConcurrentHashMap<String, Object> KEY_LOCKS = new ConcurrentHashMap<>();

    // Usage counters — incremented every time resolveKey() is called.
    private static final AtomicLong TOTAL_HITS = new AtomicLong();
    private static final AtomicLong TOTAL_MISSES = new AtomicLong();
    private static final ConcurrentHashMap<String, AtomicLong> KEY_HIT_COUNTS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AtomicLong> KEY_MISS_COUNTS = new ConcurrentHashMap<>();
    // Last-access timestamp (millis since epoch) per key — updated on every hit or miss.
    private static final ConcurrentHashMap<String, AtomicLong> KEY_LAST_ACCESS_TIMES = new ConcurrentHashMap<>();

    // Daemon thread that sweeps expired entries so stale keys don't accumulate indefinitely.
    private static final ScheduledExecutorService EVICTION_EXECUTOR;
    static {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SecretValueResolver-CacheEviction");
            t.setDaemon(true);
            return t;
        });
        executor.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            CACHE.entrySet().removeIf(e -> e.getValue().expiry <= now);
            // Remove per-key locks for keys no longer in the cache to prevent unbounded growth.
            KEY_LOCKS.keySet().removeIf(k -> !CACHE.containsKey(k));
        }, CACHE_TTL_MILLIS, CACHE_TTL_MILLIS, TimeUnit.MILLISECONDS);
        EVICTION_EXECUTOR = executor;
    }

    private SecretValueResolver() { }

    /**
     * Registers the audit sink that receives CACHE_HIT, FETCH, and ROTATION_POLL events.
     * Called once at OFBiz startup from {@code SecretAuditContainer} in webtools after the
     * entity layer is available. Pass {@code null} to deregister (e.g. during shutdown).
     */
    public static void setAuditSink(SecretAuditSink sink) {
        auditSink = sink;
    }

    /** Returns the currently registered audit sink, or {@code null} if none is set. */
    public static SecretAuditSink getAuditSink() {
        return auditSink;
    }

    /**
     * If {@code rawValue} matches {@code LOOKUP(key)}, resolves {@code key} via
     * {@link SecretProviderFactory#getInstance()}, caching the result for
     * {@code secret.cache.ttl.seconds}. Otherwise returns {@code rawValue} unchanged.
     *
     * @param rawValue the raw property value, possibly {@code null}
     * @return the resolved secret value, or {@code rawValue} unchanged if it is not a
     *         {@code LOOKUP(key)} reference; an empty string if resolution fails
     */
    public static String resolve(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        Matcher matcher = SECRET_PATTERN.matcher(rawValue.trim());
        if (!matcher.matches()) {
            return rawValue;
        }
        return resolveKey(matcher.group(1).trim());
    }

    /**
     * Resolves {@code key} directly via {@link SecretProviderFactory}, with the same TTL-based
     * caching as {@link #resolve(String)}. Use this when the caller already holds the raw key
     * (e.g. from {@code SystemProperty.systemPropertyLookup}) without a {@code LOOKUP(...)} wrapper.
     *
     * @param key the raw secret key, possibly {@code null} or empty
     * @return the resolved secret value; an empty string if resolution fails; {@code null} if {@code key} is {@code null}
     */
    public static String resolveKey(String key) {
        if (key == null) {
            return null;
        }
        if (key.isEmpty()) {
            return "";
        }
        CacheEntry cached = CACHE.get(key);
        if (cached != null && cached.expiry > System.currentTimeMillis()) {
            TOTAL_HITS.incrementAndGet();
            KEY_HIT_COUNTS.computeIfAbsent(key, k -> new AtomicLong()).incrementAndGet();
            KEY_LAST_ACCESS_TIMES.computeIfAbsent(key, k -> new AtomicLong()).set(System.currentTimeMillis());
            if (LOG_CACHE_HITS) {
                SecretAuditSink s = auditSink;
                if (s != null) {
                    s.onCacheHit(key);
                }
            }
            return cached.value;
        }
        // Per-key lock: only the first thread that finds a stale/absent entry fetches from the
        // provider. All other threads for the same key wait, then read the freshly cached value.
        Object lock = KEY_LOCKS.computeIfAbsent(key, k -> new Object());
        synchronized (lock) {
            // Double-check: another thread may have populated the cache while we waited.
            CacheEntry rechecked = CACHE.get(key);
            if (rechecked != null && rechecked.expiry > System.currentTimeMillis()) {
                TOTAL_HITS.incrementAndGet();
                KEY_HIT_COUNTS.computeIfAbsent(key, k -> new AtomicLong()).incrementAndGet();
                KEY_LAST_ACCESS_TIMES.computeIfAbsent(key, k -> new AtomicLong()).set(System.currentTimeMillis());
                if (LOG_CACHE_HITS) {
                    SecretAuditSink s = auditSink;
                    if (s != null) {
                        s.onCacheHit(key);
                    }
                }
                return rechecked.value;
            }
            try {
                String secret = SecretProviderFactory.getInstance().getSecret(key);
                CACHE.put(key, new CacheEntry(secret, System.currentTimeMillis() + CACHE_TTL_MILLIS));
                TOTAL_MISSES.incrementAndGet();
                KEY_MISS_COUNTS.computeIfAbsent(key, k -> new AtomicLong()).incrementAndGet();
                KEY_LAST_ACCESS_TIMES.computeIfAbsent(key, k -> new AtomicLong()).set(System.currentTimeMillis());
                if (LOG_FETCH_EVENTS) {
                    SecretAuditSink s = auditSink;
                    if (s != null) {
                        s.onFetch(key, "PROVIDER_CALL", "SUCCESS", null);
                    }
                }
                return secret;
            } catch (GeneralException e) {
                // Log only the exception type — never e.getMessage() — to prevent a provider that
                // accidentally embeds a secret value in its error message from leaking it to the log.
                Debug.logError("SecretValueResolver: failed to resolve secret '" + key
                        + "' (" + e.getClass().getSimpleName() + ")", MODULE);
                TOTAL_MISSES.incrementAndGet();
                KEY_MISS_COUNTS.computeIfAbsent(key, k -> new AtomicLong()).incrementAndGet();
                KEY_LAST_ACCESS_TIMES.computeIfAbsent(key, k -> new AtomicLong()).set(System.currentTimeMillis());
                if (LOG_FETCH_EVENTS) {
                    SecretAuditSink s = auditSink;
                    if (s != null) {
                        s.onFetch(key, "PROVIDER_CALL", "FAILURE", "PROVIDER_ERROR");
                    }
                }
                return "";
            }
        }
    }

    /**
     * Redacts sensitive patterns from a string before it is used in a log message or UI output.
     * Currently masks {@code ENC(...)} blobs (replacing the base64 payload with {@code ***}) so
     * that encrypted config values logged by accident are not directly usable.
     *
     * @param value the string to sanitize, possibly {@code null}
     * @return the sanitized string, or {@code null} if the input is {@code null}
     */
    public static String maskSensitive(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("ENC\\([^)]*\\)", "ENC(***)");
    }

    /**
     * Removes the cached value for {@code key}, forcing the next {@link #resolve(String)} or
     * {@link #resolveKey(String)} call to re-fetch from the provider. Call this after writing a
     * new encrypted value for {@code key} so the update is visible immediately without waiting for
     * the TTL to expire.
     *
     * @param key the raw secret key (without any {@code LOOKUP(...)} wrapper), or {@code null} to no-op
     */
    public static void invalidate(String key) {
        if (key != null) {
            CACHE.remove(key);
            KEY_LOCKS.remove(key);
        }
    }

    /**
     * Clears all cached secret values, forcing every subsequent lookup to re-fetch from the
     * provider. Useful after a bulk secret rotation or when an admin explicitly flushes the cache
     * via the Secret Manager admin screen.
     */
    public static void invalidateAll() {
        CACHE.clear();
        KEY_LOCKS.clear();
        Debug.logInfo("SecretValueResolver: secret value cache flushed", MODULE);
    }

    /**
     * Resets all in-memory usage counters to zero. Useful when an operator wants to observe
     * clean post-rotation metrics without waiting for a JVM restart.
     */
    public static void resetUsageStats() {
        TOTAL_HITS.set(0);
        TOTAL_MISSES.set(0);
        KEY_HIT_COUNTS.clear();
        KEY_MISS_COUNTS.clear();
        KEY_LAST_ACCESS_TIMES.clear();
        Debug.logInfo("SecretValueResolver: usage stats reset", MODULE);
    }

    /**
     * Returns a snapshot of usage statistics for the secret value cache. Each entry in the
     * returned map represents one unique key that has been looked up since the last JVM start.
     * The map is sorted by total lookup count (descending) for easy review in the admin UI.
     *
     * <p>Map structure: {@code key → {"hits": n, "misses": m, "total": n+m, "lastAccessedMs": t}}</p>
     */
    public static Map<String, Map<String, Long>> getUsageReport() {
        // Collect all known keys from both hit and miss maps.
        Set<String> keys = new LinkedHashSet<>(KEY_HIT_COUNTS.keySet());
        keys.addAll(KEY_MISS_COUNTS.keySet());

        Map<String, Map<String, Long>> report = new LinkedHashMap<>();
        keys.stream()
                .sorted((a, b) -> {
                    long totalA = getCount(KEY_HIT_COUNTS, a) + getCount(KEY_MISS_COUNTS, a);
                    long totalB = getCount(KEY_HIT_COUNTS, b) + getCount(KEY_MISS_COUNTS, b);
                    return Long.compare(totalB, totalA); // descending
                })
                .forEach(key -> {
                    long hits = getCount(KEY_HIT_COUNTS, key);
                    long misses = getCount(KEY_MISS_COUNTS, key);
                    Map<String, Long> stats = new LinkedHashMap<>();
                    stats.put("hits", hits);
                    stats.put("misses", misses);
                    stats.put("total", hits + misses);
                    stats.put("lastAccessedMs", getLastAccess(key));
                    report.put(key, stats);
                });
        return report;
    }

    /** Returns the aggregate hit and miss totals since JVM start as a simple summary map. */
    public static Map<String, Long> getUsageSummary() {
        // Capture atomics once to avoid a TOCTOU race where a concurrent hit/miss
        // arrives between two get() calls and makes totalLookups != totalHits + totalMisses.
        long hits = TOTAL_HITS.get();
        long misses = TOTAL_MISSES.get();
        Map<String, Long> summary = new LinkedHashMap<>();
        summary.put("totalHits", hits);
        summary.put("totalMisses", misses);
        summary.put("totalLookups", hits + misses);
        summary.put("cachedKeys", (long) CACHE.size());
        return summary;
    }

    private static long getCount(ConcurrentHashMap<String, AtomicLong> map, String key) {
        AtomicLong counter = map.get(key);
        return counter == null ? 0L : counter.get();
    }

    private static long getLastAccess(String key) {
        AtomicLong ts = KEY_LAST_ACCESS_TIMES.get(key);
        return ts == null ? 0L : ts.get();
    }

    private static final class CacheEntry {
        private final String value;
        private final long expiry;

        private CacheEntry(String value, long expiry) {
            this.value = value;
            this.expiry = expiry;
        }
    }
}
