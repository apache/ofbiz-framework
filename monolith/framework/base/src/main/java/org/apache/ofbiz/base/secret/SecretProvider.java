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

import org.apache.ofbiz.base.util.GeneralException;

/**
 * SPI for resolving secrets and credentials (e.g. database passwords, API keys).
 *
 * <p>Implementations must be thread-safe. Register a custom implementation via
 * Java's {@link java.util.ServiceLoader} by providing a file named
 * {@code META-INF/services/org.apache.ofbiz.base.secret.SecretProvider} in
 * your plugin JAR containing the fully-qualified class name of your
 * implementation. If no custom implementation is registered,
 * {@link FileBasedSecretProvider} is used as the default, which resolves
 * secrets from {@code framework/base/config/passwords.properties}.</p>
 *
 * <p>Example entry in a vault plugin's service descriptor:</p>
 * <pre>
 *   org.example.ofbiz.vault.AwsSecretsManagerProvider
 * </pre>
 *
 * <h2>Optional client-side encryption ({@code ENC(...)})</h2>
 * <p>Any provider implementation may store secret values wrapped in
 * {@code ENC(<base64>)} to add a client-side AES-256-GCM encryption layer on
 * top of whatever the remote vault already provides. Call
 * {@code ConfigCryptoUtil.decryptIfEncrypted()} on the raw value returned by
 * the remote API before caching or returning it. The master key is read from
 * the {@code OFBIZ_MASTER_KEY} environment variable at runtime and is never stored
 * in config files or in the remote vault.</p>
 */
public interface SecretProvider {

    /**
     * Returns the secret value for the given key.
     *
     * <p><strong>Contract when the key does not exist:</strong> implementations must throw
     * {@link GeneralException} rather than returning {@code null} or an empty string.
     * {@link FallbackSecretProvider} and {@link SecretValueResolver} rely on the exception to
     * distinguish "key not present" from a successful empty-value lookup. Exception messages
     * should contain text such as {@code "not found"}, {@code "NotFound"}, or
     * {@code "does not exist"} so that callers (e.g. {@link
     * org.apache.ofbiz.webtools.secret.SecretManagerServices#testSecretProviderConnection})
     * can tell the difference between a missing key and a connectivity failure.</p>
     *
     * @param key the identifier for the secret (e.g. {@code "jdbc-password.mydb"})
     * @return the resolved secret value; never {@code null} or empty
     * @throws GeneralException if the secret cannot be found or an error occurs during resolution;
     *                          for a missing key the message should include "not found", "NotFound",
     *                          or "does not exist"
     */
    String getSecret(String key) throws GeneralException;

    /**
     * Returns whether {@link FallbackSecretProvider} is allowed to fall back to
     * {@link FileBasedSecretProvider} (i.e. {@code passwords.properties}) when this
     * provider fails to resolve a secret.
     *
     * <p>Implementations should read this from their own plugin configuration
     * resource (e.g. {@code <fallback-property>.fallback.enabled}), defaulting
     * to {@code true}.</p>
     *
     * @return {@code true} if local fallback is permitted, {@code false} to require
     *         this provider to be the sole source of secrets
     */
    default boolean isFallbackEnabled() {
        return true;
    }

    /**
     * Releases any resources held by this provider (e.g. HTTP connection pools, SDK clients).
     * Called automatically by {@link SecretProviderFactory} via a JVM shutdown hook.
     *
     * <p>Implementations that hold long-lived SDK clients (e.g. AWS {@code SecretsManagerClient},
     * GCP {@code SecretManagerServiceClient}) should override this method to close them cleanly.</p>
     *
     * <p>The default implementation is a no-op, so providers with no closeable resources do not
     * need to override it.</p>
     */
    default void close() { }

    /**
     * Clears any secret values this provider has cached in memory, forcing the next
     * {@link #getSecret(String)} call for each key to re-fetch from the remote vault.
     *
     * <p>Implementations that cache resolved values (all of the bundled vault providers do, with a
     * TTL configured via their own {@code <prefix>.cache.ttl.seconds} property) should override this
     * method. It is called by {@link SecretProviderFactory#invalidateCache()}, which is in turn
     * invoked by the Secret Manager admin screen's "Flush Secret Cache" action after a secret has
     * been rotated in the remote vault, so the new value is picked up immediately without waiting
     * for the provider's own TTL to expire.</p>
     *
     * <p>The default implementation is a no-op, so providers with no internal cache do not need to
     * override it.</p>
     */
    default void invalidateCache() { }
}
