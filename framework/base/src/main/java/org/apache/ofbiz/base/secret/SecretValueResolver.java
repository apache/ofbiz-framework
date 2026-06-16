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

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final long CACHE_TTL_MILLIS = (SECURITY_PROPERTIES != null
            ? Long.parseLong(SECURITY_PROPERTIES.getProperty("secret.cache.ttl.seconds", "300").trim())
            : 300L) * 1000L;

    private static final ConcurrentHashMap<String, CacheEntry> CACHE = new ConcurrentHashMap<>();

    private SecretValueResolver() { }

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
            return cached.value;
        }
        try {
            String secret = SecretProviderFactory.getInstance().getSecret(key);
            CACHE.put(key, new CacheEntry(secret, System.currentTimeMillis() + CACHE_TTL_MILLIS));
            return secret;
        } catch (GeneralException e) {
            Debug.logError("SecretValueResolver: failed to resolve secret '" + key + "': " + e.getMessage(), MODULE);
            return "";
        }
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
