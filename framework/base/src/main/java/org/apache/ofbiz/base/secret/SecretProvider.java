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
 * the {@code OFBIZ_DB_KEY} environment variable at runtime and is never stored
 * in config files or in the remote vault.</p>
 */
public interface SecretProvider {

    /**
     * Returns the secret value for the given key.
     *
     * @param key the identifier for the secret (e.g. {@code "jdbc-password.mydb"})
     * @return the resolved secret value, never {@code null} or empty
     * @throws GeneralException if the secret cannot be found or an error occurs during resolution
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
}
