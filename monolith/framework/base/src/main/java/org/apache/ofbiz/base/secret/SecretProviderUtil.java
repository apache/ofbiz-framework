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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ofbiz.base.lang.ThreadSafe;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilProperties;

/**
 * Shared helpers used by every bundled {@link SecretProvider} plugin: an in-memory TTL
 * cache for resolved secret values, a reader for that cache's TTL property, and the
 * {@code key.alias.*} per-key naming-override convention.
 */
public final class SecretProviderUtil {

    private static final String KEY_ALIAS_PREFIX = "key.alias.";

    private SecretProviderUtil() { }

    /**
     * Simple in-memory TTL cache, used by every bundled provider to avoid a remote API call
     * on every {@link SecretProvider#getSecret(String)} invocation.
     *
     * <p>Each entry expires independently, based on the TTL passed to {@link #put}; there is
     * no background eviction thread — expiry is checked lazily on {@link #get}.</p>
     *
     * @param <K> the cache key type (every bundled provider uses {@code String}, the OFBiz
     *            logical secret key)
     * @param <V> the cached value type (every bundled provider uses {@code String}, the
     *            resolved secret value)
     */
    @ThreadSafe
    public static final class Cache<K, V> {

        private final ConcurrentHashMap<K, Entry<V>> entries = new ConcurrentHashMap<>();

        private static final class Entry<V> {
            private final V value;
            private final long expiresAt;

            Entry(V value, long ttlMs) {
                this.value = value;
                this.expiresAt = System.currentTimeMillis() + ttlMs;
            }

            boolean isExpired() {
                return System.currentTimeMillis() >= expiresAt;
            }
        }

        /**
         * Returns the cached value for {@code key}, or {@code null} if absent or expired.
         * An expired entry is treated the same as a missing one; it is overwritten on the
         * next {@link #put}, not proactively removed here.
         */
        public V get(K key) {
            Entry<V> entry = entries.get(key);
            return (entry != null && !entry.isExpired()) ? entry.value : null;
        }

        /** Stores {@code value} under {@code key}, expiring {@code ttlMs} milliseconds from now. */
        public void put(K key, V value, long ttlMs) {
            entries.put(key, new Entry<>(value, ttlMs));
        }

        /** Removes every cached entry, forcing the next {@link #get} for any key to miss. */
        public void clear() {
            entries.clear();
        }
    }

    /**
     * Reads {@code propertyKey} from {@code configResource} (default {@code defaultSeconds}),
     * returning the value in milliseconds. Falls back to {@code defaultSeconds} (and logs a
     * warning attributed to {@code moduleName}) if the configured value isn't a valid long.
     *
     * @param configResource the provider plugin's own config resource name (e.g.
     *                        {@code "aws-secrets-manager"})
     * @param propertyKey the TTL property name (e.g. {@code "aws.secretsmanager.cache.ttl.seconds"})
     * @param defaultSeconds the default TTL in seconds, used both as the property default and
     *                        as the fallback on a parse failure
     * @param moduleName the calling provider's {@code MODULE} constant, so the warning log is
     *                    attributed to the right class
     * @return the TTL in milliseconds
     */
    public static long readTtlMs(String configResource, String propertyKey, long defaultSeconds, String moduleName) {
        String raw = UtilProperties.getPropertyValue(configResource, propertyKey, String.valueOf(defaultSeconds));
        try {
            return Long.parseLong(raw.trim()) * 1000L;
        } catch (NumberFormatException e) {
            Debug.logWarning("Invalid " + propertyKey + " '" + raw + "', defaulting to " + defaultSeconds + "s",
                    moduleName);
            return defaultSeconds * 1000L;
        }
    }

    /**
     * Reads every {@code key.alias.<logicalKey>=<physicalKey>} entry from {@code configResource}
     * and returns them as {@code logicalKey -> physicalKey}.
     *
     * <p>Each provider stores its secrets under a physical name derived from the OFBiz logical
     * key — verbatim, or via a provider-specific convention (e.g. dot-replacement, a fixed
     * env-var transform). When a provider's backend can't accept that physical name for a
     * specific key (naming restrictions on {@code .}/{@code -}, length, case, a collision,
     * etc.), an explicit {@code key.alias.<logicalKey>=<physicalKey>} entry in that provider's
     * own config resource overrides just that one key. Keys with no alias entry are
     * unaffected — this is purely additive, never required.</p>
     *
     * <p>See "Key aliasing for provider naming restrictions" in
     * {@code generic-secret-resolution-design.md} §6 for the full design rationale.</p>
     *
     * @param configResource the provider plugin's own config resource name (e.g.
     *                        {@code "aws-secrets-manager"}), as passed to
     *                        {@link UtilProperties#getProperties(String)}
     * @return an immutable-content map of logical key to physical key; empty if the
     *         resource is missing or has no {@code key.alias.*} entries
     */
    public static Map<String, String> loadKeyAliases(String configResource) {
        Properties props = UtilProperties.getProperties(configResource);
        if (props == null) {
            return Map.of();
        }
        Map<String, String> aliases = new HashMap<>();
        for (String name : props.stringPropertyNames()) {
            if (name.startsWith(KEY_ALIAS_PREFIX)) {
                aliases.put(name.substring(KEY_ALIAS_PREFIX.length()), props.getProperty(name));
            }
        }
        return aliases;
    }
}
