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

import java.util.Iterator;
import java.util.ServiceLoader;

import org.apache.ofbiz.base.lang.ThreadSafe;
import org.apache.ofbiz.base.util.Debug;

/**
 * Factory that provides the active {@link SecretProvider} instance.
 *
 * <p>On first access the factory uses Java's {@link ServiceLoader} to discover
 * a custom {@link SecretProvider} registered in any plugin's
 * {@code META-INF/services/org.apache.ofbiz.base.secret.SecretProvider} file.
 * If none is found, {@link FileBasedSecretProvider} is used automatically,
 * preserving backward compatibility with {@code passwords.properties}.</p>
 *
 * <p>If a custom (remote) provider is found, it is wrapped in a
 * {@link FallbackSecretProvider} together with {@link FileBasedSecretProvider}.
 * When the remote provider's own {@code <prefix>.fallback.enabled} configuration
 * property is {@code true} (the default) and the remote provider fails (e.g. the
 * remote secret manager is unreachable), the secret is resolved from the local
 * {@code passwords.properties} file instead.</p>
 *
 * <p>This mirrors the pattern already used by
 * {@link org.apache.ofbiz.security.SecurityFactory}.</p>
 */
@ThreadSafe
public final class SecretProviderFactory {

    private static final String MODULE = SecretProviderFactory.class.getName();

    private static final SecretProvider INSTANCE = loadProvider();

    private static SecretProvider loadProvider() {
        Iterator<SecretProvider> it = ServiceLoader.load(SecretProvider.class).iterator();
        if (it.hasNext()) {
            SecretProvider provider = it.next();
            Debug.logInfo("SecretProvider: using custom implementation " + provider.getClass().getName(), MODULE);
            return new FallbackSecretProvider(provider, new FileBasedSecretProvider());
        }
        Debug.logInfo("SecretProvider: no custom implementation found, using FileBasedSecretProvider", MODULE);
        return new FileBasedSecretProvider();
    }

    /** Returns the active {@link SecretProvider} instance. */
    public static SecretProvider getInstance() {
        return INSTANCE;
    }

    private SecretProviderFactory() { }
}
