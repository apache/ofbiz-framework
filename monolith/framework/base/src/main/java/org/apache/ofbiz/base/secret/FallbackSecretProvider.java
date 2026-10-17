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

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilProperties;

/**
 * {@link SecretProvider} decorator that retries the primary provider with exponential backoff
 * before falling back to a secondary provider when the primary fails to resolve a secret.
 *
 * <p>This is used by {@link SecretProviderFactory} to wrap a custom (remote)
 * {@link SecretProvider} implementation together with a {@link FileBasedSecretProvider}.
 * On a transient failure the primary is retried up to {@code secret.provider.retry.count}
 * times (default 2) with an initial delay of {@code secret.provider.retry.delay.ms}
 * (default 500 ms) doubling on each attempt before the fallback is triggered.</p>
 *
 * <p>If the primary provider fails and fallback is disabled, or the fallback provider also
 * fails, the original exception from the primary provider is propagated to the caller.</p>
 */
public final class FallbackSecretProvider implements SecretProvider {

    private static final String MODULE = FallbackSecretProvider.class.getName();

    private static final Properties SECURITY_PROPERTIES = UtilProperties.getProperties("security");

    private static final int RETRY_COUNT = readInt("secret.provider.retry.count", 2);
    private static final long RETRY_DELAY_MS = readLong("secret.provider.retry.delay.ms", 500L);

    private final SecretProvider primary;
    private final SecretProvider fallback;

    /**
     * @param primary the primary (typically remote) provider to try first
     * @param fallback the provider to use if the primary fails after all retries and allows fallback
     */
    public FallbackSecretProvider(SecretProvider primary, SecretProvider fallback) {
        this.primary = primary;
        this.fallback = fallback;
    }

    @Override
    public String getSecret(String key) throws GeneralException {
        GeneralException lastException = null;
        long delayMs = RETRY_DELAY_MS;

        for (int attempt = 0; attempt <= RETRY_COUNT; attempt++) {
            try {
                return primary.getSecret(key);
            } catch (GeneralException e) {
                lastException = e;
                if (!primary.isFallbackEnabled()) {
                    throw e;
                }
                if (attempt < RETRY_COUNT) {
                    Debug.logWarning("SecretProvider: " + primary.getClass().getName()
                            + " failed to resolve secret '" + key + "' (" + e.getClass().getSimpleName()
                            + "), retrying in " + delayMs + " ms (attempt " + (attempt + 1) + "/" + RETRY_COUNT + ")",
                            MODULE);
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    delayMs *= 2; // exponential backoff
                }
            }
        }

        // All retries exhausted — fall back to the secondary provider.
        Debug.logWarning("SecretProvider: " + primary.getClass().getName()
                + " failed after " + (RETRY_COUNT + 1) + " attempt(s) for secret '" + key
                + "' (" + (lastException != null ? lastException.getClass().getSimpleName() : "unknown")
                + "), falling back to " + describe(fallback), MODULE);
        return fallback.getSecret(key);
    }

    /** Closes both the primary and fallback providers. */
    @Override
    public void close() {
        try {
            primary.close();
        } finally {
            fallback.close();
        }
    }

    /** Clears the in-memory cache of both the primary and fallback providers. */
    @Override
    public void invalidateCache() {
        try {
            primary.invalidateCache();
        } finally {
            fallback.invalidateCache();
        }
    }

    private static int readInt(String key, int defaultValue) {
        if (SECURITY_PROPERTIES == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(SECURITY_PROPERTIES.getProperty(key, String.valueOf(defaultValue)).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static long readLong(String key, long defaultValue) {
        if (SECURITY_PROPERTIES == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(SECURITY_PROPERTIES.getProperty(key, String.valueOf(defaultValue)).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Returns a human-readable description of the given provider for log messages.
     */
    private static String describe(SecretProvider provider) {
        if (provider instanceof FileBasedSecretProvider) {
            return provider.getClass().getName() + " (framework/base/config/passwords.properties)";
        }
        return provider.getClass().getName();
    }
}
