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

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;

/**
 * {@link SecretProvider} decorator that falls back to a secondary provider
 * when the primary provider fails to resolve a secret.
 *
 * <p>This is used by {@link SecretProviderFactory} to wrap a custom (remote)
 * {@link SecretProvider} implementation together with a
 * {@link FileBasedSecretProvider}. If the primary provider throws a
 * {@link GeneralException} (e.g. the remote secret manager is unreachable or
 * the secret does not exist there) and {@link SecretProvider#isFallbackEnabled()}
 * returns {@code true} for the primary provider, the secret is resolved from
 * the fallback provider instead.</p>
 *
 * <p>If the primary provider fails and fallback is disabled, or the fallback
 * provider also fails, the original exception from the primary provider is
 * propagated to the caller.</p>
 */
public final class FallbackSecretProvider implements SecretProvider {

    private static final String MODULE = FallbackSecretProvider.class.getName();

    private final SecretProvider primary;
    private final SecretProvider fallback;

    /**
     * @param primary the primary (typically remote) provider to try first
     * @param fallback the provider to use if the primary fails and allows fallback
     */
    public FallbackSecretProvider(SecretProvider primary, SecretProvider fallback) {
        this.primary = primary;
        this.fallback = fallback;
    }

    @Override
    public String getSecret(String key) throws GeneralException {
        try {
            return primary.getSecret(key);
        } catch (GeneralException e) {
            if (!primary.isFallbackEnabled()) {
                throw e;
            }
            Debug.logWarning("SecretProvider: " + primary.getClass().getName()
                    + " failed to resolve secret '" + key + "' (" + e.getMessage()
                    + "), falling back to " + describe(fallback), MODULE);
            return fallback.getSecret(key);
        }
    }

    /**
     * Returns a human-readable description of the given provider for log
     * messages, naming the backing {@code passwords.properties} file for
     * {@link FileBasedSecretProvider}.
     */
    private static String describe(SecretProvider provider) {
        if (provider instanceof FileBasedSecretProvider) {
            return provider.getClass().getName() + " (framework/base/config/passwords.properties)";
        }
        return provider.getClass().getName();
    }
}
